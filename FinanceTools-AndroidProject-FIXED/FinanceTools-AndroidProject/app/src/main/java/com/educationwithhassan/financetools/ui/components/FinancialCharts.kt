package com.educationwithhassan.financetools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.educationwithhassan.financetools.domain.model.ChartSlice

private val ChartPalette = listOf(
    Color(0xFF0B1F3A),
    Color(0xFF059669),
    Color(0xFF3B82F6),
    Color(0xFFD97706),
    Color(0xFF6366F1),
    Color(0xFF64748B)
)

@Composable
fun DonutBreakdownChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier
) {
    val positive = slices.filter { it.value > 0.0 }
    val total = positive.sumOf { it.value }
    if (positive.isEmpty() || total <= 0.0) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            var startAngle = -90f
            positive.forEachIndexed { index, slice ->
                val sweep = ((slice.value / total) * 360.0).toFloat()
                drawArc(
                    color = ChartPalette[index % ChartPalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24f, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            positive.forEachIndexed { index, slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ChartPalette[index % ChartPalette.size])
                    )
                    Text(
                        text = "${slice.label} (${String.format("%.1f%%", (slice.value / total) * 100.0)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
