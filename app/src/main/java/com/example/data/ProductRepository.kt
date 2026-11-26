package com.example.data

import com.example.core.NetworkMonitor
import com.example.database.CachedProductDao
import com.example.database.CachedProductEntity
import com.example.database.FavouriteDao
import com.example.database.FavouriteEntity
import com.example.database.ScanHistoryDao
import com.example.database.ScanHistoryEntity
import com.example.domain.HalalVerdict
import com.example.domain.Product
import com.example.domain.ProductDataSource
import com.example.domain.SearchFilter
import com.example.halalengine.ClassificationInput
import com.example.halalengine.HalalClassificationEngine
import com.example.networking.HalalOpenFoodFactsApi
import com.example.networking.NetworkModule
import com.example.networking.OffProductDto
import com.example.networking.OpenFoodFactsBackupApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed interface ProductLookupResult {
    data class Found(val product: Product) : ProductLookupResult
    data class NotFound(val barcode: String) : ProductLookupResult
    data class OfflineNotCached(val barcode: String) : ProductLookupResult
    data class Error(val barcode: String, val message: String) : ProductLookupResult
}

data class SearchPageResult(
    val products: List<Product>,
    val page: Int,
    val totalCount: Int,
    val hasMore: Boolean,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val usedBackupSource: Boolean = false
)

