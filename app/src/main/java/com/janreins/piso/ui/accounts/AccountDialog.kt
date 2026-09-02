package com.janreins.piso.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.Categories
import com.janreins.piso.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(
    initialAccount: Account? = null,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var kind by remember { mutableStateOf(initialAccount?.kind ?: Categories.ACCOUNT_KINDS.first()) }
    var balanceText by remember {
        mutableStateOf(if (initialAccount != null) CurrencyUtil.formatInputAmount(initialAccount.balance) else "")
    }
    var notes by remember { mutableStateOf(initialAccount?.notes ?: "") }

    var kindDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAccount == null) "Add Account" else "Edit Account",
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
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. BPI Savings, GCash, Wallet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Kind Dropdown
                ExposedDropdownMenuBox(
                    expanded = kindDropdownExpanded,
                    onExpandedChange = { kindDropdownExpanded = !kindDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = kind,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("account_kind_select"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = kindDropdownExpanded,
                        onDismissRequest = { kindDropdownExpanded = false }
                    ) {
                        Categories.ACCOUNT_KINDS.forEach { k ->
                            DropdownMenuItem(
                                text = { Text(k) },
                                onClick = {
                                    kind = k
                                    kindDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Balance Field
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        errorMessage = null
                    },
                    label = { Text(if (initialAccount == null) "Starting Balance (₱)" else "Current Balance (₱)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Main payroll account") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_notes_input"),
                    shape = RoundedCornerShape(12.dp)
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
                    if (name.isBlank()) {
                        errorMessage = "Account name cannot be blank."
                        return@Button
                    }
                    val cleanBalance = balanceText.trim().replace(",", "").replace("₱", "")
                    val parsedBalance = cleanBalance.toDoubleOrNull() ?: 0.0

                    val account = Account(
                        id = initialAccount?.id ?: 0L,
                        name = name.trim(),
                        kind = kind,
                        balance = parsedBalance,
                        notes = notes.trim()
                    )
                    onSave(account)
                },
                modifier = Modifier.testTag("account_save_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (initialAccount == null) "Add Account" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("account_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
