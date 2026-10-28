package com.minimalgastos.app.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    suspend fun add(expense: Expense) {
        dao.insert(expense)
    }

    suspend fun delete(expense: Expense) {
        dao.delete(expense)
    }

    suspend fun totalBetween(from: Long, to: Long): Double = dao.totalBetween(from, to)

    suspend fun learnedCategory(description: String): Category? =
        dao.lastCategoryFor(description.trim().lowercase())?.let { Category.fromName(it) }
}
