package com.janreins.piso.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.ui.MainViewModel
import com.janreins.piso.ui.components.ConfirmDialog
import com.janreins.piso.ui.components.PisoCard
import com.janreins.piso.ui.components.PisoEmptyState
import com.janreins.piso.ui.components.PisoTopBar
import com.janreins.piso.ui.theme.ExpenseContainer
import com.janreins.piso.ui.theme.ExpenseRed
import com.janreins.piso.ui.theme.IncomeContainer
import com.janreins.piso.ui.theme.IncomeGreen
import com.janreins.piso.ui.theme.TealContainer
import com.janreins.piso.ui.theme.TealPrimary
import com.janreins.piso.util.CurrencyUtil
import com.janreins.piso.util.DateUtil

@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val subcategories by viewModel.subcategories.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val activityFilter by viewModel.activityFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Filter transactions by selected month & filter type
    val filteredTransactions = remember(transactions, selectedMonthKey, activityFilter) {
        transactions.filter { tx ->
            val matchesMonth = DateUtil.getMonthKey(tx.dateMillis) == selectedMonthKey
            val matchesType = when (activityFilter) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE"
                "TRANSFER" -> tx.type == "TRANSFER"
                else -> true
            }
            matchesMonth && matchesType
        }
    }

    val accountsMap = remember(accounts) {
        accounts.associateBy { it.id }
    }

    Scaffold(
        modifier = modifier.testTag("activity_screen"),
        topBar = {
            PisoTopBar(
                title = "Activity",
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("activity_add_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("activity_fab"),
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Month Selector ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = DateUtil.shiftMonthKey(selectedMonthKey, -1)
                        viewModel.setSelectedMonthKey(prev)
                    },
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }

                Text(
                    text = DateUtil.getMonthDisplayName(selectedMonthKey),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        val next = DateUtil.shiftMonthKey(selectedMonthKey, 1)
                        viewModel.setSelectedMonthKey(next)
                    },
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }

            // --- Type Filters Row ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL" to "All", "INCOME" to "Income", "EXPENSE" to "Expense", "TRANSFER" to "Transfers")
                items(filters) { (key, label) ->
                    val isSelected = activityFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setActivityFilter(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (key) {
                                "INCOME" -> IncomeGreen
                                "EXPENSE" -> ExpenseRed
                                "TRANSFER" -> TealPrimary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // --- Transactions List or Empty State ---
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PisoEmptyState(
                        message = "No transactions yet – add your first one!",
                        icon = Icons.Default.ReceiptLong
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val accountName = tx.accountId?.let { accountsMap[it]?.name }
                        val toAccountName = tx.transferToId?.let { accountsMap[it]?.name }

                        PisoCard(
                            contentPadding = 14.dp,
                            onClick = {
                                if (tx.goalId != null || tx.goalFlow != null) {
                                    viewModel.showMessage("Change this from the Goals tab")
                                } else if (tx.debtId != null) {
                                    viewModel.showMessage("Change this from the Debts section")
                                } else {
                                    transactionToEdit = tx
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (tx.type) {
                                                    "INCOME" -> IncomeContainer
                                                    "EXPENSE" -> ExpenseContainer
                                                    else -> TealContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (tx.type) {
                                                "INCOME" -> Icons.Default.ArrowDownward
                                                "EXPENSE" -> Icons.Default.ArrowUpward
                                                else -> Icons.Default.ReceiptLong
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = when (tx.type) {
                                                "INCOME" -> IncomeGreen
                                                "EXPENSE" -> ExpenseRed
                                                else -> TealPrimary
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (tx.type == "TRANSFER") {
                                                if (accountName != null && toAccountName != null) {
                                                    "$accountName → $toAccountName"
                                                } else {
                                                    "Transfer"
                                                }
                                            } else {
                                                tx.category.ifBlank { tx.type }
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = buildString {
                                                if (tx.type != "TRANSFER" && tx.subcategory.isNotBlank()) {
                                                    append(tx.subcategory)
                                                    append(" · ")
                                                }
                                                append(DateUtil.formatDate(tx.dateMillis))
                                                if (accountName != null && tx.type != "TRANSFER") {
                                                    append(" · ")
                                                    append(accountName)
                                                }
                                                if (tx.note.isNotBlank()) {
                                                    append(" · ")
                                                    append(tx.note)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (tx.type) {
                                            "INCOME" -> "+${CurrencyUtil.formatPeso(tx.amount)}"
                                            "EXPENSE" -> "-${CurrencyUtil.formatPeso(tx.amount)}"
                                            else -> CurrencyUtil.formatPeso(tx.amount)
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (tx.type) {
                                                "INCOME" -> IncomeGreen
                                                "EXPENSE" -> ExpenseRed
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { transactionToDelete = tx },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // --- Add Transaction Dialog ---
    if (showAddDialog) {
        TransactionDialog(
            accounts = accounts,
            categories = categories,
            subcategories = subcategories,
            onAddCategory = { name, kind, onComplete ->
                viewModel.addCategory(name, kind) { success, _ ->
                    if (success) onComplete(name)
                }
            },
            onAddSubcategory = { parent, name, onComplete ->
                viewModel.addSubcategory(parent, name) { success, _ ->
                    if (success) onComplete(name)
                }
            },
            onDismiss = { showAddDialog = false },
            onSave = { newTx ->
                viewModel.addTransaction(newTx)
                showAddDialog = false
            }
        )
    }

    // --- Edit Transaction Dialog ---
    transactionToEdit?.let { currentTx ->
        TransactionDialog(
            initialTransaction = currentTx,
            accounts = accounts,
            categories = categories,
            subcategories = subcategories,
            onAddCategory = { name, kind, onComplete ->
                viewModel.addCategory(name, kind) { success, _ ->
                    if (success) onComplete(name)
                }
            },
            onAddSubcategory = { parent, name, onComplete ->
                viewModel.addSubcategory(parent, name) { success, _ ->
                    if (success) onComplete(name)
                }
            },
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(currentTx, updatedTx)
                transactionToEdit = null
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    transactionToDelete?.let { txToDelete ->
        ConfirmDialog(
            title = "Delete Transaction?",
            message = "Are you sure you want to delete this ${txToDelete.type.lowercase()} of ${CurrencyUtil.formatPeso(txToDelete.amount)}? Account balances will be updated.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteTransaction(txToDelete)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }
}
