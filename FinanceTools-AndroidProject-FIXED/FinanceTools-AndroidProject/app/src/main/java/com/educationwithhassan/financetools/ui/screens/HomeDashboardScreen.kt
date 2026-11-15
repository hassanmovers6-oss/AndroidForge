package com.educationwithhassan.financetools.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.ads.NativeAdCard
import com.educationwithhassan.financetools.domain.model.FinanceCategory
import com.educationwithhassan.financetools.domain.model.ToolCatalog

@Composable
fun HomeDashboardScreen(
    recentIds: List<String>,
    favoriteIds: List<String>,
    onOpenTool: (String) -> Unit,
    onOpenCategory: (FinanceCategory) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val popularTools = ToolCatalog.allTools.filter { it.popular }
    val recentTools = recentIds.mapNotNull { ToolCatalog.byId[it] }
    val favoriteTools = favoriteIds.mapNotNull { ToolCatalog.byId[it] }
    val categoriesFirstChunk = FinanceCategory.entries.take(6)
    val categoriesSecondChunk = FinanceCategory.entries.drop(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column {
                        Text("Finance Tools", style = MaterialTheme.typography.titleLarge)
                        Text("120 Offline-Ready Calculators", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSearch() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                    Text("Search all 120 tools (e.g. loan, zakat, profit)...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickShortcutCard(
                    title = "Currency FX",
                    subtitle = "Convert 15+ currencies",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenTool("currency-converter") }
                )
                QuickShortcutCard(
                    title = "Invoice PDF",
                    subtitle = "Create & export PDF",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenTool("invoice-generator") }
                )
            }
        }

        if (favoriteTools.isNotEmpty()) {
            item {
                Text("Favorite Tools", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(favoriteTools) { tool ->
                        ElevatedCard(
                            modifier = Modifier
                                .width(175.dp)
                                .clickable { onOpenTool(tool.id) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(tool.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(tool.category.shortTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (recentTools.isNotEmpty()) {
            item {
                Text("Recently Used", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentTools) { tool ->
                        OutlinedCard(
                            modifier = Modifier
                                .width(175.dp)
                                .clickable { onOpenTool(tool.id) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(tool.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text("#${tool.number} · ${tool.category.shortTitle}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Popular Calculators", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(popularTools) { tool ->
                    ElevatedCard(
                        modifier = Modifier
                            .width(190.dp)
                            .clickable { onOpenTool(tool.id) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(tool.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(tool.shortDescription, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Native Advanced Ad placement #1 (naturally separated after Popular Calculators)
        item { NativeAdCard() }

        item { Text("All 10 Finance Categories", style = MaterialTheme.typography.titleMedium) }

        items(categoriesFirstChunk) { category ->
            CategoryCard(category = category, onClick = { onOpenCategory(category) })
        }

        // Native Advanced Ad placement #2 (every 6-8 cards across category feed)
        item { NativeAdCard() }

        items(categoriesSecondChunk) { category ->
            CategoryCard(category = category, onClick = { onOpenCategory(category) })
        }
    }
}

@Composable
private fun CategoryCard(category: FinanceCategory, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(category.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Text("${category.expectedCount} tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun QuickShortcutCard(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }
    }
}
