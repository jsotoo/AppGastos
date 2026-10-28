@file:OptIn(ExperimentalMaterial3Api::class)

package com.minimalgastos.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalgastos.app.util.CsvExporter
import com.minimalgastos.app.viewmodel.ExpenseViewModel

@Composable
fun SettingsScreen(vm: ExpenseViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var budgetText by remember(settings.budget) {
        mutableStateOf(if (settings.budget > 0) settings.budget.toLong().toString() else "")
    }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        SettingsCard("Presupuesto mensual") {
            Text(
                "Recibirás una alerta al llegar al 80% y al 100%, y el panel se pondrá en rojo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c in '0'..'9' }.take(12) },
                label = { Text("Monto máximo al mes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { vm.saveBudget(budgetText) }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar presupuesto")
            }
        }

        SettingsCard("Recordatorio diario") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Avisarme para registrar mis gastos",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = {
                        vm.setReminder(it, settings.reminderHour, settings.reminderMinute)
                    }
                )
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                enabled = settings.reminderEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hora: %02d:%02d".format(settings.reminderHour, settings.reminderMinute))
            }
        }

        SettingsCard("Tus datos") {
            Text(
                "Todo se guarda solo en este teléfono. La app no tiene permiso de internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { vm.exportCsv(onlyFiltered = false) { CsvExporter.share(context, it) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportar todo a CSV")
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setReminder(true, state.hour, state.minute)
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
