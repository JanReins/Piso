package com.janreins.piso.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.piso.ui.components.PinDots
import com.janreins.piso.ui.components.PinKeypad
import com.janreins.piso.ui.components.PisoCard
import com.janreins.piso.ui.components.PisoPrimaryButton
import com.janreins.piso.ui.theme.IncomeGreen
import com.janreins.piso.ui.theme.TealContainer
import com.janreins.piso.ui.theme.TealPrimary

@Composable
fun WelcomeScreen(
    onStartUsingPiso: (name: String, pin: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var enablePinLock by remember { mutableStateOf(false) }

    // PIN Setup states
    var pinStep by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN, 3: Matched
    var initialPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("welcome_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Logo Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(TealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Name & Tagline
            Text(
                text = "Piso",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = TealPrimary
            )

            Text(
                text = "Your private money book",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Local Profile Card
            PisoCard(
                contentPadding = 18.dp,
                cornerRadius = 24.dp
            ) {
                Text(
                    text = "Create Local Profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This profile is kept 100% on your device. No cloud, no emails, no sign-ups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input Field (Required)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Your Name (required)") },
                    placeholder = { Text("e.g. Maria") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TealPrimary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Set a 4-digit PIN (optional) Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            enablePinLock = !enablePinLock
                            if (!enablePinLock) {
                                initialPin = ""
                                confirmPin = ""
                                pinStep = 1
                                errorMessage = null
                                isPinError = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Set a 4-digit PIN (optional)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (enablePinLock) "PIN lock active on app launch" else "App opens directly without lock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = enablePinLock,
                        onCheckedChange = { checked ->
                            enablePinLock = checked
                            if (!checked) {
                                initialPin = ""
                                confirmPin = ""
                                pinStep = 1
                                errorMessage = null
                                isPinError = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TealPrimary
                        ),
                        modifier = Modifier.testTag("welcome_pin_switch")
                    )
                }

                // PIN Keypad & Confirmation Steps
                AnimatedVisibility(
                    visible = enablePinLock,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (pinStep) {
                            1 -> {
                                Text(
                                    text = "Enter a 4-digit PIN",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Step 1 of 2",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                PinDots(
                                    pinLength = initialPin.length,
                                    isError = isPinError,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                PinKeypad(
                                    onDigitClick = { digit ->
                                        if (initialPin.length < 4) {
                                            isPinError = false
                                            errorMessage = null
                                            initialPin += digit
                                            if (initialPin.length == 4) {
                                                // Advance to Step 2: Confirm PIN
                                                pinStep = 2
                                                confirmPin = ""
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        if (initialPin.isNotEmpty()) {
                                            initialPin = initialPin.dropLast(1)
                                            isPinError = false
                                            errorMessage = null
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            2 -> {
                                Text(
                                    text = "Confirm 4-digit PIN",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type it again to verify",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                PinDots(
                                    pinLength = confirmPin.length,
                                    isError = isPinError,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                PinKeypad(
                                    onDigitClick = { digit ->
                                        if (confirmPin.length < 4) {
                                            isPinError = false
                                            errorMessage = null
                                            confirmPin += digit
                                            if (confirmPin.length == 4) {
                                                if (confirmPin == initialPin) {
                                                    // Matched!
                                                    pinStep = 3
                                                    errorMessage = null
                                                } else {
                                                    // Did not match
                                                    isPinError = true
                                                    errorMessage = "PINs do not match."
                                                    confirmPin = ""
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        if (confirmPin.isNotEmpty()) {
                                            confirmPin = confirmPin.dropLast(1)
                                            isPinError = false
                                            errorMessage = null
                                        }
                                    },
                                    onClearClick = {
                                        // Reset to step 1
                                        initialPin = ""
                                        confirmPin = ""
                                        pinStep = 1
                                        errorMessage = null
                                        isPinError = false
                                    },
                                    clearButtonText = "Restart",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            3 -> {
                                // PIN Successfully set
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TealContainer)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "4-digit PIN is set!",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = {
                                            initialPin = ""
                                            confirmPin = ""
                                            pinStep = 1
                                        }
                                    ) {
                                        Text("Change PIN", fontSize = 13.sp, color = TealPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PisoPrimaryButton(
                    text = "Start using Piso",
                    onClick = {
                        val cleanName = name.trim()
                        if (cleanName.isBlank()) {
                            errorMessage = "Please enter your name."
                            return@PisoPrimaryButton
                        }
                        if (enablePinLock) {
                            if (pinStep != 3 || initialPin.length != 4 || initialPin != confirmPin) {
                                errorMessage = "Please finish setting your 4-digit PIN or turn off PIN lock."
                                return@PisoPrimaryButton
                            }
                            onStartUsingPiso(cleanName, initialPin)
                        } else {
                            onStartUsingPiso(cleanName, null)
                        }
                    },
                    testTag = "welcome_start_button"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Offline Assurance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "All accounts and records stay strictly offline on this phone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
