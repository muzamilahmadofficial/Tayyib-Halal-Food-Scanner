package com.example

import com.example.alternatives.AlternativesEngine
import com.example.core.BarcodeValidator
import com.example.data.ProductRepository
import com.example.domain.EvidenceQuality
import com.example.domain.HalalVerdict
import com.example.domain.IngredientCategory
import com.example.domain.Product
import com.example.halalengine.ClassificationInput
import com.example.halalengine.HalalClassificationEngine
import com.example.halalengine.TaxonomyLoader
import com.example.halalengine.TextNormalizer
import com.example.networking.NetworkModule
import com.example.networking.OffProductDto
import com.example.networking.ProductLookupResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class HalalClassificationEngineTest {

    private lateinit var engine: HalalClassificationEngine

    @Before
    fun setUp() {
        val assetFile = listOf(
            File("src/main/assets/halal_ingredients_taxonomy.json"),
            File("app/src/main/assets/halal_ingredients_taxonomy.json")
        ).first { it.exists() }
        val taxonomy = TaxonomyLoader.parseJson(assetFile.readText())
        assertTrue("Taxonomy should load 30+ entries", taxonomy.entries.size >= 30)
        engine = HalalClassificationEngine(taxonomy)
    }

    @Test
    fun `missing ingredient data and no halal claim returns UNKNOWN never HARAM`() {
        val result = engine.analyzeProduct(ClassificationInput())
        assertEquals(HalalVerdict.UNKNOWN, result.finalVerdict)
        assertEquals(EvidenceQuality.INSUFFICIENT, result.evidenceQuality)
    }

    @Test
    fun `french pork and lard terms are classified as HARAM`() {
        val result = engine.analyzeManualText("Farine de blé, saindoux, viande de porc, sel")
        assertEquals(HalalVerdict.HARAM, result.finalVerdict)
        assertTrue(result.matchedIngredients.any { it.entry.id == "lard" })
        assertTrue(result.matchedIngredients.any { it.entry.id == "pork_general" })
    }

    @Test
    fun `french alcohol wine and beer terms are classified as HARAM`() {
        val wineRes = engine.analyzeManualText("Sucre, beurre de cacao, vin rouge, arômes")
        assertEquals(HalalVerdict.HARAM, wineRes.finalVerdict)

        val beerRes = engine.analyzeManualText("Eau, farine, bière blonde, levure")
        assertEquals(HalalVerdict.HARAM, beerRes.finalVerdict)

        val alcRes = engine.analyzeManualText("Gâteau au chocolat, alcool éthylique, sucre")
        assertEquals(HalalVerdict.HARAM, alcRes.finalVerdict)
    }

    @Test
    fun `wine vinegar and sans porc do not trigger false HARAM`() {
        val vinegarRes = engine.analyzeManualText("Huile de colza, eau, vinaigre de vin, moutarde, sel")
        assertEquals(HalalVerdict.HALAL, vinegarRes.finalVerdict)
        assertTrue(vinegarRes.matchedIngredients.any { it.entry.id == "vinegar" })

        val porkFreeRes = engine.analyzeManualText("Poulet, eau, sel, épices, sans porc, sans alcool")
        assertEquals(HalalVerdict.HALAL, porkFreeRes.finalVerdict)
    }

    @Test
    fun `mandatory E-numbers are properly categorized and explained`() {
        // E120 Carmine -> DOUBTFUL / REQUIRES_VERIFICATION
        val e120 = engine.analyzeManualText("Sugar, glucose syrup, E120, citric acid")
        assertEquals(HalalVerdict.DOUBTFUL, e120.finalVerdict)
        assertTrue(e120.matchedIngredients.any { it.entry.eNumber == "E120" })

        // E441 Gelatin -> DOUBTFUL / SOURCE_DEPENDENT
        val e441 = engine.analyzeManualText("Sucre, sirop de glucose, gélatine, arômes")
        assertEquals(HalalVerdict.DOUBTFUL, e441.finalVerdict)
        assertTrue(e441.matchedIngredients.any { it.entry.id == "e441" })

        // E471, E472, E481, E482, E476, E904, E542 -> DOUBTFUL
        listOf("E471", "E472e", "E481", "E482", "E476", "E904", "E542").forEach { code ->
            val res = engine.analyzeManualText("Wheat flour, water, $code, salt")
            assertEquals("Expected DOUBTFUL for $code", HalalVerdict.DOUBTFUL, res.finalVerdict)
        }

        // E322 Lecithin -> generally permissible -> HALAL
        val e322 = engine.analyzeManualText("Cocoa mass, sugar, cocoa butter, E322 (soy lecithin)")
        assertEquals(HalalVerdict.HALAL, e322.finalVerdict)
        assertTrue(
            e322.matchedIngredients.any {
                it.entry.eNumber == "E322" && it.entry.category == IngredientCategory.GENERALLY_PERMISSIBLE
            }
        )
    }

    @Test
    fun `french rennet and animal fat trigger DOUBTFUL`() {
        val rennet = engine.analyzeManualText("Lait pasteurisé, ferments lactiques, présure, sel")
        assertEquals(HalalVerdict.DOUBTFUL, rennet.finalVerdict)

        val animalFat = engine.analyzeManualText("Farine, sucre, graisse animale, sel")
        assertEquals(HalalVerdict.DOUBTFUL, animalFat.finalVerdict)
    }

    @Test
    fun `fish gelatin and halal gelatin are permissible while pork gelatin is HARAM`() {
        val fishGelatin = engine.analyzeManualText("Sugar, fruit juice, gélatine de poisson")
        assertEquals(HalalVerdict.HALAL, fishGelatin.finalVerdict)

        val porkGelatin = engine.analyzeManualText("Sugar, glucose syrup, gélatine de porc")
        assertEquals(HalalVerdict.HARAM, porkGelatin.finalVerdict)
    }

    @Test
    fun `vegan tag alone does not mark product as certified Halal and still flags alcohol`() {
        val veganWithWine = engine.analyzeProduct(
            ClassificationInput(
                ingredientsText = "Tomatoes, basil, red wine, olive oil",
                ingredientsAnalysisTags = listOf("en:vegan")
            )
        )
        assertEquals(HalalVerdict.HARAM, veganWithWine.finalVerdict)

        val veganClean = engine.analyzeProduct(
            ClassificationInput(
                ingredientsText = "Oats, water, sea salt",
                ingredientsAnalysisTags = listOf("en:vegan")
            )
        )
        assertEquals(HalalVerdict.HALAL, veganClean.finalVerdict)
        assertTrue(veganClean.certificationPillar.summaryEn.contains("not an official Halal certificate"))
    }

    @Test
    fun `conflicting evidence between Halal label and prohibited ingredient is flagged`() {
        val conflict = engine.analyzeProduct(
            ClassificationInput(
                ingredientsText = "Flour, sugar, lard, salt",
                labelsTags = listOf("en:halal"),
                upstreamHalalStatus = "halal"
            )
        )
        assertEquals(HalalVerdict.HARAM, conflict.finalVerdict)
        assertTrue(conflict.hasConflictingEvidence)
        assertNotNull(conflict.conflictNoteEn)
        assertNotNull(conflict.conflictNoteFr)
    }

    @Test
    fun `text normalizer folds French accents and standardizes spaced E-numbers`() {
        assertEquals("gelatine de boeuf e471 e120", TextNormalizer.normalize("Gélatine de bœuf, E 471, en:e-120"))
        assertEquals(setOf("E471", "E120"), TextNormalizer.extractENumbers("Emulsifier (E 471), Colorant INS 120"))
    }

    @Test
    fun `barcode validator accepts international formats and rejects invalid input`() {
        assertEquals("3017620422003", BarcodeValidator.normalizeAndValidate("3017620422003"))
        assertEquals("01234565", BarcodeValidator.normalizeAndValidate("01234565"))
        assertEquals("3017620422003", BarcodeValidator.normalizeAndValidate("https://example.org/product/3017620422003"))
        assertTrue(BarcodeValidator.hasValidGtinCheckDigit("3017620422003"))
        assertFalse(BarcodeValidator.isValidManualBarcode("123"))
        assertNull(BarcodeValidator.normalizeAndValidate("   "))
    }

    @Test
    fun `defensive API JSON parsing handles sparse Halal Open Food Facts payloads`() {
        val sampleJson = """
            {
              "code": "3017620422003",
              "status": 1,
              "product": {
                "code": "3017620422003",
                "product_name": "Fixture Hazelnut Spread",
                "product_name_fr": "Pâte à tartiner noisettes",
                "brands": "FixtureBrand",
                "ingredients_text_en": "Sugar, palm oil, hazelnuts, cocoa, E322",
                "additives_tags": ["en:e322"],
                "labels_tags": ["en:halal"],
                "halal_status": "halal",
                "is_halal": 1,
                "last_modified_t": 1730000000
              }
            }
        """.trimIndent()

        val adapter = NetworkModule.moshi.adapter(ProductLookupResponse::class.java)
        val parsed = adapter.fromJson(sampleJson)
        assertNotNull(parsed)
        assertEquals(1, parsed?.status)
        val dto: OffProductDto = parsed!!.product!!
        assertEquals(true, dto.resolveUpstreamIsHalal())
        assertEquals("halal", dto.halalStatus)

        val assessment = engine.analyzeProduct(
            ClassificationInput(
                ingredientsTextEn = dto.ingredientsTextEn,
                additivesTags = dto.additivesTags.orEmpty(),
                labelsTags = dto.labelsTags.orEmpty(),
                upstreamHalalStatus = dto.halalStatus,
                upstreamIsHalal = dto.resolveUpstreamIsHalal()
            )
        )
        assertEquals(HalalVerdict.HALAL, assessment.finalVerdict)
        assertEquals(EvidenceQuality.HIGH, assessment.evidenceQuality)
    }

    @Test
    fun `alternatives ranking prioritizes certified Halal candidates and excludes source barcode`() {
        val halalCertified = Product(
            barcode = "1111111111111",
            name = "Fixture Certified Biscuits",
            brands = "BrandA",
            ingredientsText = "Wheat flour, sugar, sunflower oil",
            labelsTags = listOf("en:halal"),
            categoriesTags = listOf("en:biscuits"),
            countriesTags = listOf("en:france"),
            assessment = engine.analyzeProduct(
                ClassificationInput(
                    ingredientsText = "Wheat flour, sugar, sunflower oil",
                    labelsTags = listOf("en:halal"),
                    upstreamHalalStatus = "halal"
                )
            )
        )
        val haramProduct = Product(
            barcode = "2222222222222",
            name = "Fixture Lard Biscuits",
            ingredientsText = "Wheat flour, lard",
            categoriesTags = listOf("en:biscuits"),
            assessment = engine.analyzeManualText("Wheat flour, lard")
        )
        val sourceProduct = Product(
            barcode = "3333333333333",
            name = "Source Gelatin Biscuits",
            categoriesTags = listOf("en:biscuits"),
            countriesTags = listOf("en:france"),
            assessment = engine.analyzeManualText("Sugar, pork gelatin")
        )

        // Use pure ranking logic via temporary AlternativesEngine helper without network call
        val dummyEngine = AlternativesEngine(
            productRepository = ProductRepository(
                api = NetworkModule.createApi(),
                backupApi = NetworkModule.createBackupApi(),
                engine = engine,
                historyDao = FakeNoOpHistoryDao,
                favouriteDao = FakeNoOpFavDao,
                cacheDao = FakeNoOpCacheDao,
                networkMonitor = com.example.core.NetworkMonitor(
                    object : android.content.ContextWrapper(null) {}
                )
            )
        )

        val ranked = dummyEngine.rankCandidates(
            candidates = listOf(haramProduct, sourceProduct, halalCertified),
            excludeBarcode = sourceProduct.barcode,
            referenceCategories = sourceProduct.categoriesTags,
            referenceCountries = sourceProduct.countriesTags,
            categoryLabel = "biscuits"
        )

        assertEquals(1, ranked.size)
        assertEquals("1111111111111", ranked.first().product.barcode)
    }
}

private object FakeNoOpHistoryDao : com.example.database.ScanHistoryDao {
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.database.ScanHistoryEntity>())
    override fun observeRecent(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.database.ScanHistoryEntity>())
    override suspend fun upsert(item: com.example.database.ScanHistoryEntity) = Unit
    override suspend fun deleteByBarcode(barcode: String) = Unit
    override suspend fun clearAll() = Unit
}

private object FakeNoOpFavDao : com.example.database.FavouriteDao {
    override fun observeAll() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.database.FavouriteEntity>())
    override fun observeIsFavourite(barcode: String) = kotlinx.coroutines.flow.flowOf(false)
    override suspend fun isFavourite(barcode: String) = false
    override suspend fun upsert(item: com.example.database.FavouriteEntity) = Unit
    override suspend fun deleteByBarcode(barcode: String) = Unit
}

private object FakeNoOpCacheDao : com.example.database.CachedProductDao {
    override suspend fun getByBarcode(barcode: String) = null
    override suspend fun upsert(item: com.example.database.CachedProductEntity) = Unit
    override suspend fun clearAll() = Unit
    override suspend fun count() = 0
}
