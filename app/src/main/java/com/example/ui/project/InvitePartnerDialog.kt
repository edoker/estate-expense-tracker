package com.example.ui.project

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.ProjectMember

@Composable
fun InvitePartnerDialog(
    currentMembers: Map<String, ProjectMember>,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Invite Partner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your partner's Google account email. Only invited Google accounts will be able to access this project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Partner Google Email") },
                    placeholder = { Text("partner@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage!!) }
                    } else null,
                    modifier = Modifier.fillMaxWidth().testTag("invite_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = emailInput.trim().lowercase()
                    if (cleanEmail.isBlank()) {
                        errorMessage = "Email cannot be empty"
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }
                    if (currentMembers.values.any { it.email.lowercase().trim() == cleanEmail }) {
                        errorMessage = "This partner is already a member"
                        return@Button
                    }
                    onInvite(cleanEmail)
                },
                modifier = Modifier.testTag("send_invite_button")
            ) {
                Text("Send Invitation")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_invite_dialog_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("invite_partner_dialog")
    )
}
