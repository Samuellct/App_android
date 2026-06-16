package com.interim.hours.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
        val workDays = workDayDao.getWorkDaysWithDetailsFlow().first()

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
    val primaryColor = GlanceTheme.colors.primary
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val outlineColor = GlanceTheme.colors.outline

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "Work Log",
            style = TextStyle(
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().padding(4.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "Heures",
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = String.format(java.util.Locale.FRANCE, "%.1f h", monthlyHours),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }

            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(outlineColor)
            ) {}

            Column(
                modifier = GlanceModifier.defaultWeight().padding(4.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "Salaire Net",
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = String.format(java.util.Locale.FRANCE, "%.2f €", netEarnings),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}
