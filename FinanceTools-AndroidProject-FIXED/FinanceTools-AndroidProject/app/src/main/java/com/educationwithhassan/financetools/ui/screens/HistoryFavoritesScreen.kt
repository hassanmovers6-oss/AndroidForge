package com.educationwithhassan.financetools.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.data.local.SavedCalculationEntity

@Composable
fun HistoryFavoritesScreen(
    history: List<SavedCalculationEntity>,
    savedSlotsLimit: Int,
    onOpenTool: (String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onWatchAdForBonusSlots: () -> Unit
) {
    var showSlotDialog by remember { mutableStateOf(false) }

    if (showSlotDialog) {
        AlertDialog(
            onDismissRequest = { showSlotDialog = false },
            title = { Text("Unlock +15 Saved Calculation Slots") },
            text = {
                Text(
                    "Watch one optional video ad to permanently add +15 saved calculation slots to your offline history (currently ${history.size} / $savedSlotsLimit used)."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSlotDialog = false
                    onWatchAdForBonusSlots()
                }) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSlotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Saved Calculation History", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Slots used: ${history.size} / $savedSlotsLimit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) { Text("Clear All") }
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Need more saved slots?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional: Watch a video ad to add +15 offline history slots.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showSlotDialog = true }) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+15 Slots")
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved calculations yet. Save any result to view it offline here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history, key = { it.id }) { item ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { onOpenTool(item.toolId) }) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.toolName, style = MaterialTheme.typography.titleMedium)
                                Text("${item.primaryLabel}: ${item.primaryResult}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                if (item.summaryDetails.isNotEmpty()) {
                                    Text(item.summaryDetails, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAboutPrivacyScreen(
    darkMode: Boolean,
    defaultCurrency: String,
    fxStatus: String,
    isAdvancedPdfUnlocked: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangeCurrency: (String) -> Unit,
    onSyncFx: () -> Unit,
    onWatchAdForPdfUnlock: () -> Unit,
    onOpenAdDiagnostics: () -> Unit
) {
    var showPdfUnlockDialog by remember { mutableStateOf(false) }

    if (showPdfUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showPdfUnlockDialog = false },
            title = { Text("Unlock PDF Financial Reports") },
            text = {
                Text(
                    "Watch one optional video ad to unlock full PDF Financial Report exports for all 120 calculators for 24 hours."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPdfUnlockDialog = false
                    onWatchAdForPdfUnlock()
                }) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfUnlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings & Preferences", style = MaterialTheme.typography.titleLarge)

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                    Text("Toggle light or dark fintech theme", style = MaterialTheme.typography.labelMedium)
                }
                Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default Currency ($defaultCurrency)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USD", "PKR", "GBP", "EUR", "SAR", "AED", "INR").forEach { code ->
                        FilterChip(selected = defaultCurrency == code, onClick = { onChangeCurrency(code) }, label = { Text(code) })
                    }
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exchange Rates", style = MaterialTheme.typography.titleMedium)
                Text(fxStatus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onSyncFx) { Text("Sync Live FX Rates") }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Optional PDF Report Pass", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (isAdvancedPdfUnlocked) "Active (24-hour PDF report exports unlocked across all 120 tools)."
                    else "Optional: Watch a rewarded ad to unlock full PDF report export on all 120 calculators for 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isAdvancedPdfUnlocked) {
                    OutlinedButton(onClick = { showPdfUnlockDialog = true }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Watch Ad to Unlock PDF Reports")
                    }
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AdMob SDK Diagnostics", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Inspect SDK initialization, Test vs Production environment, loaded status, and last error per ad format.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onOpenAdDiagnostics) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open AdMob Diagnostics")
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About & Privacy Policy", style = MaterialTheme.typography.titleMedium)
                Text("Finance Tools – All-in-One Calculator (com.educationwithhassan.financetools) stores all saved calculations, favorites, and preferences locally on your device using Room and DataStore. We never collect bank credentials, card numbers, or passwords.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
