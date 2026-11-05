package com.example.ui.project

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invite
import com.example.data.model.ProjectMember
import com.example.data.util.formatDate
import com.example.ui.components.DangerousHoldButton
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.components.PartnerInputField
import com.example.ui.components.PartnerOutlinedButton
import com.example.ui.theme.PartnerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersTab(
    projectName: String,
    currentUserId: String,
    isAdmin: Boolean,
    members: Map<String, ProjectMember>,
    pendingInvites: List<Invite>,
    onOpenInviteDialog: () -> Unit,
    onCancelInvite: (String) -> Unit,
    onRemoveMember: (targetUid: String, memberName: String) -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var selectedMemberForManage by remember { mutableStateOf<Pair<String, ProjectMember>?>(null) }
    var memberToRemoveConfirmation by remember { mutableStateOf<Pair<String, ProjectMember>?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Partners Header: "Partners N", "AUTHORIZED GOOGLE ACCOUNTS", and "+ Invite" button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Partners ${members.size}",
                        style = typography.screenTitle,
                        color = colors.text
                    )
                    Text(
                        text = "AUTHORIZED GOOGLE ACCOUNTS",
                        style = typography.smallLabel,
                        color = colors.textMuted
                    )
                }

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.buttonRadius))
                            .border(dimens.standardBorder, colors.accent, RoundedCornerShape(dimens.buttonRadius))
                            .clickable(onClick = onOpenInviteDialog)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("invite_partner_button")
                    ) {
                        Text(
                            text = "+ INVITE",
                            style = typography.smallLabel,
                            color = colors.accent
                        )
                    }
                }
            }
        }

        // Active Members List: NO trash icon
        items(members.toList(), key = { it.first }) { (uid, member) ->
            val isCurrent = uid == currentUserId
            val isMemberAdmin = member.role == "admin"
            val memberDisplayName = member.name.ifBlank { member.email }
            val canManage = isAdmin && !isMemberAdmin && !isCurrent

            PartnerCard(
                onClick = if (canManage) {
                    { selectedMemberForManage = Pair(uid, member) }
                } else null,
                modifier = Modifier.testTag("member_item_$uid")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCurrent) "$memberDisplayName (You)" else memberDisplayName,
                            style = typography.cardName,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = member.email,
                            style = typography.notes,
                            color = colors.textMuted
                        )
                    }

                    // Role label on the right
                    val roleLabel = if (isMemberAdmin) "ADMIN" else if (canManage) "MEMBER ›" else "MEMBER"
                    Text(
                        text = roleLabel,
                        style = typography.smallLabel.copy(fontSize = 11.sp),
                        color = if (isMemberAdmin) colors.accent else colors.textMuted
                    )
                }
            }
        }

        item {
            Text(
                text = "DANGER ZONE · INSIDE MANAGE ONLY",
                style = typography.smallLabel.copy(fontSize = 10.5.sp),
                color = colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        }

        // Pending Invitations Section (if any)
        if (pendingInvites.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PENDING INVITATIONS (${pendingInvites.size})",
                    style = typography.smallLabel,
                    color = colors.textMuted
                )
            }

            items(pendingInvites, key = { it.id }) { invite ->
                PartnerCard(
                    modifier = Modifier.testTag("project_pending_invite_${invite.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = invite.invitedEmail,
                                style = typography.bodyBold,
                                color = colors.text
                            )
                            Text(
                                text = "Invited by ${invite.invitedByName} · ${invite.createdAt.formatDate()}",
                                style = typography.notes,
                                color = colors.textMuted
                            )
                        }

                        if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(dimens.standardBorder, colors.border, RoundedCornerShape(8.dp))
                                    .clickable { onCancelInvite(invite.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("cancel_invite_button_${invite.id}")
                            ) {
                                Text(
                                    text = "CANCEL",
                                    style = typography.smallLabel.copy(fontSize = 10.sp),
                                    color = colors.dangerRed
                                )
                            }
                        } else {
                            Text(
                                text = "PENDING",
                                style = typography.smallLabel,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Manage Member Sheet (Tapping a member row)
    if (selectedMemberForManage != null) {
        val (targetUid, targetMember) = selectedMemberForManage!!
        val targetName = targetMember.name.ifBlank { targetMember.email }

        PartnerBottomSheet(
            onDismissRequest = { selectedMemberForManage = null },
            testTag = "manage_member_sheet"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Manage Partner",
                    style = typography.screenTitle,
                    color = colors.text
                )

                PartnerCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("NAME", style = typography.smallLabel, color = colors.textMuted)
                            Text(targetName, style = typography.bodyBold, color = colors.text)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EMAIL", style = typography.smallLabel, color = colors.textMuted)
                            Text(targetMember.email, style = typography.bodyBold, color = colors.text)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ROLE", style = typography.smallLabel, color = colors.textMuted)
                            Text(targetMember.role.uppercase(), style = typography.bodyBold, color = colors.text)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Small red "Remove from project" link at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val toRemove = selectedMemberForManage
                            selectedMemberForManage = null
                            memberToRemoveConfirmation = toRemove
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Remove from project",
                        style = typography.bodyBold,
                        color = colors.dangerRed
                    )
                }

                PartnerOutlinedButton(
                    text = "Close",
                    onClick = { selectedMemberForManage = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dangerous Action Bottom Sheet: "Remove <name>?"
    // Consequence: "They lose access to <Project> immediately. Their past entries stay in the ledger."
    // Type name to confirm + Hold 3 sec to remove with progress fill!
    if (memberToRemoveConfirmation != null) {
        val (targetUid, targetMember) = memberToRemoveConfirmation!!
        val targetName = targetMember.name.ifBlank { targetMember.email }
        var typedConfirmation by remember { mutableStateOf("") }
        val isNameMatched = typedConfirmation.trim().equals(targetName.trim(), ignoreCase = false)

        PartnerBottomSheet(
            onDismissRequest = { memberToRemoveConfirmation = null },
            testTag = "remove_member_sheet"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Remove $targetName?",
                    style = typography.screenTitle,
                    color = colors.dangerRed
                )

                Text(
                    text = "They lose access to $projectName immediately. Their past entries stay in the ledger.",
                    style = typography.body,
                    color = colors.textMuted
                )

                PartnerInputField(
                    value = typedConfirmation,
                    onValueChange = { typedConfirmation = it },
                    label = "TYPE $targetName TO CONFIRM",
                    placeholder = "Name",
                    singleLine = true,
                    testTag = "type_member_name_input"
                )

                Spacer(modifier = Modifier.height(4.dp))

                DangerousHoldButton(
                    actionLabel = if (isNameMatched) "HOLD 3 SEC TO REMOVE" else "TYPE NAME TO UNLOCK",
                    onHoldComplete = {
                        memberToRemoveConfirmation = null
                        onRemoveMember(targetUid, targetName)
                    },
                    enabled = isNameMatched,
                    testTag = "confirm_remove_member_hold_button"
                )

                PartnerOutlinedButton(
                    text = "Cancel",
                    onClick = { memberToRemoveConfirmation = null },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "cancel_remove_member_button"
                )
            }
        }
    }
}
