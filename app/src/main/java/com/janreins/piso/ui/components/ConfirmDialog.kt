package com.janreins.piso.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.janreins.piso.ui.theme.ExpenseRed

/**
 * Reusable dialog for confirming destructive or significant actions.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("dialog_confirm_button"),
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                } else {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text(dismissText)
            }
        }
    )
}
