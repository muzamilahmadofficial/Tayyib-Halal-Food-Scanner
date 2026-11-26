package com.example.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.legal.LegalDocumentType
import com.example.localization.tStr
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AlternativesScreen
import com.example.ui.screens.ENumberGuideScreen
import com.example.ui.screens.FavouritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IngredientCheckerScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.TayyibViewModel

object TayyibRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SCAN = "scan"
    const val ALTERNATIVES = "alternatives"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val CHECKER = "ingredient_checker"
    const val ENUMBERS = "enumber_guide"
    const val FAVOURITES = "favourites"
    const val ABOUT = "about"
    const val PRODUCT_DETAIL = "product_detail/{barcode}"
    const val LEGAL = "legal/{type}"

    fun productDetail(barcode: String) = "product_detail/$barcode"
    fun legal(type: LegalDocumentType) = "legal/${type.name}"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TopLevelTabs = listOf(
    TopLevelTab(TayyibRoutes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    TopLevelTab(TayyibRoutes.SCAN, R.string.nav_scan, Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_tab_scan"),
    TopLevelTab(TayyibRoutes.ALTERNATIVES, R.string.nav_alternatives, Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz, "nav_tab_alternatives"),
    TopLevelTab(TayyibRoutes.HISTORY, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History, "nav_tab_history"),
    TopLevelTab(TayyibRoutes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
)

@Composable
fun TayyibAppRoot(
    viewModel: TayyibViewModel,
    skipSplash: Boolean = false
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedCount.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val isActiveFav by viewModel.isActiveProductFavourite.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val alternativesState by viewModel.alternativesState.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val checkerText by viewModel.checkerText.collectAsStateWithLifecycle()
    val checkerResult by viewModel.checkerResult.collectAsStateWithLifecycle()

    val showTopLevelNav = currentRoute in TopLevelTabs.map { it.route }

    val navigateToTab: (String) -> Unit = { route ->
        if (route == TayyibRoutes.SCAN) {
            viewModel.resetScannerState()
        }
        navController.navigate(route) {
            popUpTo(TayyibRoutes.HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tStr(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tStr(tab.labelRes),
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tStr(tab.labelRes)
                                    )
                                },
                                label = { Text(tStr(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = if (skipSplash) TayyibRoutes.HOME else TayyibRoutes.SPLASH,
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) }
                    ) {
                        composable(TayyibRoutes.SPLASH) {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate(TayyibRoutes.HOME) {
                                        popUpTo(TayyibRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(TayyibRoutes.HOME) {
                            HomeScreen(
                                recentScans = recentScans,
                                discoverState = discoverState,
                                currentLanguage = settings.language,
                                onToggleLanguage = { viewModel.setLanguage(it) },
                                onSelectDiscoverCategory = { viewModel.loadDiscoverProducts(it) },
                                onRefreshDiscover = { viewModel.refreshDiscover() },
                                onOpenProduct = { product ->
                                    viewModel.openProduct(product)
                                    navController.navigate(TayyibRoutes.productDetail(product.barcode))
                                },
                                onOpenScanner = { navigateToTab(TayyibRoutes.SCAN) },
                                onOpenSearch = { navController.navigate(TayyibRoutes.SEARCH) },
                                onOpenIngredientChecker = { navController.navigate(TayyibRoutes.CHECKER) },
                                onOpenENumberGuide = { navController.navigate(TayyibRoutes.ENUMBERS) },
                                onOpenAlternatives = { navigateToTab(TayyibRoutes.ALTERNATIVES) },
                                onOpenFavourites = { navController.navigate(TayyibRoutes.FAVOURITES) },
                                onOpenHistory = { navigateToTab(TayyibRoutes.HISTORY) },
                                onOpenSettings = { navigateToTab(TayyibRoutes.SETTINGS) },
                                onOpenProductBarcode = { barcode ->
                                    viewModel.loadProductByBarcode(barcode)
                                    navController.navigate(TayyibRoutes.productDetail(barcode))
                                }
                            )
                        }

                        composable(TayyibRoutes.SCAN) {
                            ScannerScreen(
                                scannerState = scannerState,
                                isOnline = viewModel.isOnline(),
                                onBarcodeDetected = { code ->
                                    viewModel.onBarcodeScanned(code) { foundCode ->
                                        navController.navigate(TayyibRoutes.productDetail(foundCode))
                                    }
                                },
                                onCancelScan = { viewModel.cancelActiveScan() },
                                onResetScanner = { viewModel.resetScannerState() },
                                onNavigateToSearch = { code ->
                                    viewModel.updateSearchQuery(code)
                                    navController.navigate(TayyibRoutes.SEARCH)
                                },
                                onNavigateToChecker = { navController.navigate(TayyibRoutes.CHECKER) }
                            )
                        }

                        composable(TayyibRoutes.ALTERNATIVES) {
                            AlternativesScreen(
                                state = alternativesState,
                                onEnsureLoaded = { viewModel.ensureAlternativesPopulated() },
                                onSearchCategory = { q, country -> viewModel.searchAlternatives(q, country) },
                                onOpenProduct = { product ->
                                    viewModel.openProduct(product)
                                    navController.navigate(TayyibRoutes.productDetail(product.barcode))
                                },
                                onSaveFavourite = { viewModel.toggleFavourite(it) },
                                onOpenScanner = { navigateToTab(TayyibRoutes.SCAN) }
                            )
                        }

                        composable(TayyibRoutes.HISTORY) {
                            HistoryScreen(
                                history = scanHistory,
                                favouritesCount = favourites.size,
                                onOpenProductBarcode = { barcode ->
                                    viewModel.loadProductByBarcode(barcode)
                                    navController.navigate(TayyibRoutes.productDetail(barcode))
                                },
                                onOpenFavourites = { navController.navigate(TayyibRoutes.FAVOURITES) },
                                onClearHistory = { viewModel.clearHistory() },
                                onOpenScanner = { navigateToTab(TayyibRoutes.SCAN) }
                            )
                        }

                        composable(TayyibRoutes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                cachedCount = cachedCount,
                                onLanguageChange = { viewModel.setLanguage(it) },
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onSoundToggle = { viewModel.setSoundEnabled(it) },
                                onVibrationToggle = { viewModel.setVibrationEnabled(it) },
                                onClearCache = { viewModel.clearCache() },
                                onClearHistory = { viewModel.clearHistory() },
                                onOpenAbout = { navController.navigate(TayyibRoutes.ABOUT) },
                                onOpenLegal = { docType -> navController.navigate(TayyibRoutes.legal(docType)) }
                            )
                        }

                        composable(TayyibRoutes.SEARCH) {
                            SearchScreen(
                                state = searchState,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onCountryChange = { viewModel.updateSearchCountry(it) },
                                onCategoryChange = { viewModel.updateSearchCategory(it) },
                                onVerdictFilterChange = { viewModel.updateSearchVerdictFilter(it) },
                                onSearchSubmit = { viewModel.executeSearch(resetPage = true) },
                                onLoadMore = { viewModel.executeSearch(resetPage = false) },
                                onOpenProduct = { product ->
                                    viewModel.openProduct(product)
                                    navController.navigate(TayyibRoutes.productDetail(product.barcode))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(TayyibRoutes.CHECKER) {
                            IngredientCheckerScreen(
                                inputText = checkerText,
                                result = checkerResult,
                                onInputChange = { viewModel.updateCheckerText(it) },
                                onAnalyze = { viewModel.analyzeManualIngredients() },
                                onIngredientsScanned = { viewModel.applyScannedIngredients(it) },
                                onClear = { viewModel.clearChecker() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(TayyibRoutes.ENUMBERS) {
                            ENumberGuideScreen(
                                entries = viewModel.taxonomyEntries,
                                ruleVersion = viewModel.taxonomyVersion,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(TayyibRoutes.FAVOURITES) {
                            FavouritesScreen(
                                favourites = favourites,
                                onOpenProductBarcode = { barcode ->
                                    viewModel.loadProductByBarcode(barcode)
                                    navController.navigate(TayyibRoutes.productDetail(barcode))
                                },
                                onRemoveFavourite = { viewModel.removeFavourite(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(TayyibRoutes.ABOUT) {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }

                        composable(
                            route = TayyibRoutes.PRODUCT_DETAIL,
                            arguments = listOf(navArgument("barcode") { type = NavType.StringType })
                        ) { entry ->
                            val barcode = entry.arguments?.getString("barcode").orEmpty()
                            LaunchedEffect(barcode) {
                                if (barcode.isNotBlank()) {
                                    viewModel.loadProductByBarcode(barcode)
                                }
                            }
                            ProductDetailScreen(
                                detailState = detailState,
                                alternativesState = alternativesState,
                                isFavourite = isActiveFav,
                                onBack = {
                                    viewModel.resetScannerState()
                                    navController.popBackStack()
                                },
                                onToggleFavourite = { viewModel.toggleFavourite(it) },
                                onRecheckOnline = { code -> viewModel.loadProductByBarcode(code, forceRefresh = true) },
                                onFindAlternatives = { product ->
                                    viewModel.preloadAlternativesFor(product)
                                    navigateToTab(TayyibRoutes.ALTERNATIVES)
                                },
                                onOpenAlternative = { altProduct ->
                                    viewModel.openProduct(altProduct)
                                    navController.navigate(TayyibRoutes.productDetail(altProduct.barcode))
                                },
                                onOpenMethodology = {
                                    navController.navigate(TayyibRoutes.legal(LegalDocumentType.HALAL_METHODOLOGY))
                                }
                            )
                        }

                        composable(
                            route = TayyibRoutes.LEGAL,
                            arguments = listOf(navArgument("type") { type = NavType.StringType })
                        ) { entry ->
                            val typeName = entry.arguments?.getString("type")
                            val docType = runCatching { LegalDocumentType.valueOf(typeName.orEmpty()) }
                                .getOrDefault(LegalDocumentType.RELIGIOUS_DISCLAIMER)
                            LegalScreen(
                                documentType = docType,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
