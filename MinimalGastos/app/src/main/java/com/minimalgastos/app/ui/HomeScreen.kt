@file:OptIn(ExperimentalLayoutApi::class)

package com.minimalgastos.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalgastos.app.data.Category
import com.minimalgastos.app.ui.theme.budgetColors
import com.minimalgastos.app.util.Format
import com.minimalgastos.app.viewmodel.DashboardState
import com.minimalgastos.app.viewmodel.ExpenseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: ExpenseViewModel) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val month = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        Text(month, style = MaterialTheme.typography.headlineSmall)

        DashboardCard(dashboard)
        QuickEntryCard(vm)
        BreakdownCard(dashboard)
    }
}

@Composable
private fun DashboardCard(state: DashboardState) {
    val colors = budgetColors(state.level)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.content
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gastado este mes", style = MaterialTheme.typography.labelLarge)
            Text(
                Format.money(state.total),
                style = MaterialTheme.typography.headlineLarge
            )
            if (state.hasBudget) {
                LinearProgressIndicator(
                    progress = { min(state.ratio, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = colors.content,
                    trackColor = colors.content.copy(alpha = 0.25f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (state.remaining >= 0) "Te quedan ${Format.money(state.remaining)}"
                        else "Excedido por ${Format.money(-state.remaining)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${(state.ratio * 100).roundToInt()}% de ${Format.money(state.budget)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                when (state.level) {
                    100 -> Text("Superaste tu presupuesto mensual.", style = MaterialTheme.typography.labelLarge)
                    80 -> Text("Ya usaste más del 80% de tu presupuesto.", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    "Define un presupuesto mensual en Ajustes para ver cuánto te queda.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun QuickEntryCard(vm: ExpenseViewModel) {
    val focus = LocalFocusManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nuevo gasto", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = vm.amount,
                onValueChange = vm::onAmountChange,
                label = { Text("Monto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
            )
            OutlinedTextField(
                value = vm.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Descripción (ej. Uber, almuerzo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
            )

            Text(
                if (vm.isAutoCategory) "Categoría · sugerida automáticamente" else "Categoría",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = vm.selectedCategory == category,
                        onClick = { vm.selectCategory(category) },
                        label = { Text(category.label) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(category.color, CircleShape)
                            )
                        }
                    )
                }
            }

            Button(
                onClick = {
                    vm.addExpense()
                    focus.clearFocus()
                },
                enabled = vm.canAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar gasto")
            }
        }
    }
}

@Composable
private fun BreakdownCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Por categoría", style = MaterialTheme.typography.titleMedium)

            if (state.byCategory.isEmpty()) {
                Text(
                    "Aún no hay gastos este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(state)
            }

            state.byCategory.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(item.category.color, CircleShape)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            item.category.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            Format.money(item.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "  ${(item.fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = item.category.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(state: DashboardState) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val strokeWidth = 26.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            var start = -90f
            state.byCategory.forEach { item ->
                val sweep = 360f * item.fraction
                drawArc(
                    color = item.category.color,
                    startAngle = start,
                    // Pequeña separación visual entre segmentos.
                    sweepAngle = max(sweep - 1.5f, 0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Format.money(state.total), style = MaterialTheme.typography.titleMedium)
        }
    }
}
