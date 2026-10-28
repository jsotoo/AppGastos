package com.minimalgastos.app.util

import android.content.Context
import android.net.Uri
import android.content.Intent
import androidx.core.content.FileProvider
import com.minimalgastos.app.data.Category
import com.minimalgastos.app.data.Expense
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

object Format {
    /** Moneda según la región del teléfono, sin decimales. */
    fun money(value: Double): String =
        NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(value)
}

object CsvExporter {

    /** Escribe el CSV en la caché de la app y devuelve un Uri compartible mediante FileProvider. */
    fun write(context: Context, expenses: List<Expense>): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "gastos_${LocalDate.now()}.csv")
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿") // BOM para que Excel respete los acentos
            w.write("Fecha,Descripción,Categoría,Monto\n")
            val zone = ZoneId.systemDefault()
            expenses.forEach { e ->
                val date = Instant.ofEpochMilli(e.date).atZone(zone).toLocalDate()
                w.write(
                    listOf(
                        date.toString(),
                        escape(e.description),
                        escape(Category.fromName(e.category).label),
                        String.format(Locale.US, "%.2f", e.amount)
                    ).joinToString(",")
                )
                w.write("\n")
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Exportar gastos (CSV)")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escape(value: String): String {
        // Evita inyección de fórmulas en Excel/Sheets y escapa comillas.
        val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
