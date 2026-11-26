package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.database.ScanHistoryEntity
import com.example.domain.HalalVerdict
import com.example.domain.Product
import com.example.localization.AppLanguage
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FeaturedProductCard
import com.example.ui.components.HorizontalProductCardSkeleton
import com.example.ui.components.ProductSummaryCard
import com.example.ui.components.TayyibLogoMark
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.TayyibDarkGreen
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory
import com.example.ui.viewmodel.HomeDiscoverUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentScans: List<ScanHistoryEntity>,
    discoverState: HomeDiscoverUiState,
    currentLanguage: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    onSelectDiscoverCategory: (String) -> Unit,
    onRefreshDiscover: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenIngredientChecker: () -> Unit,
    onOpenENumberGuide: () -> Unit,
    onOpenAlternatives: () -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProductBarcode: (String) -> Unit
) {
    val isFr = isFrenchLocale()
    val discoverCategories = listOf(
        "biscuits" to if (isFr) "Biscuits" else "Biscuits",
        "chocolate" to if (isFr) "Chocolat" else "Chocolate",
        "beverages" to if (isFr) "Boissons" else "Beverages",
        "cheese" to if (isFr) "Fromage" else "Cheese",
        "cereals" to if (isFr) "Céréales" else "Cereals",
        "candies" to if (isFr) "Bonbons" else "Candies"
    )

    PullToRefreshBox(
        isRefreshing = discoverState.isRefreshing,
        onRefresh = onRefreshDiscover,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Brand Header with fixed-size EN/FR segmented toggle (zero layout shift on language switch)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TayyibLogoMark(size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tayyib",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tStr(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                LanguageSegmentedToggle(
                    isFrench = isFr,
                    onSelectLanguage = onToggleLanguage
                )
            }
        }

        // Editorial Hero Scan Card with Generated Artwork
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = TayyibForestGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1795716315617),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        TayyibDarkGreen.copy(alpha = 0.88f),
                                        TayyibForestGreen.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = tStr(R.string.home_welcome_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = TayyibGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tStr(R.string.home_welcome_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TayyibIvory,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tStr(R.string.home_welcome_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TayyibIvory.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onOpenScanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("home_scan_cta_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TayyibIvory,
                                contentColor = TayyibDarkGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tStr(R.string.home_scan_cta),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Search Bar Shortcut
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenSearch)
                    .testTag("home_search_bar"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = tStr(R.string.home_tool_search),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tStr(R.string.home_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Live Dynamic Halal Products Carousel (real data from Halal Open Food Facts + Open Food Facts backup)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tStr(R.string.home_discover_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = tStr(R.string.home_discover_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onOpenAlternatives) {
                        Text(tStr(R.string.home_see_all))
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("home_discover_categories")
                ) {
                    items(discoverCategories, key = { it.first }) { (queryKey, label) ->
                        FilterChip(
                            selected = discoverState.selectedCategory.equals(queryKey, ignoreCase = true),
                            onClick = { onSelectDiscoverCategory(queryKey) },
                            label = { Text(label) }
                        )
                    }
                }

                when {
                    discoverState.isLoading -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(3) {
                                HorizontalProductCardSkeleton()
                            }
                        }
                    }

                    discoverState.isOffline -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = tStr(R.string.scanner_offline_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            )
                        }
                    }

                    discoverState.products.isNotEmpty() -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("home_discover_carousel")
                        ) {
                            items(discoverState.products, key = { it.barcode }) { product ->
                                FeaturedProductCard(
                                    title = product.displayName(isFr),
                                    brand = product.brands,
                                    imageUrl = product.imageUrl,
                                    verdict = product.assessment.finalVerdict,
                                    supportingNote = product.barcode,
                                    onClick = { onOpenProduct(product) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Core Tools 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tStr(R.string.home_tools_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeToolTile(
                        title = tStr(R.string.home_tool_checker),
                        subtitle = if (isFr) "Scan Lens & texte hors ligne" else "Lens OCR & label check",
                        icon = Icons.Outlined.FactCheck,
                        onClick = onOpenIngredientChecker,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_checker")
                    )
                    HomeToolTile(
                        title = tStr(R.string.home_tool_enumbers),
                        subtitle = if (isFr) "Base E100–E1510" else "Offline additive guide",
                        icon = Icons.Outlined.Science,
                        onClick = onOpenENumberGuide,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_enumbers")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeToolTile(
                        title = tStr(R.string.home_tool_alternatives),
                        subtitle = if (isFr) "Produits similaires" else "Similar Halal products",
                        icon = Icons.Outlined.SwapHoriz,
                        onClick = onOpenAlternatives,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_alternatives")
                    )
                    HomeToolTile(
                        title = tStr(R.string.home_tool_favourites),
                        subtitle = if (isFr) "Vos produits sauvés" else "Saved for quick access",
                        icon = Icons.Outlined.BookmarkBorder,
                        onClick = onOpenFavourites,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_favourites")
                    )
                }
            }
        }

        // Recently Scanned Products
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tStr(R.string.home_recent_scans),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (recentScans.isNotEmpty()) {
                    TextButton(onClick = onOpenHistory) {
                        Text(tStr(R.string.home_see_all))
                    }
                }
            }
        }

        if (recentScans.isEmpty()) {
            item {
                EmptyStateView(
                    title = tStr(R.string.home_recent_scans),
                    subtitle = tStr(R.string.home_empty_recent),
                    actionLabel = tStr(R.string.home_tool_checker),
                    onAction = onOpenIngredientChecker
                )
            }
        } else {
            items(recentScans, key = { it.barcode }) { scan ->
                val verdict = runCatching { HalalVerdict.valueOf(scan.verdict) }
                    .getOrDefault(HalalVerdict.UNKNOWN)
                ProductSummaryCard(
                    title = scan.productName,
                    subtitle = scan.brands.ifBlank { scan.barcode },
                    barcode = scan.barcode,
                    imageUrl = scan.imageUrl,
                    verdict = verdict,
                    footerText = formatTimestamp(scan.scannedAtMillis, isFr),
                    onClick = { onOpenProductBarcode(scan.barcode) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
    }
}

@Composable
private fun HomeToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LanguageSegmentedToggle(
    isFrench: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val enBg by animateColorAsState(
        targetValue = if (!isFrench) TayyibForestGreen else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "lang_en_bg"
    )
    val frBg by animateColorAsState(
        targetValue = if (isFrench) TayyibForestGreen else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "lang_fr_bg"
    )
    val enText by animateColorAsState(
        targetValue = if (!isFrench) TayyibIvory else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "lang_en_text"
    )
    val frText by animateColorAsState(
        targetValue = if (isFrench) TayyibIvory else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "lang_fr_text"
    )

    // Fixed width ensures the header never wraps or shifts when switching between EN and FR
    Box(
        modifier = Modifier
            .height(48.dp)
            .testTag("home_language_toggle"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .width(88.dp)
                .height(36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(enBg)
                        .clickable { onSelectLanguage(AppLanguage.ENGLISH) }
                        .testTag("home_lang_en"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (!isFrench) FontWeight.Bold else FontWeight.Medium,
                        color = enText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(frBg)
                        .clickable { onSelectLanguage(AppLanguage.FRENCH) }
                        .testTag("home_lang_fr"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isFrench) FontWeight.Bold else FontWeight.Medium,
                        color = frText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
