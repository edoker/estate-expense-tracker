package com.example.data.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.ProjectMember
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin generator for Microsoft Excel (.xlsx) OpenXML spreadsheets.
 * Generates formatted workbooks containing ledger summary, member spending KPI,
 * and individual transaction records with native numbers and currency values.
 */
object ExcelReportGenerator {

    private const val MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun generateReport(
        context: Context,
        projectName: String,
        dateRangeLabel: String,
        transactions: List<Expense>,
        members: Map<String, ProjectMember>,
        memberFilterLabel: String? = null
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val cleanProject = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(reportsDir, "PartnerLedger_${cleanProject}_$timestamp.xlsx")

        val totalGave = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val totalGot = transactions.filter { it.isIncome }.sumOf { it.amount }
        val netBalance = totalGot - totalGave

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedTimestamp = dateFormat.format(Date())

        val sheetXml = buildSheetXml(
            projectName = projectName,
            dateRangeLabel = dateRangeLabel,
            memberFilterLabel = memberFilterLabel,
            generatedTimestamp = generatedTimestamp,
            totalGot = totalGot,
            totalGave = totalGave,
            netBalance = netBalance,
            transactions = transactions,
            members = members
        )

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())

                // 3. xl/_rels/workbook.xml.rels
                writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())

                // 4. xl/workbook.xml
                writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml())

                // 5. xl/styles.xml
                writeZipEntry(zos, "xl/styles.xml", buildStylesXml())

                // 6. xl/worksheets/sheet1.xml
                writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
            }
        }

        return outputFile
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Transactions Ledger" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="2">
    <numFmt numFmtId="164" formatCode="&quot;₹&quot;#,##0.00"/>
    <numFmt numFmtId="165" formatCode="dd-mmm-yyyy"/>
  </numFmts>
  <fonts count="6">
    <!-- 0: Normal Font -->
    <font><sz val="10"/><name val="Calibri"/></font>
    <!-- 1: Title Font Bold 16pt White -->
    <font><b/><sz val="15"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <!-- 2: Section Header Bold 11pt -->
    <font><b/><sz val="11"/><color rgb="FF182B49"/><name val="Calibri"/></font>
    <!-- 3: Table Header Bold 10pt White -->
    <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <!-- 4: Green Bold Positive -->
    <font><b/><sz val="10"/><color rgb="FF2E7D32"/><name val="Calibri"/></font>
    <!-- 5: Red Bold Negative -->
    <font><b/><sz val="10"/><color rgb="FFDC2626"/><name val="Calibri"/></font>
  </fonts>
  <fills count="6">
    <!-- 0: None -->
    <fill><patternFill patternType="none"/></fill>
    <!-- 1: Gray125 -->
    <fill><patternFill patternType="gray125"/></fill>
    <!-- 2: Dark Navy Fill for Title and Headers -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF182B49"/></patternFill></fill>
    <!-- 3: Light Green Fill for Received -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8F5E9"/></patternFill></fill>
    <!-- 4: Light Red Fill for Paid -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/></patternFill></fill>
    <!-- 5: Light Slate Fill for Summary -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/></patternFill></fill>
  </fills>
  <borders count="2">
    <!-- 0: None -->
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <!-- 1: Thin gray border -->
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="9">
    <!-- 0: Normal (default) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <!-- 1: Main Title Banner -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <!-- 2: Subtitle Metadata -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <!-- 3: Table Header -->
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 4: Table Data Cell (Normal with border) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <!-- 5: Positive Amount (Green, Currency) -->
    <xf numFmtId="164" fontId="4" fillId="0" borderId="1" xfId="0" applyFont="1" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right"/>
    </xf>
    <!-- 6: Negative Amount (Red, Currency) -->
    <xf numFmtId="164" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right"/>
    </xf>
    <!-- 7: KPI Box Cell -->
    <xf numFmtId="0" fontId="2" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <!-- 8: Table Data Cell Center -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    private fun buildSheetXml(
        projectName: String,
        dateRangeLabel: String,
        memberFilterLabel: String?,
        generatedTimestamp: String,
        totalGot: Double,
        totalGave: Double,
        netBalance: Double,
        transactions: List<Expense>,
        members: Map<String, ProjectMember>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths
        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="14" customWidth="1"/>""") // Date
        sb.append("""<col min="2" max="2" width="13" customWidth="1"/>""") // Type
        sb.append("""<col min="3" max="3" width="28" customWidth="1"/>""") // Party
        sb.append("""<col min="4" max="4" width="14" customWidth="1"/>""") // Mode
        sb.append("""<col min="5" max="5" width="20" customWidth="1"/>""") // Paid By
        sb.append("""<col min="6" max="6" width="30" customWidth="1"/>""") // Notes
        sb.append("""<col min="7" max="7" width="16" customWidth="1"/>""") // Amount Received
        sb.append("""<col min="8" max="8" width="16" customWidth="1"/>""") // Amount Paid
        sb.append("""</cols>""")

        sb.append("""<sheetData>""")

        var rowNum = 1

        // Row 1: Title Banner
        sb.append("""<row r="$rowNum" ht="30" customHeight="1">""")
        sb.append("""<c r="A$rowNum" s="1" t="inlineStr"><is><t>PARTNER LEDGER REPORT - ${escapeXml(projectName)}</t></is></c>""")
        sb.append("""</row>""")
        rowNum++

        // Row 2: Metadata (Period & Generation date)
        val filterSuffix = if (!memberFilterLabel.isNullOrBlank()) " | Filter: $memberFilterLabel" else ""
        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="2" t="inlineStr"><is><t>Period: ${escapeXml(dateRangeLabel)}${escapeXml(filterSuffix)} | Generated: $generatedTimestamp</t></is></c>""")
        sb.append("""</row>""")
        rowNum += 2

        // Row 4: KPI Summary Header
        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="7" t="inlineStr"><is><t>FINANCIAL SUMMARY</t></is></c>""")
        sb.append("""<c r="B$rowNum" s="7"/>""")
        sb.append("""<c r="C$rowNum" s="7"/>""")
        sb.append("""</row>""")
        rowNum++

        // Row 5: KPI Values
        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>Total Received (You Got)</t></is></c>""")
        sb.append("""<c r="B$rowNum" s="5"><v>$totalGot</v></c>""")
        sb.append("""</row>""")
        rowNum++

        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>Total Paid (You Gave)</t></is></c>""")
        sb.append("""<c r="B$rowNum" s="6"><v>$totalGave</v></c>""")
        sb.append("""</row>""")
        rowNum++

        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>Net Balance</t></is></c>""")
        val netStyle = if (netBalance >= 0) "5" else "6"
        sb.append("""<c r="B$rowNum" s="$netStyle"><v>$netBalance</v></c>""")
        sb.append("""</row>""")
        rowNum += 2

        // Row 8: Member Spending KPI Breakdown
        sb.append("""<row r="$rowNum">""")
        sb.append("""<c r="A$rowNum" s="7" t="inlineStr"><is><t>TOTAL SPENT BY MEMBER (EXPENSE BREAKDOWN)</t></is></c>""")
        sb.append("""<c r="B$rowNum" s="7" t="inlineStr"><is><t>Total Paid</t></is></c>""")
        sb.append("""<c r="C$rowNum" s="7" t="inlineStr"><is><t>% Share</t></is></c>""")
        sb.append("""</row>""")
        rowNum++

        val allPayerUids = (transactions.filter { !it.isIncome }.map { it.paidByUid } + members.keys).distinct()
        val spendingByMember = allPayerUids.mapNotNull { uid ->
            val spent = transactions.filter { !it.isIncome && it.paidByUid == uid }.sumOf { it.amount }
            val member = members[uid]
            val name = member?.name?.ifBlank { member.email.substringBefore("@") }
                ?: if (uid.isNotBlank()) "Partner (${uid.take(4)})" else "Partner"
            if (spent > 0.0 || (members.containsKey(uid) && allPayerUids.size <= 4)) {
                Triple(name, spent, if (totalGave > 0) (spent / totalGave * 100) else 0.0)
            } else null
        }.sortedByDescending { it.second }

        if (spendingByMember.isEmpty()) {
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>No member expense spending recorded in this period.</t></is></c>""")
            sb.append("""<c r="B$rowNum" s="4"><v>0</v></c>""")
            sb.append("""<c r="C$rowNum" s="4" t="inlineStr"><is><t>0%</t></is></c>""")
            sb.append("""</row>""")
            rowNum++
        } else {
            for ((memName, spent, pct) in spendingByMember) {
                sb.append("""<row r="$rowNum">""")
                sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(memName)}</t></is></c>""")
                sb.append("""<c r="B$rowNum" s="6"><v>$spent</v></c>""")
                sb.append("""<c r="C$rowNum" s="4" t="inlineStr"><is><t>${pct.toInt()}%</t></is></c>""")
                sb.append("""</row>""")
                rowNum++
            }
        }
        rowNum += 2

        // Table Header Row
        sb.append("""<row r="$rowNum" ht="22" customHeight="1">""")
        sb.append("""<c r="A$rowNum" s="3" t="inlineStr"><is><t>Date</t></is></c>""")
        sb.append("""<c r="B$rowNum" s="3" t="inlineStr"><is><t>Type</t></is></c>""")
        sb.append("""<c r="C$rowNum" s="3" t="inlineStr"><is><t>Party / Description</t></is></c>""")
        sb.append("""<c r="D$rowNum" s="3" t="inlineStr"><is><t>Mode</t></is></c>""")
        sb.append("""<c r="E$rowNum" s="3" t="inlineStr"><is><t>Paid By</t></is></c>""")
        sb.append("""<c r="F$rowNum" s="3" t="inlineStr"><is><t>Notes / Memo</t></is></c>""")
        sb.append("""<c r="G$rowNum" s="3" t="inlineStr"><is><t>Received (₹)</t></is></c>""")
        sb.append("""<c r="H$rowNum" s="3" t="inlineStr"><is><t>Paid (₹)</t></is></c>""")
        sb.append("""</row>""")
        rowNum++

        // Table Data Rows
        if (transactions.isEmpty()) {
            sb.append("""<row r="$rowNum">""")
            sb.append("""<c r="A$rowNum" s="4" t="inlineStr"><is><t>No transactions recorded for the selected filter.</t></is></c>""")
            for (col in listOf("B", "C", "D", "E", "F", "G", "H")) {
                sb.append("""<c r="$col$rowNum" s="4"/>""")
            }
            sb.append("""</row>""")
            rowNum++
        } else {
            for (txn in transactions) {
                val isIncome = txn.isIncome
                val dateStr = txn.date.formatDate()
                val typeStr = if (isIncome) "YOU GOT" else "YOU GAVE"
                val partyStr = escapeXml(txn.supplierName)
                val modeStr = escapeXml(txn.paymentMode.ifBlank { "UPI" })
                val handlerName = members[txn.paidByUid]?.name?.ifBlank { members[txn.paidByUid]?.email } ?: "Partner"
                val paidByStr = escapeXml(handlerName)
                val noteStr = escapeXml(txn.note ?: "")

                sb.append("""<row r="$rowNum">""")
                sb.append("""<c r="A$rowNum" s="8" t="inlineStr"><is><t>$dateStr</t></is></c>""")
                val typeStyle = if (isIncome) "5" else "6"
                sb.append("""<c r="B$rowNum" s="$typeStyle" t="inlineStr"><is><t>$typeStr</t></is></c>""")
                sb.append("""<c r="C$rowNum" s="4" t="inlineStr"><is><t>$partyStr</t></is></c>""")
                sb.append("""<c r="D$rowNum" s="8" t="inlineStr"><is><t>$modeStr</t></is></c>""")
                sb.append("""<c r="E$rowNum" s="4" t="inlineStr"><is><t>$paidByStr</t></is></c>""")
                sb.append("""<c r="F$rowNum" s="4" t="inlineStr"><is><t>$noteStr</t></is></c>""")

                if (isIncome) {
                    sb.append("""<c r="G$rowNum" s="5"><v>${txn.amount}</v></c>""")
                    sb.append("""<c r="H$rowNum" s="4"/>""")
                } else {
                    sb.append("""<c r="G$rowNum" s="4"/>""")
                    sb.append("""<c r="H$rowNum" s="6"><v>${txn.amount}</v></c>""")
                }

                sb.append("""</row>""")
                rowNum++
            }
        }

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")

        return sb.toString()
    }

    fun openExcel(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MIME_TYPE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Open Spreadsheet"))
        } catch (e: Exception) {
            shareExcel(context, file)
        }
    }

    fun shareExcel(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Spreadsheet (.xlsx)"))
    }
}
