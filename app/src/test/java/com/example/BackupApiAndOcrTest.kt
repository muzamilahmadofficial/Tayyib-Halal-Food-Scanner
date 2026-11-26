package com.example

import com.example.core.NetworkMonitor
import com.example.data.ProductLookupResult
import com.example.data.ProductRepository
import com.example.database.CachedProductDao
import com.example.database.CachedProductEntity
import com.example.database.FavouriteDao
import com.example.database.FavouriteEntity
import com.example.database.ScanHistoryDao
import com.example.database.ScanHistoryEntity
import com.example.domain.HalalVerdict
import com.example.domain.ProductDataSource
import com.example.domain.SearchFilter
import com.example.halalengine.HalalClassificationEngine
import com.example.halalengine.TaxonomyLoader
import com.example.networking.HalalOpenFoodFactsApi
import com.example.networking.OffProductDto
import com.example.networking.OpenFoodFactsBackupApi
import com.example.networking.ProductLookupResponse
import com.example.networking.ProductSearchResponse
import com.example.scanner.IngredientOcrExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class BackupApiAndOcrTest {

    private lateinit var engine: HalalClassificationEngine

    @Before
    fun setup() {
        val assetFile = listOf(
            File("src/main/assets/halal_ingredients_taxonomy.json"),
            File("app/src/main/assets/halal_ingredients_taxonomy.json")
        ).first { it.exists() }
        val taxonomy = TaxonomyLoader.parseJson(assetFile.readText())
        engine = HalalClassificationEngine(taxonomy)
    }

    @Test
    fun ocrCleaner_joinsHyphenatedWrapsAndExtractsFromIngredientsHeader() {
        val raw = """
            Super Crunch Biscuits
            Nutrition per 100g: 480 kcal
            Ingredients: Wheat flour, sugar, emulsi-
            fier (E471), pork gelatin, natural
            flavouring.
        """.trimIndent()

        val cleaned = IngredientOcrExtractor.cleanExtractedText(raw)
        assertTrue(cleaned.startsWith("Ingredients:"))
        assertTrue(cleaned.contains("emulsifier (E471)"))
        assertTrue(cleaned.contains("pork gelatin"))

        val assessment = engine.analyzeManualText(cleaned)
        assertEquals(HalalVerdict.HARAM, assessment.finalVerdict)
    }

    @Test
    fun ocrCleaner_handlesFrenchIngredientsLabel() {
        val raw = "Biscuits Chocolat\nIngrédients : Sucre, farine de blé, géla-\ntine de poisson, E322."
        val cleaned = IngredientOcrExtractor.cleanExtractedText(raw)
        assertTrue(cleaned.startsWith("Ingrédients"))
        assertTrue(cleaned.contains("gélatine de poisson"))
        assertTrue(cleaned.contains("E322"))
    }

    @Test
    fun repository_fallsBackToOpenFoodFacts_whenPrimaryThrowsNetworkError() = runTest {
        val primaryApi = object : HalalOpenFoodFactsApi {
            override suspend fun getProductByBarcode(barcode: String, fields: String): Response<ProductLookupResponse> {
                throw IOException("halalopenfoodfacts.org unreachable")
            }

            override suspend fun searchProducts(
                query: String,
                pageSize: Int,
                page: Int,
                isHalal: Int?,
                halalStatus: String?,
                country: String?,
                category: String?,
                fields: String
            ): Response<ProductSearchResponse> {
                throw IOException("halalopenfoodfacts.org down")
            }
        }

        val backupDto = OffProductDto(
            code = "3017620422003",
            productName = "Backup Hazelnut Spread",
            brands = "TestBrand",
            ingredientsText = "Sugar, palm oil, hazelnuts, cocoa, soy lecithin (E322), vanillin",
            labelsTags = listOf("en:halal")
        )

        val backupApi = object : OpenFoodFactsBackupApi {
            override suspend fun getProductByBarcode(barcode: String, fields: String): Response<ProductLookupResponse> {
                return Response.success(ProductLookupResponse(code = barcode, status = 1, product = backupDto))
            }

            override suspend fun searchProducts(
                searchTerms: String,
                pageSize: Int,
                page: Int,
                country: String?,
                category: String?,
                labelsTag: String?,
                fields: String
            ): Response<ProductSearchResponse> {
                return Response.success(
                    ProductSearchResponse(
                        count = 1,
                        page = page,
                        pageSize = pageSize,
                        products = listOf(backupDto)
                    )
                )
            }
        }

        val repo = createRepo(primaryApi, backupApi)

        val lookup = repo.lookupBarcode("3017620422003")
        assertTrue("Expected Found from backup API, got $lookup", lookup is ProductLookupResult.Found)
        val found = (lookup as ProductLookupResult.Found).product
        assertEquals(ProductDataSource.OPEN_FOOD_FACTS_BACKUP, found.dataSource)
        assertEquals(HalalVerdict.HALAL, found.assessment.finalVerdict)

        val search = repo.searchProducts(SearchFilter(query = "hazelnut"))
        assertTrue(search.usedBackupSource)
        assertEquals(1, search.products.size)
        assertEquals(ProductDataSource.OPEN_FOOD_FACTS_BACKUP, search.products.first().dataSource)
    }

    @Test
    fun repository_fallsBackToOpenFoodFacts_whenPrimaryReturnsHttp503() = runTest {
        val primaryApi = object : HalalOpenFoodFactsApi {
            override suspend fun getProductByBarcode(barcode: String, fields: String): Response<ProductLookupResponse> {
                return Response.error(503, "Service Unavailable".toResponseBody("text/plain".toMediaType()))
            }

            override suspend fun searchProducts(
                query: String,
                pageSize: Int,
                page: Int,
                isHalal: Int?,
                halalStatus: String?,
                country: String?,
                category: String?,
                fields: String
            ): Response<ProductSearchResponse> {
                return Response.error(503, "Service Unavailable".toResponseBody("text/plain".toMediaType()))
            }
        }

        val backupDto = OffProductDto(
            code = "5000112637922",
            productName = "Backup Gummy Candies",
            ingredientsText = "Glucose syrup, sugar, pork gelatin, citric acid"
        )

        val backupApi = object : OpenFoodFactsBackupApi {
            override suspend fun getProductByBarcode(barcode: String, fields: String): Response<ProductLookupResponse> {
                return Response.success(ProductLookupResponse(code = barcode, status = 1, product = backupDto))
            }

            override suspend fun searchProducts(
                searchTerms: String,
                pageSize: Int,
                page: Int,
                country: String?,
                category: String?,
                labelsTag: String?,
                fields: String
            ): Response<ProductSearchResponse> {
                return Response.success(ProductSearchResponse(count = 1, page = 1, pageSize = 20, products = listOf(backupDto)))
            }
        }

        val repo = createRepo(primaryApi, backupApi)
        val lookup = repo.lookupBarcode("5000112637922")
        assertTrue(lookup is ProductLookupResult.Found)
        val found = (lookup as ProductLookupResult.Found).product
        assertEquals(ProductDataSource.OPEN_FOOD_FACTS_BACKUP, found.dataSource)
        assertEquals(HalalVerdict.HARAM, found.assessment.finalVerdict)
    }

    private fun createRepo(
        primaryApi: HalalOpenFoodFactsApi,
        backupApi: OpenFoodFactsBackupApi
    ): ProductRepository {
        val fakeHistory = InMemoryHistoryDao()
        val fakeFav = InMemoryFavouriteDao()
        val fakeCache = InMemoryCacheDao()
        val context = RuntimeEnvironment.getApplication()
        val networkMonitor = object : NetworkMonitor(context) {
            override fun isOnline(): Boolean = true
        }
        return ProductRepository(
            api = primaryApi,
            backupApi = backupApi,
            engine = engine,
            historyDao = fakeHistory,
            favouriteDao = fakeFav,
            cacheDao = fakeCache,
            networkMonitor = networkMonitor
        )
    }

    private class InMemoryHistoryDao : ScanHistoryDao {
        private val items = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
        override fun observeAll(): Flow<List<ScanHistoryEntity>> = items
        override fun observeRecent(limit: Int): Flow<List<ScanHistoryEntity>> = items.map { it.take(limit) }
        override suspend fun upsert(item: ScanHistoryEntity) {
            items.value = listOf(item) + items.value.filterNot { it.barcode == item.barcode }
        }
        override suspend fun deleteByBarcode(barcode: String) {
            items.value = items.value.filterNot { it.barcode == barcode }
        }
        override suspend fun clearAll() { items.value = emptyList() }
    }

    private class InMemoryFavouriteDao : FavouriteDao {
        private val items = MutableStateFlow<List<FavouriteEntity>>(emptyList())
        override fun observeAll(): Flow<List<FavouriteEntity>> = items
        override fun observeIsFavourite(barcode: String): Flow<Boolean> = items.map { list -> list.any { it.barcode == barcode } }
        override suspend fun isFavourite(barcode: String): Boolean = items.value.any { it.barcode == barcode }
        override suspend fun upsert(item: FavouriteEntity) {
            items.value = listOf(item) + items.value.filterNot { it.barcode == item.barcode }
        }
        override suspend fun deleteByBarcode(barcode: String) {
            items.value = items.value.filterNot { it.barcode == barcode }
        }
    }

    private class InMemoryCacheDao : CachedProductDao {
        private val map = linkedMapOf<String, CachedProductEntity>()
        override suspend fun getByBarcode(barcode: String): CachedProductEntity? = map[barcode]
        override suspend fun upsert(item: CachedProductEntity) { map[item.barcode] = item }
        override suspend fun count(): Int = map.size
        override suspend fun clearAll() { map.clear() }
    }
}
