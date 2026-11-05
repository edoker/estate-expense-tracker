package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerInputField
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var projectName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "new_project_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Project",
                    style = typography.screenTitle,
                    color = colors.text
                )
                Text(
                    text = "CANCEL",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .testTag("cancel_create_project_button")
                )
            }

            Text(
                text = "Shared infrastructure or real-estate project name for you and your partners:",
                style = typography.body,
                color = colors.textMuted
            )

            PartnerInputField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    if (isError && it.isNotBlank()) isError = false
                },
                label = "PROJECT NAME",
                placeholder = "e.g. Ring Road Drainage",
                singleLine = true,
                isError = isError,
                errorMessage = if (isError) "Project name cannot be empty" else null,
                testTag = "project_name_input"
            )

            Spacer(modifier = Modifier.height(6.dp))

            PartnerPrimaryButton(
                text = "CREATE PROJECT",
                onClick = {
                    if (projectName.isBlank()) {
                        isError = true
                    } else {
                        onCreate(projectName.trim())
                    }
                },
                testTag = "confirm_create_project_button"
            )
        }
    }
}
