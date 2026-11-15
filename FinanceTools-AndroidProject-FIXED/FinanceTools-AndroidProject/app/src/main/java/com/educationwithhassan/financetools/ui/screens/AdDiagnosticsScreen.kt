package com.educationwithhassan.financetools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.ads.AdDiagnostics
import com.educationwithhassan.financetools.ads.AdFormat
import com.educationwithhassan.financetools.ads.AdMobConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdDiagnosticsScreen(onBack: () -> Unit) {
    val snapshot by AdDiagnostics.snapshot.collectAsState()
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("AdMob SDK Diagnostics", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Internal status & error log (Logcat tags: FinanceTools_*)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SDK & Environment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                DiagnosticRow("Environment", snapshot.environment)
                DiagnosticRow("SDK Initialized", if (snapshot.sdkInitialized) "Yes" else "Pending")
                DiagnosticRow("Active App ID", AdMobConfig.activeAppId)
                DiagnosticRow("Full-Screen Ad Active", if (snapshot.isFullScreenAdActive) "Yes" else "No")
                Text(
                    "Adapters: ${snapshot.sdkStatusMessage}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AdFormat.entries.forEach { format ->
            val state = snapshot.formats[format]
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(format.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state?.isLoaded == true) "Loaded" else "Not Loaded",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state?.isLoaded == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DiagnosticRow("Logcat Tag", format.logTag)
                    DiagnosticRow("Last Result", state?.lastStatus ?: "Not requested yet")
                    if (state?.lastUpdatedMs != null && state.lastUpdatedMs > 0L) {
                        DiagnosticRow("Last Updated", timeFormatter.format(Date(state.lastUpdatedMs)))
                    }
                    if (state?.lastErrorCode != null || state?.lastErrorMessage != null) {
                        Text(
                            "Last Error [code=${state.lastErrorCode}]: ${state.lastErrorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Reporting Policy Note", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "This diagnostic view intentionally does not display local impression counters. Actual ad impressions, fill rates, and earnings must always be verified in the Google AdMob console.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
