package com.janreins.piso.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.janreins.piso.data.models.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()
}
