package com.minimalgastos.app

import android.app.Application
import com.minimalgastos.app.data.AppDatabase
import com.minimalgastos.app.data.ExpenseRepository
import com.minimalgastos.app.data.SettingsRepository
import com.minimalgastos.app.notifications.Notifier
import com.minimalgastos.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Contenedor de dependencias manual (suficiente para esta app; sin Hilt). */
class GastosApp : Application() {

    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { ExpenseRepository(database.expenseDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        // Por si el sistema canceló la alarma, se reprograma al abrir la app.
        CoroutineScope(Dispatchers.IO).launch {
            val s = settingsRepository.settings.first()
            if (s.reminderEnabled) {
                ReminderScheduler.schedule(this@GastosApp, s.reminderHour, s.reminderMinute)
            }
        }
    }
}
