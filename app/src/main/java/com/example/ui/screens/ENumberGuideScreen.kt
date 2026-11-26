package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.IngredientCategory
import com.example.domain.TaxonomyEntry
import com.example.halalengine.TextNormalizer
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IngredientCategoryChip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ENumberGuideScreen(
    entries: List<TaxonomyEntry>,
    ruleVersion: String,
    onBack: () -> Unit
) {
    val isFr = isFrenchLocale()
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<IngredientCategory?>(null) }

    val filtered = remember(entries, query, selectedCategory) {
        val normQ = TextNormalizer.normalize(query)
        entries.filter { entry ->
            val categoryMatch = selectedCategory == null || entry.category == selectedCategory
            val queryMatch = normQ.isBlank() ||
                TextNormalizer.normalize(entry.nameEn).contains(normQ) ||
                TextNormalizer.normalize(entry.nameFr).contains(normQ) ||
                (entry.eNumber != null && TextNormalizer.normalize(entry.eNumber).contains(normQ)) ||
                entry.alternativeSpellings.any { TextNormalizer.normalize(it).contains(normQ) }
            categoryMatch && queryMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tStr(R.string.enumber_title))
                        Text(
                            text = if (isFr) "Base hors ligne · v$ruleVersion" else "Offline Database · v$ruleVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("enumber_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tStr(R.string.action_back)
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
                .testTag("enumber_guide_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(tStr(R.string.enumber_search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enumber_search_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(tStr(R.string.enumber_filter_all)) }
                        )
                        FilterChip(
                            selected = selectedCategory == IngredientCategory.CLEARLY_PROHIBITED,
                            onClick = { selectedCategory = IngredientCategory.CLEARLY_PROHIBITED },
                            label = { Text(if (isFr) "Interdit" else "Prohibited") }
                        )
                        FilterChip(
                            selected = selectedCategory == IngredientCategory.SOURCE_DEPENDENT,
                            onClick = { selectedCategory = IngredientCategory.SOURCE_DEPENDENT },
                            label = { Text(if (isFr) "Selon origine" else "Source-Dependent") }
                        )
                        FilterChip(
                            selected = selectedCategory == IngredientCategory.REQUIRES_VERIFICATION,
                            onClick = { selectedCategory = IngredientCategory.REQUIRES_VERIFICATION },
                            label = { Text(if (isFr) "À vérifier" else "Verify") }
                        )
                        FilterChip(
                            selected = selectedCategory == IngredientCategory.GENERALLY_PERMISSIBLE,
                            onClick = { selectedCategory = IngredientCategory.GENERALLY_PERMISSIBLE },
                            label = { Text(if (isFr) "Permis" else "Permissible") }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateView(
                        title = tStr(R.string.search_no_results),
                        subtitle = if (isFr) {
                            "Essayez un autre code (ex. E120, E471, E441) ou un nom d'ingrédient."
                        } else {
                            "Try another E-number (e.g. E120, E471, E441) or ingredient name."
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    ENumberCard(entry = entry, isFr = isFr)
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun ENumberCard(entry: TaxonomyEntry, isFr: Boolean) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!entry.eNumber.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = entry.eNumber,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isFr) entry.nameFr else entry.nameEn,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IngredientCategoryChip(category = entry.category)
            }

            // Show alternate language name so international users can match bilingual packaging
            Text(
                text = if (isFr) "EN : ${entry.nameEn}" else "FR: ${entry.nameFr}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${tStr(R.string.enumber_function)}: ${if (isFr) entry.functionFr else entry.functionEn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${tStr(R.string.enumber_origin)}: ${if (isFr) entry.typicalOriginFr else entry.typicalOriginEn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (entry.alternativeOriginsEn.isNotBlank() && entry.alternativeOriginsEn != "None") {
                Text(
                    text = "${tStr(R.string.enumber_alternatives)}: ${if (isFr) entry.alternativeOriginsFr else entry.alternativeOriginsEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = if (isFr) entry.explanationFr else entry.explanationEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
