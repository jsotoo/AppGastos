package com.minimalgastos.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.minimalgastos.app.MainActivity
import com.minimalgastos.app.R
import com.minimalgastos.app.util.Format

object Notifier {
    private const val CH_REMINDER = "reminder"
    private const val CH_BUDGET = "budget"
    private const val ID_REMINDER = 1
    private const val ID_BUDGET = 2

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Recordatorio diario", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Te recuerda registrar tus gastos del día" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_BUDGET, "Alertas de presupuesto", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Avisa al llegar al 80% y al 100% del presupuesto mensual" }
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showReminder(context: Context) {
        post(
            context, ID_REMINDER, CH_REMINDER,
            "¿Ya registraste tus gastos?",
            "Toma 10 segundos anotar lo que gastaste hoy."
        )
    }

    fun showBudgetAlert(context: Context, level: Int, spent: Double, budget: Double) {
        val title = if (level >= 100) "Superaste tu presupuesto" else "Vas en el 80% de tu presupuesto"
        val text = "Has gastado ${Format.money(spent)} de ${Format.money(budget)} este mes."
        post(context, ID_BUDGET, CH_BUDGET, title, text)
    }

    @SuppressLint("MissingPermission") // se verifica en canNotify()
    private fun post(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_gastos)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
