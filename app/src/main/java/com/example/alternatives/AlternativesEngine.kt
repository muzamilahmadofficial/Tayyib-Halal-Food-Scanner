package com.example.alternatives

import com.example.data.ProductRepository
import com.example.domain.AlternativeRecommendation
import com.example.domain.EvidenceQuality
import com.example.domain.HalalVerdict
import com.example.domain.Product
import com.example.domain.SearchFilter
import com.example.halalengine.TextNormalizer

class AlternativesEngine(
    private val productRepository: ProductRepository
) {
    /**
     * Searches Halal Open Food Facts for real similar products with supporting Halal evidence.
     * Never fabricates products or treats vegan products as certified Halal.
     */
    suspend fun findAlternativesForProduct(sourceProduct: Product): List<AlternativeRecommendation> {
        val candidateQueries = buildCandidateQueries(sourceProduct)
        if (candidateQueries.isEmpty()) return emptyList()

        for (queryTerm in candidateQueries) {
            val result = productRepository.searchProducts(
                filter = SearchFilter(
                    query = queryTerm,
                    verdictFilter = HalalVerdict.HALAL
                ),
                page = 1,
                pageSize = 24
            )

            val ranked = rankCandidates(
                candidates = result.products,
                excludeBarcode = sourceProduct.barcode,
                referenceCategories = sourceProduct.categoriesTags,
                referenceCountries = sourceProduct.countriesTags,
                categoryLabel = queryTerm
            )
            if (ranked.isNotEmpty()) {
                return ranked
            }
        }

        return emptyList()
    }

    private fun buildCandidateQueries(product: Product): List<String> {
        val genericIgnored = setOf("snacks", "food", "foods", "groceries", "plant based foods", "sweet snacks")
        val normalizedCats = product.categoriesTags
            .asReversed()
            .map { TextNormalizer.normalizeTag(it) }
            .filter { it.length >= 4 }
            .distinct()

        val specific = normalizedCats.firstOrNull { it !in genericIgnored }
        val broader = normalizedCats.firstOrNull { it != specific && it.length >= 4 }
        val nameKeywords = product.name
            .replace(Regex("[^A-Za-zÀ-ÿ0-9 ]"), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 4 }
            .take(2)
            .joinToString(" ")

        return listOfNotNull(specific, broader, nameKeywords.takeIf { it.isNotBlank() })
            .distinct()
            .take(3)
    }

    /**
     * Explores Halal alternatives by user-supplied category or product type.
     */
    suspend fun searchAlternativesByCategory(
        categoryQuery: String,
        countryFilter: String = ""
    ): Pair<List<AlternativeRecommendation>, Boolean> {
        val cleaned = categoryQuery.trim()
        if (cleaned.isEmpty()) return emptyList<AlternativeRecommendation>() to false

        val result = productRepository.searchProducts(
            filter = SearchFilter(
                query = cleaned,
                country = countryFilter,
                verdictFilter = HalalVerdict.HALAL
            ),
            page = 1,
            pageSize = 24
        )

        if (result.isOffline) return emptyList<AlternativeRecommendation>() to true

        val ranked = rankCandidates(
            candidates = result.products,
            excludeBarcode = null,
            referenceCategories = emptyList(),
            referenceCountries = emptyList(),
            categoryLabel = cleaned
        )
        return ranked to false
    }

    fun rankCandidates(
        candidates: List<Product>,
        excludeBarcode: String?,
        referenceCategories: List<String>,
        referenceCountries: List<String>,
        categoryLabel: String
    ): List<AlternativeRecommendation> {
        val refCatSet = referenceCategories.map { TextNormalizer.normalizeTag(it) }.toSet()
        val refCountrySet = referenceCountries.map { TextNormalizer.normalizeTag(it) }.toSet()

        return candidates
            .asSequence()
            .filter { it.barcode != excludeBarcode }
            .filter { it.assessment.finalVerdict == HalalVerdict.HALAL }
            .filter { !it.assessment.hasConflictingEvidence }
            .distinctBy { it.barcode }
            .map { candidate ->
                val candCats = candidate.categoriesTags.map { TextNormalizer.normalizeTag(it) }.toSet()
                val candCountries = candidate.countriesTags.map { TextNormalizer.normalizeTag(it) }.toSet()
                val hasHalalLabel = candidate.labelsTags.any { it.contains("halal", ignoreCase = true) }

                var score = 0
                score += when (candidate.assessment.evidenceQuality) {
                    EvidenceQuality.HIGH -> 40
                    EvidenceQuality.MODERATE -> 25
                    EvidenceQuality.LIMITED -> 10
                    EvidenceQuality.INSUFFICIENT -> 0
                }
                if (hasHalalLabel) score += 25
                if (!candidate.ingredientsText.isNullOrBlank()) score += 15
                if (!candidate.imageUrl.isNullOrBlank()) score += 10
                if (refCatSet.isNotEmpty() && candCats.intersect(refCatSet).isNotEmpty()) score += 15
                if (refCountrySet.isNotEmpty() && candCountries.intersect(refCountrySet).isNotEmpty()) score += 10
                if (!candidate.assessment.isStaleData) score += 5

                val evidenceEn = buildString {
                    if (hasHalalLabel) append("Halal label declared")
                    else append("Clean declared ingredients")
                    append(" • Evidence: ${candidate.assessment.evidenceQuality.name.lowercase().replaceFirstChar(Char::uppercase)}")
                }
                val evidenceFr = buildString {
                    if (hasHalalLabel) append("Mention Halal déclarée")
                    else append("Ingrédients déclarés conformes")
                    val qFr = when (candidate.assessment.evidenceQuality) {
                        EvidenceQuality.HIGH -> "Élevée"
                        EvidenceQuality.MODERATE -> "Modérée"
                        EvidenceQuality.LIMITED -> "Limitée"
                        EvidenceQuality.INSUFFICIENT -> "Insuffisante"
                    }
                    append(" • Preuve : $qFr")
                }

                score to AlternativeRecommendation(
                    product = candidate,
                    matchReasonEn = "Similar category: $categoryLabel",
                    matchReasonFr = "Catégorie similaire : $categoryLabel",
                    supportingEvidenceEn = evidenceEn,
                    supportingEvidenceFr = evidenceFr
                )
            }
            .sortedByDescending { it.first }
            .map { it.second }
            .take(15)
            .toList()
    }

    private fun extractBestCategoryQuery(product: Product): String {
        val specificCategory = product.categoriesTags
            .asReversed()
            .map { TextNormalizer.normalizeTag(it) }
            .firstOrNull { tag ->
                tag.length >= 4 &&
                    tag !in setOf("snacks", "food", "foods", "beverages", "plant based foods", "groceries")
            }
        return specificCategory ?: product.categoriesTags.lastOrNull()?.let { TextNormalizer.normalizeTag(it) }.orEmpty()
    }
}
