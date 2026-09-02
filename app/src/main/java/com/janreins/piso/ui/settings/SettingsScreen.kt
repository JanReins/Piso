package com.janreins.piso.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.piso.data.local.ThemeMode
import com.janreins.piso.data.models.BackupData
import com.janreins.piso.ui.MainViewModel
import com.janreins.piso.ui.components.ConfirmDialog
import com.janreins.piso.ui.components.PisoCard
import com.janreins.piso.ui.components.PisoTopBar
import com.janreins.piso.ui.theme.ExpenseRed
import com.janreins.piso.ui.theme.IncomeGreen
import com.janreins.piso.ui.theme.TealContainer
import com.janreins.piso.ui.theme.TealPrimary
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Dialog States
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportTextDialog by remember { mutableStateOf<String?>(null) }
    var showImportTextDialog by remember { mutableStateOf(false) }

    var pendingFileImportJson by remember { mutableStateOf<String?>(null) }

    // System File Pickers
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup { jsonString ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.showMessage("Backup file saved successfully")
                } catch (e: Exception) {
                    viewModel.showMessage("Failed to write backup file")
                }
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.use { it.readText() }

                // Validate before asking
                val parsed = BackupData.fromJsonString(content)
                if (parsed != null) {
                    pendingFileImportJson = content
                } else {
                    viewModel.showMessage("This is not a valid Piso backup.")
                }
            } catch (e: Exception) {
                viewModel.showMessage("This is not a valid Piso backup.")
            }
        }
    }

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
            // --- Section 1: Offline Account Profile ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Offline Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                PisoCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = TealPrimary.copy(alpha = 0.25f),
                    contentPadding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed in locally as ${userProfile.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "This account lives only on this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("edit_name_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit name", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (userProfile.hasPassword) {
                                    viewModel.lockApp()
                                } else {
                                    showSetPasswordDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("lock_piso_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Piso", fontSize = 13.sp)
                        }
                    }
                }
            }

            // --- Section 2: Password Security ---
            item {
                Text(
                    text = "Security & App Lock",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                PisoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (userProfile.hasPassword) TealContainer else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userProfile.hasPassword) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (userProfile.hasPassword) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userProfile.hasPassword) "Password Lock Active" else "No Password Set",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (userProfile.hasPassword) "Lock screen requires password on opening." else "Piso opens directly without password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (userProfile.hasPassword) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("change_password_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showRemovePasswordDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("remove_password_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ExpenseRed
                                )
                            ) {
                                Icon(imageVector = Icons.Default.NoEncryption, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showSetPasswordDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("set_password_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Password")
                        }
                    }
                }
            }

            // --- Section 3: Appearance (Theme Selection) ---
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                PisoCard(contentPadding = 16.dp) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Choose your preferred interface theme. Default is Light.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionItem(
                            title = "Light",
                            icon = Icons.Default.LightMode,
                            isSelected = userProfile.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_option_light")
                        )

                        ThemeOptionItem(
                            title = "Dark",
                            icon = Icons.Default.DarkMode,
                            isSelected = userProfile.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_option_dark")
                        )

                        ThemeOptionItem(
                            title = "System",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = userProfile.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_option_system")
                        )
                    }
                }
            }

            // --- Section 4: Backup & Restore (Real Files + Fallback) ---
            item {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                PisoCard(contentPadding = 16.dp) {
                    Text(
                        text = "File Backup & Import",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Save your data to a .json file in Files/Drive, or restore from an existing Piso backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val filename = "Piso_Backup_$dateStr.json"
                                createDocLauncher.launch(filename)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_backup_file_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export File", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("import_backup_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import File", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Clipboard Fallback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manual text copy/paste fallback:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.exportBackup { json ->
                                        showExportTextDialog = json
                                    }
                                },
                                modifier = Modifier.testTag("export_text_fallback_button")
                            ) {
                                Text("Copy Text", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { showImportTextDialog = true },
                                modifier = Modifier.testTag("import_text_fallback_button")
                            ) {
                                Text("Paste Text", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // --- Section 5: Danger Zone ---
            item {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                        text = "Permanently deletes all accounts, transactions, budgets, goals, debts, and investments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
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

    // --- Dialogs ---

    // 1. Edit Name Dialog
    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(userProfile.displayName) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile Name", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            errorMsg = null
                        },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.trim().isBlank()) {
                            errorMsg = "Name cannot be blank."
                            return@Button
                        }
                        viewModel.updateDisplayName(newName.trim())
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("save_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    modifier = Modifier.testTag("cancel_name_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Set Password Dialog
    if (showSetPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var showPass by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPasswordDialog = false },
            title = { Text("Set Password Lock", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create a password to lock Piso when the app opens.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = {
                            newPass = it
                            errorMsg = null
                        },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("set_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = {
                            confirmPass = it
                            errorMsg = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("set_confirm_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.isBlank()) {
                            errorMsg = "Password cannot be blank."
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            errorMsg = "Passwords do not match."
                            return@Button
                        }
                        if (newPass.length < 4) {
                            errorMsg = "Password should be at least 4 characters."
                            return@Button
                        }
                        viewModel.setPassword(newPass)
                        showSetPasswordDialog = false
                    },
                    modifier = Modifier.testTag("confirm_set_password_button")
                ) {
                    Text("Set Password")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSetPasswordDialog = false },
                    modifier = Modifier.testTag("cancel_set_password_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Change Password Dialog
    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var showPass by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = {
                            currentPass = it
                            errorMsg = null
                        },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("change_current_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = {
                            newPass = it
                            errorMsg = null
                        },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("change_new_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = {
                            confirmPass = it
                            errorMsg = null
                        },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("change_confirm_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPass.isBlank()) {
                            errorMsg = "Please enter your current password."
                            return@Button
                        }
                        if (newPass.isBlank()) {
                            errorMsg = "New password cannot be blank."
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            errorMsg = "New passwords do not match."
                            return@Button
                        }
                        if (newPass.length < 4) {
                            errorMsg = "New password should be at least 4 characters."
                            return@Button
                        }
                        val success = viewModel.changePassword(currentPass, newPass)
                        if (success) {
                            showChangePasswordDialog = false
                        } else {
                            errorMsg = "Incorrect current password."
                        }
                    },
                    modifier = Modifier.testTag("confirm_change_password_button")
                ) {
                    Text("Change")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    modifier = Modifier.testTag("cancel_change_password_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Remove Password Dialog
    if (showRemovePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRemovePasswordDialog = false },
            title = { Text("Remove Password Lock", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your current password to remove lock protection from Piso:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = {
                            currentPass = it
                            errorMsg = null
                        },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("remove_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPass.isBlank()) {
                            errorMsg = "Please enter your password."
                            return@Button
                        }
                        val success = viewModel.removePassword(currentPass)
                        if (success) {
                            showRemovePasswordDialog = false
                        } else {
                            errorMsg = "Incorrect password."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_remove_password_button")
                ) {
                    Text("Remove Lock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemovePasswordDialog = false },
                    modifier = Modifier.testTag("cancel_remove_password_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. File Import Confirm Dialog
    pendingFileImportJson?.let { fileJson ->
        AlertDialog(
            onDismissRequest = { pendingFileImportJson = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Restore From File?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This replaces all current Piso data. Continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackup(fileJson) { success ->
                            if (success) {
                                pendingFileImportJson = null
                            } else {
                                viewModel.showMessage("This is not a valid Piso backup.")
                                pendingFileImportJson = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_file_import_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingFileImportJson = null },
                    modifier = Modifier.testTag("cancel_file_import_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Clear All Data Confirm Dialog
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

    // 7. Manual Text Export Dialog
    showExportTextDialog?.let { jsonString ->
        AlertDialog(
            onDismissRequest = { showExportTextDialog = null },
            title = { Text("Backup Text Ready", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        text = "You can copy this backup to clipboard or share it:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                        showExportTextDialog = null
                    },
                    modifier = Modifier.testTag("copy_backup_text_button")
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
                        showExportTextDialog = null
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        )
    }

    // 8. Manual Text Import Dialog
    if (showImportTextDialog) {
        var importJsonText by remember { mutableStateOf("") }
        var importErrorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportTextDialog = false },
            title = { Text("Paste Backup JSON", style = MaterialTheme.typography.titleLarge) },
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
                            .height(130.dp)
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
                        val parsed = BackupData.fromJsonString(importJsonText)
                        if (parsed == null) {
                            importErrorMessage = "This is not a valid Piso backup."
                            return@Button
                        }
                        viewModel.importBackup(importJsonText) { success ->
                            if (success) {
                                showImportTextDialog = false
                            } else {
                                importErrorMessage = "This is not a valid Piso backup."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_paste_import_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportTextDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val bgColor = if (isSelected) TealContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
