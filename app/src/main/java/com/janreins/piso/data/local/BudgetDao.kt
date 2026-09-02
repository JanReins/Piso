package com.janreins.piso.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.janreins.piso.data.models.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY category ASC")
    fun getBudgetsForMonth(monthKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()
}
