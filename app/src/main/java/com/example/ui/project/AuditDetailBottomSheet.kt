package com.example.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Audit
import com.example.data.util.formatDate
import com.example.data.util.formatDateTime
import com.example.data.util.formatIndianCurrency
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditDetailBottomSheet(
    audit: Audit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    val action = audit.action.lowercase()
    val (actionTag, actionColor, actionBg) = when (action) {
        "created" -> Triple("+ CREATED", colors.accent, colors.accentSoft)
        "deleted" -> Triple("DELETED", colors.dangerRed, colors.redSoft)
        else -> Triple("EDITED", colors.accent, colors.accentSoft)
    }

    val snapshot = audit.snapshot

    // Extract ONLY the 10 requested fields from snapshot or fallback
    val dateValue = remember(snapshot, audit) {
        val rawDate = snapshot["date"]
        when (rawDate) {
            is Timestamp -> rawDate.formatDate()
            is com.google.firebase.Timestamp -> rawDate.formatDate()
            is Long -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(rawDate)
            is String -> rawDate
            else -> audit.timestamp?.formatDate() ?: "-"
        }
    }

    val supplierName = snapshot["supplierName"]?.toString() ?: "-"
    val note = snapshot["note"]?.toString() ?: "-"

    val rawAmount = snapshot["amount"]?.toString()?.toDoubleOrNull()
    val amountFormatted = if (rawAmount != null) formatIndianCurrency(rawAmount) else snapshot["amount"]?.toString() ?: "-"

    val rawType = snapshot["type"]?.toString()?.uppercase() ?: "GAVE"
    val isGave = rawType == "GAVE"
    val typeColor = if (isGave) colors.dangerRed else colors.accent

    val attachmentsValue = remember(snapshot) {
        val raw = snapshot["attachments"]
        if (raw is List<*>) {
            if (raw.isEmpty()) "None"
            else {
                val first = raw.firstOrNull()
                if (first is Map<*, *>) {
                    first["name"]?.toString() ?: first["fileName"]?.toString() ?: "${raw.size} attached"
                } else {
                    "${raw.size} file${if (raw.size > 1) "s" else ""}"
                }
            }
        } else {
            raw?.toString() ?: "None"
        }
    }

    val paymentMode = snapshot["paymentMode"]?.toString()?.uppercase() ?: "UPI"
    val paidByName = snapshot["paidByName"]?.toString()
        ?: snapshot["paidBy"]?.toString()
        ?: audit.actorName.ifBlank { "Partner" }
    val enteredByName = snapshot["enteredByName"]?.toString()
        ?: audit.actorName.ifBlank { "Partner" }
    val idValue = audit.entityId.ifBlank { audit.id }

    val formattedHeaderDate = remember(audit.timestamp) {
        val d = audit.timestamp?.toDate() ?: Date()
        try {
            SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault()).format(d).uppercase()
        } catch (e: Exception) {
            audit.timestamp?.formatDateTime()?.uppercase() ?: ""
        }
    }

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "audit_detail_bottom_sheet"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Tag at top (+ CREATED / EDITED / DELETED) and READ-ONLY RECORD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionBg)
                        .border(dimens.standardBorder, actionColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionTag,
                        style = typography.smallLabel.copy(fontSize = 11.sp),
                        color = actionColor
                    )
                }

                Text(
                    text = "READ-ONLY RECORD",
                    style = typography.smallLabel.copy(fontSize = 11.sp),
                    color = colors.textMuted
                )
            }

            // Summary Title
            Text(
                text = audit.summary,
                style = typography.screenTitle.copy(fontSize = 20.sp, lineHeight = 26.sp),
                color = colors.text,
                modifier = Modifier.testTag("audit_detail_summary")
            )

            // Subtitle: Time
            Text(
                text = formattedHeaderDate,
                style = typography.smallLabel.copy(fontSize = 11.5.sp),
                color = colors.textMuted
            )

            // Card with ONLY the requested clean label/value rows:
            // date, supplierName, note, amount, attachments, paymentMode, paidByName, type, enteredByName, id
            PartnerCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuditItemRow("DATE", dateValue)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("SUPPLIER", supplierName)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("NOTE", note)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("AMOUNT", amountFormatted, valueColor = typeColor)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("ATTACHMENTS", attachmentsValue, valueColor = colors.accent)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("PAYMENT MODE", paymentMode)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("PAID BY", paidByName)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("TYPE", rawType, valueColor = typeColor)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("ENTERED BY", enteredByName)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

                    AuditItemRow("ID", idValue)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Big CLOSE Button
            PartnerPrimaryButton(
                text = "CLOSE",
                onClick = onDismiss,
                testTag = "audit_close_button"
            )
        }
    }
}

@Composable
private fun AuditItemRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = PartnerTheme.colors.text
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
            color = valueColor,
            maxLines = 1
        )
    }
}
