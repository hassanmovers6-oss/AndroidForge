package com.educationwithhassan.financetools.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.domain.engine.CurrencyRatesProvider
import com.educationwithhassan.financetools.domain.model.*
import com.educationwithhassan.financetools.ui.components.DonutBreakdownChart
import com.educationwithhassan.financetools.ui.components.InvoicePdfExporter

@Composable
fun CalculatorDetailScreen(
    tool: FinanceTool,
    defaultCurrency: String,
    isFavorite: Boolean,
    isAdvancedPdfUnlocked: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onCalculate: (Map<String, String>, List<DynamicRow>, String) -> CalculationOutput,
    onSave: (Map<String, String>, String, CalculationOutput) -> Boolean,
    onWatchAdForPdfUnlock: () -> Unit,
    onWatchAdForBonusSlots: () -> Unit
) {
    val context = LocalContext.current
    var currency by remember(tool.id) { mutableStateOf(defaultCurrency) }
    val inputs = remember(tool.id) {
        mutableStateMapOf<String, String>().apply {
            tool.fields.forEach { put(it.key, it.defaultValue) }
        }
    }
    val dynamicRows = remember(tool.id) {
        mutableStateListOf<DynamicRow>().apply { addAll(tool.defaultRows) }
    }
    var result by remember(tool.id) { mutableStateOf<CalculationOutput?>(null) }
    var showPdfRewardDialog by remember { mutableStateOf(false) }
    var showSlotLimitDialog by remember { mutableStateOf(false) }
    var saveFeedback by remember(tool.id) { mutableStateOf<String?>(null) }

    if (showPdfRewardDialog) {
        AlertDialog(
            onDismissRequest = { showPdfRewardDialog = false },
            title = { Text("Unlock PDF Financial Report") },
            text = {
                Text(
                    "Watch one short video ad to unlock full PDF Financial Report exports across all 120 calculators for 24 hours. All calculators and Invoice/Receipt PDFs remain free."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPdfRewardDialog = false
                    onWatchAdForPdfUnlock()
                }) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfRewardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSlotLimitDialog) {
        AlertDialog(
            onDismissRequest = { showSlotLimitDialog = false },
            title = { Text("Saved History Limit Reached") },
            text = {
                Text(
                    "Your current saved calculation slots are full. You can clear older items in History or watch a short rewarded video ad to unlock +15 additional saved slots."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSlotLimitDialog = false
                    onWatchAdForBonusSlots()
                }) {
                    Text("Watch Ad (+15 Slots)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSlotLimitDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column {
                    Text(tool.name, style = MaterialTheme.typography.titleLarge)
                    Text("#${tool.number} · ${tool.category.shortTitle}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(tool.shortDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (tool.supportsCurrency) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text("Currency: $currency (${CurrencyRatesProvider.symbolFor(currency).trim()})")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CurrencyRatesProvider.supportedCurrencies.forEach { (code, sym) ->
                        DropdownMenuItem(text = { Text("$code ($sym)") }, onClick = { currency = code; expanded = false })
                    }
                }
            }
        }

        tool.fields.forEach { field ->
            if (field.kind == InputKind.SELECT) {
                var expanded by remember { mutableStateOf(false) }
                val currentLabel = field.options.firstOrNull { it.value == inputs[field.key] }?.label ?: field.defaultValue
                Column {
                    Text(field.label, style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(currentLabel)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            field.options.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt.label) }, onClick = { inputs[field.key] = opt.value; expanded = false })
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = inputs[field.key] ?: "",
                    onValueChange = { inputs[field.key] = it },
                    label = { Text(field.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (field.kind == InputKind.TEXT || field.kind == InputKind.DATE) KeyboardType.Text else KeyboardType.Decimal
                    )
                )
            }
        }

        if (tool.listKind != DynamicListKind.NONE) {
            Text("Entries (${dynamicRows.size})", style = MaterialTheme.typography.titleMedium)
            dynamicRows.forEachIndexed { idx, row ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = row.label,
                            onValueChange = { dynamicRows[idx] = row.copy(label = it) },
                            label = { Text("Label / Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.amount,
                                onValueChange = { dynamicRows[idx] = row.copy(amount = it) },
                                label = { Text("Amount / Value") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            TextButton(onClick = { if (dynamicRows.size > 1) dynamicRows.removeAt(idx) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = {
                dynamicRows.add(DynamicRow("r_${System.currentTimeMillis()}", "New Entry", "100", "1", "General", ""))
            }) {
                Text("+ Add Row")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    saveFeedback = null
                    result = onCalculate(inputs.toMap(), dynamicRows.toList(), currency)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Calculate")
            }
            OutlinedButton(
                onClick = {
                    tool.fields.forEach { inputs[it.key] = it.defaultValue }
                    dynamicRows.clear(); dynamicRows.addAll(tool.defaultRows)
                    saveFeedback = null
                    result = null
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset")
            }
        }

        result?.let { out ->
            if (!out.valid) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
                    Text(out.errorMessage ?: "Invalid inputs.", modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error)
                }
            } else {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(out.primaryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(out.primaryFormatted, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)

                        out.metrics.forEach { m ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(m.label, style = MaterialTheme.typography.bodyMedium)
                                Text(m.formattedValue, style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        if (out.chartSlices.isNotEmpty()) {
                            HorizontalDivider()
                            DonutBreakdownChart(out.chartSlices)
                        }

                        if (out.tableRows.isNotEmpty()) {
                            HorizontalDivider()
                            Text(out.tableHeaders.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
                            out.tableRows.take(12).forEach { r ->
                                Text(r.joinToString("  |  "), style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        out.notes.forEach { note ->
                            Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        saveFeedback?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val saved = onSave(inputs.toMap(), currency, out)
                                if (saved) {
                                    saveFeedback = "Saved to History."
                                } else {
                                    showSlotLimitDialog = true
                                }
                            }) {
                                Text("Save")
                            }
                            OutlinedButton(onClick = {
                                val text = "${tool.name}\n${out.primaryLabel}: ${out.primaryFormatted}\n" +
                                    out.metrics.joinToString("\n") { "${it.label}: ${it.formattedValue}" }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Result"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                            if (tool.supportsPdfExport || isAdvancedPdfUnlocked) {
                                OutlinedButton(onClick = { InvoicePdfExporter.exportAndSharePdf(context, tool, out) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF")
                                }
                            } else {
                                OutlinedButton(onClick = { showPdfRewardDialog = true }) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF (Ad)")
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Formula Explanation", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(tool.formulaExplanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
