package com.example.ui.project

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectMember
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerInputField
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitePartnerDialog(
    currentMembers: Map<String, ProjectMember>,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "invite_partner_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invite Partner",
                    style = typography.screenTitle,
                    color = colors.text
                )
                Text(
                    text = "CANCEL",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .testTag("cancel_invite_dialog_button")
                )
            }

            Text(
                text = "Enter your partner's Google account email. Only authorized accounts will be able to access this project.",
                style = typography.body,
                color = colors.textMuted
            )

            PartnerInputField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    if (errorMessage != null) errorMessage = null
                },
                label = "PARTNER GOOGLE EMAIL",
                placeholder = "partner@gmail.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = errorMessage != null,
                errorMessage = errorMessage,
                testTag = "invite_email_input"
            )

            Spacer(modifier = Modifier.height(4.dp))

            PartnerPrimaryButton(
                text = "SEND INVITATION",
                onClick = {
                    val cleanEmail = emailInput.trim().lowercase()
                    if (cleanEmail.isBlank()) {
                        errorMessage = "Email cannot be empty"
                        return@PartnerPrimaryButton
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        errorMessage = "Please enter a valid email address"
                        return@PartnerPrimaryButton
                    }
                    if (currentMembers.values.any { it.email.lowercase().trim() == cleanEmail }) {
                        errorMessage = "This partner is already a member"
                        return@PartnerPrimaryButton
                    }
                    onInvite(cleanEmail)
                },
                testTag = "send_invite_button"
            )
        }
    }
}
