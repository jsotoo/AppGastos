package com.minimalgastos.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val budget: Double = 0.0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val BUDGET = doublePreferencesKey("budget")
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val ALERT_MONTH = stringPreferencesKey("alert_month")
        val ALERT_LEVEL = intPreferencesKey("alert_level")
    }

    val settings: Flow<Settings> = store.data
        .map { p ->
            Settings(
                budget = p[Keys.BUDGET] ?: 0.0,
                reminderEnabled = p[Keys.REMINDER_ON] ?: false,
                reminderHour = p[Keys.REMINDER_HOUR] ?: 21,
                reminderMinute = p[Keys.REMINDER_MINUTE] ?: 0
            )
        }
        .distinctUntilChanged()

    suspend fun setBudget(value: Double) {
        store.edit { it[Keys.BUDGET] = value }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        store.edit {
            it[Keys.REMINDER_ON] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    /** Último nivel de alerta (0, 80, 100) ya notificado y el mes (yyyy-MM) al que corresponde. */
    suspend fun alertState(): Pair<String, Int> {
        val p = store.data.first()
        return (p[Keys.ALERT_MONTH] ?: "") to (p[Keys.ALERT_LEVEL] ?: 0)
    }

    suspend fun setAlertState(month: String, level: Int) {
        store.edit {
            it[Keys.ALERT_MONTH] = month
            it[Keys.ALERT_LEVEL] = level
        }
    }
}
