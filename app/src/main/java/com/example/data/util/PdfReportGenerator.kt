package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.ProjectMember
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 36f

    fun generateReport(
        context: Context,
        projectName: String,
        dateRangeLabel: String,
        transactions: List<Expense>,
        members: Map<String, ProjectMember>
    ): File {
        val pdfDocument = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val totalGave = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val totalGot = transactions.filter { it.isIncome }.sumOf { it.amount }
        val netBalance = totalGot - totalGave

        val rowsPerPage = 22
        val totalTransactions = transactions.size
        val totalPages = if (totalTransactions == 0) 1 else ((totalTransactions + rowsPerPage - 1) / rowsPerPage)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedTimestamp = dateFormat.format(Date())

        var txnIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN

            // Draw Header on First Page
            if (pageNumber == 1) {
                // Header Banner
                paint.color = Color.rgb(24, 43, 73) // Deep Navy
                paint.style = Paint.Style.FILL
                val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f)
                canvas.drawRoundRect(headerRect, 8f, 8f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 18f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("PARTNER LEDGER REPORT", MARGIN + 16f, y + 28f, textPaint)

                textPaint.textSize = 11f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.rgb(210, 225, 245)
                canvas.drawText("Project: $projectName   |   Period: $dateRangeLabel", MARGIN + 16f, y + 48f, textPaint)

                y += 76f

                // Summary Cards
                val cardWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 3f
                val cardHeight = 56f

                // Card 1: Total Received (You Got)
                drawSummaryCard(
                    canvas, MARGIN, y, cardWidth, cardHeight,
                    title = "TOTAL RECEIVED (YOU GOT)",
                    amountStr = formatIndianCurrency(totalGot),
                    bgColor = Color.rgb(235, 248, 238),
                    accentColor = Color.rgb(46, 125, 50)
                )

                // Card 2: Total Paid (You Gave)
                drawSummaryCard(
                    canvas, MARGIN + cardWidth + 8f, y, cardWidth, cardHeight,
                    title = "TOTAL PAID (YOU GAVE)",
                    amountStr = formatIndianCurrency(totalGave),
                    bgColor = Color.rgb(254, 242, 242),
                    accentColor = Color.rgb(211, 47, 47)
                )

                // Card 3: Net Balance
                val netColor = if (netBalance >= 0) Color.rgb(46, 125, 50) else Color.rgb(211, 47, 47)
                val netPrefix = if (netBalance > 0) "+" else ""
                drawSummaryCard(
                    canvas, MARGIN + (cardWidth * 2) + 16f, y, cardWidth, cardHeight,
                    title = "NET BALANCE",
                    amountStr = "$netPrefix${formatIndianCurrency(netBalance)}",
                    bgColor = Color.rgb(243, 244, 246),
                    accentColor = netColor
                )

                y += cardHeight + 18f
            } else {
                // Secondary Page Header
                paint.color = Color.rgb(245, 247, 250)
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, paint)

                textPaint.color = Color.rgb(55, 65, 81)
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Project: $projectName (Continued)  |  $dateRangeLabel", MARGIN + 8f, y + 18f, textPaint)

                y += 36f
            }

            // Table Header
            val tableTop = y
            val tableWidth = PAGE_WIDTH - (MARGIN * 2)
            paint.color = Color.rgb(229, 231, 235)
            canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 24f, paint)

            textPaint.color = Color.rgb(17, 24, 39)
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val colDate = MARGIN + 8f
            val colType = MARGIN + 75f
            val colParty = MARGIN + 130f
            val colMode = MARGIN + 295f
            val colHandled = MARGIN + 355f
            val colAmount = PAGE_WIDTH - MARGIN - 8f

            canvas.drawText("Date", colDate, tableTop + 16f, textPaint)
            canvas.drawText("Type", colType, tableTop + 16f, textPaint)
            canvas.drawText("Party / Description", colParty, tableTop + 16f, textPaint)
            canvas.drawText("Mode", colMode, tableTop + 16f, textPaint)
            canvas.drawText("Handled By", colHandled, tableTop + 16f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", colAmount, tableTop + 16f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            y += 24f

            // Table Rows
            val endIdx = minOf(txnIndex + rowsPerPage, totalTransactions)
            if (totalTransactions == 0) {
                textPaint.color = Color.GRAY
                textPaint.textSize = 11f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("No transactions found for the selected period.", MARGIN + 12f, y + 24f, textPaint)
                y += 40f
            } else {
                for (i in txnIndex until endIdx) {
                    val txn = transactions[i]
                    val isEven = (i - txnIndex) % 2 == 0
                    val rowHeight = 22f

                    if (isEven) {
                        paint.color = Color.rgb(249, 250, 251)
                        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, paint)
                    }

                    textPaint.textSize = 9f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    // Date
                    textPaint.color = Color.rgb(55, 65, 81)
                    val dateStr = txn.date.formatDate()
                    canvas.drawText(dateStr, colDate, y + 14f, textPaint)

                    // Type
                    val isIncome = txn.isIncome
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (isIncome) {
                        textPaint.color = Color.rgb(46, 125, 50)
                        canvas.drawText("YOU GOT", colType, y + 14f, textPaint)
                    } else {
                        textPaint.color = Color.rgb(211, 47, 47)
                        canvas.drawText("YOU GAVE", colType, y + 14f, textPaint)
                    }

                    // Party Name (truncated if too long)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.color = Color.rgb(17, 24, 39)
                    var partyText = txn.supplierName
                    if (partyText.length > 28) {
                        partyText = partyText.substring(0, 26) + "..."
                    }
                    canvas.drawText(partyText, colParty, y + 14f, textPaint)

                    // Mode
                    textPaint.color = Color.rgb(75, 85, 99)
                    canvas.drawText(txn.paymentMode.ifBlank { "UPI" }, colMode, y + 14f, textPaint)

                    // Handled By
                    val handler = members[txn.paidByUid]?.name?.ifBlank { members[txn.paidByUid]?.email } ?: "Partner"
                    var handlerText = handler
                    if (handlerText.length > 16) {
                        handlerText = handlerText.substring(0, 14) + ".."
                    }
                    canvas.drawText(handlerText, colHandled, y + 14f, textPaint)

                    // Amount
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (isIncome) {
                        textPaint.color = Color.rgb(46, 125, 50)
                        canvas.drawText("+${formatIndianCurrency(txn.amount)}", colAmount, y + 14f, textPaint)
                    } else {
                        textPaint.color = Color.rgb(211, 47, 47)
                        canvas.drawText("-${formatIndianCurrency(txn.amount)}", colAmount, y + 14f, textPaint)
                    }
                    textPaint.textAlign = Paint.Align.LEFT

                    y += rowHeight
                }
                txnIndex = endIdx
            }

            // Footer
            val footerY = PAGE_HEIGHT - MARGIN + 12f
            paint.color = Color.rgb(209, 213, 219)
            paint.strokeWidth = 0.8f
            canvas.drawLine(MARGIN, footerY - 14f, PAGE_WIDTH - MARGIN, footerY - 14f, paint)

            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(107, 114, 128)
            canvas.drawText("Generated on $generatedTimestamp  •  PartnerLedger", MARGIN, footerY, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN, footerY, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            pdfDocument.finishPage(page)
        }

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val cleanProject = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(reportsDir, "PartnerLedger_${cleanProject}_$timestamp.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        amountStr: String,
        bgColor: Int,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        val cardRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)

        paint.color = accentColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.rgb(75, 85, 99)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 10f, y + 18f, textPaint)

        textPaint.color = accentColor
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amountStr, x + 10f, y + 42f, textPaint)
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Open Report"))
        } catch (e: Exception) {
            // Fallback to share
            sharePdf(context, file)
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }
}
