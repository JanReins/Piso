package com.janreins.piso.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.janreins.piso.data.models.Investment
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Query("SELECT * FROM investments ORDER BY currentValue DESC, id DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): Investment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestments(investments: List<Investment>)

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    @Query("DELETE FROM investments")
    suspend fun clearInvestments()
}
