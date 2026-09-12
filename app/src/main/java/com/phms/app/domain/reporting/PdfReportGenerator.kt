package com.phms.app.domain.reporting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.phms.app.domain.calculator.PnLSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {
    fun generatePnLReportPdf(context: Context, pnl: PnLSummary): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
        }

        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 2f
        }

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = df.format(Date())

        var y = 60f
        canvas.drawText("PIG HOUSE MANAGEMENT SYSTEM (PHMS)", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Monthly Financial & Profitability Report", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Generated on: $dateStr", 40f, y, textPaint)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 40f

        titlePaint.textSize = 16f
        canvas.drawText("FINANCIAL SUMMARY", 40f, y, titlePaint)
        y += 30f

        canvas.drawText("Total Sales Revenue: KSh ${String.format("%.2f", pnl.totalRevenue)}", 40f, y, textPaint)
        y += 25f
        canvas.drawText("Feed Expenses: KSh ${String.format("%.2f", pnl.feedCost)}", 40f, y, textPaint)
        y += 25f
        canvas.drawText("Health & Vet Expenses: KSh ${String.format("%.2f", pnl.healthCost)}", 40f, y, textPaint)
        y += 25f
        canvas.drawText("Labor Expenses (Est.): KSh ${String.format("%.2f", pnl.estimatedLaborCost)}", 40f, y, textPaint)
        y += 35f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        val netColor = if (pnl.netProfit >= 0) Color.parseColor("#2E7D32") else Color.RED
        val netPaint = Paint().apply {
            color = netColor
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText("Net Profit / Loss: KSh ${String.format("%.2f", pnl.netProfit)}", 40f, y, netPaint)
        y += 40f

        titlePaint.textSize = 16f
        canvas.drawText("HERD KPI METRICS", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Active Herd Inventory: ${pnl.activePigCount} pigs", 40f, y, textPaint)
        y += 25f
        canvas.drawText("Average Production Cost per Pig: KSh ${String.format("%.2f", pnl.costPerPig)}", 40f, y, textPaint)

        document.finishPage(page)

        val file = File(context.cacheDir, "PHMS_Financial_Report_${System.currentTimeMillis()}.pdf")
        try {
            document.writeTo(FileOutputStream(file))
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }
}
