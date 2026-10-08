package com.janreins.piso

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.local.MIGRATION_1_2
import com.janreins.piso.data.local.MIGRATION_2_3
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a database exactly as version 2 of the app left it, then opens it with the current
 * Room schema. Room validates every table after migrating, so a migration that doesn't produce
 * the expected schema fails here instead of crashing on a user's phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion2Database() {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        listOf(
            "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `balance` REAL NOT NULL, `notes` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateMillis` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `subcategory` TEXT NOT NULL DEFAULT '', `amount` REAL NOT NULL, `note` TEXT NOT NULL, `accountId` INTEGER, `transferToId` INTEGER, `goalId` INTEGER, `goalFlow` TEXT, `debtId` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `limitAmount` REAL NOT NULL, `monthKey` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `isCompleted` INTEGER NOT NULL, `deadlineMillis` INTEGER, `accountId` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `originalAmount` REAL NOT NULL, `remainingAmount` REAL NOT NULL, `notes` TEXT NOT NULL, `dueMillis` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `investments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `currentValue` REAL NOT NULL, `notes` TEXT NOT NULL, `quantity` TEXT, `lastUpdatedMillis` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `user_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `isArchived` INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS `user_subcategories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parentCategoryName` TEXT NOT NULL, `name` TEXT NOT NULL, `isArchived` INTEGER NOT NULL DEFAULT 0)",
            // Categories: "Food" is expense-only, "Salary" income-only, "Other" exists for both
            "INSERT INTO user_categories (name, kind, isArchived) VALUES ('Food', 'EXPENSE', 0), ('Other', 'EXPENSE', 0), ('Salary', 'INCOME', 0), ('Other', 'INCOME', 0)",
            "INSERT INTO user_subcategories (parentCategoryName, name, isArchived) VALUES ('Food', 'Groceries', 0), ('Salary', 'Bonus', 0), ('Other', 'Misc', 1), ('Gone', 'Orphan', 0)",
            "INSERT INTO accounts (name, kind, balance, notes) VALUES ('Wallet', 'Cash', 1234.5, '')"
        ).forEach { db.execSQL(it) }
        db.version = 2
        db.close()
    }

    @Test
    fun migrate2To3_assignsSubcategoryKindsAndKeepsData() = runBlocking {
        createVersion2Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val subs = database.categoryDao().getAllSubcategories().first()
                .map { Triple(it.parentCategoryName, it.name, it.parentKind) }
                .toSet()

            assertEquals(
                setOf(
                    Triple("Food", "Groceries", "EXPENSE"),
                    Triple("Salary", "Bonus", "INCOME"),
                    // Shared name: one copy for each kind
                    Triple("Other", "Misc", "EXPENSE"),
                    Triple("Other", "Misc", "INCOME"),
                    Triple("Gone", "Orphan", "EXPENSE")
                ),
                subs
            )
            assertEquals(true, database.categoryDao().getAllSubcategories().first().filter { it.name == "Misc" }.all { it.isArchived })
            assertEquals(1234.5, database.accountDao().getAllAccounts().first().single().balance, 0.0)
        } finally {
            database.close()
        }
    }
}
