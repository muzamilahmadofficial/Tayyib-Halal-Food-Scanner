package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.core.BarcodeValidator
import com.example.data.ProductLookupResult
import com.example.database.FavouriteEntity
import com.example.database.ScanHistoryEntity
import com.example.domain.AlternativeRecommendation
import com.example.domain.HalalVerdict
import com.example.domain.ManualCheckResult
import com.example.domain.Product
import com.example.domain.SearchFilter
import com.example.domain.TaxonomyEntry
import com.example.localization.AppLanguage
import com.example.settings.AppThemeMode
import com.example.settings.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScannerUiState {
    data object Ready : ScannerUiState
    data class Loading(val barcode: String) : ScannerUiState
    data class ProductFound(val product: Product) : ScannerUiState
    data class NotFound(val barcode: String) : ScannerUiState
    data class OfflineNotCached(val barcode: String) : ScannerUiState
    data class Error(val barcode: String, val message: String) : ScannerUiState
}

sealed interface ProductDetailUiState {
    data object Empty : ProductDetailUiState
    data class Loading(val barcode: String) : ProductDetailUiState
    data class Loaded(val product: Product) : ProductDetailUiState
    data class NotFound(val barcode: String) : ProductDetailUiState
    data class OfflineNotCached(val barcode: String) : ProductDetailUiState
    data class Error(val barcode: String, val message: String) : ProductDetailUiState
}

