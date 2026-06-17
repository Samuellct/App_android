package com.interim.hours.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*

object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null
    private val lock = Any()

    fun triggerUpdate(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            updateJob?.cancel()
            updateJob = scope.launch {
                delay(300) // Debounce window of 300ms
                try {
                    WorkLogWidget().updateAll(appContext)
                    WorkLogVerticalWidget().updateAll(appContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
