package com.interim.hours.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.interim.hours.ui.MainActivity
import com.interim.hours.data.database.WorkDayDao
import com.interim.hours.utils.SalaryCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WorkLogWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun workDayDao(): WorkDayDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val workDayDao = entryPoint.workDayDao()

        // Fetch workdays and calculate monthly hours and earnings
        val workDays = workDayDao.getWorkDaysWithDetails()

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var monthlyHours = 0.0
        var monthlyEarnings = 0.0

        workDays.forEach { item ->
            val day = item.workDay
            val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
            val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
            val dayEarnings = SalaryCalculator.calculateEarnings(day, item.mission, item.bonuses)

            if (day.dateMillis >= startOfMonth) {
                monthlyHours += cleanDuration
                monthlyEarnings += dayEarnings
            }
        }

        // Add weekly overtime premiums for the current month
        val daysGroupedByWeek = workDays.groupBy { item ->
            SalaryCalculator.getYearAndWeek(item.workDay.dateMillis)
        }

        daysGroupedByWeek.forEach { (_, weekDaysList) ->
            val overtimePremium = SalaryCalculator.calculateWeeklyOvertimePremium(weekDaysList)
            if (overtimePremium > 0.0) {
                val hasDayInCurrentMonth = weekDaysList.any { it.workDay.dateMillis >= startOfMonth }
                if (hasDayInCurrentMonth) {
                    monthlyEarnings += overtimePremium
                }
            }
        }

        val netEarnings = monthlyEarnings * 0.77

        provideContent {
            GlanceWidgetContent(
                monthlyHours = monthlyHours,
                netEarnings = netEarnings
            )
        }
    }
}

@Composable
fun GlanceWidgetContent(
    monthlyHours: Double,
    netEarnings: Double
) {
    val backgroundColor = GlanceTheme.colors.widgetBackground
    
    // Dynamic text and color styling matching the app's purple & green color palette
    val titleColor = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    
    val labelStyle = TextStyle(
        color = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8)),
        fontSize = 13.sp, // Increased for accessibility
        fontWeight = FontWeight.Medium
    )
    
    // Violet/indigo for Hours
    val hoursValueStyle = TextStyle(
        color = ColorProvider(day = Color(0xFF4F46E5), night = Color(0xFF818CF8)),
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    )
    
    // Vert/teal for Earnings
    val earningsValueStyle = TextStyle(
        color = ColorProvider(day = Color(0xFF10B981), night = Color(0xFF2DD4BF)),
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    )
    
    val dividerColor = ColorProvider(day = Color(0xFFE2E8F0), night = Color(0xFF334155))

    // Format the current month in French
    val currentMonthName = Calendar.getInstance().getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        java.util.Locale.FRANCE
    )?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.FRANCE) else it.toString() } ?: ""
    val firstChar = currentMonthName.firstOrNull()?.lowercaseChar()
    val prefix = if (firstChar in setOf('a', 'e', 'i', 'o', 'u', 'y', 'é', 'è', 'à', 'ù', 'â', 'ê', 'î', 'ô', 'û', 'ä', 'ë', 'ï', 'ö', 'ü')) "d'" else "de "
    val statsTitle = "Stats $prefix$currentMonthName"

    // Format values to strings to dynamically determine text sizes (responsive layout)
    val hoursStr = String.format(java.util.Locale.FRANCE, "%.1f h", monthlyHours)
    val earningsStr = String.format(java.util.Locale.FRANCE, "%.2f €", netEarnings)

    val hoursFontSize = when {
        hoursStr.length > 7 -> 16.sp
        hoursStr.length > 6 -> 18.sp
        else -> 22.sp
    }

    val earningsFontSize = when {
        earningsStr.length > 9 -> 16.sp
        earningsStr.length > 7 -> 18.sp
        else -> 22.sp
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(backgroundColor)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        // Top section
        Text(
            text = "Work Log",
            style = TextStyle(
                color = titleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp // Increased for accessibility
            )
        )
        
        Spacer(modifier = GlanceModifier.height(6.dp))
        
        Text(
            text = statsTitle,
            style = labelStyle
        )

        // Center values between top title and bottom spacing
        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = hoursStr,
                    style = hoursValueStyle.copy(fontSize = hoursFontSize)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "heures",
                    style = labelStyle.copy(fontSize = 11.sp)
                )
            }

            Box(
                modifier = GlanceModifier
                    .width(1.5.dp)
                    .height(35.dp)
                    .background(dividerColor)
            ) {}

            Column(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = earningsStr,
                    style = earningsValueStyle.copy(fontSize = earningsFontSize)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "est. net",
                    style = labelStyle.copy(fontSize = 11.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}
