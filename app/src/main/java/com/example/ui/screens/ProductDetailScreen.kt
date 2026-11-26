package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.AnalyzedIngredient
import com.example.domain.HalalVerdict
import com.example.domain.Product
import com.example.domain.ProductDataSource
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.EvidenceQualityChip
import com.example.ui.components.FeaturedProductCard
import com.example.ui.components.HorizontalProductCardSkeleton
import com.example.ui.components.IngredientCategoryChip
import com.example.ui.components.ReportIssueDialog
import com.example.ui.components.VerdictBadge
import com.example.ui.components.cleanOffTag
import com.example.ui.components.formatTimestamp
import com.example.ui.components.launchReportEmailDraft
import com.example.ui.theme.LocalTayyibColors
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibIvory
import com.example.ui.viewmodel.AlternativesUiState
import com.example.ui.viewmodel.ProductDetailUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    detailState: ProductDetailUiState,
    alternativesState: AlternativesUiState,
    isFavourite: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: (Product) -> Unit,
    onRecheckOnline: (String) -> Unit,
    onFindAlternatives: (Product) -> Unit,
    onOpenAlternative: (Product) -> Unit,
    onOpenMethodology: () -> Unit
) {
    val context = LocalContext.current
    val isFr = isFrenchLocale()
    var showReportDialog by remember { mutableStateOf(false) }

    val loadedProduct = (detailState as? ProductDetailUiState.Loaded)?.product

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tStr(R.string.detail_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tStr(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (loadedProduct != null) {
                        IconButton(
                            onClick = { onToggleFavourite(loadedProduct) },
                            modifier = Modifier.testTag("detail_favourite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavourite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isFavourite) tStr(R.string.detail_favourite_remove) else tStr(R.string.detail_favourite_add),
                                tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("${loadedProduct.displayName(isFr)} (${loadedProduct.barcode})")
                                    appendLine("Tayyib Assessment: ${loadedProduct.assessment.finalVerdict.name}")
                                    appendLine(if (isFr) loadedProduct.assessment.explanationFr else loadedProduct.assessment.explanationEn)
                                    append("Source: Halal Open Food Facts / Tayyib: Halal Food Scanner")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                            },
                            modifier = Modifier.testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = tStr(R.string.detail_share)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("product_detail_screen")
        ) {
            when (detailState) {
                is ProductDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TayyibForestGreen)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(tStr(R.string.scanner_loading), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                is ProductDetailUiState.NotFound -> {
                    EmptyStateView(
                        title = tStr(R.string.scanner_not_found_title),
                        subtitle = "${tStr(R.string.scanner_not_found_desc)}\n\n${detailState.barcode}",
                        actionLabel = tStr(R.string.action_retry),
                        onAction = { onRecheckOnline(detailState.barcode) }
                    )
                }

                is ProductDetailUiState.OfflineNotCached -> {
                    EmptyStateView(
                        title = if (isFr) "Produit non mis en cache" else "Not Available Offline",
                        subtitle = if (isFr) {
                            "Une connexion Internet est nécessaire pour consulter ce produit pour la première fois."
                        } else {
                            "An internet connection is required to fetch this product for the first time."
                        },
                        actionLabel = tStr(R.string.action_retry),
                        onAction = { onRecheckOnline(detailState.barcode) }
                    )
                }

                is ProductDetailUiState.Error -> {
                    EmptyStateView(
                        title = if (isFr) "Erreur réseau" else "Unable to Load Product",
                        subtitle = detailState.message,
                        actionLabel = tStr(R.string.action_retry),
                        onAction = { onRecheckOnline(detailState.barcode) }
                    )
                }

                ProductDetailUiState.Empty -> {
                    EmptyStateView(
                        title = tStr(R.string.detail_title),
                        subtitle = tStr(R.string.home_empty_recent)
                    )
                }

                is ProductDetailUiState.Loaded -> {
                    val product = detailState.product
                    val assessment = product.assessment
                    val semantic = LocalTayyibColors.current
                    val accent = semantic.accentFor(assessment.finalVerdict)
                    val container = semantic.containerFor(assessment.finalVerdict)

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Offline Cache Warning Banner
                        if (product.isFromCache) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = semantic.doubtfulContainer,
                                    border = BorderStroke(1.dp, semantic.doubtful.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = semantic.doubtful
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${tStr(R.string.detail_cached_warning)} (${formatTimestamp(product.fetchedAtMillis, isFr)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Product Identity Header Card
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!product.imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = product.imageUrl,
                                                contentDescription = product.displayName(isFr),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(88.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Inventory2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.displayName(isFr),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (product.brands.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = product.brands,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${tStr(R.string.detail_barcode)}: ${product.barcode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (product.countriesTags.isNotEmpty()) {
                                            val countries = product.countriesTags.take(3).joinToString(", ") { cleanOffTag(it) }
                                            Text(
                                                text = "${tStr(R.string.detail_countries)}: $countries",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Primary Verdict Hero Banner with smooth reveal animation
                        item {
                            var visible by remember(product.barcode) { mutableStateOf(false) }
                            LaunchedEffect(product.barcode) { visible = true }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = container),
                                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            VerdictBadge(verdict = assessment.finalVerdict, large = true)
                                            Text(
                                                text = "v${assessment.ruleVersion}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        EvidenceQualityChip(quality = assessment.evidenceQuality)

                                        Text(
                                            text = if (isFr) assessment.explanationFr else assessment.explanationEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (assessment.hasConflictingEvidence) {
                                            val conflictText = if (isFr) assessment.conflictNoteFr else assessment.conflictNoteEn
                                            if (!conflictText.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.WarningAmber,
                                                            contentDescription = null,
                                                            tint = semantic.doubtful,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = conflictText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Inline Suggested Halal Alternatives directly on Haram or Doubtful product page
                        if (assessment.finalVerdict == HalalVerdict.HARAM ||
                            assessment.finalVerdict == HalalVerdict.DOUBTFUL
                        ) {
                            val matchingAlts = alternativesState.sourceProduct?.barcode == product.barcode
                            val inlineRecs = if (matchingAlts) alternativesState.recommendations else emptyList()
                            val loadingAlts = matchingAlts && alternativesState.isLoading

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_inline_alternatives_section"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tStr(R.string.detail_inline_alternatives_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        TextButton(
                                            onClick = { onFindAlternatives(product) },
                                            modifier = Modifier.testTag("detail_see_all_alternatives")
                                        ) {
                                            Text(tStr(R.string.home_see_all))
                                        }
                                    }

                                    when {
                                        loadingAlts -> {
                                            Text(
                                                text = tStr(R.string.detail_inline_alternatives_loading),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(3) {
                                                    HorizontalProductCardSkeleton()
                                                }
                                            }
                                        }

                                        inlineRecs.isNotEmpty() -> {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.testTag("detail_inline_alternatives_carousel")
                                            ) {
                                                items(inlineRecs.take(10), key = { it.product.barcode }) { rec ->
                                                    FeaturedProductCard(
                                                        title = rec.product.displayName(isFr),
                                                        brand = rec.product.brands,
                                                        imageUrl = rec.product.imageUrl,
                                                        verdict = rec.product.assessment.finalVerdict,
                                                        supportingNote = if (isFr) rec.supportingEvidenceFr else rec.supportingEvidenceEn,
                                                        onClick = { onOpenAlternative(rec.product) }
                                                    )
                                                }
                                            }
                                        }

                                        else -> {
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(
                                                        text = tStr(R.string.alternatives_empty),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Button(
                                                        onClick = { onFindAlternatives(product) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(48.dp)
                                                            .testTag("detail_find_alternatives_button"),
                                                        shape = RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = TayyibForestGreen,
                                                            contentColor = TayyibIvory
                                                        )
                                                    ) {
                                                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(tStr(R.string.detail_find_alternatives), fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4-Pillar Assessment Transparency Breakdown
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = tStr(R.string.detail_evidence_breakdown),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    PillarRow(
                                        label = tStr(R.string.detail_community_assessment),
                                        detail = if (isFr) assessment.communityPillar.summaryFr else assessment.communityPillar.summaryEn,
                                        verdict = assessment.communityPillar.verdictHint
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    PillarRow(
                                        label = tStr(R.string.detail_ingredient_analysis),
                                        detail = if (isFr) assessment.ingredientPillar.summaryFr else assessment.ingredientPillar.summaryEn,
                                        verdict = assessment.ingredientPillar.verdictHint
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    PillarRow(
                                        label = tStr(R.string.detail_certification_check),
                                        detail = if (isFr) assessment.certificationPillar.summaryFr else assessment.certificationPillar.summaryEn,
                                        verdict = assessment.certificationPillar.verdictHint
                                    )
                                }
                            }
                        }

                        // Detected Additives & Flagged Ingredients
                        if (assessment.matchedIngredients.isNotEmpty()) {
                            item {
                                Text(
                                    text = tStr(R.string.detail_additives),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(assessment.matchedIngredients, key = { it.entry.id }) { matched ->
                                DetectedIngredientCard(matched = matched, isFr = isFr)
                            }
                        }

                        // Full Ingredient List
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tStr(R.string.detail_ingredients),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = product.displayIngredients(isFr) ?: tStr(R.string.detail_ingredients_missing),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Allergens
                        if (product.allergensTags.isNotEmpty()) {
                            item {
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = tStr(R.string.detail_allergens),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            product.allergensTags.distinct().forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = cleanOffTag(tag),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Data Source, Freshness & Actions
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tStr(R.string.detail_data_source),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val sourceText = when (product.dataSource) {
                                        ProductDataSource.HALAL_OPEN_FOOD_FACTS -> if (isFr) {
                                            "Source principale : Halal Open Food Facts (ODbL). Tayyib est indépendant et ne remplace pas la vérification de l'emballage."
                                        } else {
                                            "Primary source: Halal Open Food Facts (ODbL). Tayyib is independent and does not replace checking the physical package."
                                        }
                                        ProductDataSource.OPEN_FOOD_FACTS_BACKUP -> if (isFr) {
                                            "Source de secours automatique : Open Food Facts (world.openfoodfacts.org • ODbL). Halal Open Food Facts étant indisponible ou sans fiche pour ce code, Tayyib a automatiquement interrogé Open Food Facts et analysé les ingrédients localement."
                                        } else {
                                            "Automatic backup source: Open Food Facts (world.openfoodfacts.org • ODbL). Halal Open Food Facts was unavailable or did not list this barcode, so Tayyib automatically retrieved it from Open Food Facts and analyzed the ingredients locally."
                                        }
                                        ProductDataSource.OFFLINE_CACHE -> if (isFr) {
                                            "Source : Cache local hors ligne (données précédemment récupérées depuis Halal Open Food Facts / Open Food Facts • ODbL)."
                                        } else {
                                            "Source: Local offline cache (previously retrieved from Halal Open Food Facts / Open Food Facts • ODbL)."
                                        }
                                    }
                                    Text(
                                        text = sourceText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    product.lastModifiedTimestamp?.let { ts ->
                                        Text(
                                            text = if (isFr) {
                                                "Dernière mise à jour source : ${formatTimestamp(ts * 1000L, true)}"
                                            } else {
                                                "Source last updated: ${formatTimestamp(ts * 1000L, false)}"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onRecheckOnline(product.barcode) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("detail_recheck_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(tStr(R.string.detail_recheck), maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { showReportDialog = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("detail_report_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(tStr(R.string.detail_report_issue), maxLines = 1)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onOpenMethodology,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tStr(R.string.legal_methodology))
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    if (showReportDialog && loadedProduct != null) {
        ReportIssueDialog(
            barcode = loadedProduct.barcode,
            productName = loadedProduct.displayName(isFr),
            onDismiss = { showReportDialog = false },
            onLaunchEmailDraft = { type, notes ->
                showReportDialog = false
                launchReportEmailDraft(
                    context = context,
                    barcode = loadedProduct.barcode,
                    productName = loadedProduct.displayName(isFr),
                    issueType = type,
                    notes = notes,
                    isFrench = isFr
                )
            }
        )
    }
}

@Composable
private fun PillarRow(
    label: String,
    detail: String,
    verdict: HalalVerdict
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerdictBadge(verdict = verdict)
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetectedIngredientCard(
    matched: AnalyzedIngredient,
    isFr: Boolean
) {
    val entry = matched.entry
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFr) entry.nameFr else entry.nameEn,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IngredientCategoryChip(category = entry.category)
            }
            Text(
                text = "${if (isFr) "Origine" else "Origin"}: ${if (isFr) entry.typicalOriginFr else entry.typicalOriginEn}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isFr) entry.explanationFr else entry.explanationEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
