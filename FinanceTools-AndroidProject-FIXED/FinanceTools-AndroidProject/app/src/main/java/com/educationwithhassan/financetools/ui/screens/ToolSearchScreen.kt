package com.educationwithhassan.financetools.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.domain.model.FinanceCategory
import com.educationwithhassan.financetools.domain.model.ToolCatalog

@Composable
fun ToolSearchScreen(onOpenTool: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    val results = remember(query, selectedCategory) { ToolCatalog.search(query, selectedCategory) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search 120 calculators by name, keyword, or category...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (120)") }
                )
            }
            items(FinanceCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text("${cat.shortTitle} (${cat.expectedCount})") }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("${results.size} tools found", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No matching financial tools found for \"$query\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.id }) { tool ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTool(tool.id) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("#${tool.number} · ${tool.name}", style = MaterialTheme.typography.titleMedium)
                            Text(tool.shortDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryToolsScreen(
    category: FinanceCategory,
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit
) {
    val tools = remember(category) { ToolCatalog.forCategory(category) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleLarge)
                Text("${tools.size} calculators · ${category.description}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("Back") }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools, key = { it.id }) { tool ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTool(tool.id) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("#${tool.number} · ${tool.name}", style = MaterialTheme.typography.titleMedium)
                        Text(tool.shortDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
