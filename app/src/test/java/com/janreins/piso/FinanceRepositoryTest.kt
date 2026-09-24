package com.janreins.piso

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Debt
import com.janreins.piso.data.models.Goal
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.repository.FinanceRepository
import com.janreins.piso.util.CurrencyUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("₱1,250.00", CurrencyUtil.formatPeso(1250.0))
        assertEquals("₱0.00", CurrencyUtil.formatPeso(0.0))
        assertEquals("-₱500.50", CurrencyUtil.formatPeso(-500.5))
    }

    @Test
    fun testAddIncomeUpdatesAccountBalance() = runBlocking {
        val accountId = repository.insertAccount(
            Account(name = "BPI Savings", kind = "Bank", balance = 10000.0)
        )

        repository.addTransaction(
            Transaction(
                dateMillis = System.currentTimeMillis(),
                type = "INCOME",
                category = "Salary",
                amount = 25000.0,
                accountId = accountId
            )
        )

        val updatedAccount = database.accountDao().getAccountById(accountId)
        assertNotNull(updatedAccount)
        assertEquals(35000.0, updatedAccount!!.balance, 0.001)
    }

    @Test
    fun testAddExpenseDeductsAccountBalance() = runBlocking {
        val accountId = repository.insertAccount(
            Account(name = "GCash", kind = "E-Wallet", balance = 5000.0)
        )

        repository.addTransaction(
            Transaction(
                dateMillis = System.currentTimeMillis(),
                type = "EXPENSE",
                category = "Food",
                amount = 450.0,
                accountId = accountId
            )
        )

        val updatedAccount = database.accountDao().getAccountById(accountId)
        assertNotNull(updatedAccount)
        assertEquals(4550.0, updatedAccount!!.balance, 0.001)
    }

    @Test
    fun testTransferBetweenAccounts() = runBlocking {
        val fromId = repository.insertAccount(
            Account(name = "Bank Account", kind = "Bank", balance = 20000.0)
        )
        val toId = repository.insertAccount(
            Account(name = "GCash", kind = "E-Wallet", balance = 1000.0)
        )

        repository.addTransaction(
            Transaction(
                dateMillis = System.currentTimeMillis(),
                type = "TRANSFER",
                category = "Transfer",
                amount = 3000.0,
                accountId = fromId,
                transferToId = toId
            )
        )

        val fromAcc = database.accountDao().getAccountById(fromId)
        val toAcc = database.accountDao().getAccountById(toId)

        assertEquals(17000.0, fromAcc!!.balance, 0.001)
        assertEquals(4000.0, toAcc!!.balance, 0.001)
    }

    @Test
    fun testAddMoneyToGoal() = runBlocking {
        val accountId = repository.insertAccount(
            Account(name = "Wallet", kind = "Cash", balance = 10000.0)
        )
        val goalId = repository.insertGoal(
            Goal(name = "Emergency Fund", targetAmount = 50000.0, currentAmount = 5000.0)
        )

        val completed = repository.addMoneyToGoal(goalId, 2000.0, accountId)

        val updatedGoal = database.goalDao().getGoalById(goalId)
        val updatedAccount = database.accountDao().getAccountById(accountId)

        assertEquals(7000.0, updatedGoal!!.currentAmount, 0.001)
        assertEquals(8000.0, updatedAccount!!.balance, 0.001)
        assertEquals(false, completed)
    }

    @Test
    fun testRecordDebtPayment() = runBlocking {
        val accountId = repository.insertAccount(
            Account(name = "Checking", kind = "Bank", balance = 15000.0)
        )
        val debtId = repository.insertDebt(
            Debt(name = "Credit Card", kind = "Credit Card", originalAmount = 10000.0, remainingAmount = 10000.0)
        )

        repository.recordDebtPayment(debtId, 2500.0, accountId)

        val updatedDebt = database.debtDao().getDebtById(debtId)
        val updatedAccount = database.accountDao().getAccountById(accountId)

        assertEquals(7500.0, updatedDebt!!.remainingAmount, 0.001)
        assertEquals(12500.0, updatedAccount!!.balance, 0.001)
    }

    @Test
    fun testBackupExportAndImport() = runBlocking {
        repository.insertAccount(Account(name = "Test Account", kind = "Cash", balance = 1234.0))

        val json = repository.exportBackupJson()
        assertTrue(json.contains("Test Account"))

        repository.clearAllData()
        assertEquals(0, repository.allAccounts.first().size)

        val success = repository.importBackupJson(json)
        assertTrue(success)
        assertEquals(1, repository.allAccounts.first().size)
    }

    @Test
    fun testEditingDebtPaymentKeepsDebtBalance() = runBlocking {
        val accountId = repository.insertAccount(Account(name = "Checking", kind = "Bank", balance = 5000.0))
        val debtId = repository.insertDebt(
            Debt(name = "Loan", kind = "Personal", originalAmount = 3000.0, remainingAmount = 3000.0)
        )
        repository.recordDebtPayment(debtId, 1000.0, accountId)
        val paymentTx = repository.allTransactions.first().single()

        repository.updateTransaction(paymentTx, paymentTx.copy(amount = 1500.0, note = "Edited"))

        assertEquals(1500.0, database.debtDao().getDebtById(debtId)!!.remainingAmount, 0.001)
        assertEquals(3500.0, database.accountDao().getAccountById(accountId)!!.balance, 0.001)
    }

    @Test
    fun testEditingGoalContributionKeepsGoalBalance() = runBlocking {
        val accountId = repository.insertAccount(Account(name = "Wallet", kind = "Cash", balance = 5000.0))
        val goalId = repository.insertGoal(Goal(name = "Laptop", targetAmount = 1000.0))
        repository.addMoneyToGoal(goalId, 400.0, accountId)
        val goalTx = repository.allTransactions.first().single()

        repository.updateTransaction(goalTx, goalTx.copy(amount = 1000.0))

        val goal = database.goalDao().getGoalById(goalId)!!
        assertEquals(1000.0, goal.currentAmount, 0.001)
        assertTrue(goal.isCompleted)
        assertEquals(4000.0, database.accountDao().getAccountById(accountId)!!.balance, 0.001)
    }

    @Test
    fun testDeletingOverpaymentRestoresOnlyOriginalAmount() = runBlocking {
        val debtId = repository.insertDebt(
            Debt(name = "IOU", kind = "Family", originalAmount = 800.0, remainingAmount = 800.0)
        )
        repository.recordDebtPayment(debtId, 1000.0, null)
        assertEquals(0.0, database.debtDao().getDebtById(debtId)!!.remainingAmount, 0.001)

        repository.deleteTransaction(repository.allTransactions.first().single())

        assertEquals(800.0, database.debtDao().getDebtById(debtId)!!.remainingAmount, 0.001)
    }

    @Test
    fun testRenamingCategoryOnlyAffectsItsOwnKind() = runBlocking {
        val expenseOther = UserCategory(name = "Other", kind = "EXPENSE")
        val incomeOther = UserCategory(name = "Other", kind = "INCOME")
        val expenseId = repository.insertCategory(expenseOther)
        repository.insertCategory(incomeOther)
        repository.addTransaction(Transaction(type = "EXPENSE", category = "Other", amount = 10.0))
        repository.addTransaction(Transaction(type = "INCOME", category = "Other", amount = 20.0))

        repository.updateCategoryName(expenseOther.copy(id = expenseId), "Misc")

        val txs = repository.allTransactions.first()
        assertEquals("Misc", txs.single { it.type == "EXPENSE" }.category)
        assertEquals("Other", txs.single { it.type == "INCOME" }.category)
    }
}
