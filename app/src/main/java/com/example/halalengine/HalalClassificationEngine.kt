package com.example.halalengine

import com.example.domain.AnalyzedIngredient
import com.example.domain.AssessmentPillar
import com.example.domain.CertificationInfo
import com.example.domain.EvidenceQuality
import com.example.domain.HalalAssessment
import com.example.domain.HalalVerdict
import com.example.domain.IngredientCategory
import com.example.domain.TaxonomyEntry

data class ClassificationInput(
    val ingredientsText: String? = null,
    val ingredientsTextEn: String? = null,
    val ingredientsTextFr: String? = null,
    val ingredientsTags: List<String> = emptyList(),
    val additivesTags: List<String> = emptyList(),
    val labelsTags: List<String> = emptyList(),
    val ingredientsAnalysisTags: List<String> = emptyList(),
    val upstreamHalalStatus: String? = null,
    val upstreamIsHalal: Boolean? = null,
    val lastModifiedTimestamp: Long? = null,
    val isFromCache: Boolean = false
)

class HalalClassificationEngine(
    private val taxonomy: LoadedTaxonomy
) {
    private data class CompiledRule(
        val entry: TaxonomyEntry,
        val normalizedPatterns: List<Pair<String, Regex>>
    )

    private val compiledRules: List<CompiledRule> = taxonomy.entries.map { entry ->
        val rawTerms = buildSet {
            addAll(entry.alternativeSpellings)
            entry.eNumber?.let { add(it) }
        }
        val patterns = rawTerms
            .map { TextNormalizer.normalize(it) }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
            .map { term ->
                term to Regex("(?<![a-z0-9])${Regex.escape(term)}(?![a-z0-9])")
            }
        CompiledRule(entry, patterns)
    }

    // Phrases that represent safe/permissible culinary exceptions rather than prohibited triggers
    private val safeVinegarPhrases = listOf(
        "wine vinegar", "vinaigre de vin", "red wine vinegar", "white wine vinegar",
        "cider vinegar", "vinaigre de cidre", "spirit vinegar", "vinaigre d'alcool",
        "vinaigre d alcool", "alcohol vinegar", "balsamic vinegar", "vinaigre balsamique",
        "malt vinegar", "vinaigre de malt"
    )

    private val safeNegationPhrases = listOf(
        "pork free", "pork-free", "sans porc", "no pork", "free from pork",
        "alcohol free", "alcohol-free", "sans alcool", "no alcohol", "0 0% alcohol", "0% alcohol",
        "0 0% alcool", "0% alcool", "non alcoholic", "non-alcoholic"
    )

    private val safeHalalGelatinPhrases = listOf(
        "fish gelatin", "fish gelatine", "gelatine de poisson", "marine gelatin",
        "halal gelatin", "halal gelatine", "halal beef gelatin", "gelatine bovine halal",
        "gelatine halal", "plant gelatin", "gelatine vegetale"
    )

    private val plantEmulsifierHints = listOf(
        "plant based", "plant origin", "vegetable origin", "origine vegetale",
        "d'origine vegetale", "d origine vegetale", "vegetable source", "from vegetable oil"
    )

    private val recognizedCertifiers = listOf(
        "jakim" to "JAKIM (Malaysia)",
        "bpjph" to "BPJPH (Indonesia)",
        "mui" to "MUI (Indonesia)",
        "ifanca" to "IFANCA (North America)",
        "avs" to "AVS (France)",
        "achahada" to "Achahada (France)",
        "hmc" to "HMC (UK)",
        "hfa" to "HFA (UK)",
        "halal control" to "Halal Control (EU)",
        "sfda" to "SFDA (Saudi Arabia)",
        "esma" to "ESMA (UAE)"
    )

    fun analyzeProduct(input: ClassificationInput): HalalAssessment {
        val combinedRawIngredients = listOfNotNull(
            input.ingredientsText,
            input.ingredientsTextEn,
            input.ingredientsTextFr
        ).filter { it.isNotBlank() }.joinToString(" , ")

        val normalizedAdditives = input.additivesTags.map { TextNormalizer.normalizeTag(it) }
        val normalizedIngredientTags = input.ingredientsTags.map { TextNormalizer.normalizeTag(it) }
        val normalizedLabels = input.labelsTags.map { TextNormalizer.normalizeTag(it) }
        val normalizedAnalysis = input.ingredientsAnalysisTags.map { TextNormalizer.normalizeTag(it) }

        val combinedSearchCorpus = buildString {
            append(TextNormalizer.normalize(combinedRawIngredients))
            if (normalizedAdditives.isNotEmpty()) {
                append(" ")
                append(normalizedAdditives.joinToString(" "))
            }
            if (normalizedIngredientTags.isNotEmpty()) {
                append(" ")
                append(normalizedIngredientTags.joinToString(" "))
            }
        }.trim()

        val hasIngredientsData = combinedRawIngredients.isNotBlank() ||
            normalizedAdditives.isNotEmpty() ||
            normalizedIngredientTags.isNotEmpty()

        val matched = matchIngredients(combinedSearchCorpus)

        // Check certification / label declarations
        val certInfo = evaluateCertification(normalizedLabels, normalizedAnalysis)

        // Check vegan / vegetarian declarations (NOTE: Vegan/Vegetarian != Certified Halal!)
        val isDeclaredVegan = normalizedAnalysis.any { it == "vegan" } ||
            normalizedLabels.any { it.contains("vegan") || it.contains("vegetalien") }
        val isDeclaredVegetarian = normalizedAnalysis.any { it == "vegetarian" } ||
            normalizedLabels.any { it.contains("vegetarian") || it.contains("vegetarien") }
        val hasPlantOriginHint = plantEmulsifierHints.any { combinedSearchCorpus.contains(it) }

        // Pillar 1: Community / Upstream Halal Open Food Facts assessment
        val communityPillar = evaluateCommunityPillar(input.upstreamHalalStatus, input.upstreamIsHalal)

        // Pillar 2: Local Ingredient & E-number Analysis
        val prohibitedMatches = matched.filter { it.entry.category == IngredientCategory.CLEARLY_PROHIBITED }
        val sourceDependentMatches = matched.filter {
            it.entry.category == IngredientCategory.SOURCE_DEPENDENT ||
                it.entry.category == IngredientCategory.REQUIRES_VERIFICATION
        }

        // If declared vegan AND only fatty-acid emulsifiers (E471/E472/E481/E482/E476/E422) appeared without alcohol/carmine/gelatin,
        // we still transparently note the emulsifier, while recognizing plant-origin context when vegan/plant-based is explicitly stated.
        val unresolvedDoubtfulMatches = sourceDependentMatches.filter { item ->
            val id = item.entry.id
            val isFattyAcidEmulsifier = id in setOf("e471", "e472", "e481", "e482", "e476", "e422", "e432_e436", "e470a", "e473", "e475", "e491", "e570")
            val mitigatedByPlantClaim = isFattyAcidEmulsifier && (hasPlantOriginHint || (isDeclaredVegan && certInfo.hasHalalLabelClaim))
            !mitigatedByPlantClaim
        }

        val ingredientPillar = evaluateIngredientPillar(
            hasIngredientsData = hasIngredientsData,
            prohibited = prohibitedMatches,
            doubtful = unresolvedDoubtfulMatches,
            isDeclaredVegan = isDeclaredVegan,
            isDeclaredVegetarian = isDeclaredVegetarian
        )

        // Pillar 3: Certification info
        val certificationPillar = AssessmentPillar(
            summaryEn = certInfo.verificationNoteEn,
            summaryFr = certInfo.verificationNoteFr,
            verdictHint = if (certInfo.hasHalalLabelClaim) HalalVerdict.HALAL else HalalVerdict.UNKNOWN
        )

        // Conflict detection across community status, local ingredients, and packaging labels
        var hasConflict = false
        var conflictEn: String? = null
        var conflictFr: String? = null

        if (certInfo.hasHalalLabelClaim && prohibitedMatches.isNotEmpty()) {
            hasConflict = true
            val names = prohibitedMatches.joinToString(", ") { it.entry.nameEn }
            conflictEn = "Conflicting evidence: packaging labels mention Halal, yet the declared ingredients contain $names. Verify the physical packaging carefully."
            conflictFr = "Données contradictoires : l'emballage mentionne Halal, mais les ingrédients déclarés contiennent ${prohibitedMatches.joinToString(", ") { it.entry.nameFr }}. Vérifiez attentivement l'étiquette."
        } else if (communityPillar.verdictHint == HalalVerdict.HALAL && prohibitedMatches.isNotEmpty()) {
            hasConflict = true
            conflictEn = "Conflicting evidence: community status indicates Halal, while local ingredient rules detected clearly prohibited terms (${prohibitedMatches.joinToString(", ") { it.entry.nameEn }})."
            conflictFr = "Données contradictoires : le statut communautaire indique Halal, alors que l'analyse locale a détecté des ingrédients interdits (${prohibitedMatches.joinToString(", ") { it.entry.nameFr }})."
        } else if (communityPillar.verdictHint == HalalVerdict.HARAM && hasIngredientsData && prohibitedMatches.isEmpty() && unresolvedDoubtfulMatches.isEmpty()) {
            hasConflict = true
            conflictEn = "Conflicting evidence: community database marks this product Haram, though the listed ingredients alone did not trigger a local prohibited rule. Inspect full packaging."
            conflictFr = "Données contradictoires : la base communautaire classe ce produit Haram, bien que la liste d'ingrédients analysée localement ne montre pas d'ingrédient interdit explicite."
        } else if (certInfo.hasHalalLabelClaim && unresolvedDoubtfulMatches.isNotEmpty()) {
            hasConflict = true
            conflictEn = "Note: a Halal label is declared on the product, while ingredient(s) such as ${unresolvedDoubtfulMatches.joinToString(", ") { it.entry.nameEn }} typically require source verification. Confirm the certification logo on packaging."
            conflictFr = "Remarque : une mention Halal figure sur le produit, mais certains ingrédients (${unresolvedDoubtfulMatches.joinToString(", ") { it.entry.nameFr }}) nécessitent habituellement une vérification d'origine."
        }

        // Final verdict synthesis following strict religious accuracy rules:
        // 1. Clearly prohibited ingredient -> HARAM (or DOUBTFUL only if strong contradiction? No: explicit pork/alcohol in ingredients is HARAM, with conflict note if label disagrees).
        // 2. Upstream HARAM without explicit contradicting halal certificate -> HARAM (if no conflict) or DOUBTFUL (if conflict with halal label).
        // 3. Unresolved source-dependent / verification-required ingredients -> DOUBTFUL (even if vegan/vegetarian!).
        // 4. Upstream DOUBTFUL -> DOUBTFUL.
        // 5. Missing ingredients & no Halal label & no upstream Halal/Haram -> UNKNOWN (NEVER Haram!).
        // 6. Clean ingredients (no prohibited, no unresolved doubtful) + (Halal label OR upstream HALAL OR complete ingredient declaration free of triggers) -> HALAL.
        val finalVerdict: HalalVerdict
        val evidenceQuality: EvidenceQuality
        val explanationEn: String
        val explanationFr: String

        when {
            prohibitedMatches.isNotEmpty() -> {
                finalVerdict = HalalVerdict.HARAM
                evidenceQuality = if (hasConflict) EvidenceQuality.MODERATE else EvidenceQuality.HIGH
                val namesEn = prohibitedMatches.joinToString(", ") { it.entry.nameEn }
                val namesFr = prohibitedMatches.joinToString(", ") { it.entry.nameFr }
                explanationEn = "Reliable ingredient evidence identifies clearly prohibited substance(s): $namesEn."
                explanationFr = "L'analyse des ingrédients identifie clairement un ou plusieurs éléments interdits : $namesFr."
            }

            communityPillar.verdictHint == HalalVerdict.HARAM -> {
                finalVerdict = if (certInfo.hasHalalLabelClaim) HalalVerdict.DOUBTFUL else HalalVerdict.HARAM
                evidenceQuality = if (hasConflict) EvidenceQuality.LIMITED else EvidenceQuality.MODERATE
                explanationEn = if (certInfo.hasHalalLabelClaim) {
                    "Upstream community data marks this product Haram while a Halal label is also tagged. Please verify the physical packaging."
                } else {
                    "Halal Open Food Facts marks this product as non-compliant / Haram based on community product data."
                }
                explanationFr = if (certInfo.hasHalalLabelClaim) {
                    "La base communautaire indique Haram alors qu'une mention Halal est présente. Veuillez vérifier l'emballage."
                } else {
                    "Halal Open Food Facts classe ce produit comme Haram selon les données communautaires."
                }
            }

            unresolvedDoubtfulMatches.isNotEmpty() -> {
                finalVerdict = HalalVerdict.DOUBTFUL
                evidenceQuality = if (hasIngredientsData) EvidenceQuality.MODERATE else EvidenceQuality.LIMITED
                val namesEn = unresolvedDoubtfulMatches.joinToString(", ") { it.entry.nameEn }
                val namesFr = unresolvedDoubtfulMatches.joinToString(", ") { it.entry.nameFr }
                explanationEn = "Contains source-dependent or verification-required ingredient(s): $namesEn. Check the packaging for origin details or trusted Halal certification."
                explanationFr = "Contient des ingrédients dont l'origine nécessite vérification : $namesFr. Vérifiez l'emballage ou une certification Halal reconnue."
            }

            communityPillar.verdictHint == HalalVerdict.DOUBTFUL -> {
                finalVerdict = HalalVerdict.DOUBTFUL
                evidenceQuality = EvidenceQuality.MODERATE
                explanationEn = "Community assessment flags this product as doubtful or needing further source verification."
                explanationFr = "L'évaluation communautaire signale ce produit comme douteux ou nécessitant une vérification complémentaire."
            }

            !hasIngredientsData && !certInfo.hasHalalLabelClaim && communityPillar.verdictHint != HalalVerdict.HALAL -> {
                finalVerdict = HalalVerdict.UNKNOWN
                evidenceQuality = EvidenceQuality.INSUFFICIENT
                explanationEn = "Insufficient ingredient and certification data in the database. Missing information does not mean the product is Haram—inspect the physical packaging or use the manual ingredient checker."
                explanationFr = "Données d'ingrédients et de certification insuffisantes dans la base. L'absence d'information ne signifie pas que le produit est Haram : vérifiez l'emballage ou utilisez l'analyseur manuel."
            }

            hasIngredientsData -> {
                finalVerdict = HalalVerdict.HALAL
                evidenceQuality = when {
                    certInfo.hasHalalLabelClaim && communityPillar.verdictHint == HalalVerdict.HALAL -> EvidenceQuality.HIGH
                    certInfo.hasHalalLabelClaim || communityPillar.verdictHint == HalalVerdict.HALAL -> EvidenceQuality.MODERATE
                    else -> EvidenceQuality.LIMITED
                }
                val veganSuffixEn = if (isDeclaredVegan || isDeclaredVegetarian) {
                    " Note: vegetarian/vegan tags indicate no declared animal flesh, not an official Halal certificate."
                } else ""
                val veganSuffixFr = if (isDeclaredVegan || isDeclaredVegetarian) {
                    " Remarque : les mentions végétarien/végane n'équivalent pas à une certification Halal officielle."
                } else ""
                explanationEn = buildString {
                    append("No prohibited or source-dependent ingredients were detected in the available ingredient list under Tayyib's methodology.")
                    if (certInfo.hasHalalLabelClaim) append(" A Halal packaging label is also declared.")
                    append(veganSuffixEn)
                }
                explanationFr = buildString {
                    append("Aucun ingrédient interdit ou à origine douteuse n'a été détecté dans la liste disponible selon la méthodologie Tayyib.")
                    if (certInfo.hasHalalLabelClaim) append(" Une mention Halal est également déclarée sur l'emballage.")
                    append(veganSuffixFr)
                }
            }

            certInfo.hasHalalLabelClaim || communityPillar.verdictHint == HalalVerdict.HALAL -> {
                finalVerdict = HalalVerdict.HALAL
                evidenceQuality = EvidenceQuality.LIMITED
                explanationEn = "A Halal label or community Halal status is recorded, though the full ingredient text is missing from the database. Confirm ingredients on packaging."
                explanationFr = "Une mention Halal ou un statut communautaire Halal est présent, mais la liste complète des ingrédients manque. Vérifiez l'emballage."
            }

            else -> {
                finalVerdict = HalalVerdict.UNKNOWN
                evidenceQuality = EvidenceQuality.INSUFFICIENT
                explanationEn = "Unable to confirm Halal status due to incomplete product data."
                explanationFr = "Impossible de confirmer le statut Halal en raison de données incomplètes."
            }
        }

        val twoYearsSeconds = 2L * 365L * 24L * 3600L
        val nowSeconds = System.currentTimeMillis() / 1000L
        val isStale = input.isFromCache || (input.lastModifiedTimestamp != null && (nowSeconds - input.lastModifiedTimestamp) > twoYearsSeconds)

        return HalalAssessment(
            finalVerdict = finalVerdict,
            evidenceQuality = evidenceQuality,
            ruleVersion = taxonomy.schemaVersion,
            explanationEn = explanationEn,
            explanationFr = explanationFr,
            communityPillar = communityPillar,
            ingredientPillar = ingredientPillar,
            certificationPillar = certificationPillar,
            matchedIngredients = matched,
            hasConflictingEvidence = hasConflict,
            conflictNoteEn = conflictEn,
            conflictNoteFr = conflictFr,
            isStaleData = isStale
        )
    }

    /**
     * Offline manual ingredient text analysis.
     */
    fun analyzeManualText(rawText: String): HalalAssessment {
        if (rawText.isBlank()) {
            return HalalAssessment(
                finalVerdict = HalalVerdict.UNKNOWN,
                evidenceQuality = EvidenceQuality.INSUFFICIENT,
                ruleVersion = taxonomy.schemaVersion,
                explanationEn = "Enter or paste an ingredient list to run an offline analysis.",
                explanationFr = "Saisissez ou collez une liste d'ingrédients pour lancer l'analyse hors ligne.",
                communityPillar = AssessmentPillar(
                    summaryEn = "Manual text check (no online database record)",
                    summaryFr = "Analyse de texte manuel (hors base en ligne)",
                    verdictHint = HalalVerdict.UNKNOWN
                ),
                ingredientPillar = AssessmentPillar(
                    summaryEn = "No ingredients supplied yet",
                    summaryFr = "Aucun ingrédient fourni",
                    verdictHint = HalalVerdict.UNKNOWN
                ),
                certificationPillar = AssessmentPillar(
                    summaryEn = "Not applicable to pasted text",
                    summaryFr = "Non applicable au texte saisi",
                    verdictHint = HalalVerdict.UNKNOWN
                ),
                matchedIngredients = emptyList(),
                hasConflictingEvidence = false
            )
        }
        return analyzeProduct(ClassificationInput(ingredientsText = rawText))
    }

    fun matchIngredients(normalizedCorpus: String): List<AnalyzedIngredient> {
        if (normalizedCorpus.isBlank()) return emptyList()

        // Strip known safe negation / vinegar / certified fish-gelatin phrases into a masked copy
        // so "wine vinegar" never triggers "wine" and "pork-free" never triggers "pork",
        // while preserving original detection of "vinegar" and "halal_fish_gelatin".
        var maskedForRiskRules = normalizedCorpus
        safeNegationPhrases.forEach { phrase ->
            val normPhrase = TextNormalizer.normalize(phrase)
            maskedForRiskRules = maskedForRiskRules.replace(normPhrase, " ")
        }
        safeVinegarPhrases.forEach { phrase ->
            val normPhrase = TextNormalizer.normalize(phrase)
            maskedForRiskRules = maskedForRiskRules.replace(normPhrase, " vinegar ")
        }
        safeHalalGelatinPhrases.forEach { phrase ->
            val normPhrase = TextNormalizer.normalize(phrase)
            maskedForRiskRules = maskedForRiskRules.replace(normPhrase, " safe_marine_gelling_token ")
        }

        val matchesById = linkedMapOf<String, AnalyzedIngredient>()

        for (rule in compiledRules) {
            val targetText = when (rule.entry.id) {
                "vinegar", "halal_fish_gelatin" -> normalizedCorpus
                else -> maskedForRiskRules
            }
            for ((term, regex) in rule.normalizedPatterns) {
                val match = regex.find(targetText)
                if (match != null) {
                    // Avoid triggering generic unspecified gelatin if explicit pork gelatin already matched the exact same span
                    if (rule.entry.id == "e441" && matchesById.containsKey("pork_gelatin")) {
                        continue
                    }
                    matchesById[rule.entry.id] = AnalyzedIngredient(
                        matchedToken = term,
                        entry = rule.entry
                    )
                    break
                }
            }
        }

        return matchesById.values.sortedBy {
            when (it.entry.category) {
                IngredientCategory.CLEARLY_PROHIBITED -> 0
                IngredientCategory.SOURCE_DEPENDENT -> 1
                IngredientCategory.REQUIRES_VERIFICATION -> 2
                IngredientCategory.INSUFFICIENT_INFO -> 3
                IngredientCategory.GENERALLY_PERMISSIBLE -> 4
            }
        }
    }

    private fun evaluateCommunityPillar(
        upstreamStatus: String?,
        upstreamIsHalal: Boolean?
    ): AssessmentPillar {
        val status = upstreamStatus?.trim()?.lowercase()
        return when {
            status == "halal" || upstreamIsHalal == true -> AssessmentPillar(
                summaryEn = "Halal Open Food Facts status: Halal",
                summaryFr = "Statut Halal Open Food Facts : Halal",
                verdictHint = HalalVerdict.HALAL
            )
            status == "haram" || status == "excluded" || status == "non-halal" -> AssessmentPillar(
                summaryEn = "Halal Open Food Facts status: Haram / Excluded",
                summaryFr = "Statut Halal Open Food Facts : Haram / Exclu",
                verdictHint = HalalVerdict.HARAM
            )
            status == "doubtful" || status == "mushbooh" || status == "questionable" -> AssessmentPillar(
                summaryEn = "Halal Open Food Facts status: Doubtful",
                summaryFr = "Statut Halal Open Food Facts : Douteux",
                verdictHint = HalalVerdict.DOUBTFUL
            )
            else -> AssessmentPillar(
                summaryEn = "No definitive community verdict available",
                summaryFr = "Aucun verdict communautaire définitif disponible",
                verdictHint = HalalVerdict.UNKNOWN
            )
        }
    }

    private fun evaluateIngredientPillar(
        hasIngredientsData: Boolean,
        prohibited: List<AnalyzedIngredient>,
        doubtful: List<AnalyzedIngredient>,
        isDeclaredVegan: Boolean,
        isDeclaredVegetarian: Boolean
    ): AssessmentPillar {
        return when {
            !hasIngredientsData -> AssessmentPillar(
                summaryEn = "Ingredient declaration missing from product record",
                summaryFr = "Liste d'ingrédients absente de la fiche produit",
                verdictHint = HalalVerdict.UNKNOWN
            )
            prohibited.isNotEmpty() -> AssessmentPillar(
                summaryEn = "Prohibited ingredient(s) detected: ${prohibited.joinToString(", ") { it.entry.nameEn }}",
                summaryFr = "Ingrédient(s) interdit(s) détecté(s) : ${prohibited.joinToString(", ") { it.entry.nameFr }}",
                verdictHint = HalalVerdict.HARAM
            )
            doubtful.isNotEmpty() -> AssessmentPillar(
                summaryEn = "Source-dependent / verification-needed: ${doubtful.joinToString(", ") { it.entry.nameEn }}",
                summaryFr = "Origine à vérifier : ${doubtful.joinToString(", ") { it.entry.nameFr }}",
                verdictHint = HalalVerdict.DOUBTFUL
            )
            else -> {
                val extraEn = when {
                    isDeclaredVegan -> " (also tagged vegan; vegan does not equal certified Halal)"
                    isDeclaredVegetarian -> " (also tagged vegetarian; vegetarian does not equal certified Halal)"
                    else -> ""
                }
                val extraFr = when {
                    isDeclaredVegan -> " (également étiqueté végane ; ne remplace pas une certification Halal)"
                    isDeclaredVegetarian -> " (également étiqueté végétarien ; ne remplace pas une certification Halal)"
                    else -> ""
                }
                AssessmentPillar(
                    summaryEn = "No prohibited or source-dependent items flagged in declared text$extraEn",
                    summaryFr = "Aucun ingrédient interdit ou douteux repéré dans le texte déclaré$extraFr",
                    verdictHint = HalalVerdict.HALAL
                )
            }
        }
    }

    private fun evaluateCertification(
        normalizedLabels: List<String>,
        normalizedAnalysis: List<String>
    ): CertificationInfo {
        val halalLabels = normalizedLabels.filter { it.contains("halal") }
        val mentionedBodies = recognizedCertifiers
            .filter { (key, _) -> normalizedLabels.any { it.contains(key) } }
            .map { it.second }

        val hasHalalClaim = halalLabels.isNotEmpty() || mentionedBodies.isNotEmpty()
        val noteEn = when {
            mentionedBodies.isNotEmpty() ->
                "Packaging labels reference ${mentionedBodies.joinToString(", ")}. Note: label text is community-reported and not verified live against an official certifier registry."
            hasHalalClaim ->
                "A Halal packaging label is declared in product metadata. Verify the official certification logo directly on the package."
            normalizedAnalysis.any { it == "vegan" } ->
                "No Halal certification declared (product is tagged vegan, which is not an official Halal certificate)."
            else ->
                "No Halal certification metadata recorded for this product."
        }
        val noteFr = when {
            mentionedBodies.isNotEmpty() ->
                "Les mentions d'emballage citent ${mentionedBodies.joinToString(", ")}. Remarque : données communautaires non vérifiées en direct sur un registre officiel."
            hasHalalClaim ->
                "Une mention Halal figure dans les métadonnées du produit. Vérifiez le logo de certification directement sur l'emballage."
            normalizedAnalysis.any { it == "vegan" } ->
                "Aucune certification Halal déclarée (le produit est indiqué végane, ce qui n'est pas une certification Halal)."
            else ->
                "Aucune information de certification Halal enregistrée pour ce produit."
        }

        return CertificationInfo(
            hasHalalLabelClaim = hasHalalClaim,
            declaredLabels = halalLabels,
            recognizedBodiesMentioned = mentionedBodies,
            isOfficiallyVerifiedViaRegistry = false,
            verificationNoteEn = noteEn,
            verificationNoteFr = noteFr
        )
    }
}
