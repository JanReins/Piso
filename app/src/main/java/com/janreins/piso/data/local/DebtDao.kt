package com.janreins.piso.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.janreins.piso.data.models.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY remainingAmount > 0 DESC, id DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE remainingAmount > 0 ORDER BY id DESC")
    fun getOpenDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<Debt>)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts")
    suspend fun clearDebts()
}
