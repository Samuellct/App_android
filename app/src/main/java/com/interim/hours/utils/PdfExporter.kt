package com.interim.hours.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.interim.hours.data.model.WorkDayWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object PdfExporter {

    fun generatePdfFile(
        context: Context,
        monthStr: String,
        workDays: List<WorkDayWithDetails>
    ): File? {
        if (workDays.isEmpty()) return null

        val mission = workDays.first().mission

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints setup
        val paintTitle = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor(mission.colorHex)
        }
        val paintSubtitle = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
        }
        val paintSectionHeader = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val paintText = Paint().apply {
            textSize = 9f
            color = android.graphics.Color.BLACK
        }
        val paintBoldText = Paint().apply {
            textSize = 9f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val paintLine = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f

        // 1. Header
        canvas.drawText("WORK LOG - RELEVÉ D'HEURES", 40f, y, paintTitle)
        y += 22f
        canvas.drawText("Période : $monthStr", 40f, y, paintSubtitle)
        y += 30f

        // 2. Info Cards
        canvas.drawText("INFORMATIONS MISSION", 40f, y, paintSectionHeader)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        canvas.drawText("Entreprise : ${mission.company}", 40f, y, paintText)
        canvas.drawText("Agence : ${mission.agency}", 300f, y, paintText)
        y += 16f
        canvas.drawText("Taux horaire brut : ${String.format(Locale.FRANCE, "%.2f €/h", mission.hourlyRate)}", 40f, y, paintText)
        canvas.drawText("Lieu : ${mission.siteAddress.ifEmpty { "Non spécifié" }}", 300f, y, paintText)
        y += 30f

        // 3. Worked days table
        canvas.drawText("DÉTAIL DES JOURNÉES", 40f, y, paintSectionHeader)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Table headers
        val cols = listOf(40f, 120f, 220f, 280f, 340f, 410f, 480f)
        canvas.drawText("Date", cols[0], y, paintBoldText)
        canvas.drawText("Horaires", cols[1], y, paintBoldText)
        canvas.drawText("Pause", cols[2], y, paintBoldText)
        canvas.drawText("Heures", cols[3], y, paintBoldText)
        canvas.drawText("Nuit", cols[4], y, paintBoldText)
        canvas.drawText("Primes", cols[5], y, paintBoldText)
        canvas.drawText("Gain (Brut)", cols[6], y, paintBoldText)

        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 16f

        val dateFormatter = SimpleDateFormat("EEE dd/MM", Locale.FRANCE)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.FRANCE)

        var totalHours = 0.0
        var totalNightHours = 0.0
        var totalDailyEarnings = 0.0
        var totalPrimes = 0.0

        workDays.sortedBy { it.workDay.dateMillis }.forEach { item ->
            val day = item.workDay
            val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
            val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
            totalHours += cleanDuration

            val nightHours = if (mission.nightRatePercentage > 0.0) {
                val rawNight = SalaryCalculator.calculateNightHours(
                    day.startTimeMillis,
                    day.endTimeMillis,
                    mission.nightStartHour,
                    mission.nightEndHour
                )
                minOf(rawNight, cleanDuration)
            } else 0.0
            totalNightHours += nightHours

            val earnings = SalaryCalculator.calculateEarnings(day, mission, item.bonuses)
            totalDailyEarnings += earnings

            val dayPrimes = item.bonuses.sumOf { it.amount }
            totalPrimes += dayPrimes

            // Draw row
            canvas.drawText(dateFormatter.format(Date(day.dateMillis)).replaceFirstChar { it.uppercase() }, cols[0], y, paintText)
            canvas.drawText("${timeFormatter.format(Date(day.startTimeMillis))} - ${timeFormatter.format(Date(day.endTimeMillis))}", cols[1], y, paintText)
            canvas.drawText("${day.breakMinutes} min", cols[2], y, paintText)
            canvas.drawText(String.format(Locale.FRANCE, "%.1f h", cleanDuration), cols[3], y, paintText)
            canvas.drawText(String.format(Locale.FRANCE, "%.1f h", nightHours), cols[4], y, paintText)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", dayPrimes), cols[5], y, paintText)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", earnings), cols[6], y, paintText)

            y += 18f
        }

        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // 4. Weekly Overtime Premium Calculation
        val weeklyOvertimePremium = SalaryCalculator.calculateWeeklyOvertimePremium(workDays)
        val totalGross = totalDailyEarnings + weeklyOvertimePremium
        val totalNet = totalGross * 0.77

        // 5. Monthly Totals Summary
        canvas.drawText("RÉCAPITULATIF MENSUEL", 40f, y, paintSectionHeader)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        val sumCols = listOf(40f, 300f)
        canvas.drawText("Heures totales : ${String.format(Locale.FRANCE, "%.1f h", totalHours)}", sumCols[0], y, paintBoldText)
        canvas.drawText("Total de base (avec primes) : ${String.format(Locale.FRANCE, "%.2f € Brut", totalDailyEarnings)}", sumCols[1], y, paintText)
        y += 16f
        canvas.drawText("Dont heures de nuit : ${String.format(Locale.FRANCE, "%.1f h", totalNightHours)}", sumCols[0], y, paintText)
        canvas.drawText("Majoration heures sup. hebdo : ${String.format(Locale.FRANCE, "%.2f € Brut", weeklyOvertimePremium)}", sumCols[1], y, paintText)
        y += 24f

        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Big Gross and Net boxes
        val paintBox = Paint().apply {
            color = android.graphics.Color.parseColor(mission.colorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val paintBoxFill = Paint().apply {
            color = android.graphics.Color.parseColor(mission.colorHex)
            alpha = 15
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(40f, y, 280f, y + 50f, 8f, 8f, paintBoxFill)
        canvas.drawRoundRect(40f, y, 280f, y + 50f, 8f, 8f, paintBox)
        canvas.drawText("TOTAL ESTIMÉ BRUT", 52f, y + 18f, paintSubtitle)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", totalGross), 52f, y + 40f, paintTitle)

        canvas.drawRoundRect(300f, y, 555f, y + 50f, 8f, 8f, paintBoxFill)
        canvas.drawRoundRect(300f, y, 555f, y + 50f, 8f, 8f, paintBox)
        canvas.drawText("TOTAL ESTIMÉ NET (~23%)", 312f, y + 18f, paintSubtitle)
        val paintNetTitle = Paint(paintTitle).apply {
            color = android.graphics.Color.parseColor("#10B981") // Success Green
        }
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", totalNet), 312f, y + 40f, paintNetTitle)

        y += 85f

        // 6. Signature blocks
        canvas.drawText("Signature de l'intérimaire :", 40f, y, paintBoldText)
        canvas.drawText("Signature de l'entreprise :", 300f, y, paintBoldText)
        y += 45f
        canvas.drawLine(40f, y, 200f, y, paintLine)
        canvas.drawLine(300f, y, 460f, y, paintLine)

        pdfDocument.finishPage(page)

        // Write file to cache
        val fileName = "work_log_${monthStr.lowercase().replace(" ", "_")}.pdf"
        val cacheFile = File(context.cacheDir, fileName)

        try {
            val fileOutputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()
            return cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    fun exportMonthlyPdf(
        context: Context,
        monthStr: String, // e.g. "Juin 2026"
        workDays: List<WorkDayWithDetails>,
        onShareIntentReady: (Intent) -> Unit
    ) {
        val cacheFile = generatePdfFile(context, monthStr, workDays) ?: return

        // Create Share Intent
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Work Log - Relevé d'heures $monthStr")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        onShareIntentReady(shareIntent)
    }

}
