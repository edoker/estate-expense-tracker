package com.example.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.Audit
import com.example.data.util.formatDate
import com.example.ui.components.PartnerCard
import com.example.ui.components.PartnerDateDivider
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.theme.PartnerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditTrailTab(
    auditTrail: List<Audit>
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    var selectedAuditForDetail by remember { mutableStateOf<Audit?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Group audit entries by date
    val groupedAudits = remember(auditTrail) {
        val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        auditTrail.groupBy { audit ->
            val date = audit.timestamp?.toDate() ?: Date()
            try {
                dateOnlyFormat.format(date)
            } catch (e: Exception) {
                audit.timestamp?.formatDate() ?: ""
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header: "Activity N" with "READ-ONLY" tag
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity ${auditTrail.size}",
                    style = typography.screenTitle,
                    color = colors.text
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(dimens.standardBorder, colors.border, RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "READ-ONLY",
                        style = typography.smallLabel.copy(fontSize = 11.sp),
                        color = colors.textMuted
                    )
                }
            }
        }

        if (auditTrail.isEmpty()) {
            item {
                PartnerCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No audit records yet",
                            style = typography.cardName,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Every expense created, modified, or deleted will appear here in chronological order.",
                            style = typography.body,
                            color = colors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedAudits.forEach { (dateStr, auditsOnDate) ->
                item(key = "audit_divider_$dateStr") {
                    PartnerDateDivider(dateText = dateStr)
                }

                items(auditsOnDate.size, key = { auditsOnDate[it].id }) { idx ->
                    val audit = auditsOnDate[idx]
                    val action = audit.action.lowercase()

                    val (iconType, actionTint, actionBg, actionLabel) = when (action) {
                        "created" -> Quadruple(PartnerIconType.AUDIT_CREATED, colors.accent, colors.accentSoft, "CREATED")
                        "deleted" -> Quadruple(PartnerIconType.AUDIT_DELETED, colors.dangerRed, colors.redSoft, "DELETED")
                        else -> Quadruple(PartnerIconType.AUDIT_EDITED, colors.accent, colors.accentSoft, "EDITED")
                    }

                    val date = audit.timestamp?.toDate() ?: Date()
                    val timeStr = try {
                        timeFormat.format(date).lowercase()
                    } catch (e: Exception) {
                        ""
                    }
                    val actorTime = "${audit.actorName.ifBlank { "Partner" }} · $timeStr"

                    PartnerCard(
                        onClick = { selectedAuditForDetail = audit },
                        modifier = Modifier.testTag("audit_item_${audit.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 44px rounded square icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(dimens.iconSquareRadius))
                                    .background(actionBg)
                                    .border(dimens.emphasizedBorder, actionTint, RoundedCornerShape(dimens.iconSquareRadius)),
                                contentAlignment = Alignment.Center
                            ) {
                                PartnerIcon(
                                    type = iconType,
                                    tint = actionTint,
                                    size = 20.dp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = actionLabel,
                                    style = typography.smallLabel.copy(fontSize = 11.sp),
                                    color = actionTint
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = audit.summary,
                                    style = typography.cardName.copy(fontSize = 15.sp),
                                    color = colors.text,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = actorTime,
                                    style = typography.secondary,
                                    color = colors.textMuted
                                )
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
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (selectedAuditForDetail != null) {
        AuditDetailBottomSheet(
            audit = selectedAuditForDetail!!,
            sheetState = sheetState,
            onDismiss = { selectedAuditForDetail = null }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