class ProductRepository(
    private val api: HalalOpenFoodFactsApi,
    private val backupApi: OpenFoodFactsBackupApi,
    private val engine: HalalClassificationEngine,
    private val historyDao: ScanHistoryDao,
    private val favouriteDao: FavouriteDao,
    private val cacheDao: CachedProductDao,
    private val networkMonitor: NetworkMonitor
) {
    private val dtoAdapter = NetworkModule.moshi.adapter(OffProductDto::class.java)

    val scanHistory: Flow<List<ScanHistoryEntity>> = historyDao.observeAll()
    val recentScans: Flow<List<ScanHistoryEntity>> = historyDao.observeRecent(5)
    val favourites: Flow<List<FavouriteEntity>> = favouriteDao.observeAll()

    fun observeIsFavourite(barcode: String): Flow<Boolean> = favouriteDao.observeIsFavourite(barcode)

    suspend fun lookupBarcode(
        barcode: String,
        forceRefresh: Boolean = false,
        recordInHistory: Boolean = true
    ): ProductLookupResult = withContext(Dispatchers.IO) {
        val online = networkMonitor.isOnline()

        if (!online) {
            val cached = loadFromCache(barcode)
            return@withContext if (cached != null) {
                ProductLookupResult.Found(cached)
            } else {
                ProductLookupResult.OfflineNotCached(barcode)
            }
        }

        var primaryError: String? = null
        var primaryReturnedNotFound = false

        // 1. Primary live lookup: Halal Open Food Facts (https://halalopenfoodfacts.org/)
        try {
            val response = api.getProductByBarcode(barcode)
            if (response.isSuccessful) {
                val body = response.body()
                val dto = body?.product
                if (body?.status == 1 && dto != null) {
                    val safeDto = if (dto.code.isNullOrBlank()) dto.copy(code = barcode) else dto
                    cacheProductDto(barcode, safeDto)
                    val product = mapDtoToProduct(
                        dto = safeDto,
                        isFromCache = false,
                        fetchedAt = System.currentTimeMillis(),
                        dataSource = ProductDataSource.HALAL_OPEN_FOOD_FACTS
                    )
                    if (recordInHistory) saveScanHistory(product)
                    syncFavouriteIfPresent(product)
                    return@withContext ProductLookupResult.Found(product)
                } else {
                    primaryReturnedNotFound = true
                }
            } else if (response.code() == 404) {
                primaryReturnedNotFound = true
            } else {
                primaryError = "Primary HTTP ${response.code()}"
            }
        } catch (e: Exception) {
            primaryError = e.localizedMessage ?: "Primary service unreachable"
        }

        // 2. Automatic live backup: Open Food Facts official API v2 (https://world.openfoodfacts.org/)
        var backupError: String? = null
        var backupReturnedNotFound = false
        try {
            val backupResponse = backupApi.getProductByBarcode(barcode)
            if (backupResponse.isSuccessful) {
                val body = backupResponse.body()
                val dto = body?.product
                if (body?.status == 1 && dto != null) {
                    val safeDto = if (dto.code.isNullOrBlank()) dto.copy(code = barcode) else dto
                    cacheProductDto(barcode, safeDto)
                    val product = mapDtoToProduct(
                        dto = safeDto,
                        isFromCache = false,
                        fetchedAt = System.currentTimeMillis(),
                        dataSource = ProductDataSource.OPEN_FOOD_FACTS_BACKUP
                    )
                    if (recordInHistory) saveScanHistory(product)
                    syncFavouriteIfPresent(product)
                    return@withContext ProductLookupResult.Found(product)
                } else {
                    backupReturnedNotFound = true
                }
            } else if (backupResponse.code() == 404) {
                backupReturnedNotFound = true
            } else {
                backupError = "Backup HTTP ${backupResponse.code()}"
            }
        } catch (e: Exception) {
            backupError = e.localizedMessage ?: "Backup service unreachable"
        }

        // If at least one source cleanly confirmed not found and neither had a fatal error on both
        if ((primaryReturnedNotFound || backupReturnedNotFound) && backupError == null) {
            return@withContext ProductLookupResult.NotFound(barcode)
        }

        // 3. Offline local cache fallback if both live endpoints are temporarily unreachable
        val cached = loadFromCache(barcode)
        if (cached != null && !forceRefresh) {
            return@withContext ProductLookupResult.Found(cached)
        }

        if (primaryReturnedNotFound && backupReturnedNotFound) {
            ProductLookupResult.NotFound(barcode)
        } else {
            ProductLookupResult.Error(
                barcode = barcode,
                message = backupError ?: primaryError ?: "Service temporarily unavailable"
            )
        }
    }

    suspend fun searchProducts(
        filter: SearchFilter,
        page: Int = 1,
        pageSize: Int = 20
    ): SearchPageResult = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext SearchPageResult(
                products = emptyList(),
                page = page,
                totalCount = 0,
                hasMore = false,
                isOffline = true
            )
        }

        val query = filter.query.trim()
        if (query.isEmpty() && filter.category.isBlank() && filter.country.isBlank()) {
            return@withContext SearchPageResult(emptyList(), page, 0, false)
        }

        val effectiveQuery = buildString {
            if (query.isNotBlank()) append(query)
            if (filter.category.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(filter.category.trim())
            }
        }.ifBlank { filter.country.trim() }

        val isHalalParam = if (filter.verdictFilter == HalalVerdict.HALAL) 1 else null
        val halalStatusParam = when (filter.verdictFilter) {
            HalalVerdict.HARAM -> "haram"
            HalalVerdict.DOUBTFUL -> "doubtful"
            HalalVerdict.UNKNOWN -> "unknown"
            else -> null
        }

        var primaryFailure: String? = null

        // 1. Primary live search: Halal Open Food Facts
        try {
            val response = api.searchProducts(
                query = effectiveQuery,
                pageSize = pageSize,
                page = page,
                isHalal = isHalalParam,
                halalStatus = halalStatusParam,
                country = filter.country.trim().takeIf { it.isNotBlank() },
                category = filter.category.trim().takeIf { it.isNotBlank() }
            )

            if (response.isSuccessful) {
                val body = response.body()
                val dtos = body?.products.orEmpty()
                val now = System.currentTimeMillis()
                val mapped = dtos
                    .filter { !it.code.isNullOrBlank() }
                    .map { dto ->
                        cacheProductDto(dto.code!!, dto)
                        mapDtoToProduct(
                            dto = dto,
                            isFromCache = false,
                            fetchedAt = now,
                            dataSource = ProductDataSource.HALAL_OPEN_FOOD_FACTS
                        )
                    }
                    .filter { product ->
                        filter.verdictFilter == null || product.assessment.finalVerdict == filter.verdictFilter
                    }

                val total = body?.count ?: mapped.size
                val hasMore = (page * pageSize) < total && dtos.size >= pageSize

                return@withContext SearchPageResult(
                    products = mapped,
                    page = page,
                    totalCount = total,
                    hasMore = hasMore,
                    usedBackupSource = false
                )
            } else {
                primaryFailure = "Primary HTTP ${response.code()}"
            }
        } catch (e: Exception) {
            primaryFailure = e.localizedMessage ?: "Primary search unavailable"
        }

        // 2. Automatic live backup search: Open Food Facts official search API
        try {
            val backupResponse = backupApi.searchProducts(
                searchTerms = effectiveQuery,
                pageSize = pageSize,
                page = page,
                country = filter.country.trim().takeIf { it.isNotBlank() },
                category = filter.category.trim().takeIf { it.isNotBlank() },
                labelsTag = if (filter.verdictFilter == HalalVerdict.HALAL) "halal" else null
            )

            if (backupResponse.isSuccessful) {
                val body = backupResponse.body()
                val dtos = body?.products.orEmpty()
                val now = System.currentTimeMillis()
                val mapped = dtos
                    .filter { !it.code.isNullOrBlank() }
                    .map { dto ->
                        cacheProductDto(dto.code!!, dto)
                        mapDtoToProduct(
                            dto = dto,
                            isFromCache = false,
                            fetchedAt = now,
                            dataSource = ProductDataSource.OPEN_FOOD_FACTS_BACKUP
                        )
                    }
                    .filter { product ->
                        filter.verdictFilter == null || product.assessment.finalVerdict == filter.verdictFilter
                    }

                val total = body?.count ?: mapped.size
                val hasMore = (page * pageSize) < total && dtos.size >= pageSize

                return@withContext SearchPageResult(
                    products = mapped,
                    page = page,
                    totalCount = total,
                    hasMore = hasMore,
                    usedBackupSource = true
                )
            } else {
                SearchPageResult(
                    products = emptyList(),
                    page = page,
                    totalCount = 0,
                    hasMore = false,
                    errorMessage = "HTTP ${backupResponse.code()}"
                )
            }
        } catch (e: Exception) {
            SearchPageResult(
                products = emptyList(),
                page = page,
                totalCount = 0,
                hasMore = false,
                errorMessage = e.localizedMessage ?: primaryFailure ?: "Search failed"
            )
        }
    }

    suspend fun toggleFavourite(product: Product): Boolean = withContext(Dispatchers.IO) {
        val exists = favouriteDao.isFavourite(product.barcode)
        if (exists) {
            favouriteDao.deleteByBarcode(product.barcode)
            false
        } else {
            favouriteDao.upsert(
                FavouriteEntity(
                    barcode = product.barcode,
                    productName = product.name.ifBlank { product.barcode },
                    brands = product.brands,
                    imageUrl = product.imageUrl,
                    lastKnownVerdict = product.assessment.finalVerdict.name,
                    lastVerifiedMillis = product.fetchedAtMillis
                )
            )
            true
        }
    }

    suspend fun removeFavourite(barcode: String) = withContext(Dispatchers.IO) {
        favouriteDao.deleteByBarcode(barcode)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDao.clearAll()
    }

    suspend fun getCachedCount(): Int = withContext(Dispatchers.IO) {
        cacheDao.count()
    }

    suspend fun getCachedProduct(barcode: String): Product? = withContext(Dispatchers.IO) {
        loadFromCache(barcode)
    }

    fun mapDtoToProduct(
        dto: OffProductDto,
        isFromCache: Boolean,
        fetchedAt: Long,
        dataSource: ProductDataSource = if (isFromCache) ProductDataSource.OFFLINE_CACHE else ProductDataSource.HALAL_OPEN_FOOD_FACTS
    ): Product {
        val barcode = dto.code.orEmpty()
        val assessment = engine.analyzeProduct(
            ClassificationInput(
                ingredientsText = dto.ingredientsText,
                ingredientsTextEn = dto.ingredientsTextEn,
                ingredientsTextFr = dto.ingredientsTextFr,
                ingredientsTags = dto.ingredientsTags.orEmpty(),
                additivesTags = dto.additivesTags.orEmpty(),
                labelsTags = dto.labelsTags.orEmpty(),
                ingredientsAnalysisTags = dto.ingredientsAnalysisTags.orEmpty(),
                upstreamHalalStatus = dto.halalStatus,
                upstreamIsHalal = dto.resolveUpstreamIsHalal(),
                lastModifiedTimestamp = dto.lastModifiedT,
                isFromCache = isFromCache
            )
        )

        val resolvedName = listOf(dto.productName, dto.productNameEn, dto.productNameFr)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()

        return Product(
            barcode = barcode,
            name = resolvedName,
            nameEn = dto.productNameEn,
            nameFr = dto.productNameFr,
            brands = dto.brands.orEmpty(),
            imageUrl = dto.resolveImageUrl(),
            ingredientsText = dto.ingredientsText,
            ingredientsTextEn = dto.ingredientsTextEn,
            ingredientsTextFr = dto.ingredientsTextFr,
            ingredientsTags = dto.ingredientsTags.orEmpty(),
            additivesTags = dto.additivesTags.orEmpty(),
            allergensTags = dto.allergensTags.orEmpty(),
            labelsTags = dto.labelsTags.orEmpty(),
            categoriesTags = dto.categoriesTags.orEmpty(),
            countriesTags = dto.countriesTags.orEmpty(),
            ingredientsAnalysisTags = dto.ingredientsAnalysisTags.orEmpty(),
            upstreamHalalStatus = dto.halalStatus,
            upstreamIsHalal = dto.resolveUpstreamIsHalal(),
            productUrl = dto.productUrl ?: dto.url,
            lastModifiedTimestamp = dto.lastModifiedT,
            nutritionGrade = dto.nutriscoreGrade,
            fetchedAtMillis = fetchedAt,
            isFromCache = isFromCache,
            dataSource = dataSource,
            assessment = assessment
        )
    }

    private suspend fun saveScanHistory(product: Product) {
        historyDao.upsert(
            ScanHistoryEntity(
                barcode = product.barcode,
                productName = product.name.ifBlank { product.barcode },
                brands = product.brands,
                imageUrl = product.imageUrl,
                verdict = product.assessment.finalVerdict.name,
                evidenceQuality = product.assessment.evidenceQuality.name,
                scannedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun syncFavouriteIfPresent(product: Product) {
        if (favouriteDao.isFavourite(product.barcode)) {
            favouriteDao.upsert(
                FavouriteEntity(
                    barcode = product.barcode,
                    productName = product.name.ifBlank { product.barcode },
                    brands = product.brands,
                    imageUrl = product.imageUrl,
                    lastKnownVerdict = product.assessment.finalVerdict.name,
                    lastVerifiedMillis = product.fetchedAtMillis
                )
            )
        }
    }

    private suspend fun cacheProductDto(barcode: String, dto: OffProductDto) {
        runCatching {
            val json = dtoAdapter.toJson(dto)
            cacheDao.upsert(
                CachedProductEntity(
                    barcode = barcode,
                    rawProductJson = json,
                    cachedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun loadFromCache(barcode: String): Product? {
        val entity = cacheDao.getByBarcode(barcode) ?: return null
        val dto = runCatching { dtoAdapter.fromJson(entity.rawProductJson) }.getOrNull() ?: return null
        return mapDtoToProduct(dto, isFromCache = true, fetchedAt = entity.cachedAtMillis)
    }
}
