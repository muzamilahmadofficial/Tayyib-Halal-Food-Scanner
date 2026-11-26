package com.example.halalengine

import android.content.Context
import com.example.domain.EvidenceQuality
import com.example.domain.IngredientCategory
import com.example.domain.TaxonomyEntry
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class TaxonomyFileDto(
    val schemaVersion: String = "1.2.0",
    val lastUpdated: String = "",
    val attribution: String = "",
    val entries: List<TaxonomyEntryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TaxonomyEntryDto(
    val id: String,
    val nameEn: String,
    val nameFr: String,
    val alternativeSpellings: List<String> = emptyList(),
    val eNumber: String? = null,
    val functionEn: String = "",
    val functionFr: String = "",
    val typicalOriginEn: String = "",
    val typicalOriginFr: String = "",
    val alternativeOriginsEn: String = "",
    val alternativeOriginsFr: String = "",
    val category: String = "INSUFFICIENT_INFO",
    val explanationEn: String = "",
    val explanationFr: String = "",
    val evidenceQuality: String = "MODERATE",
    val reference: String = "",
    val ruleVersion: String = "1.2.0"
)

data class LoadedTaxonomy(
    val schemaVersion: String,
    val lastUpdated: String,
    val entries: List<TaxonomyEntry>
)

object TaxonomyLoader {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun loadFromAssets(context: Context, assetName: String = "halal_ingredients_taxonomy.json"): LoadedTaxonomy {
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        return parseJson(json)
    }

    fun parseJson(json: String): LoadedTaxonomy {
        val adapter = moshi.adapter(TaxonomyFileDto::class.java)
        val dto = adapter.fromJson(json) ?: return LoadedTaxonomy("1.2.0", "", emptyList())
        val entries = dto.entries.map { item ->
            TaxonomyEntry(
                id = item.id,
                nameEn = item.nameEn,
                nameFr = item.nameFr,
                alternativeSpellings = item.alternativeSpellings,
                eNumber = item.eNumber,
                functionEn = item.functionEn,
                functionFr = item.functionFr,
                typicalOriginEn = item.typicalOriginEn,
                typicalOriginFr = item.typicalOriginFr,
                alternativeOriginsEn = item.alternativeOriginsEn,
                alternativeOriginsFr = item.alternativeOriginsFr,
                category = runCatching { IngredientCategory.valueOf(item.category) }
                    .getOrDefault(IngredientCategory.INSUFFICIENT_INFO),
                explanationEn = item.explanationEn,
                explanationFr = item.explanationFr,
                evidenceQuality = runCatching { EvidenceQuality.valueOf(item.evidenceQuality) }
                    .getOrDefault(EvidenceQuality.MODERATE),
                reference = item.reference,
                ruleVersion = item.ruleVersion
            )
        }
        return LoadedTaxonomy(
            schemaVersion = dto.schemaVersion,
            lastUpdated = dto.lastUpdated,
            entries = entries
        )
    }
}
