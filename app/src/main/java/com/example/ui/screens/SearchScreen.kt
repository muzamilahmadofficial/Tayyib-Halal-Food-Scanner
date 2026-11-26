package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.HalalVerdict
import com.example.domain.Product
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductSummaryCard
import com.example.ui.components.cleanOffTag
import com.example.ui.components.verdictLabel
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.viewmodel.SearchUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onVerdictFilterChange: (HalalVerdict?) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onBack: () -> Unit
) {
    val isFr = isFrenchLocale()
    var showAdvancedFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tStr(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tStr(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAdvancedFilters = !showAdvancedFilters },
                        modifier = Modifier.testTag("search_filter_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = if (isFr) "Filtres avancés" else "Advanced Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("search_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.filter.query,
                            onValueChange = onQueryChange,
                            placeholder = { Text(tStr(R.string.search_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_query_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onSearchSubmit,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("search_submit_button")
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = tStr(R.string.search_title))
                        }
                    }

                    if (showAdvancedFilters) {
                        OutlinedTextField(
                            value = state.filter.country,
                            onValueChange = onCountryChange,
                            placeholder = { Text(tStr(R.string.search_country_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_country_input")
                        )
                        OutlinedTextField(
                            value = state.filter.category,
                            onValueChange = onCategoryChange,
                            placeholder = { Text(tStr(R.string.search_category_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_category_input")
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = state.filter.verdictFilter == null,
                            onClick = { onVerdictFilterChange(null) },
                            label = { Text(tStr(R.string.enumber_filter_all)) }
                        )
                        HalalVerdict.entries.forEach { verdict ->
                            FilterChip(
                                selected = state.filter.verdictFilter == verdict,
                                onClick = {
                                    val next = if (state.filter.verdictFilter == verdict) null else verdict
                                    onVerdictFilterChange(next)
                                },
                                label = { Text(verdictLabel(verdict)) }
                            )
                        }
                    }
                }
            }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TayyibForestGreen)
                        }
                    }
                }

                state.isOffline -> {
                    item {
                        EmptyStateView(
                            title = if (isFr) "Mode hors ligne" else "Offline Mode",
                            subtitle = if (isFr) {
                                "Connectez-vous à Internet pour rechercher dans Halal Open Food Facts."
                            } else {
                                "Connect to the internet to search live products on Halal Open Food Facts."
                            }
                        )
                    }
                }

                state.errorMessage != null -> {
                    item {
                        EmptyStateView(
                            title = if (isFr) "Erreur de recherche" else "Search Unavailable",
                            subtitle = state.errorMessage,
                            actionLabel = tStr(R.string.action_retry),
                            onAction = onSearchSubmit
                        )
                    }
                }

                !state.hasSearched -> {
                    item {
                        EmptyStateView(
                            title = tStr(R.string.search_title),
                            subtitle = tStr(R.string.search_empty)
                        )
                    }
                }

                state.results.isEmpty() -> {
                    item {
                        EmptyStateView(
                            title = tStr(R.string.search_no_results),
                            subtitle = if (isFr) {
                                "Essayez un autre mot-clé, un autre pays ou réinitialisez le filtre de verdict."
                            } else {
                                "Try a different keyword, brand, barcode, or clear the classification filter."
                            }
                        )
                    }
                }

                else -> {
                    items(state.results, key = { it.barcode }) { product ->
                        val countryText = product.countriesTags.firstOrNull()?.let { cleanOffTag(it) }.orEmpty()
                        ProductSummaryCard(
                            title = product.displayName(isFr),
                            subtitle = buildString {
                                if (product.brands.isNotBlank()) append(product.brands)
                                if (countryText.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(countryText)
                                }
                            },
                            barcode = product.barcode,
                            imageUrl = product.imageUrl,
                            verdict = product.assessment.finalVerdict,
                            footerText = product.barcode,
                            onClick = { onOpenProduct(product) }
                        )
                    }

                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = TayyibForestGreen
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("search_load_more_button")
                                    ) {
                                        Text(tStr(R.string.search_load_more))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}
