package com.janreins.piso.data.repository

import androidx.room.withTransaction
import com.janreins.piso.data.local.AppDatabase
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.BackupData
import com.janreins.piso.data.models.Budget
import com.janreins.piso.data.models.Categories
import com.janreins.piso.data.models.Debt
import com.janreins.piso.data.models.Goal
import com.janreins.piso.data.models.Investment
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.models.UserSubcategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.max

/**
 * Single source of truth repository managing all financial operations and live account balances.
 */
class FinanceRepository(private val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val goalDao = database.goalDao()
    private val debtDao = database.debtDao()
    private val investmentDao = database.investmentDao()
    private val categoryDao = database.categoryDao()

    // --- Flows ---
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val activeGoals: Flow<List<Goal>> = goalDao.getActiveGoals()
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
    val openDebts: Flow<List<Debt>> = debtDao.getOpenDebts()
    val allInvestments: Flow<List<Investment>> = investmentDao.getAllInvestments()
    val allCategories: Flow<List<UserCategory>> = categoryDao.getAllCategories()
    val allSubcategories: Flow<List<UserSubcategory>> = categoryDao.getAllSubcategories()

    fun getRecentTransactions(limit: Int = 6): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    fun getBudgetsForMonth(monthKey: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(monthKey)

    fun getCategoriesByKind(kind: String): Flow<List<UserCategory>> =
        categoryDao.getCategoriesByKind(kind)

    fun getSubcategoriesForParent(parentName: String): Flow<List<UserSubcategory>> =
        categoryDao.getSubcategoriesForParent(parentName)

    // --- Seeding Default Categories ---
    suspend fun seedDefaultCategoriesIfEmpty() {
        val existing = categoryDao.getAllCategories().first()
        if (existing.isEmpty()) {
            val expenseCats = Categories.EXPENSE.map {
                UserCategory(name = it, kind = "EXPENSE", isArchived = false)
            }
            val incomeCats = Categories.INCOME.map {
                UserCategory(name = it, kind = "INCOME", isArchived = false)
            }
            categoryDao.insertCategories(expenseCats + incomeCats)

            val defaultFoodSubcategories = listOf(
                UserSubcategory(parentCategoryName = "Food", name = "Groceries", isArchived = false),
                UserSubcategory(parentCategoryName = "Food", name = "Dining out", isArchived = false),
                UserSubcategory(parentCategoryName = "Food", name = "Coffee", isArchived = false)
            )
            categoryDao.insertSubcategories(defaultFoodSubcategories)
        }
    }

    // --- User Category CRUD ---
    suspend fun insertCategory(category: UserCategory): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategoryName(category: UserCategory, newName: String) {
        val trimmedNewName = newName.trim()
        if (trimmedNewName.isEmpty() || trimmedNewName == category.name) return
        database.withTransaction {
            categoryDao.updateCategory(category.copy(name = trimmedNewName))
            categoryDao.updateSubcategoriesParentName(category.name, trimmedNewName)
            categoryDao.updateTransactionsCategoryName(category.name, trimmedNewName)
            categoryDao.updateBudgetsCategoryName(category.name, trimmedNewName)
        }
    }

    suspend fun setCategoryArchived(category: UserCategory, isArchived: Boolean) {
        categoryDao.updateCategory(category.copy(isArchived = isArchived))
    }

    suspend fun deleteCategory(category: UserCategory) {
        database.withTransaction {
            val count = categoryDao.countTransactionsForCategory(category.name)
            if (count > 0) {
                // If transactions use this category, archive it to keep historical data intact
                categoryDao.updateCategory(category.copy(isArchived = true))
            } else {
                categoryDao.deleteCategory(category)
            }
        }
    }

    // --- User Subcategory CRUD ---
    suspend fun insertSubcategory(subcategory: UserSubcategory): Long =
        categoryDao.insertSubcategory(subcategory)

    suspend fun updateSubcategoryName(subcategory: UserSubcategory, newName: String) {
        val trimmedNewName = newName.trim()
        if (trimmedNewName.isEmpty() || trimmedNewName == subcategory.name) return
        database.withTransaction {
            categoryDao.updateSubcategory(subcategory.copy(name = trimmedNewName))
            categoryDao.updateTransactionsSubcategoryName(
                subcategory.parentCategoryName,
                subcategory.name,
                trimmedNewName
            )
        }
    }

    suspend fun setSubcategoryArchived(subcategory: UserSubcategory, isArchived: Boolean) {
        categoryDao.updateSubcategory(subcategory.copy(isArchived = isArchived))
    }

    suspend fun deleteSubcategory(subcategory: UserSubcategory) {
        database.withTransaction {
            val count = categoryDao.countTransactionsForSubcategory(
                subcategory.parentCategoryName,
                subcategory.name
            )
            if (count > 0) {
                // Archive if referenced
                categoryDao.updateSubcategory(subcategory.copy(isArchived = true))
            } else {
                categoryDao.deleteSubcategory(subcategory)
            }
        }
    }

    // --- Accounts ---
    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun hasTransactionsForAccount(accountId: Long): Boolean =
        transactionDao.getTransactionCountForAccount(accountId) > 0

    suspend fun deleteAccount(account: Account): Boolean {
        if (hasTransactionsForAccount(account.id)) {
            return false
        }
        accountDao.deleteAccount(account)
        return true
    }

    // --- Transactions & Balance Logic ---
    suspend fun addTransaction(tx: Transaction): Long {
        return database.withTransaction {
            applyTransactionBalance(tx)
            transactionDao.insertTransaction(tx)
        }
    }

    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        database.withTransaction {
            // Revert old transaction effect
            revertTransactionBalance(oldTx)
            // Apply new transaction effect
            applyTransactionBalance(newTx)
            transactionDao.updateTransaction(newTx)
        }
    }

    suspend fun deleteTransaction(tx: Transaction) {
        database.withTransaction {
            revertTransactionBalance(tx)
            transactionDao.deleteTransaction(tx)
        }
    }

    private suspend fun applyTransactionBalance(tx: Transaction) {
        when (tx.type) {
            "INCOME" -> {
                tx.accountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        accountDao.updateAccount(acc.copy(balance = acc.balance + tx.amount))
                    }
                }
            }
            "EXPENSE" -> {
                tx.accountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        accountDao.updateAccount(acc.copy(balance = acc.balance - tx.amount))
                    }
                }
            }
            "TRANSFER" -> {
                tx.accountId?.let { fromId ->
                    val fromAcc = accountDao.getAccountById(fromId)
                    if (fromAcc != null) {
                        accountDao.updateAccount(fromAcc.copy(balance = fromAcc.balance - tx.amount))
                    }
                }
                tx.transferToId?.let { toId ->
                    val toAcc = accountDao.getAccountById(toId)
                    if (toAcc != null) {
                        accountDao.updateAccount(toAcc.copy(balance = toAcc.balance + tx.amount))
                    }
                }
            }
        }
    }

    private suspend fun revertTransactionBalance(tx: Transaction) {
        when (tx.type) {
            "INCOME" -> {
                tx.accountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        accountDao.updateAccount(acc.copy(balance = acc.balance - tx.amount))
                    }
                }
            }
            "EXPENSE" -> {
                tx.accountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        accountDao.updateAccount(acc.copy(balance = acc.balance + tx.amount))
                    }
                }
                // If it was linked to a debt payment, restore remaining debt amount
                tx.debtId?.let { debtId ->
                    val debt = debtDao.getDebtById(debtId)
                    if (debt != null) {
                        debtDao.updateDebt(debt.copy(remainingAmount = debt.remainingAmount + tx.amount))
                    }
                }
                // If it was linked to a goal contribution, adjust goal current amount
                if (tx.goalFlow == "IN" && tx.goalId != null) {
                    val goal = goalDao.getGoalById(tx.goalId)
                    if (goal != null) {
                        val newAmount = max(0.0, goal.currentAmount - tx.amount)
                        goalDao.updateGoal(goal.copy(currentAmount = newAmount, isCompleted = false))
                    }
                }
            }
            "TRANSFER" -> {
                tx.accountId?.let { fromId ->
                    val fromAcc = accountDao.getAccountById(fromId)
                    if (fromAcc != null) {
                        accountDao.updateAccount(fromAcc.copy(balance = fromAcc.balance + tx.amount))
                    }
                }
                tx.transferToId?.let { toId ->
                    val toAcc = accountDao.getAccountById(toId)
                    if (toAcc != null) {
                        accountDao.updateAccount(toAcc.copy(balance = toAcc.balance - tx.amount))
                    }
                }
            }
        }
    }

    // --- Goals ---
    suspend fun insertGoal(goal: Goal): Long = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)

    suspend fun addMoneyToGoal(goalId: Long, amount: Double, fromAccountId: Long?): Boolean {
        return database.withTransaction {
            val goal = goalDao.getGoalById(goalId) ?: return@withTransaction false
            val updatedAmount = goal.currentAmount + amount
            val completed = updatedAmount >= goal.targetAmount
            val updatedGoal = goal.copy(
                currentAmount = updatedAmount,
                isCompleted = completed
            )
            goalDao.updateGoal(updatedGoal)

            // Deduct from account & record goal transaction
            if (fromAccountId != null) {
                val acc = accountDao.getAccountById(fromAccountId)
                if (acc != null) {
                    accountDao.updateAccount(acc.copy(balance = acc.balance - amount))
                }
            }

            val tx = Transaction(
                dateMillis = System.currentTimeMillis(),
                type = "EXPENSE",
                category = "Savings",
                subcategory = "",
                amount = amount,
                note = "Added to ${goal.name}",
                accountId = fromAccountId,
                goalId = goalId,
                goalFlow = "IN"
            )
            transactionDao.insertTransaction(tx)
            completed
        }
    }

    // --- Debts ---
    suspend fun insertDebt(debt: Debt): Long = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.deleteDebt(debt)

    suspend fun recordDebtPayment(debtId: Long, amount: Double, fromAccountId: Long?) {
        database.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction
            val newRemaining = max(0.0, debt.remainingAmount - amount)
            debtDao.updateDebt(debt.copy(remainingAmount = newRemaining))

            if (fromAccountId != null) {
                val acc = accountDao.getAccountById(fromAccountId)
                if (acc != null) {
                    accountDao.updateAccount(acc.copy(balance = acc.balance - amount))
                }
            }

            val tx = Transaction(
                dateMillis = System.currentTimeMillis(),
                type = "EXPENSE",
                category = "Debt",
                subcategory = "",
                amount = amount,
                note = "Payment for ${debt.name}",
                accountId = fromAccountId,
                debtId = debtId
            )
            transactionDao.insertTransaction(tx)
        }
    }

    // --- Budgets ---
    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- Investments ---
    suspend fun insertInvestment(investment: Investment): Long = investmentDao.insertInvestment(investment)
    suspend fun updateInvestment(investment: Investment) = investmentDao.updateInvestment(investment)
    suspend fun deleteInvestment(investment: Investment) = investmentDao.deleteInvestment(investment)

    // --- Backup & Restore & Clear ---
    suspend fun exportBackupJson(): String {
        val accounts = accountDao.getAllAccounts().first()
        val transactions = transactionDao.getAllTransactions().first()
        val budgets = budgetDao.getAllBudgets().first()
        val goals = goalDao.getAllGoals().first()
        val debts = debtDao.getAllDebts().first()
        val investments = investmentDao.getAllInvestments().first()
        val categories = categoryDao.getAllCategories().first()
        val subcategories = categoryDao.getAllSubcategories().first()

        val data = BackupData(
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            debts = debts,
            investments = investments,
            categories = categories,
            subcategories = subcategories
        )
        return data.toJsonString()
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        val data = BackupData.fromJsonString(jsonString) ?: return false
        return database.withTransaction {
            accountDao.clearAccounts()
            transactionDao.clearTransactions()
            budgetDao.clearBudgets()
            goalDao.clearGoals()
            debtDao.clearDebts()
            investmentDao.clearInvestments()

            accountDao.insertAccounts(data.accounts)
            transactionDao.insertTransactions(data.transactions)
            budgetDao.insertBudgets(data.budgets)
            goalDao.insertGoals(data.goals)
            debtDao.insertDebts(data.debts)
            investmentDao.insertInvestments(data.investments)

            if (data.categories.isNotEmpty()) {
                categoryDao.clearCategories()
                categoryDao.clearSubcategories()
                categoryDao.insertCategories(data.categories)
                categoryDao.insertSubcategories(data.subcategories)
            } else {
                seedDefaultCategoriesIfEmpty()
            }
            true
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            accountDao.clearAccounts()
            transactionDao.clearTransactions()
            budgetDao.clearBudgets()
            goalDao.clearGoals()
            debtDao.clearDebts()
            investmentDao.clearInvestments()
            // Reset categories to defaults
            categoryDao.clearCategories()
            categoryDao.clearSubcategories()
            seedDefaultCategoriesIfEmpty()
        }
    }
}