data class SearchUiState(
    val filter: SearchFilter = SearchFilter(),
    val results: List<Product> = emptyList(),
    val page: Int = 1,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

data class AlternativesUiState(
    val sourceProduct: Product? = null,
    val query: String = "",
    val country: String = "",
    val recommendations: List<AlternativeRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val isOffline: Boolean = false
)

data class HomeDiscoverUiState(
    val selectedCategory: String = "biscuits",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val usedBackupSource: Boolean = false
)

class TayyibViewModel(private val container: AppContainer) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = container.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val recentScans: StateFlow<List<ScanHistoryEntity>> = container.productRepository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = container.productRepository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favourites: StateFlow<List<FavouriteEntity>> = container.productRepository.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taxonomyEntries: List<TaxonomyEntry> = container.taxonomy.entries
    val taxonomyVersion: String = container.taxonomy.schemaVersion

    private val _cachedCount = MutableStateFlow(0)
    val cachedCount: StateFlow<Int> = _cachedCount.asStateFlow()

    private val _scannerState = MutableStateFlow<ScannerUiState>(ScannerUiState.Ready)
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    private val _detailState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Empty)
    val detailState: StateFlow<ProductDetailUiState> = _detailState.asStateFlow()

    private val _activeBarcode = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isActiveProductFavourite: StateFlow<Boolean> = _activeBarcode
        .flatMapLatest { code ->
            if (code == null) flowOf(false)
            else container.productRepository.observeIsFavourite(code)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _alternativesState = MutableStateFlow(AlternativesUiState())
    val alternativesState: StateFlow<AlternativesUiState> = _alternativesState.asStateFlow()

    private val _discoverState = MutableStateFlow(HomeDiscoverUiState())
    val discoverState: StateFlow<HomeDiscoverUiState> = _discoverState.asStateFlow()

    private val _checkerText = MutableStateFlow("")
    val checkerText: StateFlow<String> = _checkerText.asStateFlow()

    private val _checkerResult = MutableStateFlow<ManualCheckResult?>(null)
    val checkerResult: StateFlow<ManualCheckResult?> = _checkerResult.asStateFlow()

    private var activeScanJob: Job? = null
    private var activeDetailJob: Job? = null
    private var activeAlternativesJob: Job? = null
    private var activeDiscoverJob: Job? = null

    init {
        refreshCachedCount()
        loadDiscoverProducts(category = "biscuits")
    }

    fun loadDiscoverProducts(category: String = _discoverState.value.selectedCategory, isPullRefresh: Boolean = false) {
        activeDiscoverJob?.cancel()
        activeDiscoverJob = viewModelScope.launch {
            _discoverState.update {
                it.copy(
                    selectedCategory = category,
                    isLoading = !isPullRefresh && it.products.isEmpty() || category != it.selectedCategory,
                    isRefreshing = isPullRefresh,
                    isOffline = false
                )
            }
            val result = container.productRepository.searchProducts(
                filter = SearchFilter(query = category, verdictFilter = HalalVerdict.HALAL),
                page = 1,
                pageSize = 14
            )
            _discoverState.update {
                it.copy(
                    selectedCategory = category,
                    products = result.products.take(12),
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = result.isOffline,
                    usedBackupSource = result.usedBackupSource
                )
            }
        }
    }

    fun refreshDiscover() {
        loadDiscoverProducts(category = _discoverState.value.selectedCategory, isPullRefresh = true)
    }

    fun ensureAlternativesPopulated() {
        val current = _alternativesState.value
        if (!current.hasSearched && !current.isLoading && current.sourceProduct == null) {
            searchAlternatives(categoryQuery = "biscuits")
        }
    }

    fun isOnline(): Boolean = container.networkMonitor.isOnline()

    fun refreshCachedCount() {
        viewModelScope.launch {
            _cachedCount.value = container.productRepository.getCachedCount()
        }
    }

    // --- Scanner Operations ---

    fun onBarcodeScanned(rawBarcode: String, onNavigateToDetail: (String) -> Unit) {
        val barcode = BarcodeValidator.normalizeAndValidate(rawBarcode) ?: return
        if (_scannerState.value is ScannerUiState.Loading) return

        val settings = userSettings.value
        container.soundFeedbackManager.playScanConfirm(settings.soundEnabled)
        container.hapticFeedbackManager.vibrateScanConfirm(settings.vibrationEnabled)

        activeScanJob?.cancel()
        activeScanJob = viewModelScope.launch {
            _scannerState.value = ScannerUiState.Loading(barcode)
            when (val result = container.productRepository.lookupBarcode(barcode, forceRefresh = false, recordInHistory = true)) {
                is ProductLookupResult.Found -> {
                    val product = result.product
                    _activeBarcode.value = product.barcode
                    _detailState.value = ProductDetailUiState.Loaded(product)
                    _scannerState.value = ScannerUiState.ProductFound(product)

                    // Play verdict-specific audio & haptics strictly AFTER classification finishes
                    val latestSettings = userSettings.value
                    container.soundFeedbackManager.playVerdict(product.assessment.finalVerdict, latestSettings.soundEnabled)
                    container.hapticFeedbackManager.vibrateForVerdict(product.assessment.finalVerdict, latestSettings.vibrationEnabled)

                    refreshCachedCount()
                    if (product.assessment.finalVerdict == HalalVerdict.HARAM ||
                        product.assessment.finalVerdict == HalalVerdict.DOUBTFUL
                    ) {
                        preloadAlternativesFor(product)
                    }
                    onNavigateToDetail(product.barcode)
                }

                is ProductLookupResult.NotFound -> {
                    _scannerState.value = ScannerUiState.NotFound(result.barcode)
                    val latestSettings = userSettings.value
                    container.soundFeedbackManager.playVerdict(HalalVerdict.UNKNOWN, latestSettings.soundEnabled)
                    container.hapticFeedbackManager.vibrateForVerdict(HalalVerdict.UNKNOWN, latestSettings.vibrationEnabled)
                }

                is ProductLookupResult.OfflineNotCached -> {
                    _scannerState.value = ScannerUiState.OfflineNotCached(result.barcode)
                }

                is ProductLookupResult.Error -> {
                    _scannerState.value = ScannerUiState.Error(result.barcode, result.message)
                }
            }
        }
    }

    fun cancelActiveScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        _scannerState.value = ScannerUiState.Ready
    }

    fun resetScannerState() {
        _scannerState.value = ScannerUiState.Ready
    }

    // --- Product Details ---

    fun openProduct(product: Product) {
        _activeBarcode.value = product.barcode
        _detailState.value = ProductDetailUiState.Loaded(product)
        if (product.assessment.finalVerdict == HalalVerdict.HARAM ||
            product.assessment.finalVerdict == HalalVerdict.DOUBTFUL
        ) {
            preloadAlternativesFor(product)
        }
    }

    fun loadProductByBarcode(barcode: String, forceRefresh: Boolean = false) {
        _activeBarcode.value = barcode
        val current = _detailState.value
        if (!forceRefresh && current is ProductDetailUiState.Loaded && current.product.barcode == barcode) {
            return
        }

        activeDetailJob?.cancel()
        activeDetailJob = viewModelScope.launch {
            _detailState.value = ProductDetailUiState.Loading(barcode)
            when (val res = container.productRepository.lookupBarcode(barcode, forceRefresh = forceRefresh, recordInHistory = forceRefresh)) {
                is ProductLookupResult.Found -> {
                    _detailState.value = ProductDetailUiState.Loaded(res.product)
                    refreshCachedCount()
                    if (res.product.assessment.finalVerdict == HalalVerdict.HARAM ||
                        res.product.assessment.finalVerdict == HalalVerdict.DOUBTFUL
                    ) {
                        preloadAlternativesFor(res.product)
                    }
                }
                is ProductLookupResult.NotFound -> _detailState.value = ProductDetailUiState.NotFound(barcode)
                is ProductLookupResult.OfflineNotCached -> _detailState.value = ProductDetailUiState.OfflineNotCached(barcode)
                is ProductLookupResult.Error -> _detailState.value = ProductDetailUiState.Error(barcode, res.message)
            }
        }
    }

    fun toggleFavourite(product: Product) {
        viewModelScope.launch {
            container.productRepository.toggleFavourite(product)
        }
    }

    fun removeFavourite(barcode: String) {
        viewModelScope.launch {
            container.productRepository.removeFavourite(barcode)
        }
    }

    // --- Alternatives ---

    fun preloadAlternativesFor(product: Product) {
        activeAlternativesJob?.cancel()
        activeAlternativesJob = viewModelScope.launch {
            _alternativesState.update {
                it.copy(
                    sourceProduct = product,
                    isLoading = true,
                    hasSearched = true,
                    isOffline = !container.networkMonitor.isOnline()
                )
            }
            if (!container.networkMonitor.isOnline()) {
                _alternativesState.update { it.copy(isLoading = false, recommendations = emptyList(), isOffline = true) }
                return@launch
            }
            val recs = container.alternativesEngine.findAlternativesForProduct(product)
            _alternativesState.update {
                it.copy(
                    recommendations = recs,
                    isLoading = false,
                    isOffline = false
                )
            }
        }
    }

    fun searchAlternatives(categoryQuery: String, country: String = "") {
        activeAlternativesJob?.cancel()
        activeAlternativesJob = viewModelScope.launch {
            _alternativesState.update {
                it.copy(
                    sourceProduct = null,
                    query = categoryQuery,
                    country = country,
                    isLoading = true,
                    hasSearched = true,
                    isOffline = false
                )
            }
            val (recs, offline) = container.alternativesEngine.searchAlternativesByCategory(categoryQuery, country)
            _alternativesState.update {
                it.copy(
                    recommendations = recs,
                    isLoading = false,
                    isOffline = offline
                )
            }
        }
    }

    // --- Product Search ---

    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(filter = it.filter.copy(query = query)) }
    }

    fun updateSearchCountry(country: String) {
        _searchState.update { it.copy(filter = it.filter.copy(country = country)) }
    }

    fun updateSearchCategory(category: String) {
        _searchState.update { it.copy(filter = it.filter.copy(category = category)) }
    }

    fun updateSearchVerdictFilter(verdict: HalalVerdict?) {
        _searchState.update { it.copy(filter = it.filter.copy(verdictFilter = verdict)) }
        if (_searchState.value.hasSearched) {
            executeSearch(resetPage = true)
        }
    }

    fun executeSearch(resetPage: Boolean = true) {
        val current = _searchState.value
        val filter = current.filter
        val trimmedQuery = filter.query.trim()

        // If user entered a direct barcode in Search, look it up directly
        if (resetPage && BarcodeValidator.isValidManualBarcode(trimmedQuery) && filter.category.isBlank() && filter.country.isBlank()) {
            viewModelScope.launch {
                _searchState.update { it.copy(isLoading = true, hasSearched = true, errorMessage = null, isOffline = false) }
                when (val res = container.productRepository.lookupBarcode(trimmedQuery, recordInHistory = false)) {
                    is ProductLookupResult.Found -> {
                        val matchesFilter = filter.verdictFilter == null || res.product.assessment.finalVerdict == filter.verdictFilter
                        _searchState.update {
                            it.copy(
                                results = if (matchesFilter) listOf(res.product) else emptyList(),
                                page = 1,
                                totalCount = if (matchesFilter) 1 else 0,
                                hasMore = false,
                                isLoading = false
                            )
                        }
                    }
                    is ProductLookupResult.NotFound -> {
                        _searchState.update { it.copy(results = emptyList(), totalCount = 0, hasMore = false, isLoading = false) }
                    }
                    is ProductLookupResult.OfflineNotCached -> {
                        _searchState.update { it.copy(results = emptyList(), isLoading = false, isOffline = true) }
                    }
                    is ProductLookupResult.Error -> {
                        _searchState.update { it.copy(results = emptyList(), isLoading = false, errorMessage = res.message) }
                    }
                }
            }
            return
        }

        val targetPage = if (resetPage) 1 else current.page + 1
        viewModelScope.launch {
            _searchState.update {
                it.copy(
                    isLoading = resetPage,
                    isLoadingMore = !resetPage,
                    hasSearched = true,
                    errorMessage = null,
                    isOffline = false
                )
            }
            val result = container.productRepository.searchProducts(filter, page = targetPage)
            _searchState.update { state ->
                state.copy(
                    results = if (resetPage) result.products else (state.results + result.products).distinctBy { it.barcode },
                    page = result.page,
                    totalCount = result.totalCount,
                    hasMore = result.hasMore,
                    isLoading = false,
                    isLoadingMore = false,
                    isOffline = result.isOffline,
                    errorMessage = result.errorMessage
                )
            }
            refreshCachedCount()
        }
    }

    // --- Offline Manual Ingredient Checker ---

    fun updateCheckerText(text: String) {
        _checkerText.value = text
    }

    fun analyzeManualIngredients() {
        val text = _checkerText.value
        val assessment = container.classificationEngine.analyzeManualText(text)
        _checkerResult.value = ManualCheckResult(inputText = text, assessment = assessment)
    }

    fun applyScannedIngredients(extractedText: String) {
        val cleaned = extractedText.trim()
        if (cleaned.isEmpty()) return
        _checkerText.value = cleaned
        val assessment = container.classificationEngine.analyzeManualText(cleaned)
        _checkerResult.value = ManualCheckResult(inputText = cleaned, assessment = assessment)

        val settings = userSettings.value
        container.soundFeedbackManager.playVerdict(assessment.finalVerdict, settings.soundEnabled)
        container.hapticFeedbackManager.vibrateForVerdict(assessment.finalVerdict, settings.vibrationEnabled)
    }

    fun clearChecker() {
        _checkerText.value = ""
        _checkerResult.value = null
    }

    // --- Settings & Local Storage Controls ---

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setVibrationEnabled(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { container.productRepository.clearHistory() }
    }

    fun clearCache() {
        viewModelScope.launch {
            container.productRepository.clearCache()
            refreshCachedCount()
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TayyibViewModel(container) as T
        }
    }
}
