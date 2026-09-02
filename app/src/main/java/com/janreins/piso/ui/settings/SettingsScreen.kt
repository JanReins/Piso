package com.janreins.piso.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.piso.ui.MainViewModel
import com.janreins.piso.ui.components.ConfirmDialog
import com.janreins.piso.ui.components.PisoCard
import com.janreins.piso.ui.components.PisoOutlinedButton
import com.janreins.piso.ui.components.PisoPrimaryButton
import com.janreins.piso.ui.components.PisoTopBar
import com.janreins.piso.ui.theme.ExpenseContainer
import com.janreins.piso.ui.theme.ExpenseRed
import com.janreins.piso.ui.theme.TealContainer
import com.janreins.piso.ui.theme.TealPrimary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        topBar = {
            PisoTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- App Identity Header ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                PisoCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = TealPrimary.copy(alpha = 0.2f),
                    contentPadding = 20.dp
                ) {
                    Text(
                        text = "Piso",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your private money book",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            // --- Privacy & Local Storage Note ---
            item {
                PisoCard(contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Piso does not connect to banks, servers, or the cloud. All data is kept securely in your phone's private storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Backup & Restore Section ---
            item {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                PisoCard(contentPadding = 16.dp) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Export your entire money book to a standard JSON backup file, or restore data from a previous backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportBackup { json ->
                                    showExportDialog = json
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_backup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importJsonText = ""
                                importErrorMessage = null
                                showImportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_backup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup", fontSize = 14.sp)
                        }
                    }
                }
            }

            // --- Clear Data Section ---
            item {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )
                Spacer(modifier = Modifier.height(8.dp))

                PisoCard(
                    borderColor = ExpenseRed.copy(alpha = 0.3f),
                    contentPadding = 16.dp
                ) {
                    Text(
                        text = "Clear All Data",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    )
                    Text(
                        text = "Permanently deletes all accounts, transactions, budgets, goals, debts, and investments. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_data_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ExpenseRed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Data", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- Clear All Data Confirm Dialog ---
    if (showClearConfirmDialog) {
        ConfirmDialog(
            title = "Clear All Data?",
            message = "Are you completely sure? This will remove all your records and reset your money book to a fresh state.",
            confirmText = "Clear Everything",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllData()
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }

    // --- Export Dialog ---
    showExportDialog?.let { jsonString ->
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("Backup Ready", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        text = "Your JSON backup is ready. You can copy it to clipboard or share it via another app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = jsonString.take(400) + if (jsonString.length > 400) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Piso Backup", jsonString)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showMessage("Backup copied to clipboard")
                        showExportDialog = null
                    },
                    modifier = Modifier.testTag("copy_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Piso Backup")
                            putExtra(Intent.EXTRA_TEXT, jsonString)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                        showExportDialog = null
                    },
                    modifier = Modifier.testTag("share_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        )
    }

    // --- Import Dialog ---
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your previously exported Piso JSON data below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importErrorMessage = null
                        },
                        placeholder = { Text("Paste JSON backup here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_json_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (importErrorMessage != null) {
                        Text(
                            text = importErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            importErrorMessage = "Please paste JSON backup data."
                            return@Button
                        }
                        viewModel.importBackup(importJsonText) { success ->
                            if (success) {
                                showImportDialog = false
                            } else {
                                importErrorMessage = "Invalid or corrupted backup JSON."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    modifier = Modifier.testTag("cancel_import_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
