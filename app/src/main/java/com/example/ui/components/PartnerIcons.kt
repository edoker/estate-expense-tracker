package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PartnerTheme

enum class PartnerIconType {
    ARROW_OUT, // You gave (up-right)
    ARROW_IN,  // You got (down-left)
    TAB_EXPENSES, // receipt
    TAB_AUDIT, // history clock
    TAB_MEMBERS, // two people
    MODE_UPI, // QR code
    MODE_CASH, // banknote
    MODE_CHEQUE, // cheque slip
    MODE_NETBANKING, // bank building
    AUDIT_CREATED, // plus
    AUDIT_EDITED, // pencil
    AUDIT_DELETED, // bin
    EXPORT_TRAY, // download tray arrow
    BACK,
    MORE_VERT,
    CLOSE,
    CHEVRON_RIGHT,
    CHECK,
    PLUS
}

@Composable
fun PartnerIcon(
    type: PartnerIconType,
    modifier: Modifier = Modifier,
    tint: Color = PartnerTheme.colors.accent,
    size: Dp = 24.dp,
    strokeWidthPx: Float = when (type) {
        PartnerIconType.ARROW_OUT, PartnerIconType.ARROW_IN -> 3.3f
        PartnerIconType.BACK, PartnerIconType.MORE_VERT, PartnerIconType.CLOSE -> 3.0f
        PartnerIconType.CHEVRON_RIGHT, PartnerIconType.CHECK, PartnerIconType.AUDIT_CREATED,
        PartnerIconType.AUDIT_EDITED, PartnerIconType.AUDIT_DELETED -> 2.6f
        PartnerIconType.TAB_EXPENSES, PartnerIconType.TAB_AUDIT, PartnerIconType.TAB_MEMBERS,
        PartnerIconType.MODE_UPI, PartnerIconType.MODE_CASH, PartnerIconType.MODE_CHEQUE,
        PartnerIconType.MODE_NETBANKING, PartnerIconType.EXPORT_TRAY, PartnerIconType.PLUS -> 2.2f
    }
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scale = w / 24f
        val stroke = Stroke(
            width = strokeWidthPx * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (type) {
            PartnerIconType.ARROW_OUT -> drawArrowOut(tint, stroke, scale)
            PartnerIconType.ARROW_IN -> drawArrowIn(tint, stroke, scale)
            PartnerIconType.TAB_EXPENSES -> drawReceipt(tint, stroke, scale)
            PartnerIconType.TAB_AUDIT -> drawHistoryClock(tint, stroke, scale)
            PartnerIconType.TAB_MEMBERS -> drawTwoPeople(tint, stroke, scale)
            PartnerIconType.MODE_UPI -> drawQrCode(tint, stroke, scale)
            PartnerIconType.MODE_CASH -> drawBanknote(tint, stroke, scale)
            PartnerIconType.MODE_CHEQUE -> drawCheque(tint, stroke, scale)
            PartnerIconType.MODE_NETBANKING -> drawBankBuilding(tint, stroke, scale)
            PartnerIconType.AUDIT_CREATED -> drawPlus(tint, stroke, scale)
            PartnerIconType.AUDIT_EDITED -> drawPencil(tint, stroke, scale)
            PartnerIconType.AUDIT_DELETED -> drawBin(tint, stroke, scale)
            PartnerIconType.EXPORT_TRAY -> drawExportTray(tint, stroke, scale)
            PartnerIconType.BACK -> drawBackArrow(tint, stroke, scale)
            PartnerIconType.MORE_VERT -> drawMoreVert(tint, stroke, scale)
            PartnerIconType.CLOSE -> drawClose(tint, stroke, scale)
            PartnerIconType.CHEVRON_RIGHT -> drawChevronRight(tint, stroke, scale)
            PartnerIconType.CHECK -> drawCheck(tint, stroke, scale)
            PartnerIconType.PLUS -> drawPlus(tint, stroke, scale)
        }
    }
}

