package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.database.FavouriteEntity
import com.example.database.ScanHistoryEntity
import com.example.domain.HalalVerdict
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductSummaryCard
import com.example.ui.components.formatTimestamp

@Composable
fun HistoryScreen(
    history: List<ScanHistoryEntity>,
    favouritesCount: Int,
    onOpenProductBarcode: (String) -> Unit,
    onOpenFavourites: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val isFr = isFrenchLocale()
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("history_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tStr(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("history_clear_button")
                    ) {
                        Text(tStr(R.string.history_clear))
                    }
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = onOpenFavourites,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_open_favourites_button")
            ) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("${tStr(R.string.favourites_title)} ($favouritesCount)")
            }
        }

        if (history.isEmpty()) {
            item {
                EmptyStateView(
                    title = tStr(R.string.history_title),
                    subtitle = tStr(R.string.history_empty),
                    actionLabel = tStr(R.string.home_scan_cta),
                    onAction = onOpenScanner
                )
            }
        } else {
            items(history, key = { it.barcode }) { item ->
                val verdict = runCatching { HalalVerdict.valueOf(item.verdict) }
                    .getOrDefault(HalalVerdict.UNKNOWN)
                ProductSummaryCard(
                    title = item.productName,
                    subtitle = item.brands.ifBlank { item.barcode },
                    barcode = item.barcode,
                    imageUrl = item.imageUrl,
                    verdict = verdict,
                    footerText = formatTimestamp(item.scannedAtMillis, isFr),
                    onClick = { onOpenProductBarcode(item.barcode) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(tStr(R.string.history_clear)) },
            text = { Text(tStr(R.string.history_clear_confirm)) },
            confirmButton = {
                Button(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) {
                    Text(tStr(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(tStr(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    favourites: List<FavouriteEntity>,
    onOpenProductBarcode: (String) -> Unit,
    onRemoveFavourite: (String) -> Unit,
    onBack: () -> Unit
) {
    val isFr = isFrenchLocale()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tStr(R.string.favourites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("favourites_back_button")) {
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
                .testTag("favourites_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favourites.isEmpty()) {
                item {
                    EmptyStateView(
                        title = tStr(R.string.favourites_title),
                        subtitle = tStr(R.string.favourites_empty)
                    )
                }
            } else {
                items(favourites, key = { it.barcode }) { fav ->
                    val verdict = runCatching { HalalVerdict.valueOf(fav.lastKnownVerdict) }
                        .getOrDefault(HalalVerdict.UNKNOWN)
                    ProductSummaryCard(
                        title = fav.productName,
                        subtitle = fav.brands.ifBlank { fav.barcode },
                        barcode = fav.barcode,
                        imageUrl = fav.imageUrl,
                        verdict = verdict,
                        footerText = if (isFr) {
                            "Vérifié : ${formatTimestamp(fav.lastVerifiedMillis, true)}"
                        } else {
                            "Verified: ${formatTimestamp(fav.lastVerifiedMillis, false)}"
                        },
                        onClick = { onOpenProductBarcode(fav.barcode) },
                        trailingAction = {
                            IconButton(onClick = { onRemoveFavourite(fav.barcode) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = tStr(R.string.detail_favourite_remove),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
