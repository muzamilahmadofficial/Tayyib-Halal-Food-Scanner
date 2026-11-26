package com.example.networking

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductLookupResponse(
    val code: String? = null,
    val status: Int? = null,
    @Json(name = "status_verbose") val statusVerbose: String? = null,
    val product: OffProductDto? = null
)

@JsonClass(generateAdapter = true)
data class ProductSearchResponse(
    val count: Int? = null,
    val page: Int? = null,
    @Json(name = "page_count") val pageCount: Int? = null,
    @Json(name = "page_size") val pageSize: Int? = null,
    val products: List<OffProductDto>? = null
)

@JsonClass(generateAdapter = true)
data class OffProductDto(
    val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_en") val productNameEn: String? = null,
    @Json(name = "product_name_fr") val productNameFr: String? = null,
    val brands: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "image_front_small_url") val imageFrontSmallUrl: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "ingredients_text_en") val ingredientsTextEn: String? = null,
    @Json(name = "ingredients_text_fr") val ingredientsTextFr: String? = null,
    @Json(name = "ingredients_tags") val ingredientsTags: List<String>? = null,
    @Json(name = "additives_tags") val additivesTags: List<String>? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null,
    @Json(name = "labels_tags") val labelsTags: List<String>? = null,
    @Json(name = "categories_tags") val categoriesTags: List<String>? = null,
    @Json(name = "countries_tags") val countriesTags: List<String>? = null,
    @Json(name = "ingredients_analysis_tags") val ingredientsAnalysisTags: List<String>? = null,
    @Json(name = "halal_status") val halalStatus: String? = null,
    val halal: Any? = null,
    @Json(name = "is_halal") val isHalal: Any? = null,
    @Json(name = "product_url") val productUrl: String? = null,
    val url: String? = null,
    @Json(name = "last_modified_t") val lastModifiedT: Long? = null,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String? = null
) {
    fun resolveImageUrl(): String? =
        listOf(imageFrontUrl, imageUrl, imageFrontSmallUrl)
            .firstOrNull { !it.isNullOrBlank() && it.startsWith("http") }

    fun resolveUpstreamIsHalal(): Boolean? {
        val raw = isHalal ?: halal ?: return null
        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() == 1
            is String -> when (raw.lowercase()) {
                "1", "true", "yes", "halal" -> true
                "0", "false", "no", "haram" -> false
                else -> null
            }
            else -> null
        }
    }
}
