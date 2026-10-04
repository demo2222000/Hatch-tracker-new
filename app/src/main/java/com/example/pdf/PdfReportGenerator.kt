package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AlertLog
import com.example.data.model.IncubationBatch
import com.example.data.model.SensorReading
import com.example.data.model.TurningLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun generateAndShareBatchReport(
        batch: IncubationBatch,
        readings: List<SensorReading>,
        turnings: List<TurningLog>,
        alerts: List<AlertLog>
    ): Intent? {
        val pdfDocument = PdfDocument()

        // Page dimensions: Standard A4 at 72dpi = 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(194, 65, 12) // Warm Amber #C2410C
            isAntiAlias = true
        }

        val textTitlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Deep Slate
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textSubTitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        val textHeaderPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textBodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            isAntiAlias = true
        }

        val textBoldBodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        var y = 40f

        // Top decorative bar
        canvas.drawRect(0f, 0f, 595f, 10f, primaryPaint)

        // Title and Subtitle
        canvas.drawText("HATCHMASTER INCUBATION REPORT", 40f, y + 20f, textTitlePaint)
        canvas.drawText(
            "Generated on: ${timeFormat.format(Date())} | Local Offline Device Record",
            40f,
            y + 36f,
            textSubTitlePaint
        )
        y += 60f

        // Batch Overview Box
        val overviewRect = RectF(40f, y, 555f, y + 105f)
        canvas.drawRoundRect(overviewRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(overviewRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("BATCH INFORMATION", 56f, y + 22f, textHeaderPaint)

        val col1X = 56f
        val col2X = 220f
        val col3X = 400f

        canvas.drawText("Batch Name:", col1X, y + 42f, textSubTitlePaint)
        canvas.drawText(batch.name, col1X, y + 56f, textBoldBodyPaint)

        canvas.drawText("Species:", col1X, y + 74f, textSubTitlePaint)
        canvas.drawText(batch.species, col1X, y + 88f, textBodyPaint)

        canvas.drawText("Start Date:", col2X, y + 42f, textSubTitlePaint)
        canvas.drawText(dateFormat.format(Date(batch.startDate)), col2X, y + 56f, textBodyPaint)

        canvas.drawText("Expected Hatch:", col2X, y + 74f, textSubTitlePaint)
        canvas.drawText(dateFormat.format(Date(batch.expectedHatchDate)), col2X, y + 88f, textBodyPaint)

        canvas.drawText("Status:", col3X, y + 42f, textSubTitlePaint)
        canvas.drawText(batch.status, col3X, y + 56f, textBoldBodyPaint)

        canvas.drawText("Lockdown Day:", col3X, y + 74f, textSubTitlePaint)
        canvas.drawText("Day ${batch.lockdownDay} of ${batch.incubationDays}", col3X, y + 88f, textBodyPaint)

        y += 125f

        // Hatch Results & Egg Breakdown
        val resultsRect = RectF(40f, y, 555f, y + 90f)
        canvas.drawRoundRect(resultsRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(resultsRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("EGG COUNTS & HATCHING PERCENTAGE", 56f, y + 20f, textHeaderPaint)

        val hatchRate = String.format(Locale.US, "%.1f%%", batch.hatchingPercentage)

        canvas.drawText("Total Eggs:", 56f, y + 42f, textSubTitlePaint)
        canvas.drawText("${batch.totalEggs}", 56f, y + 56f, textBoldBodyPaint)

        canvas.drawText("Hatched:", 145f, y + 42f, textSubTitlePaint)
        canvas.drawText("${batch.hatchedEggs}", 145f, y + 56f, textBoldBodyPaint)

        canvas.drawText("Infertile / Clear:", 235f, y + 42f, textSubTitlePaint)
        canvas.drawText("${batch.infertileEggs}", 235f, y + 56f, textBodyPaint)

        canvas.drawText("Early / Late Quit:", 345f, y + 42f, textSubTitlePaint)
        canvas.drawText("${batch.earlyQuitEggs + batch.lateQuitEggs}", 345f, y + 56f, textBodyPaint)

        canvas.drawText("Hatching Rate:", 460f, y + 42f, textSubTitlePaint)
        val ratePaint = Paint(textBoldBodyPaint).apply {
            color = Color.rgb(16, 185, 129) // Emerald
            textSize = 14f
        }
        canvas.drawText(hatchRate, 460f, y + 60f, ratePaint)

        y += 110f

        // Climate Statistics
        val climateRect = RectF(40f, y, 555f, y + 100f)
        canvas.drawRoundRect(climateRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(climateRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("TEMPERATURE & HUMIDITY STABILITY", 56f, y + 20f, textHeaderPaint)

        val avgTemp = if (readings.isNotEmpty()) readings.map { it.temperatureC }.average() else batch.targetTempC
        val minTemp = if (readings.isNotEmpty()) readings.minOf { it.temperatureC } else batch.minTempC
        val maxTemp = if (readings.isNotEmpty()) readings.maxOf { it.temperatureC } else batch.maxTempC

        val avgHum = if (readings.isNotEmpty()) readings.map { it.humidityPct }.average() else batch.targetHumidityPct
        val minHum = if (readings.isNotEmpty()) readings.minOf { it.humidityPct } else batch.minHumidityPct
        val maxHum = if (readings.isNotEmpty()) readings.maxOf { it.humidityPct } else batch.maxHumidityPct

        canvas.drawText("Avg Temperature:", 56f, y + 42f, textSubTitlePaint)
        canvas.drawText("${String.format(Locale.US, "%.1f", avgTemp)}°C (Target: ${batch.targetTempC}°C)", 56f, y + 56f, textBoldBodyPaint)
        canvas.drawText("Range: ${String.format(Locale.US, "%.1f", minTemp)}°C - ${String.format(Locale.US, "%.1f", maxTemp)}°C", 56f, y + 72f, textBodyPaint)

        canvas.drawText("Avg Humidity:", 300f, y + 42f, textSubTitlePaint)
        canvas.drawText("${String.format(Locale.US, "%.0f", avgHum)}% (Target: ${batch.targetHumidityPct.toInt()}%)", 300f, y + 56f, textBoldBodyPaint)
        canvas.drawText("Range: ${String.format(Locale.US, "%.0f", minHum)}% - ${String.format(Locale.US, "%.0f", maxHum)}%", 300f, y + 72f, textBodyPaint)

        y += 120f

        // Egg Turning & Alert Records
        canvas.drawText("EGG TURNING LOGS & ALERTS SUMMARY", 40f, y, textHeaderPaint)
        y += 16f

        val completedTurns = turnings.count { it.action == "COMPLETED" }
        canvas.drawText("Total Turning Events Logged: $completedTurns turns (${turnings.size} total entries recorded)", 40f, y, textBodyPaint)
        y += 18f

        // Turning events list (last 3)
        turnings.take(3).forEach { turn ->
            val timeStr = timeFormat.format(Date(turn.timestamp))
            canvas.drawText("• $timeStr - Action: ${turn.action} | Side: ${turn.orientation} ${if (turn.notes.isNotBlank()) "(${turn.notes})" else ""}", 50f, y, textBodyPaint)
            y += 14f
        }

        y += 10f
        canvas.drawText("Total Climate & Milestone Alerts: ${alerts.size} triggers", 40f, y, textBodyPaint)
        y += 18f

        alerts.take(3).forEach { alert ->
            val timeStr = timeFormat.format(Date(alert.timestamp))
            canvas.drawText("• $timeStr - [${alert.alertType}] ${alert.title}", 50f, y, textBodyPaint)
            y += 14f
        }

        y += 14f

        // Notes section
        if (batch.notes.isNotBlank()) {
            canvas.drawText("BATCH NOTES & OBSERVATIONS:", 40f, y, textHeaderPaint)
            y += 16f
            canvas.drawText(batch.notes.take(120), 40f, y, textBodyPaint)
            y += 24f
        }

        // Footer disclaimer
        val footerPaint = Paint(textSubTitlePaint).apply {
            textSize = 8f
        }
        canvas.drawText(
            "Disclaimer: Incubation success depends on incubator calibration, fertile egg quality, and ambient climate. Stored 100% locally on device.",
            40f,
            810f,
            footerPaint
        )

        pdfDocument.finishPage(page)

        // Save to File
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val pdfFile = File(reportsDir, "HatchMaster_Report_Batch_${batch.id}.pdf")
        try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Return sharing intent with FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Incubation Report - ${batch.name}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the local incubation report for batch '${batch.name}' (${batch.species}).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            return Intent.createChooser(shareIntent, "Share Incubation Report PDF")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } catch (e: Exception) {
            pdfDocument.close()
            return null
        }
    }
}
