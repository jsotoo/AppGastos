package com.minimalgastos.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.minimalgastos.app.data.Settings
import com.minimalgastos.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Recordatorio diario con AlarmManager (setAndAllowWhileIdle: no requiere permiso de alarmas exactas;
 * puede adelantarse o retrasarse unos minutos con el ahorro de batería).
 */
object ReminderScheduler {

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 1001,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }
}

/** Ejecuta [block] con los ajustes actuales sin bloquear el hilo principal del receiver. */
private fun BroadcastReceiver.withSettings(context: Context, block: suspend (Settings) -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block(SettingsRepository(context.applicationContext).settings.first())
        } finally {
            pending.finish()
        }
    }
}

/** Se dispara a la hora configurada: muestra el aviso y programa el del día siguiente. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        withSettings(context) { s ->
            if (!s.reminderEnabled) return@withSettings
            Notifier.showReminder(context)
            ReminderScheduler.schedule(context, s.reminderHour, s.reminderMinute)
        }
    }
}

/** Las alarmas se pierden al reiniciar: se vuelven a programar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        withSettings(context) { s ->
            if (s.reminderEnabled) ReminderScheduler.schedule(context, s.reminderHour, s.reminderMinute)
        }
    }
}
