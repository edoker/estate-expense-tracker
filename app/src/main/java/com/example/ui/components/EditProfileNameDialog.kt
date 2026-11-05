package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog allowing the user to confirm or edit their display name.
 * Used on first sign-in (pre-populated with Google Auth name) and anytime
 * from the profile or options menu.
 */
@Composable
fun EditProfileNameDialog(
    initialName: String,
    email: String,
    isInitialSetup: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    val isValid = nameInput.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = {
            if (!isInitialSetup) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = if (isInitialSetup) Icons.Default.Badge else Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (isInitialSetup) "Confirm Your Name" else "Edit Display Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isInitialSetup) {
                        "We fetched this name from your Google account. You can keep it or change it now — this is the name your partners will see on all projects, transactions, and reports."
                    } else {
                        "Update the name that partners see on your shared projects, transactions, and reports."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 50) nameInput = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Alex Sharma") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (email.isNotBlank()) "Account: $email" else "",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${nameInput.length}/50",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(nameInput.trim())
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_display_name_button")
            ) {
                Text(if (isInitialSetup) "Confirm & Continue" else "Save Name")
            }
        },
        dismissButton = {
            if (!isInitialSetup) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_edit_name_button")
                ) {
                    Text("Cancel")
                }
            } else {
                // For initial setup, allow user to keep the Google name immediately if they don't want to type
                TextButton(
                    onClick = {
                        val fallback = initialName.ifBlank { email.substringBefore("@").ifBlank { "Partner" } }
                        onConfirm(fallback.trim())
                    },
                    modifier = Modifier.testTag("keep_google_name_button")
                ) {
                    Text("Keep Google Name")
                }
            }
        }
    )
}
