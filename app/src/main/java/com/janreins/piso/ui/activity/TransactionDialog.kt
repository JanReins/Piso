package com.janreins.piso.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Categories
import com.janreins.piso.data.models.Transaction
import com.janreins.piso.ui.theme.ExpenseRed
import com.janreins.piso.ui.theme.IncomeGreen
import com.janreins.piso.ui.theme.TealPrimary
import com.janreins.piso.util.CurrencyUtil
import com.janreins.piso.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    initialTransaction: Transaction? = null,
    preselectedType: String = "EXPENSE",
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var type by remember {
        mutableStateOf(initialTransaction?.type ?: preselectedType)
    }
    var amountText by remember {
        mutableStateOf(if (initialTransaction != null) CurrencyUtil.formatInputAmount(initialTransaction.amount) else "")
    }
    var category by remember {
        mutableStateOf(
            initialTransaction?.category ?: if (type == "INCOME") Categories.INCOME.first() else Categories.EXPENSE.first()
        )
    }
    var note by remember {
        mutableStateOf(initialTransaction?.note ?: "")
    }
    var dateMillis by remember {
        mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis())
    }

    var selectedAccountId by remember {
        mutableStateOf(initialTransaction?.accountId ?: accounts.firstOrNull()?.id)
    }
    var transferToAccountId by remember {
        mutableStateOf(initialTransaction?.transferToId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id)
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var transferToDropdownExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer").forEach { (typeKey, label) ->
                        val isSelected = type == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                type = typeKey
                                if (typeKey == "INCOME" && !Categories.INCOME.contains(category)) {
                                    category = Categories.INCOME.first()
                                } else if (typeKey == "EXPENSE" && !Categories.EXPENSE.contains(category)) {
                                    category = Categories.EXPENSE.first()
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (typeKey) {
                                    "INCOME" -> IncomeGreen
                                    "EXPENSE" -> ExpenseRed
                                    else -> TealPrimary
                                },
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount (₱)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown (Hidden for Transfer)
                if (type != "TRANSFER") {
                    val categoryList = if (type == "INCOME") Categories.INCOME else Categories.EXPENSE
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("transaction_category_select"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categoryList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Account Selection
                if (accounts.isNotEmpty()) {
                    val currentAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"
                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentAccountName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (type == "TRANSFER") "From Account" else "Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("transaction_account_select"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${CurrencyUtil.formatPeso(acc.balance)})") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Transfer To Account (Only for TRANSFER)
                    if (type == "TRANSFER") {
                        val toAccountName = accounts.find { it.id == transferToAccountId }?.name ?: "Select Destination"
                        ExposedDropdownMenuBox(
                            expanded = transferToDropdownExpanded,
                            onExpandedChange = { transferToDropdownExpanded = !transferToDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = toAccountName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Transfer To") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transferToDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                                    .testTag("transaction_transfer_to_select"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = transferToDropdownExpanded,
                                onDismissRequest = { transferToDropdownExpanded = false }
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${CurrencyUtil.formatPeso(acc.balance)})") },
                                        onClick = {
                                            transferToAccountId = acc.id
                                            transferToDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Grocery, Lunch with friends") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date display
                Text(
                    text = "Date: ${DateUtil.formatDate(dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = CurrencyUtil.parsePositiveAmount(amountText)
                    if (parsedAmount == null || parsedAmount <= 0) {
                        errorMessage = "Please enter an amount greater than 0."
                        return@Button
                    }
                    if (accounts.isNotEmpty() && selectedAccountId == null) {
                        errorMessage = "Please select an account."
                        return@Button
                    }
                    if (type == "TRANSFER" && selectedAccountId == transferToAccountId) {
                        errorMessage = "Transfer source and destination accounts must be different."
                        return@Button
                    }

                    val tx = Transaction(
                        id = initialTransaction?.id ?: 0L,
                        dateMillis = dateMillis,
                        type = type,
                        category = if (type == "TRANSFER") "Transfer" else category.trim(),
                        amount = parsedAmount,
                        note = note.trim(),
                        accountId = selectedAccountId,
                        transferToId = if (type == "TRANSFER") transferToAccountId else null,
                        goalId = initialTransaction?.goalId,
                        goalFlow = initialTransaction?.goalFlow,
                        debtId = initialTransaction?.debtId
                    )
                    onSave(tx)
                },
                modifier = Modifier.testTag("transaction_save_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (initialTransaction == null) "Save" else "Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("transaction_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
