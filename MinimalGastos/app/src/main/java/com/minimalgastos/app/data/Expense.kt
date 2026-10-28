package com.minimalgastos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expenses", indices = [Index("date")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    /** Nombre del enum [Category]. */
    val category: String,
    /** Epoch millis. */
    val date: Long
)
