package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.Product
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductSummaryCard
import com.example.ui.components.ProductSummaryCardSkeleton
import com.example.ui.components.cleanOffTag
import com.example.ui.viewmodel.AlternativesUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlternativesScreen(
    state: AlternativesUiState,
    onEnsureLoaded: () -> Unit,
    onSearchCategory: (String, String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onSaveFavourite: (Product) -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val isFr = isFrenchLocale()
    var query by remember(state.query) { mutableStateOf(state.query) }
    var country by remember(state.country) { mutableStateOf(state.country) }

    LaunchedEffect(Unit) {
        onEnsureLoaded()
    }

    val quickCategories = if (isFr) {
        listOf("biscuits", "chocolat", "bonbons", "fromage", "boissons", "céréales")
    } else {
        listOf("biscuits", "chocolate", "candies", "cheese", "beverages", "cereals")
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.recommendations.isNotEmpty(),
        onRefresh = {
            onSearchCategory(query.ifBlank { "biscuits" }, country)
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("alternatives_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = tStr(R.string.alternatives_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tStr(R.string.alternatives_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.sourceProduct != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isFr) {
                                "Alternatives suggérées pour : ${state.sourceProduct.displayName(true)}"
                            } else {
                                "Alternatives for: ${state.sourceProduct.displayName(false)}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFr) {
                                "Chaque produit ci-dessous provient de Halal Open Food Facts et est réévalué par le moteur Tayyib."
                            } else {
                                "Every candidate below is retrieved from Halal Open Food Facts and independently evaluated by Tayyib."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tStr(R.string.alternatives_search_category)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { onSearchCategory(query, country) },
                            modifier = Modifier.testTag("alternatives_search_button")
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = tStr(R.string.search_title))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchCategory(query, country) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alternatives_query_input")
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    placeholder = { Text(tStr(R.string.search_country_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.forEach { cat ->
                        FilterChip(
                            selected = query.equals(cat, ignoreCase = true),
                            onClick = {
                                query = cat
                                onSearchCategory(cat, country)
                            },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        when {
            state.isLoading -> {
                items(4) {
                    ProductSummaryCardSkeleton()
                }
            }

            state.isOffline -> {
                item {
                    EmptyStateView(
                        title = if (isFr) "Connexion requise" else "Internet Required",
                        subtitle = if (isFr) {
                            "La recherche d'alternatives Halal en ligne nécessite un accès Internet."
                        } else {
                            "Searching live Halal alternatives from Halal Open Food Facts requires an internet connection."
                        }
                    )
                }
            }

            state.hasSearched && state.recommendations.isEmpty() -> {
                item {
                    EmptyStateView(
                        title = tStr(R.string.alternatives_empty),
                        subtitle = if (isFr) {
                            "Tayyib n'invente jamais de produits fictifs. Essayez une autre catégorie ou scannez un autre article."
                        } else {
                            "Tayyib never fabricates products. Try a broader food category or scan another product."
                        },
                        actionLabel = tStr(R.string.home_scan_cta),
                        onAction = onOpenScanner
                    )
                }
            }

            !state.hasSearched -> {
                item {
                    EmptyStateView(
                        title = tStr(R.string.alternatives_title),
                        subtitle = if (isFr) {
                            "Choisissez une catégorie ci-dessus ou scannez un produit Haram / Douteux pour voir des alternatives Halal disponibles."
                        } else {
                            "Pick a food category above or scan a Haram / Doubtful product to explore available Halal alternatives."
                        },
                        actionLabel = tStr(R.string.home_scan_cta),
                        onAction = onOpenScanner
                    )
                }
            }

            else -> {
                items(state.recommendations, key = { it.product.barcode }) { rec ->
                    val prod = rec.product
                    val countriesLabel = prod.countriesTags.take(2).joinToString(", ") { cleanOffTag(it) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProductSummaryCard(
                            title = prod.displayName(isFr),
                            subtitle = buildString {
                                if (prod.brands.isNotBlank()) append(prod.brands)
                                if (countriesLabel.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(countriesLabel)
                                }
                            },
                            barcode = prod.barcode,
                            imageUrl = prod.imageUrl,
                            verdict = prod.assessment.finalVerdict,
                            footerText = if (isFr) rec.supportingEvidenceFr else rec.supportingEvidenceEn,
                            onClick = { onOpenProduct(prod) },
                            trailingAction = {
                                Row {
                                    IconButton(onClick = { onSaveFavourite(prod) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkBorder,
                                            contentDescription = tStr(R.string.detail_favourite_add),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        val share = "${prod.displayName(isFr)} (${prod.barcode}) — ${if (isFr) rec.supportingEvidenceFr else rec.supportingEvidenceEn}"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, share)
                                        }
                                        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = tStr(R.string.detail_share),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
    }
}
