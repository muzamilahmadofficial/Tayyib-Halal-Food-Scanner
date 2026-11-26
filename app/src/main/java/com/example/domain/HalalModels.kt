package com.example.domain

enum class HalalVerdict {
    HALAL,
    HARAM,
    DOUBTFUL,
    UNKNOWN
}

enum class ProductDataSource {
    HALAL_OPEN_FOOD_FACTS,
    OPEN_FOOD_FACTS_BACKUP,
    OFFLINE_CACHE
}

enum class EvidenceQuality {
    HIGH,
    MODERATE,
    LIMITED,
    INSUFFICIENT
}

enum class IngredientCategory {
    CLEARLY_PROHIBITED,
    SOURCE_DEPENDENT,
    REQUIRES_VERIFICATION,
    GENERALLY_PERMISSIBLE,
    INSUFFICIENT_INFO
}

data class TaxonomyEntry(
    val id: String,
    val nameEn: String,
    val nameFr: String,
    val alternativeSpellings: List<String>,
    val eNumber: String?,
    val functionEn: String,
    val functionFr: String,
    val typicalOriginEn: String,
    val typicalOriginFr: String,
    val alternativeOriginsEn: String,
    val alternativeOriginsFr: String,
    val category: IngredientCategory,
    val explanationEn: String,
    val explanationFr: String,
    val evidenceQuality: EvidenceQuality,
    val reference: String,
    val ruleVersion: String
)

data class AnalyzedIngredient(
    val matchedToken: String,
    val entry: TaxonomyEntry
)

data class CertificationInfo(
    val hasHalalLabelClaim: Boolean,
    val declaredLabels: List<String>,
    val recognizedBodiesMentioned: List<String>,
    val isOfficiallyVerifiedViaRegistry: Boolean = false,
    val verificationNoteEn: String,
    val verificationNoteFr: String
)

data class AssessmentPillar(
    val summaryEn: String,
    val summaryFr: String,
    val verdictHint: HalalVerdict
)

data class HalalAssessment(
    val finalVerdict: HalalVerdict,
    val evidenceQuality: EvidenceQuality,
    val ruleVersion: String,
    val explanationEn: String,
    val explanationFr: String,
    val communityPillar: AssessmentPillar,
    val ingredientPillar: AssessmentPillar,
    val certificationPillar: AssessmentPillar,
    val matchedIngredients: List<AnalyzedIngredient>,
    val hasConflictingEvidence: Boolean,
    val conflictNoteEn: String? = null,
    val conflictNoteFr: String? = null,
    val isStaleData: Boolean = false
)

data class Product(
    val barcode: String,
    val name: String,
    val nameEn: String? = null,
    val nameFr: String? = null,
    val brands: String = "",
    val imageUrl: String? = null,
    val ingredientsText: String? = null,
    val ingredientsTextEn: String? = null,
    val ingredientsTextFr: String? = null,
    val ingredientsTags: List<String> = emptyList(),
    val additivesTags: List<String> = emptyList(),
    val allergensTags: List<String> = emptyList(),
    val labelsTags: List<String> = emptyList(),
    val categoriesTags: List<String> = emptyList(),
    val countriesTags: List<String> = emptyList(),
    val ingredientsAnalysisTags: List<String> = emptyList(),
    val upstreamHalalStatus: String? = null,
    val upstreamIsHalal: Boolean? = null,
    val productUrl: String? = null,
    val lastModifiedTimestamp: Long? = null,
    val nutritionGrade: String? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false,
    val dataSource: ProductDataSource = ProductDataSource.HALAL_OPEN_FOOD_FACTS,
    val assessment: HalalAssessment
) {
    fun displayName(isFrench: Boolean): String {
        val localized = if (isFrench) nameFr else nameEn
        return localized?.takeIf { it.isNotBlank() } ?: name.ifBlank { barcode }
    }

    fun displayIngredients(isFrench: Boolean): String? {
        val localized = if (isFrench) ingredientsTextFr else ingredientsTextEn
        return localized?.takeIf { it.isNotBlank() } ?: ingredientsText?.takeIf { it.isNotBlank() }
    }
}

data class AlternativeRecommendation(
    val product: Product,
    val matchReasonEn: String,
    val matchReasonFr: String,
    val supportingEvidenceEn: String,
    val supportingEvidenceFr: String
)

data class ManualCheckResult(
    val inputText: String,
    val assessment: HalalAssessment
)

enum class ReportIssueType(val labelEn: String, val labelFr: String) {
    INCORRECT_DETAILS("Incorrect product details", "Détails du produit incorrects"),
    INCORRECT_INGREDIENTS("Incorrect ingredients", "Ingrédients incorrects"),
    INCORRECT_BARCODE("Incorrect barcode", "Code-barres incorrect"),
    INCORRECT_CLASSIFICATION("Incorrect Halal classification", "Classification Halal incorrecte"),
    INCORRECT_IMAGE("Incorrect product image", "Image du produit incorrecte"),
    MISSING_CERTIFICATION("Missing Halal certification", "Certification Halal manquante"),
    INCORRECT_ALTERNATIVES("Incorrect alternatives", "Alternatives incorrectes")
}

data class SearchFilter(
    val query: String = "",
    val country: String = "",
    val category: String = "",
    val verdictFilter: HalalVerdict? = null
)
