package com.example.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.data.service.PartnerLedgerMessagingService
import com.example.data.util.formatDate
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.components.PartnerOutlinedButton
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme
import com.google.firebase.messaging.FirebaseMessaging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: String,
    displayName: String,
    email: String,
    projects: List<Project>,
    pendingInvites: List<Invite>,
    operationMessage: String?,
    onClearOperationMessage: () -> Unit,
    onProjectClick: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onAcceptInvite: (Invite) -> Unit,
    onDeclineInvite: (Invite) -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var menuExpanded by remember { mutableStateOf(false) }
    var showNewProjectSheet by remember { mutableStateOf(false) }
    var showEditNameSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationMessage) {
        if (!operationMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationMessage)
            onClearOperationMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PartnerLedger",
                        style = typography.screenTitle,
                        color = colors.text
                    )
                },
                actions = {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("top_bar_menu_button")
                    ) {
                        PartnerIcon(
                            type = PartnerIconType.MORE_VERT,
                            tint = colors.text,
                            size = 20.dp
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(colors.surface)
                            .testTag("home_dropdown_menu")
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy FCM Token", style = typography.body, color = colors.text) },
                            onClick = {
                                menuExpanded = false
                                val token = PartnerLedgerMessagingService.getCachedToken(context)
                                if (!token.isNullOrBlank()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("FCM Token", token))
                                    android.widget.Toast.makeText(context, "FCM Token copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    FirebaseMessaging.getInstance().token.addOnSuccessListener { fetchedToken ->
                                        PartnerLedgerMessagingService.saveCachedToken(context, fetchedToken)
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("FCM Token", fetchedToken))
                                        android.widget.Toast.makeText(context, "FCM Token copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                    }.addOnFailureListener {
                                        android.widget.Toast.makeText(context, "FCM token not available yet", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("copy_fcm_token_button")
                        )

                        DropdownMenuItem(
                            text = { Text("Sign out", style = typography.bodyBold, color = colors.dangerRed) },
                            onClick = {
                                menuExpanded = false
                                onSignOutClick()
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // User Greeting Card (Tap to edit name)
                PartnerCard(
                    onClick = { showEditNameSheet = true },
                    modifier = Modifier.testTag("greeting_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                .background(colors.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                            Text(
                                text = initial,
                                style = typography.cardName,
                                color = colors.accent
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hello, $displayName",
                                style = typography.cardName,
                                color = colors.text,
                                modifier = Modifier.testTag("greeting_text")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = email,
                                style = typography.secondary,
                                color = colors.textMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Edit affordance chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentSoft)
                                .border(dimens.standardBorder, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("edit_name_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PartnerIcon(
                                    type = PartnerIconType.AUDIT_EDITED,
                                    tint = colors.accent,
                                    size = 13.dp
                                )
                                Text(
                                    text = "EDIT",
                                    style = typography.smallLabel.copy(fontSize = 10.sp),
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }

            // Pending Invitations Section (if any)
            if (pendingInvites.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invitations_section"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PROJECT INVITATIONS (${pendingInvites.size})",
                            style = typography.smallLabel,
                            color = colors.textMuted
                        )

                        pendingInvites.forEach { invite ->
                            PartnerCard(
                                modifier = Modifier.testTag("invite_card_${invite.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = invite.projectName,
                                            style = typography.cardName,
                                            color = colors.text
                                        )
                                        Text(
                                            text = "Invited by ${invite.invitedByName} · ${invite.createdAt.formatDate()}",
                                            style = typography.notes,
                                            color = colors.textMuted
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PartnerOutlinedButton(
                                            text = "Decline",
                                            onClick = { onDeclineInvite(invite) },
                                            height = 42.dp,
                                            testTag = "decline_invite_button_${invite.id}"
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(dimens.buttonRadius))
                                                .background(colors.accentButtonFill)
                                                .clickable { onAcceptInvite(invite) }
                                                .padding(horizontal = 18.dp)
                                                .testTag("accept_invite_button_${invite.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "ACCEPT",
                                                style = typography.primaryButton.copy(fontSize = 13.sp),
                                                color = colors.accentButtonText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Projects Header with ONE "+ New Project" button only
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROJECTS",
                        style = typography.smallLabel,
                        color = colors.textMuted
                    )

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(dimens.buttonRadius))
                            .background(colors.accentButtonFill)
                            .clickable { showNewProjectSheet = true }
                            .padding(horizontal = 14.dp)
                            .testTag("new_project_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PartnerIcon(
                                type = PartnerIconType.PLUS,
                                tint = colors.accentButtonText,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEW PROJECT",
                                style = typography.primaryButton.copy(fontSize = 12.sp),
                                color = colors.accentButtonText
                            )
                        }
                    }
                }
            }

            // Projects List or Empty State
            if (projects.isEmpty()) {
                item {
                    PartnerCard(
                        modifier = Modifier.testTag("empty_projects_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                    .background(colors.accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No projects yet",
                                style = typography.cardName,
                                color = colors.text,
                                modifier = Modifier.testTag("no_projects_text")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Create a project to start logging supplier payments with partners, or wait for an invitation.",
                                style = typography.body,
                                color = colors.textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            PartnerPrimaryButton(
                                text = "+ CREATE FIRST PROJECT",
                                onClick = { showNewProjectSheet = true },
                                testTag = "empty_state_create_button"
                            )
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val isAdmin = project.isAdmin(currentUserId)
                    val memberCount = project.memberUids.size

                    PartnerCard(
                        onClick = { onProjectClick(project.id) },
                        modifier = Modifier.testTag("project_item_${project.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                    .background(colors.accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    style = typography.cardName,
                                    color = colors.text
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Admin/Partner role tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isAdmin) colors.accentSoft else Color.Transparent)
                                            .border(
                                                width = dimens.standardBorder,
                                                color = if (isAdmin) colors.accent else colors.border,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAdmin) "ADMIN" else "MEMBER",
                                            style = typography.smallLabel.copy(fontSize = 10.sp),
                                            color = if (isAdmin) colors.accent else colors.textMuted
                                        )
                                    }

                                    Text(
                                        text = "$memberCount partner${if (memberCount > 1) "s" else ""}",
                                        style = typography.secondary,
                                        color = colors.textMuted
                                    )

                                    Text(
                                        text = "· ${project.createdAt.formatDate()}",
                                        style = typography.secondary,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            PartnerIcon(
                                type = PartnerIconType.CHEVRON_RIGHT,
                                tint = colors.textMuted,
                                size = 20.dp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNewProjectSheet) {
        NewProjectBottomSheet(
            onDismiss = { showNewProjectSheet = false },
            onCreate = { projectName ->
                showNewProjectSheet = false
                onCreateProject(projectName)
            }
        )
    }

    if (showEditNameSheet) {
        EditNameBottomSheet(
            currentName = displayName,
            onDismiss = { showEditNameSheet = false },
            onSave = { newName ->
                showEditNameSheet = false
                onUpdateDisplayName(newName)
            }
        )
    }
}
