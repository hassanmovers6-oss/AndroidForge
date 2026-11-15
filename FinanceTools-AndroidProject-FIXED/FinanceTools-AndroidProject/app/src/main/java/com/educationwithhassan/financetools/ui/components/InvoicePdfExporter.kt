package com.educationwithhassan.financetools.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.educationwithhassan.financetools.domain.model.CalculationOutput
import com.educationwithhassan.financetools.domain.model.FinanceTool
import java.io.File
import java.io.FileOutputStream

object InvoicePdfExporter {
    fun exportAndSharePdf(context: Context, tool: FinanceTool, output: CalculationOutput) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0B1F3A") }
        val bodyPaint = Paint().apply { textSize = 12f; color = android.graphics.Color.parseColor("#1E293B") }
        val accentPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#059669") }

        var y = 60f
        canvas.drawText(tool.name, 40f, y, titlePaint)
        y += 28f
        canvas.drawText("${output.primaryLabel}: ${output.primaryFormatted}", 40f, y, accentPaint)
        y += 26f

        output.metrics.forEach { m ->
            canvas.drawText("${m.label}: ${m.formattedValue}", 40f, y, bodyPaint)
            y += 18f
        }
        y += 12f
        output.tableRows.take(20).forEach { row ->
            canvas.drawText(row.joinToString("  |  "), 40f, y, bodyPaint)
            y += 16f
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "${tool.id}_export.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${tool.name} PDF"))
    }
}
