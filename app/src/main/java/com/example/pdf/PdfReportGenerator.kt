package com.example.pdf

import android.content.Context
import android.content.Intent
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
    private val tableTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    fun generateAndShareBatchReport(
        batch: IncubationBatch,
        readings: List<SensorReading>,
        turnings: List<TurningLog>,
        alerts: List<AlertLog>
    ): Intent? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(194, 65, 12) // Warm Amber #C2410C
            isAntiAlias = true
        }

        val textTitlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textSubTitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            isAntiAlias = true
        }

        val textSectionHeaderPaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textHeaderPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textBodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            isAntiAlias = true
        }

        val textBoldBodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
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

        val rowDividerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 1f
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var pageNumber = 1

        fun drawPageHeader(canvas: android.graphics.Canvas, subHeading: String) {
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, primaryPaint)
            canvas.drawText("HATCHMASTER INCUBATION BATCH REPORT", 40f, 32f, textTitlePaint)
            canvas.drawText(
                "Batch: ${batch.name} (${batch.species}) | $subHeading",
                40f,
                46f,
                textSubTitlePaint
            )
        }

        fun drawPageFooter(canvas: android.graphics.Canvas, currentPage: Int) {
            val footerPaint = Paint(textSubTitlePaint).apply { textSize = 8f }
            canvas.drawText(
                "Generated: ${timeFormat.format(Date())} | 100% Local Offline Device Record",
                40f,
                815f,
                footerPaint
            )
            canvas.drawText(
                "Page $currentPage",
                520f,
                815f,
                footerPaint
            )
        }

        // ================= PAGE 1: EXECUTIVE SUMMARY =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        var canvas = page1.canvas

        drawPageHeader(canvas, "Executive Summary & Performance Metrics")

        var y = 64f

        // 1. Batch Overview Box
        val overviewRect = RectF(40f, y, 555f, y + 115f)
        canvas.drawRoundRect(overviewRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(overviewRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("BATCH OVERVIEW & SCHEDULE", 56f, y + 22f, textSectionHeaderPaint)

        val col1X = 56f
        val col2X = 220f
        val col3X = 390f

        canvas.drawText("Batch Identifier:", col1X, y + 42f, textSubTitlePaint)
        canvas.drawText(batch.name, col1X, y + 56f, textBoldBodyPaint)

        canvas.drawText("Species:", col1X, y + 74f, textSubTitlePaint)
        canvas.drawText(batch.species, col1X, y + 88f, textBodyPaint)

        canvas.drawText("Incubation Start Date:", col2X, y + 42f, textSubTitlePaint)
        canvas.drawText(dateFormat.format(Date(batch.startDate)), col2X, y + 56f, textBodyPaint)

        canvas.drawText("Expected Hatch Date:", col2X, y + 74f, textSubTitlePaint)
        canvas.drawText(dateFormat.format(Date(batch.expectedHatchDate)), col2X, y + 88f, textBoldBodyPaint)

        canvas.drawText("Current Cycle Status:", col3X, y + 42f, textSubTitlePaint)
        canvas.drawText(batch.status, col3X, y + 56f, textBoldBodyPaint)

        canvas.drawText("Lockdown Window:", col3X, y + 74f, textSubTitlePaint)
        canvas.drawText("Day ${batch.lockdownDay} of ${batch.incubationDays} (Stop turning)", col3X, y + 88f, textBodyPaint)

        y += 130f

        // 2. Egg Counts & Hatching Rate Box
        val resultsRect = RectF(40f, y, 555f, y + 105f)
        canvas.drawRoundRect(resultsRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(resultsRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("EGG AUDIT & HATCHING RATE", 56f, y + 22f, textSectionHeaderPaint)

        canvas.drawText("Total Eggs Set:", 56f, y + 44f, textSubTitlePaint)
        canvas.drawText("${batch.totalEggs}", 56f, y + 58f, textBoldBodyPaint)

        canvas.drawText("Hatched Chicks:", 145f, y + 44f, textSubTitlePaint)
        val hatchedPaint = Paint(textBoldBodyPaint).apply { color = Color.rgb(16, 185, 129) }
        canvas.drawText("${batch.hatchedEggs}", 145f, y + 58f, hatchedPaint)

        canvas.drawText("Infertile / Clear:", 235f, y + 44f, textSubTitlePaint)
        canvas.drawText("${batch.infertileEggs}", 235f, y + 58f, textBodyPaint)

        canvas.drawText("Early Quits (Blood Ring):", 330f, y + 44f, textSubTitlePaint)
        canvas.drawText("${batch.earlyQuitEggs}", 330f, y + 58f, textBodyPaint)

        canvas.drawText("Late Quits (Shell):", 445f, y + 44f, textSubTitlePaint)
        canvas.drawText("${batch.lateQuitEggs}", 445f, y + 58f, textBodyPaint)

        // Hatching rate highlight
        canvas.drawText("Final Hatching Rate Calculation:", 56f, y + 80f, textSubTitlePaint)
        val rateStr = String.format(Locale.US, "%.1f%%  ( %d hatched / %d initial )", batch.hatchingPercentage, batch.hatchedEggs, batch.totalEggs)
        val bigRatePaint = Paint(textBoldBodyPaint).apply {
            color = Color.rgb(16, 185, 129)
            textSize = 13f
        }
        canvas.drawText(rateStr, 56f, y + 96f, bigRatePaint)

        y += 120f

        // 3. Climate Telemetry Summary Box
        val climateRect = RectF(40f, y, 555f, y + 105f)
        canvas.drawRoundRect(climateRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(climateRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("CLIMATE PARAMETERS & TELEMETRY STABILITY", 56f, y + 22f, textSectionHeaderPaint)

        val avgTemp = if (readings.isNotEmpty()) readings.map { it.temperatureC }.average() else batch.targetTempC
        val minTemp = if (readings.isNotEmpty()) readings.minOf { it.temperatureC } else batch.minTempC
        val maxTemp = if (readings.isNotEmpty()) readings.maxOf { it.temperatureC } else batch.maxTempC

        val avgHum = if (readings.isNotEmpty()) readings.map { it.humidityPct }.average() else batch.targetHumidityPct
        val minHum = if (readings.isNotEmpty()) readings.minOf { it.humidityPct } else batch.minHumidityPct
        val maxHum = if (readings.isNotEmpty()) readings.maxOf { it.humidityPct } else batch.maxHumidityPct

        canvas.drawText("Temperature Telemetry:", 56f, y + 44f, textSubTitlePaint)
        canvas.drawText("Average: ${String.format(Locale.US, "%.1f", avgTemp)}°C  (Target: ${batch.targetTempC}°C)", 56f, y + 58f, textBoldBodyPaint)
        canvas.drawText("Observed Range: ${String.format(Locale.US, "%.1f", minTemp)}°C - ${String.format(Locale.US, "%.1f", maxTemp)}°C", 56f, y + 74f, textBodyPaint)

        canvas.drawText("Relative Humidity Telemetry:", 300f, y + 44f, textSubTitlePaint)
        canvas.drawText("Average: ${String.format(Locale.US, "%.0f", avgHum)}%  (Target: ${batch.targetHumidityPct.toInt()}%)", 300f, y + 58f, textBoldBodyPaint)
        canvas.drawText("Observed Range: ${String.format(Locale.US, "%.0f", minHum)}% - ${String.format(Locale.US, "%.0f", maxHum)}% (Lockdown: ${batch.lockdownHumidityPct.toInt()}%)", 300f, y + 74f, textBodyPaint)

        y += 120f

        // 4. Activity Statistics Summary Box
        val statsRect = RectF(40f, y, 555f, y + 80f)
        canvas.drawRoundRect(statsRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(statsRect, 8f, 8f, cardBorderPaint)

        canvas.drawText("OPERATIONAL TOTALS", 56f, y + 20f, textSectionHeaderPaint)

        val completedTurnsCount = turnings.count { it.action == "COMPLETED" }
        canvas.drawText("Total Egg Rotations Logged:", 56f, y + 42f, textSubTitlePaint)
        canvas.drawText("$completedTurnsCount turns completed (${turnings.size} total events)", 56f, y + 56f, textBoldBodyPaint)

        canvas.drawText("Total Alerts & Warnings:", 300f, y + 42f, textSubTitlePaint)
        canvas.drawText("${alerts.size} system alarm notifications recorded", 300f, y + 56f, textBoldBodyPaint)

        y += 95f

        // 5. Notes & Observations
        if (batch.notes.isNotBlank()) {
            val notesRect = RectF(40f, y, 555f, y + 75f)
            canvas.drawRoundRect(notesRect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(notesRect, 8f, 8f, cardBorderPaint)

            canvas.drawText("BREEDER NOTES & HUSBANDRY OBSERVATIONS", 56f, y + 20f, textSectionHeaderPaint)
            canvas.drawText(batch.notes.take(150), 56f, y + 40f, textBodyPaint)
            if (batch.notes.length > 150) {
                canvas.drawText(batch.notes.substring(150).take(150), 56f, y + 56f, textBodyPaint)
            }
        }

        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page1)

        // ================= PAGE 2+: COMPLETE EGG TURNING HISTORY =================
        if (turnings.isNotEmpty()) {
            pageNumber++
            var turningPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = turningPage.canvas
            drawPageHeader(canvas, "Complete Egg-Turning History Log")

            y = 70f
            canvas.drawText("COMPLETE EGG-TURNING EVENT LOG (${turnings.size} ENTRIES)", 40f, y, textSectionHeaderPaint)
            y += 16f

            // Table Header
            canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderBgPaint)
            canvas.drawText("Timestamp", 48f, y + 14f, textHeaderPaint)
            canvas.drawText("Action", 160f, y + 14f, textHeaderPaint)
            canvas.drawText("Orientation / Side", 260f, y + 14f, textHeaderPaint)
            canvas.drawText("Notes / Method", 390f, y + 14f, textHeaderPaint)
            y += 24f

            turnings.forEachIndexed { index, log ->
                if (y > 780f) {
                    drawPageFooter(canvas, pageNumber)
                    pdfDocument.finishPage(turningPage)
                    pageNumber++
                    turningPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = turningPage.canvas
                    drawPageHeader(canvas, "Complete Egg-Turning History Log (Cont.)")
                    y = 70f
                    canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderBgPaint)
                    canvas.drawText("Timestamp", 48f, y + 14f, textHeaderPaint)
                    canvas.drawText("Action", 160f, y + 14f, textHeaderPaint)
                    canvas.drawText("Orientation / Side", 260f, y + 14f, textHeaderPaint)
                    canvas.drawText("Notes / Method", 390f, y + 14f, textHeaderPaint)
                    y += 24f
                }

                canvas.drawLine(40f, y + 14f, 555f, y + 14f, rowDividerPaint)
                canvas.drawText(tableTimeFormat.format(Date(log.timestamp)), 48f, y + 10f, textBodyPaint)
                canvas.drawText(log.action, 160f, y + 10f, textBoldBodyPaint)
                canvas.drawText(log.orientation, 260f, y + 10f, textBodyPaint)
                canvas.drawText(log.notes.ifEmpty { "Confirmed on schedule" }.take(30), 390f, y + 10f, textBodyPaint)
                y += 20f
            }

            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(turningPage)
        }

        // ================= PAGE 3+: COMPLETE ALERTS & TELEMETRY =================
        if (alerts.isNotEmpty() || readings.isNotEmpty()) {
            pageNumber++
            var alertPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = alertPage.canvas
            drawPageHeader(canvas, "Alerts History & Telemetry Samples")

            y = 70f

            if (alerts.isNotEmpty()) {
                canvas.drawText("COMPLETE ALERT NOTIFICATIONS HISTORY (${alerts.size} EVENTS)", 40f, y, textSectionHeaderPaint)
                y += 16f

                // Alerts Table Header
                canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderBgPaint)
                canvas.drawText("Timestamp", 48f, y + 14f, textHeaderPaint)
                canvas.drawText("Type", 140f, y + 14f, textHeaderPaint)
                canvas.drawText("Alert Message / Trigger", 240f, y + 14f, textHeaderPaint)
                canvas.drawText("Status", 480f, y + 14f, textHeaderPaint)
                y += 24f

                alerts.forEach { alert ->
                    if (y > 780f) {
                        drawPageFooter(canvas, pageNumber)
                        pdfDocument.finishPage(alertPage)
                        pageNumber++
                        alertPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = alertPage.canvas
                        drawPageHeader(canvas, "Alerts History (Cont.)")
                        y = 70f
                        canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderBgPaint)
                        canvas.drawText("Timestamp", 48f, y + 14f, textHeaderPaint)
                        canvas.drawText("Type", 140f, y + 14f, textHeaderPaint)
                        canvas.drawText("Alert Message / Trigger", 240f, y + 14f, textHeaderPaint)
                        canvas.drawText("Status", 480f, y + 14f, textHeaderPaint)
                        y += 24f
                    }

                    canvas.drawLine(40f, y + 14f, 555f, y + 14f, rowDividerPaint)
                    canvas.drawText(tableTimeFormat.format(Date(alert.timestamp)), 48f, y + 10f, textBodyPaint)
                    canvas.drawText(alert.alertType.take(15), 140f, y + 10f, textBoldBodyPaint)
                    canvas.drawText(alert.title.take(38), 240f, y + 10f, textBodyPaint)
                    canvas.drawText(if (alert.isAcknowledged) "Acknowledged" else "Active", 480f, y + 10f, textSubTitlePaint)
                    y += 20f
                }
                y += 16f
            }

            // Sensor Readings Log Table
            if (readings.isNotEmpty() && y < 700f) {
                canvas.drawText("RECORDED SENSOR TELEMETRY (${readings.size} TOTAL LOGS)", 40f, y, textSectionHeaderPaint)
                y += 16f

                canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderBgPaint)
                canvas.drawText("Timestamp", 48f, y + 14f, textHeaderPaint)
                canvas.drawText("Temperature", 160f, y + 14f, textHeaderPaint)
                canvas.drawText("Relative Humidity", 280f, y + 14f, textHeaderPaint)
                canvas.drawText("Sensor Source", 410f, y + 14f, textHeaderPaint)
                y += 24f

                readings.take(25).forEach { r ->
                    if (y > 780f) {
                        drawPageFooter(canvas, pageNumber)
                        pdfDocument.finishPage(alertPage)
                        pageNumber++
                        alertPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = alertPage.canvas
                        drawPageHeader(canvas, "Telemetry Readings (Cont.)")
                        y = 70f
                    }

                    canvas.drawLine(40f, y + 14f, 555f, y + 14f, rowDividerPaint)
                    canvas.drawText(tableTimeFormat.format(Date(r.timestamp)), 48f, y + 10f, textBodyPaint)
                    canvas.drawText("${String.format(Locale.US, "%.1f", r.temperatureC)}°C", 160f, y + 10f, textBoldBodyPaint)
                    canvas.drawText("${r.humidityPct.toInt()}% RH", 280f, y + 10f, textBodyPaint)
                    canvas.drawText("${r.sensorSource} (${if (r.isSimulated) "Demo" else "Hardware"})", 410f, y + 10f, textSubTitlePaint)
                    y += 20f
                }
            }

            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(alertPage)
        }

        // Save PDF to cache file
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

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Incubation Report - ${batch.name}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the local multi-page incubation report for batch '${batch.name}' (${batch.species}).")
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
