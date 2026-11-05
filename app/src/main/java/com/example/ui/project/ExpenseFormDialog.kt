package com.example.ui.project

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.ExpenseAttachment
import com.example.data.model.ProjectMember
import com.example.data.util.AttachmentManager
import com.example.data.util.formatDate
import com.example.ui.components.PartnerBottomSheet
import com.example.ui.components.PartnerIcon
import com.example.ui.components.PartnerIconType
import com.example.ui.components.PartnerInputField
import com.example.ui.components.PartnerPrimaryButton
import com.example.ui.theme.PartnerTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    projectId: String,
    initialExpense: Expense? = null,
    targetExpenseId: String = "",
    initialType: String = initialExpense?.type ?: "GAVE",
    members: Map<String, ProjectMember>,
    currentUserId: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (
        supplierName: String,
        supplierPhone: String?,
        amount: Double,
        date: Timestamp,
        paidByUid: String,
        note: String?,
        type: String,
        paymentMode: String,
        attachments: List<ExpenseAttachment>,
        expenseId: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val effectiveExpenseId = remember {
        initialExpense?.id?.ifBlank { null } ?: targetExpenseId.ifBlank { UUID.randomUUID().toString() }
    }

    var type by remember { mutableStateOf(initialExpense?.type ?: initialType) }
    val isGave = type == "GAVE"

    var paymentMode by remember { mutableStateOf(initialExpense?.paymentMode ?: "UPI") }
    var supplierName by remember { mutableStateOf(initialExpense?.supplierName ?: "") }
    var supplierPhone by remember { mutableStateOf(initialExpense?.supplierPhone ?: "") }
    var amountInput by remember {
        mutableStateOf(if (initialExpense != null && initialExpense.amount > 0) initialExpense.amount.toLong().toString() else "")
    }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }

    var selectedTimestamp by remember {
        mutableStateOf(initialExpense?.date ?: Timestamp.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var paidByUid by remember {
        mutableStateOf(initialExpense?.paidByUid ?: currentUserId)
    }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    val attachedFiles = remember {
        mutableStateListOf<ExpenseAttachment>().apply {
            if (initialExpense != null) addAll(initialExpense.attachments)
        }
    }

    var isUploading by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIndex != -1) {
                                val name = cursor.getString(nameIndex)
                                if (!name.isNullOrBlank()) {
                                    supplierName = name
                                    nameError = false
                                }
                            }
                            if (numberIndex != -1) {
                                val number = cursor.getString(numberIndex)
                                if (!number.isNullOrBlank()) {
                                    supplierPhone = number.replace("\\s".toRegex(), "")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    try {
                        context.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    val name = cursor.getString(nameIndex)
                                    if (!name.isNullOrBlank()) {
                                        supplierName = name
                                        nameError = false
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 5 - attachedFiles.size
            val urisToProcess = uris.take(remainingSlots)
            coroutineScope.launch {
                isUploading = true
                uploadErrorMessage = null
                for (uri in urisToProcess) {
                    val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: ""
                    var isPdf = mimeType == "application/pdf"
                    if (!isPdf) {
                        try {
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIdx != -1) {
                                        val name = cursor.getString(nameIdx)?.lowercase() ?: ""
                                        if (name.endsWith(".pdf")) isPdf = true
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    val prepResult = AttachmentManager.prepareAttachment(context, uri, isImage = !isPdf)
                    prepResult.fold(
                        onSuccess = { prep ->
                            val uploadResult = AttachmentManager.uploadAttachment(
                                projectId = projectId,
                                expenseId = effectiveExpenseId,
                                preparedFile = prep,
                                uploadedByUid = currentUserId,
                                onProgress = {}
                            )
                            uploadResult.fold(
                                onSuccess = { attachment ->
                                    attachedFiles.add(attachment)
                                },
                                onFailure = { err ->
                                    uploadErrorMessage = err.message ?: "Upload failed"
                                }
                            )
                        },
                        onFailure = { err ->
                            uploadErrorMessage = err.message ?: "Failed to process attachment"
                        }
                    )
                }
                isUploading = false
            }
        }
    }

    PartnerBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        testTag = "expense_form_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title and Cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialExpense != null) "Edit entry" else "New entry",
                    style = typography.screenTitle,
                    color = colors.text
                )
                Text(
                    text = "CANCEL",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .testTag("cancel_expense_button")
                )
            }

            // -------------------------------------------------------------
            // Top Segmented Switch: YOU GAVE / YOU GOT
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // YOU GAVE Button
                val gaveSelected = isGave
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(dimens.buttonRadius))
                        .background(if (gaveSelected) colors.redSoft else colors.surface)
                        .border(
                            width = if (gaveSelected) dimens.emphasizedBorder else dimens.standardBorder,
                            color = if (gaveSelected) colors.dangerRed else colors.border,
                            shape = RoundedCornerShape(dimens.buttonRadius)
                        )
                        .clickable { type = "GAVE" }
                        .testTag("type_gave_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PartnerIcon(
                            type = PartnerIconType.ARROW_OUT,
                            tint = if (gaveSelected) colors.dangerRed else colors.textMuted,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YOU GAVE",
                            style = typography.primaryButton.copy(fontSize = 13.sp),
                            color = if (gaveSelected) colors.dangerRed else colors.textMuted
                        )
                    }
                }

                // YOU GOT Button
                val gotSelected = !isGave
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(dimens.buttonRadius))
                        .background(if (gotSelected) colors.accentSoft else colors.surface)
                        .border(
                            width = if (gotSelected) dimens.emphasizedBorder else dimens.standardBorder,
                            color = if (gotSelected) colors.accent else colors.border,
                            shape = RoundedCornerShape(dimens.buttonRadius)
                        )
                        .clickable { type = "GOT" }
                        .testTag("type_got_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PartnerIcon(
                            type = PartnerIconType.ARROW_IN,
                            tint = if (gotSelected) colors.accent else colors.textMuted,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YOU GOT",
                            style = typography.primaryButton.copy(fontSize = 13.sp),
                            color = if (gotSelected) colors.accent else colors.textMuted
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Party Name Input with Contact Picker & Direct Typing
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                PartnerInputField(
                    value = supplierName,
                    onValueChange = {
                        supplierName = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = if (isGave) "PAID TO" else "RECEIVED FROM",
                    placeholder = "Type name or pick from contacts",
                    isError = nameError,
                    errorMessage = if (nameError) "Party name is required" else null,
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentSoft)
                                .border(dimens.standardBorder, colors.accent, RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        contactPickerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val fallback = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                            contactPickerLauncher.launch(fallback)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Cannot open contacts", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("pick_contact_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PartnerIcon(
                                    type = PartnerIconType.TAB_MEMBERS,
                                    tint = colors.accent,
                                    size = 14.dp
                                )
                                Text(
                                    text = "CONTACTS",
                                    style = typography.smallLabel.copy(fontSize = 10.sp),
                                    color = colors.accent
                                )
                            }
                        }
                    },
                    testTag = "supplier_name_input"
                )

                if (supplierPhone.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHONE: $supplierPhone",
                            style = typography.notes,
                            color = colors.textMuted
                        )
                        Text(
                            text = "CLEAR PHONE",
                            style = typography.smallLabel.copy(fontSize = 10.sp),
                            color = colors.dangerRed,
                            modifier = Modifier
                                .clickable { supplierPhone = "" }
                                .padding(4.dp)
                                .testTag("clear_supplier_phone_button")
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Large Amount Field in Accent Color
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AMOUNT ₹",
                    style = typography.smallLabel,
                    color = if (amountError) colors.dangerRed else colors.textMuted,
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                )

                val amountColor = if (isGave) colors.dangerRed else colors.accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.buttonRadius))
                        .background(colors.surface)
                        .border(
                            width = dimens.standardBorder,
                            color = if (amountError) colors.dangerRed else colors.border,
                            shape = RoundedCornerShape(dimens.buttonRadius)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("amount_input")
                ) {
                    if (amountInput.isEmpty()) {
                        Text(
                            text = "0",
                            style = typography.netBalance.copy(fontSize = 28.sp),
                            color = colors.textMuted.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = amountInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            amountInput = filtered
                            if (amountError && filtered.isNotBlank()) amountError = false
                        },
                        singleLine = true,
                        textStyle = typography.netBalance.copy(
                            fontSize = 28.sp,
                            color = amountColor
                        ),
                        cursorBrush = SolidColor(amountColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (amountError) {
                    Text(
                        text = "Valid amount is required",
                        style = typography.notes,
                        color = colors.dangerRed,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // Side-by-Side: Date & Paid/Received By
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATE",
                        style = typography.smallLabel,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(dimens.buttonRadius))
                            .background(colors.surface)
                            .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.buttonRadius))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp)
                            .testTag("date_picker_button"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = selectedTimestamp.formatDate(),
                            style = typography.bodyBold,
                            color = colors.text
                        )
                    }
                }

                // Paid By / Received By Column
                Column(modifier = Modifier.weight(1f)) {
                    val partnerActionLabel = if (isGave) "PAID BY" else "RECEIVED BY"
                    Text(
                        text = partnerActionLabel,
                        style = typography.smallLabel,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedMemberName = members[paidByUid]?.name?.ifBlank { members[paidByUid]?.email }
                            ?: currentUserName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(dimens.buttonRadius))
                                .background(colors.surface)
                                .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.buttonRadius))
                                .clickable { memberDropdownExpanded = true }
                                .padding(horizontal = 12.dp)
                                .testTag("paid_by_dropdown"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = selectedMemberName,
                                style = typography.bodyBold,
                                color = colors.text,
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            members.forEach { (uid, member) ->
                                val name = member.name.ifBlank { member.email }
                                DropdownMenuItem(
                                    text = { Text(name, style = typography.body, color = colors.text) },
                                    onClick = {
                                        paidByUid = uid
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Payment Mode: 2x2 Grid of Icon Buttons
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PAYMENT MODE",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )

                val modes = listOf(
                    Pair("UPI", PartnerIconType.MODE_UPI),
                    Pair("Cash", PartnerIconType.MODE_CASH),
                    Pair("Cheque", PartnerIconType.MODE_CHEQUE),
                    Pair("Netbanking", PartnerIconType.MODE_NETBANKING)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentModeTile(
                            mode = modes[0].first,
                            iconType = modes[0].second,
                            isSelected = paymentMode == modes[0].first,
                            onClick = { paymentMode = modes[0].first },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentModeTile(
                            mode = modes[1].first,
                            iconType = modes[1].second,
                            isSelected = paymentMode == modes[1].first,
                            onClick = { paymentMode = modes[1].first },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentModeTile(
                            mode = modes[2].first,
                            iconType = modes[2].second,
                            isSelected = paymentMode == modes[2].first,
                            onClick = { paymentMode = modes[2].first },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentModeTile(
                            mode = modes[3].first,
                            iconType = modes[3].second,
                            isSelected = paymentMode == modes[3].first,
                            onClick = { paymentMode = modes[3].first },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Optional Note
            // -------------------------------------------------------------
            PartnerInputField(
                value = note,
                onValueChange = { note = it },
                label = "NOTE (OPTIONAL)",
                placeholder = "What was this for?",
                testTag = "expense_note_input"
            )

            // -------------------------------------------------------------
            // Attachments Row ("+ Attach", up to 5, JPG/PNG/PDF)
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.buttonRadius))
                        .background(colors.surface)
                        .border(dimens.standardBorder, colors.border, RoundedCornerShape(dimens.buttonRadius))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATTACHMENTS ${attachedFiles.size}/5 · JPG PNG PDF",
                            style = typography.smallLabel.copy(fontSize = 11.sp),
                            color = colors.textMuted
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(dimens.standardBorder, colors.border, RoundedCornerShape(8.dp))
                                .clickable(enabled = attachedFiles.size < 5) {
                                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("attach_receipt_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ ATTACH",
                                style = typography.smallLabel,
                                color = if (attachedFiles.size < 5) colors.accent else colors.textMuted
                            )
                        }
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent
                    )
                }

                if (attachedFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        attachedFiles.forEach { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface)
                                    .border(dimens.standardBorder, colors.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (att.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (att.isPdf) colors.dangerRed else colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = att.name,
                                        style = typography.notes,
                                        color = if (att.isPdf) colors.text else colors.accent,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { attachedFiles.remove(att) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = colors.dangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (uploadErrorMessage != null) {
                    Text(
                        text = uploadErrorMessage!!,
                        style = typography.notes,
                        color = colors.dangerRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // -------------------------------------------------------------
            // Big SAVE ENTRY Button
            // -------------------------------------------------------------
            PartnerPrimaryButton(
                text = "SAVE ENTRY",
                onClick = {
                    val parsedAmount = amountInput.toDoubleOrNull()
                    var valid = true

                    if (supplierName.isBlank()) {
                        nameError = true
                        valid = false
                    }
                    if (parsedAmount == null || parsedAmount <= 0) {
                        amountError = true
                        valid = false
                    }

                    if (valid && parsedAmount != null) {
                        onSave(
                            supplierName.trim(),
                            supplierPhone.ifBlank { null },
                            parsedAmount,
                            selectedTimestamp,
                            paidByUid,
                            note.trim().ifBlank { null },
                            type,
                            paymentMode,
                            attachedFiles.toList(),
                            effectiveExpenseId
                        )
                    }
                },
                testTag = "save_expense_button"
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp.toDate().time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        selectedTimestamp = Timestamp(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", style = typography.bodyBold, color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", style = typography.body, color = colors.textMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PaymentModeTile(
    mode: String,
    iconType: PartnerIconType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    val bg = if (isSelected) colors.accentSoft else colors.surface
    val borderCol = if (isSelected) colors.accent else colors.border
    val tint = if (isSelected) colors.accent else colors.textMuted
    val textCol = if (isSelected) colors.accent else colors.text

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .background(bg)
            .border(dimens.standardBorder, borderCol, RoundedCornerShape(dimens.buttonRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("mode_$mode"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            PartnerIcon(
                type = iconType,
                tint = tint,
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mode,
                style = typography.bodyBold.copy(fontSize = 13.sp),
                color = textCol
            )
        }
    }
}
