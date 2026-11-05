package com.example.ui.project

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.AttachmentManager
import com.example.data.util.formatDate
import com.example.data.util.formatIndianCurrency
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerDangerButton
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.components.PartnerOutlinedButton
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.components.PartnerSquareIconButton
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    members: Map<String, ProjectMember>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoveAttachment: ((ExpenseAttachment) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedImageForViewer by remember { mutableStateOf<ExpenseAttachment?>(null) }
    var isOpeningPdf by remember { mutableStateOf(false) }

    val isIncome = expense.isIncome
    val payerName = members[expense.paidByUid]?.name?.ifBlank { members[expense.paidByUid]?.email }
        ?: "Partner"
    val entererName = members[expense.enteredByUid]?.name?.ifBlank { members[expense.enteredByUid]?.email }
        ?: "Partner"

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("expense_detail_dialog"),
            topBar = {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = "Entry details",
                                style = typography.screenTitle,
                                color = colors.text
                            )
                            Text(
                                text = "PARTNER ACCESS",
                                style = typography.smallLabel,
                                color = colors.textMuted
                            )
                        }
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            PartnerSquareIconButton(
                                iconType = PartnerIconType.BACK,
                                onClick = onDismiss,
                                modifier = Modifier.testTag("detail_close_button")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            },
            bottomBar = {
                // Bottom Actions: Red-outline Delete button + Large EDIT ENTRY primary button
                Surface(
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.screenPadding)
                            .padding(top = 10.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PartnerDangerButton(
                            text = "Delete",
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.testTag("delete_expense_button")
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            PartnerPrimaryButton(
                                text = "EDIT ENTRY",
                                onClick = onEdit,
                                testTag = "edit_expense_button"
                            )
                        }
                    }
                }
            },
            containerColor = colors.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = dimens.screenPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // -------------------------------------------------------------
                // Hero Card: red or teal tinted, 2.5px border, bold arrow icon,
                // label "AMOUNT PAID · YOU GAVE" or "AMOUNT RECEIVED · YOU GOT",
                // 36px signed amount, supplier name, phone
                // -------------------------------------------------------------
                val heroBg = if (isIncome) colors.accentSoft else colors.redSoft
                val heroBorder = if (isIncome) colors.accent else colors.dangerRed
                val heroAmountColor = if (isIncome) colors.accent else colors.dangerRed

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cardRadius))
                        .background(heroBg)
                        .border(dimens.thickBorder, heroBorder, RoundedCornerShape(dimens.cardRadius))
                        .padding(20.dp)
                        .testTag("hero_amount_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PartnerIcon(
                                type = if (isIncome) PartnerIconType.ARROW_IN else PartnerIconType.ARROW_OUT,
                                tint = heroBorder,
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isIncome) "AMOUNT RECEIVED · YOU GOT" else "AMOUNT PAID · YOU GAVE",
                                style = typography.smallLabel,
                                color = heroBorder
                            )
                        }

                        // 36px signed amount
                        val sign = if (isIncome) "+ " else "− "
                        Text(
                            text = "$sign${formatIndianCurrency(expense.amount)}",
                            style = typography.heroAmount,
                            color = heroAmountColor,
                            modifier = Modifier.testTag("detail_hero_amount")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Supplier name
                        Text(
                            text = expense.supplierName,
                            style = typography.cardName,
                            color = colors.text
                        )

                        // Supplier phone if available
                        if (!expense.supplierPhone.isNullOrBlank()) {
                            Text(
                                text = expense.supplierPhone,
                                style = typography.body,
                                color = colors.textMuted,
                                modifier = Modifier.clickable {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${expense.supplierPhone}"))
                                    context.startActivity(dialIntent)
                                }
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // Details Card: Date, Mode, Paid/Received By, Entered By, Note
                // -------------------------------------------------------------
                PartnerCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DetailItemRow(
                            label = "DATE",
                            value = (expense.date ?: expense.createdAt ?: Timestamp.now()).formatDate()
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                        DetailItemRow(
                            label = "MODE",
                            value = expense.paymentMode.ifBlank { "UPI" }.uppercase()
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                        DetailItemRow(
                            label = if (isIncome) "RECEIVED BY" else "PAID BY",
                            value = payerName
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                        DetailItemRow(
                            label = "ENTERED BY",
                            value = entererName
                        )

                        if (!expense.note.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "NOTE",
                                    style = typography.smallLabel,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = expense.note,
                                    style = typography.bodyBold,
                                    color = colors.text
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // Attachments Card (thumbnails, file name, size, tap to view)
                // -------------------------------------------------------------
                if (expense.attachments.isNotEmpty()) {
                    PartnerCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ATTACHMENTS ${expense.attachments.size}",
                                style = typography.smallLabel,
                                color = colors.textMuted
                            )

                            expense.attachments.forEach { att ->
                                val isPdf = att.isPdf

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                        .clickable {
                                            if (isPdf) {
                                                coroutineScope.launch {
                                                    isOpeningPdf = true
                                                    val result = AttachmentManager.openPdf(context, att)
                                                    isOpeningPdf = false
                                                    result.onFailure {
                                                        Toast.makeText(context, "Could not open PDF file", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                selectedImageForViewer = att
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 44px rounded square with thumbnail/icon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                            .background(colors.surface)
                                            .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.iconSquareRadius)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = if (isPdf) colors.dangerRed else colors.accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.name,
                                            style = typography.bodyBold,
                                            color = colors.text,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${att.readableSize} · TAP TO VIEW",
                                            style = typography.secondary,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog for Delete
    if (showDeleteConfirmation) {
        PartnerBottomSheet(
            onDismissRequest = { showDeleteConfirmation = false },
            testTag = "delete_confirmation_sheet"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Delete this entry?",
                    style = typography.screenTitle,
                    color = colors.dangerRed
                )

                Text(
                    text = "Are you sure you want to delete this payment of ${formatIndianCurrency(expense.amount)} for \"${expense.supplierName}\"? This action will be recorded in the audit trail.",
                    style = typography.body,
                    color = colors.textMuted
                )

                PartnerDangerButton(
                    text = "CONFIRM DELETE",
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_delete_button"
                )

                PartnerOutlinedButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (selectedImageForViewer != null) {
        FullScreenImageViewerDialog(
            attachment = selectedImageForViewer!!,
            onDismiss = { selectedImageForViewer = null }
        )
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.smallLabel,
            color = colors.textMuted
        )
        Text(
            text = value,
            style = typography.bodyBold,
            color = colors.text
        )
    }
}