// Direction arrow OUT (You gave): path "M7 17L17 7M8.5 7H17v8.5" (up-right)
private fun DrawScope.drawArrowOut(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(7f * s, 17f * s)
        lineTo(17f * s, 7f * s)
        moveTo(8.5f * s, 7f * s)
        lineTo(17f * s, 7f * s)
        lineTo(17f * s, 15.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Direction arrow IN (You got): path "M17 7L7 17M15.5 17H7V8.5" (down-left)
private fun DrawScope.drawArrowIn(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(17f * s, 7f * s)
        lineTo(7f * s, 17f * s)
        moveTo(15.5f * s, 17f * s)
        lineTo(7f * s, 17f * s)
        lineTo(7f * s, 8.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Expenses tab icon (receipt outline)
private fun DrawScope.drawReceipt(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(5f * s, 3f * s)
        lineTo(19f * s, 3f * s)
        lineTo(19f * s, 21f * s)
        lineTo(16f * s, 19.5f * s)
        lineTo(13.5f * s, 21f * s)
        lineTo(10.5f * s, 19.5f * s)
        lineTo(8f * s, 21f * s)
        lineTo(5f * s, 19.5f * s)
        close()
        // Horizontal lines on receipt
        moveTo(8.5f * s, 8f * s)
        lineTo(15.5f * s, 8f * s)
        moveTo(8.5f * s, 12f * s)
        lineTo(15.5f * s, 12f * s)
        moveTo(8.5f * s, 16f * s)
        lineTo(12.5f * s, 16f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Audit Trail tab icon (history clock)
private fun DrawScope.drawHistoryClock(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Clock arc with counter-clockwise arrow
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(4f * s, 4f * s, 20f * s, 20f * s),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 270f,
            forceMoveTo = false
        )
        // Clock hands
        moveTo(12f * s, 8f * s)
        lineTo(12f * s, 12f * s)
        lineTo(15f * s, 14f * s)
        // Arrow head at top
        moveTo(2.5f * s, 8f * s)
        lineTo(4f * s, 4f * s)
        lineTo(8f * s, 5.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Members tab icon (two people outline)
private fun DrawScope.drawTwoPeople(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Front person head
        addOval(androidx.compose.ui.geometry.Rect(6f * s, 4f * s, 12f * s, 10f * s))
        // Front person shoulders
        moveTo(4f * s, 19f * s)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(4f * s, 11f * s, 14f * s, 21f * s),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        // Second person head behind
        moveTo(13.5f * s, 5.5f * s)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(12f * s, 4f * s, 18f * s, 10f * s),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 160f,
            forceMoveTo = false
        )
        // Second person shoulder behind
        moveTo(14f * s, 13f * s)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(11f * s, 12f * s, 20f * s, 20f * s),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
    }
    drawPath(path, tint, style = stroke)
}

// Payment Mode UPI (QR code)
private fun DrawScope.drawQrCode(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Top-left finder
        moveTo(3.5f * s, 3.5f * s)
        lineTo(9.5f * s, 3.5f * s)
        lineTo(9.5f * s, 9.5f * s)
        lineTo(3.5f * s, 9.5f * s)
        close()
        moveTo(6.5f * s, 6.5f * s)
        lineTo(6.5f * s, 6.51f * s)

        // Top-right finder
        moveTo(14.5f * s, 3.5f * s)
        lineTo(20.5f * s, 3.5f * s)
        lineTo(20.5f * s, 9.5f * s)
        lineTo(14.5f * s, 9.5f * s)
        close()
        moveTo(17.5f * s, 6.5f * s)
        lineTo(17.5f * s, 6.51f * s)

        // Bottom-left finder
        moveTo(3.5f * s, 14.5f * s)
        lineTo(9.5f * s, 14.5f * s)
        lineTo(9.5f * s, 20.5f * s)
        lineTo(3.5f * s, 20.5f * s)
        close()
        moveTo(6.5f * s, 17.5f * s)
        lineTo(6.5f * s, 17.51f * s)

        // Bottom-right dots/patterns
        moveTo(14.5f * s, 14.5f * s)
        lineTo(17.5f * s, 14.5f * s)
        lineTo(17.5f * s, 17.5f * s)
        moveTo(20.5f * s, 14.5f * s)
        lineTo(20.5f * s, 20.5f * s)
        moveTo(14.5f * s, 20.5f * s)
        lineTo(17.5f * s, 20.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Payment Mode Cash (banknote)
private fun DrawScope.drawBanknote(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Note outline
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(3f * s, 6f * s, 21f * s, 18f * s),
                cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
            )
        )
        // Center circle
        addOval(androidx.compose.ui.geometry.Rect(9.5f * s, 9.5f * s, 14.5f * s, 14.5f * s))
        // Side vertical lines
        moveTo(6f * s, 10f * s)
        lineTo(6f * s, 14f * s)
        moveTo(18f * s, 10f * s)
        lineTo(18f * s, 14f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Payment Mode Cheque (cheque slip)
private fun DrawScope.drawCheque(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(3f * s, 5.5f * s, 21f * s, 18.5f * s),
                cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
            )
        )
        // Payee line
        moveTo(6f * s, 9.5f * s)
        lineTo(18f * s, 9.5f * s)
        // Amount line
        moveTo(6f * s, 13f * s)
        lineTo(13f * s, 13f * s)
        // Signature squiggly line
        moveTo(14.5f * s, 16f * s)
        lineTo(16f * s, 15f * s)
        lineTo(17.5f * s, 16.5f * s)
        lineTo(19f * s, 15.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Payment Mode Netbanking (bank building)
private fun DrawScope.drawBankBuilding(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Pediment / roof triangle
        moveTo(3.5f * s, 8.5f * s)
        lineTo(12f * s, 4f * s)
        lineTo(20.5f * s, 8.5f * s)
        close()
        // Architrave line
        moveTo(3.5f * s, 9.5f * s)
        lineTo(20.5f * s, 9.5f * s)
        // 3 Pillars
        moveTo(6f * s, 10.5f * s)
        lineTo(6f * s, 18.5f * s)
        moveTo(12f * s, 10.5f * s)
        lineTo(12f * s, 18.5f * s)
        moveTo(18f * s, 10.5f * s)
        lineTo(18f * s, 18.5f * s)
        // Base
        moveTo(3f * s, 19.5f * s)
        lineTo(21f * s, 19.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Plus icon
private fun DrawScope.drawPlus(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(12f * s, 6f * s)
        lineTo(12f * s, 18f * s)
        moveTo(6f * s, 12f * s)
        lineTo(18f * s, 12f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Pencil icon
private fun DrawScope.drawPencil(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(16.5f * s, 4.5f * s)
        lineTo(19.5f * s, 7.5f * s)
        lineTo(8f * s, 19f * s)
        lineTo(4.5f * s, 19.5f * s)
        lineTo(5f * s, 16f * s)
        close()
        moveTo(14.5f * s, 6.5f * s)
        lineTo(17.5f * s, 9.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Bin / trash icon
private fun DrawScope.drawBin(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Lid
        moveTo(4f * s, 7f * s)
        lineTo(20f * s, 7f * s)
        moveTo(9f * s, 7f * s)
        lineTo(9f * s, 4.5f * s)
        lineTo(15f * s, 4.5f * s)
        lineTo(15f * s, 7f * s)
        // Can body
        moveTo(6f * s, 7f * s)
        lineTo(7f * s, 20f * s)
        lineTo(17f * s, 20f * s)
        lineTo(18f * s, 7f * s)
        // Slits
        moveTo(10f * s, 11f * s)
        lineTo(10f * s, 16f * s)
        moveTo(14f * s, 11f * s)
        lineTo(14f * s, 16f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Export tray icon
private fun DrawScope.drawExportTray(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        // Tray
        moveTo(4f * s, 15f * s)
        lineTo(4f * s, 19.5f * s)
        lineTo(20f * s, 19.5f * s)
        lineTo(20f * s, 15f * s)
        // Down arrow
        moveTo(12f * s, 4.5f * s)
        lineTo(12f * s, 15f * s)
        moveTo(8f * s, 11f * s)
        lineTo(12f * s, 15f * s)
        lineTo(16f * s, 11f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Back arrow
private fun DrawScope.drawBackArrow(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(14.5f * s, 6f * s)
        lineTo(8.5f * s, 12f * s)
        lineTo(14.5f * s, 18f * s)
    }
    drawPath(path, tint, style = stroke)
}

// More vert
private fun DrawScope.drawMoreVert(tint: Color, stroke: Stroke, s: Float) {
    drawCircle(tint, radius = 1.8f * s, center = Offset(12f * s, 6f * s))
    drawCircle(tint, radius = 1.8f * s, center = Offset(12f * s, 12f * s))
    drawCircle(tint, radius = 1.8f * s, center = Offset(12f * s, 18f * s))
}

// Close
private fun DrawScope.drawClose(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(6f * s, 6f * s)
        lineTo(18f * s, 18f * s)
        moveTo(18f * s, 6f * s)
        lineTo(6f * s, 18f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Chevron Right
private fun DrawScope.drawChevronRight(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(9.5f * s, 6.5f * s)
        lineTo(15.5f * s, 12f * s)
        lineTo(9.5f * s, 17.5f * s)
    }
    drawPath(path, tint, style = stroke)
}

// Check
private fun DrawScope.drawCheck(tint: Color, stroke: Stroke, s: Float) {
    val path = Path().apply {
        moveTo(5f * s, 12f * s)
        lineTo(10f * s, 17f * s)
        lineTo(19f * s, 7f * s)
    }
    drawPath(path, tint, style = stroke)
}

/**
 * Official Google 4-Color "G" Icon
 */
@Composable
fun GoogleOfficialLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f

        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        // Draw the 4 colored arcs of the Google 'G'
        // Blue horizontal bar and right arc
        val bluePath = Path().apply {
            moveTo(23.5f * s, 12.2f * s)
            lineTo(23.5f * s, 12f * s)
            cubicTo(23.5f * s, 11.2f * s, 23.4f * s, 10.4f * s, 23.3f * s, 9.7f * s)
            lineTo(12f * s, 9.7f * s)
            lineTo(12f * s, 14.3f * s)
            lineTo(18.5f * s, 14.3f * s)
            cubicTo(18.2f * s, 15.9f * s, 17.3f * s, 17.2f * s, 15.9f * s, 18.2f * s)
            lineTo(19.8f * s, 21.2f * s)
            cubicTo(22.1f * s, 19.1f * s, 23.5f * s, 15.9f * s, 23.5f * s, 12.2f * s)
            close()
        }
        drawPath(bluePath, blue)

        // Green bottom arc
        val greenPath = Path().apply {
            moveTo(12f * s, 24f * s)
            cubicTo(15.2f * s, 24f * s, 18f * s, 22.9f * s, 19.8f * s, 21.2f * s)
            lineTo(15.9f * s, 18.2f * s)
            cubicTo(14.8f * s, 18.9f * s, 13.5f * s, 19.3f * s, 12f * s, 19.3f * s)
            cubicTo(8.9f * s, 19.3f * s, 6.3f * s, 17.2f * s, 5.3f * s, 14.4f * s)
            lineTo(1.3f * s, 17.5f * s)
            cubicTo(3.3f * s, 21.4f * s, 7.3f * s, 24f * s, 12f * s, 24f * s)
            close()
        }
        drawPath(greenPath, green)

        // Yellow left arc
        val yellowPath = Path().apply {
            moveTo(5.3f * s, 14.4f * s)
            cubicTo(5f * s, 13.6f * s, 4.9f * s, 12.8f * s, 4.9f * s, 12f * s)
            cubicTo(4.9f * s, 11.2f * s, 5f * s, 10.4f * s, 5.3f * s, 9.6f * s)
            lineTo(1.3f * s, 6.5f * s)
            cubicTo(0.5f * s, 8.2f * s, 0f * s, 10f * s, 0f * s, 12f * s)
            cubicTo(0f * s, 14f * s, 0.5f * s, 15.8f * s, 1.3f * s, 17.5f * s)
            lineTo(5.3f * s, 14.4f * s)
            close()
        }
        drawPath(yellowPath, yellow)

        // Red top arc
        val redPath = Path().apply {
            moveTo(12f * s, 4.7f * s)
            cubicTo(13.8f * s, 4.7f * s, 15.3f * s, 5.3f * s, 16.6f * s, 6.5f * s)
            lineTo(20f * s, 3.1f * s)
            cubicTo(17.9f * s, 1.2f * s, 15.2f * s, 0f * s, 12f * s, 0f * s)
            cubicTo(7.3f * s, 0f * s, 3.3f * s, 2.6f * s, 1.3f * s, 6.5f * s)
            lineTo(5.3f * s, 9.6f * s)
            cubicTo(6.3f * s, 6.8f * s, 8.9f * s, 4.7f * s, 12f * s, 4.7f * s)
            close()
        }
        drawPath(redPath, red)
    }
}
