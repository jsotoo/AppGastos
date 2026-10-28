package com.minimalgastos.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date >= :from AND date < :to")
    suspend fun totalBetween(from: Long, to: Long): Double

    /** Última categoría que el usuario usó con esta misma descripción (aprendizaje local). */
    @Query(
        "SELECT category FROM expenses WHERE LOWER(TRIM(description)) = :description " +
            "ORDER BY date DESC LIMIT 1"
    )
    suspend fun lastCategoryFor(description: String): String?

    @Insert
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)
}
