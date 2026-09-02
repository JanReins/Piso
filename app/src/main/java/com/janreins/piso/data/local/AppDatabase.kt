package com.janreins.piso.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Budget
import com.janreins.piso.data.models.Debt
import com.janreins.piso.data.models.Goal
import com.janreins.piso.data.models.Investment
import com.janreins.piso.data.models.Transaction

/**
 * Main Room Database for Piso personal money book.
 */
@Database(
    entities = [
        Account::class,
        Transaction::class,
        Budget::class,
        Goal::class,
        Debt::class,
        Investment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
    abstract fun investmentDao(): InvestmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "piso_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
