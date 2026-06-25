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

    private fun parseColorSafe(colorHex: String, defaultColor: Int = android.graphics.Color.BLACK): Int {
        return try {
            android.graphics.Color.parseColor(colorHex)
        } catch (e: Exception) {
            defaultColor
        }
    }

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
        
        var currentPage = page
        var currentCanvas = currentPage.canvas

        // Paints setup
        val paintTitle = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = parseColorSafe(mission.colorHex)
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
        currentCanvas.drawText("WORK LOG - RELEVÉ D'HEURES", 40f, y, paintTitle)
        y += 22f
        currentCanvas.drawText("Période : $monthStr", 40f, y, paintSubtitle)
        y += 30f

        // 2. Info Cards
        currentCanvas.drawText("INFORMATIONS MISSION", 40f, y, paintSectionHeader)
        y += 6f
        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        currentCanvas.drawText("Entreprise : ${mission.company}", 40f, y, paintText)
        currentCanvas.drawText("Agence : ${mission.agency}", 300f, y, paintText)
        y += 16f
        currentCanvas.drawText("Taux horaire brut : ${String.format(Locale.FRANCE, "%.2f €/h", mission.hourlyRate)}", 40f, y, paintText)
        currentCanvas.drawText("Lieu : ${mission.siteAddress.ifEmpty { "Non spécifié" }}", 300f, y, paintText)
        y += 30f

        // 3. Worked days table
        currentCanvas.drawText("DÉTAIL DES JOURNÉES", 40f, y, paintSectionHeader)
        y += 6f
        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Table headers
        val cols = listOf(40f, 120f, 220f, 280f, 340f, 410f, 480f)
        currentCanvas.drawText("Date", cols[0], y, paintBoldText)
        currentCanvas.drawText("Horaires", cols[1], y, paintBoldText)
        currentCanvas.drawText("Pause", cols[2], y, paintBoldText)
        currentCanvas.drawText("Heures", cols[3], y, paintBoldText)
        currentCanvas.drawText("Nuit", cols[4], y, paintBoldText)
        currentCanvas.drawText("Primes", cols[5], y, paintBoldText)
        currentCanvas.drawText("Gain (Brut)", cols[6], y, paintBoldText)

        y += 6f
        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
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

            // Dynamic Pagination: check if we exceed page height
            if (y > 780f) {
                pdfDocument.finishPage(currentPage)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                currentPage = pdfDocument.startPage(newPageInfo)
                currentCanvas = currentPage.canvas
                y = 50f // reset to top margin
                
                // Redraw table headers on the new page
                currentCanvas.drawText("Date", cols[0], y, paintBoldText)
                currentCanvas.drawText("Horaires", cols[1], y, paintBoldText)
                currentCanvas.drawText("Pause", cols[2], y, paintBoldText)
                currentCanvas.drawText("Heures", cols[3], y, paintBoldText)
                currentCanvas.drawText("Nuit", cols[4], y, paintBoldText)
                currentCanvas.drawText("Primes", cols[5], y, paintBoldText)
                currentCanvas.drawText("Gain (Brut)", cols[6], y, paintBoldText)
                
                y += 6f
                currentCanvas.drawLine(40f, y, 555f, y, paintLine)
                y += 16f
            }

            val primesLabel = if (item.bonuses.isNotEmpty()) {
                val names = item.bonuses.joinToString(",") { it.name }
                val label = "${String.format(Locale.FRANCE, "%.2f €", dayPrimes)} ($names)"
                if (label.length > 18) label.take(15) + ".." else label
            } else {
                "0,00 €"
            }

            // Draw row
            currentCanvas.drawText(dateFormatter.format(Date(day.dateMillis)).replaceFirstChar { it.uppercase() }, cols[0], y, paintText)
            currentCanvas.drawText("${timeFormatter.format(Date(day.startTimeMillis))} - ${timeFormatter.format(Date(day.endTimeMillis))}", cols[1], y, paintText)
            currentCanvas.drawText("${day.breakMinutes} min", cols[2], y, paintText)
            currentCanvas.drawText(String.format(Locale.FRANCE, "%.1f h", cleanDuration), cols[3], y, paintText)
            currentCanvas.drawText(String.format(Locale.FRANCE, "%.1f h", nightHours), cols[4], y, paintText)
            currentCanvas.drawText(primesLabel, cols[5], y, paintText)
            currentCanvas.drawText(String.format(Locale.FRANCE, "%.2f €", earnings), cols[6], y, paintText)

            y += 18f
        }

        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Check if totals card can fit on current page. If not, push to new page.
        if (y > 550f) {
            pdfDocument.finishPage(currentPage)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            currentPage = pdfDocument.startPage(newPageInfo)
            currentCanvas = currentPage.canvas
            y = 50f
        }

        // 4. Weekly Overtime Premium Calculation
        val weeklyOvertimePremium = SalaryCalculator.calculateWeeklyOvertimePremium(workDays)
        val totalGross = totalDailyEarnings + weeklyOvertimePremium
        val totalNet = totalGross * 0.77

        // 5. Monthly Totals Summary
        currentCanvas.drawText("RÉCAPITULATIF MENSUEL", 40f, y, paintSectionHeader)
        y += 6f
        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        val sumCols = listOf(40f, 300f)
        currentCanvas.drawText("Heures totales : ${String.format(Locale.FRANCE, "%.1f h", totalHours)}", sumCols[0], y, paintBoldText)
        currentCanvas.drawText("Total de base (avec primes) : ${String.format(Locale.FRANCE, "%.2f € Brut", totalDailyEarnings)}", sumCols[1], y, paintText)
        y += 16f
        currentCanvas.drawText("Dont heures de nuit : ${String.format(Locale.FRANCE, "%.1f h", totalNightHours)}", sumCols[0], y, paintText)
        currentCanvas.drawText("Majoration heures sup. hebdo : ${String.format(Locale.FRANCE, "%.2f € Brut", weeklyOvertimePremium)}", sumCols[1], y, paintText)
        y += 24f

        currentCanvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Big Gross and Net boxes
        val paintBox = Paint().apply {
            color = parseColorSafe(mission.colorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val paintBoxFill = Paint().apply {
            color = parseColorSafe(mission.colorHex)
            alpha = 15
            style = Paint.Style.FILL
        }

        currentCanvas.drawRoundRect(40f, y, 280f, y + 50f, 8f, 8f, paintBoxFill)
        currentCanvas.drawRoundRect(40f, y, 280f, y + 50f, 8f, 8f, paintBox)
        currentCanvas.drawText("TOTAL ESTIMÉ BRUT", 52f, y + 18f, paintSubtitle)
        currentCanvas.drawText(String.format(Locale.FRANCE, "%.2f €", totalGross), 52f, y + 40f, paintTitle)

        currentCanvas.drawRoundRect(300f, y, 555f, y + 50f, 8f, 8f, paintBoxFill)
        currentCanvas.drawRoundRect(300f, y, 555f, y + 50f, 8f, 8f, paintBox)
        currentCanvas.drawText("TOTAL ESTIMÉ NET (~23%)", 312f, y + 18f, paintSubtitle)
        val paintNetTitle = Paint(paintTitle).apply {
            color = android.graphics.Color.parseColor("#10B981") // Success Green
        }
        currentCanvas.drawText(String.format(Locale.FRANCE, "%.2f €", totalNet), 312f, y + 40f, paintNetTitle)

        y += 85f

        // 6. Signature blocks
        currentCanvas.drawText("Signature de l'intérimaire :", 40f, y, paintBoldText)
        currentCanvas.drawText("Signature de l'entreprise :", 300f, y, paintBoldText)
        y += 45f
        currentCanvas.drawLine(40f, y, 200f, y, paintLine)
        currentCanvas.drawLine(300f, y, 460f, y, paintLine)

        pdfDocument.finishPage(currentPage)

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
