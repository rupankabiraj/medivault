package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ai.DocumentOcrService
import com.example.data.model.BillCategory
import com.example.data.model.FamilyMember
import com.example.data.model.InferredBillData
import com.example.data.model.InferredPrescriptionData
import com.example.data.model.InferredReportData
import com.example.data.model.MedicalBill
import com.example.data.model.MedicalReport
import com.example.data.model.Medication
import com.example.data.model.MedicineForm
import com.example.data.model.MedicineFrequency
import com.example.data.model.MedicineTiming
import com.example.data.model.PaymentStatus
import com.example.data.model.Prescription
import com.example.data.model.PrescriptionStatus
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.util.AttachmentUtils
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun DocumentAttachmentPickerSection(
    attachmentUri: String?,
    attachmentName: String?,
    aiExtractedNotes: String?,
    documentTypeTitle: String,
    isInferring: Boolean,
    onPickImage: () -> Unit,
    onTriggerInfer: () -> Unit,
    onRemoveAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scanned $documentTypeTitle Attachment",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!attachmentUri.isNullOrBlank()) {
                    IconButton(
                        onClick = onRemoveAttachment,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (attachmentUri.isNullOrBlank()) {
                // Button to attach
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_attach_document"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Scanned Image / Document")
                }
            } else {
                // Attached Preview Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val file = File(attachmentUri)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (file.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(file)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attached Document",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachmentName ?: "Scanned_document.jpg",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Attached & Saved locally",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldTertiary
                        )
                    }

                    OutlinedButton(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Replace", fontSize = 12.sp)
                    }
                }

                // AI Inference Action Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Gemini AI Auto-Fill",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                )
                            }

                            if (isInferring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = TealPrimary
                                )
                            } else {
                                Button(
                                    onClick = onTriggerInfer,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    modifier = Modifier.testTag("btn_auto_infer_gemini")
                                ) {
                                    Text("Auto-Extract Details", fontSize = 12.sp)
                                }
                            }
                        }

                        if (isInferring) {
                            Text(
                                text = "Analyzing image with Gemini 2.5 Flash... Reading text, amounts & clinical parameters.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TealPrimary
                            )
                        } else if (!aiExtractedNotes.isNullOrBlank()) {
                            Text(
                                text = "✨ $aiExtractedNotes",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "Tap 'Auto-Extract Details' to automatically fill amounts, doctors, test findings, and medications!",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD / EDIT BILL SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditBillSheet(
    bill: MedicalBill?,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        memberId: Long,
        provider: String,
        doctor: String,
        date: Long,
        category: String,
        invoiceNum: String,
        total: Double,
        insured: Double,
        outOfPocket: Double,
        status: String,
        dueDate: Long?,
        notes: String,
        lineItems: String,
        attachmentUri: String?,
        attachmentName: String?,
        aiExtractedNotes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ocrService = remember { DocumentOcrService(context) }

    var provider by remember { mutableStateOf(bill?.providerName ?: "") }
    var doctor by remember { mutableStateOf(bill?.doctorName ?: "") }
    var category by remember { mutableStateOf(bill?.category ?: BillCategory.CONSULTATION.name) }
    var invoiceNum by remember { mutableStateOf(bill?.invoiceNumber ?: "") }
    var totalAmountStr by remember { mutableStateOf(if (bill != null) bill.totalAmount.toString() else "") }
    var insuredAmountStr by remember { mutableStateOf(if (bill != null) bill.insuranceCoveredAmount.toString() else "0") }
    var paymentStatus by remember { mutableStateOf(bill?.paymentStatus ?: PaymentStatus.PAID.name) }
    var lineItems by remember { mutableStateOf(bill?.lineItemsRaw ?: "") }
    var notes by remember { mutableStateOf(bill?.notes ?: "") }
    var memberId by remember { mutableLongStateOf(bill?.memberId ?: selectedMemberId ?: members.firstOrNull()?.id ?: 1L) }

    // Attachment state
    var attachmentUri by remember { mutableStateOf(bill?.attachmentUri) }
    var attachmentName by remember { mutableStateOf(bill?.attachmentName) }
    var aiExtractedNotes by remember { mutableStateOf(bill?.aiExtractedNotes) }
    var isInferring by remember { mutableStateOf(false) }

    // Gallery Picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = AttachmentUtils.saveAttachmentToInternalStorage(context, uri, prefix = "bill")
            if (saved != null) {
                attachmentUri = saved.first.removePrefix("file://")
                attachmentName = saved.second
                Toast.makeText(context, "Scanned bill attached successfully!", Toast.LENGTH_SHORT).show()

                // Auto-infer immediately for user convenience
                scope.launch {
                    isInferring = true
                    val result = ocrService.extractBillDetails(uri)
                    isInferring = false
                    result.onSuccess { data ->
                        if (provider.isBlank() && data.providerName.isNotBlank()) provider = data.providerName
                        if (doctor.isBlank() && data.doctorName.isNotBlank()) doctor = data.doctorName
                        if (data.totalAmount > 0) totalAmountStr = data.totalAmount.toString()
                        if (data.insuranceCoveredAmount > 0) insuredAmountStr = data.insuranceCoveredAmount.toString()
                        if (data.invoiceNumber.isNotBlank()) invoiceNum = data.invoiceNumber
                        if (data.lineItemsRaw.isNotBlank()) lineItems = data.lineItemsRaw
                        if (data.notes.isNotBlank()) notes = data.notes
                        category = data.category
                        paymentStatus = data.paymentStatus
                        aiExtractedNotes = data.aiSummary
                        Toast.makeText(context, "AI extracted bill details successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Inference note: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun triggerOcrExtraction() {
        if (attachmentUri.isNullOrBlank()) return
        val fileUri = Uri.fromFile(File(attachmentUri!!))
        scope.launch {
            isInferring = true
            val result = ocrService.extractBillDetails(fileUri)
            isInferring = false
            result.onSuccess { data ->
                provider = data.providerName
                doctor = data.doctorName
                category = data.category
                if (data.totalAmount > 0) totalAmountStr = data.totalAmount.toString()
                if (data.insuranceCoveredAmount > 0) insuredAmountStr = data.insuranceCoveredAmount.toString()
                invoiceNum = data.invoiceNumber
                lineItems = data.lineItemsRaw
                notes = data.notes
                paymentStatus = data.paymentStatus
                aiExtractedNotes = data.aiSummary
                Toast.makeText(context, "Details extracted and filled!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Extraction error: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bill == null) "Log Medical Bill" else "Edit Medical Bill",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Document Attachment & AI OCR Section
            DocumentAttachmentPickerSection(
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes,
                documentTypeTitle = "Bill / Invoice",
                isInferring = isInferring,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onTriggerInfer = { triggerOcrExtraction() },
                onRemoveAttachment = {
                    attachmentUri = null
                    attachmentName = null
                    aiExtractedNotes = null
                }
            )

            // Member Selector
            Text(text = "Patient / Family Member:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (m in members) {
                    FilterChip(
                        selected = memberId == m.id,
                        onClick = { memberId = m.id },
                        label = { Text("${m.name} (${m.relationship})") }
                    )
                }
            }

            // Provider & Doctor
            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text("Hospital / Clinic / Pharmacy Name *") },
                placeholder = { Text("e.g. City Hospital, Apollo Pharmacy") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bill_provider")
            )

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Doctor / Specialist Name") },
                placeholder = { Text("e.g. Dr. Emily Watson") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection
            Text(text = "Expense Category:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (cat in BillCategory.values()) {
                    FilterChip(
                        selected = category == cat.name,
                        onClick = { category = cat.name },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            // Financial Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = totalAmountStr,
                    onValueChange = { totalAmountStr = it },
                    label = { Text("Total Bill (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_bill_total")
                )

                OutlinedTextField(
                    value = insuredAmountStr,
                    onValueChange = { insuredAmountStr = it },
                    label = { Text("Insurance Paid (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Payment / Claim Status
            Text(text = "Payment Status:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (st in PaymentStatus.values()) {
                    FilterChip(
                        selected = paymentStatus == st.name,
                        onClick = { paymentStatus = st.name },
                        label = { Text(st.label) }
                    )
                }
            }

            // Invoice Number
            OutlinedTextField(
                value = invoiceNum,
                onValueChange = { invoiceNum = it },
                label = { Text("Invoice / Receipt Number") },
                placeholder = { Text("e.g. INV-2026-8891") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Line Items Breakdown
            OutlinedTextField(
                value = lineItems,
                onValueChange = { lineItems = it },
                label = { Text("Itemized Charges / Services") },
                placeholder = { Text("Consultation: ₹500\nECG Test: ₹800\nMedicines: ₹450") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Additional Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Claim Reference") },
                placeholder = { Text("e.g. Paid via UPI, Claim Reference #CLM123") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = {
                    val total = totalAmountStr.toDoubleOrNull() ?: 0.0
                    val insured = insuredAmountStr.toDoubleOrNull() ?: 0.0
                    val outOfPocket = (total - insured).coerceAtLeast(0.0)
                    if (provider.isBlank()) {
                        Toast.makeText(context, "Please enter hospital or provider name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(
                        bill?.id ?: 0L,
                        memberId,
                        provider,
                        doctor,
                        bill?.billDate ?: System.currentTimeMillis(),
                        category,
                        invoiceNum,
                        total,
                        insured,
                        outOfPocket,
                        paymentStatus,
                        bill?.dueDate,
                        notes,
                        lineItems,
                        attachmentUri,
                        attachmentName,
                        aiExtractedNotes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bill_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (bill == null) "Save Medical Bill" else "Update Medical Bill",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// ADD / EDIT REPORT SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditReportSheet(
    report: MedicalReport?,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        memberId: Long,
        testName: String,
        category: String,
        reportDate: Long,
        facility: String,
        doctor: String,
        summary: String,
        status: String,
        biomarkers: String,
        followUpDate: Long?,
        notes: String,
        attachmentUri: String?,
        attachmentName: String?,
        aiExtractedNotes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ocrService = remember { DocumentOcrService(context) }

    var testName by remember { mutableStateOf(report?.testName ?: "") }
    var category by remember { mutableStateOf(report?.category ?: ReportCategory.PATHOLOGY_BLOOD.name) }
    var facility by remember { mutableStateOf(report?.labOrFacility ?: "") }
    var doctor by remember { mutableStateOf(report?.orderingDoctor ?: "") }
    var summary by remember { mutableStateOf(report?.summaryFindings ?: "") }
    var status by remember { mutableStateOf(report?.status ?: ReportStatus.NORMAL.name) }
    var biomarkers by remember { mutableStateOf(report?.biomarkersRaw ?: "") }
    var notes by remember { mutableStateOf(report?.notes ?: "") }
    var memberId by remember { mutableLongStateOf(report?.memberId ?: selectedMemberId ?: members.firstOrNull()?.id ?: 1L) }

    // Attachment state
    var attachmentUri by remember { mutableStateOf(report?.attachmentUri) }
    var attachmentName by remember { mutableStateOf(report?.attachmentName) }
    var aiExtractedNotes by remember { mutableStateOf(report?.aiExtractedNotes) }
    var isInferring by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = AttachmentUtils.saveAttachmentToInternalStorage(context, uri, prefix = "report")
            if (saved != null) {
                attachmentUri = saved.first.removePrefix("file://")
                attachmentName = saved.second
                Toast.makeText(context, "Scanned lab report attached!", Toast.LENGTH_SHORT).show()

                // Auto-infer immediately
                scope.launch {
                    isInferring = true
                    val result = ocrService.extractReportDetails(uri)
                    isInferring = false
                    result.onSuccess { data ->
                        if (testName.isBlank()) testName = data.testName
                        category = data.category
                        if (facility.isBlank() && data.labOrFacility.isNotBlank()) facility = data.labOrFacility
                        if (doctor.isBlank() && data.orderingDoctor.isNotBlank()) doctor = data.orderingDoctor
                        summary = data.summaryFindings
                        status = data.status
                        if (data.biomarkers.isNotEmpty()) {
                            biomarkers = data.biomarkers.joinToString("\n") {
                                "${it.name}|${it.value}|${it.unit}|${it.referenceRange}|${it.flag}"
                            }
                        }
                        if (data.doctorAdvice.isNotBlank()) notes = data.doctorAdvice
                        aiExtractedNotes = data.aiSummary
                        Toast.makeText(context, "AI extracted diagnostic findings & biomarkers!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Inference note: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun triggerOcrExtraction() {
        if (attachmentUri.isNullOrBlank()) return
        val fileUri = Uri.fromFile(File(attachmentUri!!))
        scope.launch {
            isInferring = true
            val result = ocrService.extractReportDetails(fileUri)
            isInferring = false
            result.onSuccess { data ->
                testName = data.testName
                category = data.category
                facility = data.labOrFacility
                doctor = data.orderingDoctor
                summary = data.summaryFindings
                status = data.status
                if (data.biomarkers.isNotEmpty()) {
                    biomarkers = data.biomarkers.joinToString("\n") {
                        "${it.name}|${it.value}|${it.unit}|${it.referenceRange}|${it.flag}"
                    }
                }
                if (data.doctorAdvice.isNotBlank()) notes = data.doctorAdvice
                aiExtractedNotes = data.aiSummary
                Toast.makeText(context, "Findings & biomarkers populated!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Extraction error: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (report == null) "Log Diagnostic Report" else "Edit Diagnostic Report",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Document Attachment Section
            DocumentAttachmentPickerSection(
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes,
                documentTypeTitle = "Lab / Diagnostic Report",
                isInferring = isInferring,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onTriggerInfer = { triggerOcrExtraction() },
                onRemoveAttachment = {
                    attachmentUri = null
                    attachmentName = null
                    aiExtractedNotes = null
                }
            )

            // Member Selector
            Text(text = "Patient / Family Member:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (m in members) {
                    FilterChip(
                        selected = memberId == m.id,
                        onClick = { memberId = m.id },
                        label = { Text("${m.name} (${m.relationship})") }
                    )
                }
            }

            // Test Name
            OutlinedTextField(
                value = testName,
                onValueChange = { testName = it },
                label = { Text("Diagnostic Test Name *") },
                placeholder = { Text("e.g. Complete Blood Count (CBC), Lipid Profile, Chest X-Ray") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_report_test_name")
            )

            // Category Selection
            Text(text = "Report Category:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (cat in ReportCategory.values()) {
                    FilterChip(
                        selected = category == cat.name,
                        onClick = { category = cat.name },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            // Facility and Doctor
            OutlinedTextField(
                value = facility,
                onValueChange = { facility = it },
                label = { Text("Diagnostic Lab / Hospital Facility *") },
                placeholder = { Text("e.g. Lal PathLabs, Metropolis Healthcare") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Ordering Physician / Pathologist") },
                placeholder = { Text("e.g. Dr. Robert Vance") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Clinical Status Evaluation
            Text(text = "Clinical Result Status:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (st in ReportStatus.values()) {
                    FilterChip(
                        selected = status == st.name,
                        onClick = { status = st.name },
                        label = { Text(st.displayName) }
                    )
                }
            }

            // Clinical Summary / Key Findings
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Clinical Findings & Summary *") },
                placeholder = { Text("e.g. HbA1c is elevated at 7.2%. Mild microcytic hypochromic anemia noted.") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // Biomarkers Data (Pipe formatted)
            OutlinedTextField(
                value = biomarkers,
                onValueChange = { biomarkers = it },
                label = { Text("Biomarkers Data (Name|Value|Unit|Range|Flag)") },
                placeholder = { Text("Hemoglobin|11.5|g/dL|13.5-17.5|Low\nPlatelets|250000|cells/mcL|150000-450000|Normal") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes / Advice
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Doctor Advice / Follow-up Instructions") },
                placeholder = { Text("e.g. Repeat Lipid profile after 3 months of statin therapy") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = {
                    if (testName.isBlank()) {
                        Toast.makeText(context, "Please enter test name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(
                        report?.id ?: 0L,
                        memberId,
                        testName,
                        category,
                        report?.reportDate ?: System.currentTimeMillis(),
                        facility,
                        doctor,
                        summary,
                        status,
                        biomarkers,
                        report?.followUpDate,
                        notes,
                        attachmentUri,
                        attachmentName,
                        aiExtractedNotes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_report_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (report == null) "Save Lab Report" else "Update Lab Report",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// ADD / EDIT PRESCRIPTION SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPrescriptionSheet(
    prescription: Prescription?,
    medications: List<Medication>,
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        rxId: Long,
        memberId: Long,
        doctor: String,
        specialty: String,
        clinic: String,
        diagnosis: String,
        datePrescribed: Long,
        durationDays: Int,
        isOngoing: Boolean,
        followUpDate: Long?,
        advice: String,
        status: String,
        medicationsList: List<Medication>,
        attachmentUri: String?,
        attachmentName: String?,
        aiExtractedNotes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ocrService = remember { DocumentOcrService(context) }

    var doctor by remember { mutableStateOf(prescription?.doctorName ?: "") }
    var specialty by remember { mutableStateOf(prescription?.specialty ?: "") }
    var clinic by remember { mutableStateOf(prescription?.clinicOrHospital ?: "") }
    var diagnosis by remember { mutableStateOf(prescription?.diagnosis ?: "") }
    var durationDaysStr by remember { mutableStateOf((prescription?.durationDays ?: 14).toString()) }
    var advice by remember { mutableStateOf(prescription?.doctorAdvice ?: "") }
    var status by remember { mutableStateOf(prescription?.status ?: PrescriptionStatus.ACTIVE.name) }
    var isOngoing by remember { mutableStateOf(prescription?.isOngoing ?: false) }
    var memberId by remember { mutableLongStateOf(prescription?.memberId ?: selectedMemberId ?: members.firstOrNull()?.id ?: 1L) }

    // Dynamic Medications list
    val medsList = remember {
        mutableStateListOf<Medication>().apply {
            addAll(medications)
            if (isEmpty() && prescription == null) {
                add(
                    Medication(
                        memberId = memberId,
                        medicineName = "",
                        dosage = "500 mg",
                        form = MedicineForm.TABLET.name,
                        frequency = MedicineFrequency.TWICE_DAILY.name,
                        timing = MedicineTiming.AFTER_FOOD.displayName,
                        slotMorning = true,
                        slotEvening = true,
                        pillsRemaining = 20,
                        totalPrescribedPills = 20
                    )
                )
            }
        }
    }

    // Attachment state
    var attachmentUri by remember { mutableStateOf(prescription?.attachmentUri) }
    var attachmentName by remember { mutableStateOf(prescription?.attachmentName) }
    var aiExtractedNotes by remember { mutableStateOf(prescription?.aiExtractedNotes) }
    var isInferring by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = AttachmentUtils.saveAttachmentToInternalStorage(context, uri, prefix = "prescription")
            if (saved != null) {
                attachmentUri = saved.first.removePrefix("file://")
                attachmentName = saved.second
                Toast.makeText(context, "Scanned prescription attached!", Toast.LENGTH_SHORT).show()

                // Auto-infer immediately
                scope.launch {
                    isInferring = true
                    val result = ocrService.extractPrescriptionDetails(uri)
                    isInferring = false
                    result.onSuccess { data ->
                        if (doctor.isBlank() && data.doctorName.isNotBlank()) doctor = data.doctorName
                        if (specialty.isBlank() && data.specialty.isNotBlank()) specialty = data.specialty
                        if (clinic.isBlank() && data.clinicOrHospital.isNotBlank()) clinic = data.clinicOrHospital
                        if (diagnosis.isBlank() && data.diagnosis.isNotBlank()) diagnosis = data.diagnosis
                        if (data.doctorAdvice.isNotBlank()) advice = data.doctorAdvice
                        if (data.durationDays > 0) durationDaysStr = data.durationDays.toString()

                        if (data.medications.isNotEmpty()) {
                            medsList.clear()
                            for (m in data.medications) {
                                medsList.add(
                                    Medication(
                                        memberId = memberId,
                                        medicineName = m.name,
                                        dosage = m.dosage,
                                        form = m.form,
                                        frequency = m.frequency,
                                        timing = m.timing,
                                        pillsRemaining = m.pillsCount,
                                        totalPrescribedPills = m.pillsCount,
                                        slotMorning = m.slotMorning,
                                        slotAfternoon = m.slotAfternoon,
                                        slotEvening = m.slotEvening,
                                        slotNight = m.slotNight,
                                        instructions = m.timing
                                    )
                                )
                            }
                        }
                        aiExtractedNotes = data.aiSummary
                        Toast.makeText(context, "AI extracted prescription & medicines!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Inference note: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun triggerOcrExtraction() {
        if (attachmentUri.isNullOrBlank()) return
        val fileUri = Uri.fromFile(File(attachmentUri!!))
        scope.launch {
            isInferring = true
            val result = ocrService.extractPrescriptionDetails(fileUri)
            isInferring = false
            result.onSuccess { data ->
                doctor = data.doctorName
                specialty = data.specialty
                clinic = data.clinicOrHospital
                diagnosis = data.diagnosis
                advice = data.doctorAdvice
                if (data.durationDays > 0) durationDaysStr = data.durationDays.toString()

                if (data.medications.isNotEmpty()) {
                    medsList.clear()
                    for (m in data.medications) {
                        medsList.add(
                            Medication(
                                memberId = memberId,
                                medicineName = m.name,
                                dosage = m.dosage,
                                form = m.form,
                                frequency = m.frequency,
                                timing = m.timing,
                                pillsRemaining = m.pillsCount,
                                totalPrescribedPills = m.pillsCount,
                                slotMorning = m.slotMorning,
                                slotAfternoon = m.slotAfternoon,
                                slotEvening = m.slotEvening,
                                slotNight = m.slotNight,
                                instructions = m.timing
                            )
                        )
                    }
                }
                aiExtractedNotes = data.aiSummary
                Toast.makeText(context, "Prescription details populated!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Extraction error: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (prescription == null) "Log Doctor Prescription" else "Edit Prescription",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Document Attachment Section
            DocumentAttachmentPickerSection(
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes,
                documentTypeTitle = "Prescription",
                isInferring = isInferring,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onTriggerInfer = { triggerOcrExtraction() },
                onRemoveAttachment = {
                    attachmentUri = null
                    attachmentName = null
                    aiExtractedNotes = null
                }
            )

            // Member Selector
            Text(text = "Patient / Family Member:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (m in members) {
                    FilterChip(
                        selected = memberId == m.id,
                        onClick = { memberId = m.id },
                        label = { Text("${m.name} (${m.relationship})") }
                    )
                }
            }

            // Doctor & Clinic
            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Prescribing Doctor Name *") },
                placeholder = { Text("e.g. Dr. Rajesh Sharma") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rx_doctor")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Specialty") },
                    placeholder = { Text("e.g. Cardiologist") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = clinic,
                    onValueChange = { clinic = it },
                    label = { Text("Clinic / Hospital") },
                    placeholder = { Text("e.g. Care Hospital") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Diagnosis
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Diagnosis / Primary Condition *") },
                placeholder = { Text("e.g. Seasonal Flu, Hypertension Management") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rx_diagnosis")
            )

            // Duration & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = durationDaysStr,
                    onValueChange = { durationDaysStr = it },
                    label = { Text("Duration (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Status:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (st in PrescriptionStatus.values()) {
                            FilterChip(
                                selected = status == st.name,
                                onClick = { status = st.name },
                                label = { Text(st.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Doctor Advice
            OutlinedTextField(
                value = advice,
                onValueChange = { advice = it },
                label = { Text("Doctor's Advice & Lifestyle Guidelines") },
                placeholder = { Text("e.g. Drink plenty of warm water, avoid cold beverages") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Prescribed Medicines Section
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prescribed Medicines (${medsList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        medsList.add(
                            Medication(
                                memberId = memberId,
                                medicineName = "",
                                dosage = "500 mg",
                                form = MedicineForm.TABLET.name,
                                frequency = MedicineFrequency.TWICE_DAILY.name,
                                timing = MedicineTiming.AFTER_FOOD.displayName,
                                slotMorning = true,
                                slotEvening = true,
                                pillsRemaining = 14,
                                totalPrescribedPills = 14
                            )
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Medicine")
                }
            }

            // Render each medication card
            medsList.forEachIndexed { index, med ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Medicine #${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (medsList.size > 1) {
                                IconButton(
                                    onClick = { medsList.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Medicine Name & Dosage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = med.medicineName,
                                onValueChange = { newName ->
                                    medsList[index] = med.copy(medicineName = newName)
                                },
                                label = { Text("Name *") },
                                placeholder = { Text("e.g. Paracetamol") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = med.dosage,
                                onValueChange = { newDosage ->
                                    medsList[index] = med.copy(dosage = newDosage)
                                },
                                label = { Text("Dosage") },
                                placeholder = { Text("500 mg") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Form & Timing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = med.timing,
                                onValueChange = { newTiming ->
                                    medsList[index] = med.copy(timing = newTiming)
                                },
                                label = { Text("Timing") },
                                placeholder = { Text("After Meals") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = med.pillsRemaining.toString(),
                                onValueChange = { newCount ->
                                    val count = newCount.toIntOrNull() ?: 0
                                    medsList[index] = med.copy(pillsRemaining = count, totalPrescribedPills = count)
                                },
                                label = { Text("Pills Count") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Time slots reminder checkboxes
                        Text(text = "Daily Schedule Slots:", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = med.slotMorning,
                                    onCheckedChange = { medsList[index] = med.copy(slotMorning = it) }
                                )
                                Text("Morn", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = med.slotAfternoon,
                                    onCheckedChange = { medsList[index] = med.copy(slotAfternoon = it) }
                                )
                                Text("Noon", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = med.slotEvening,
                                    onCheckedChange = { medsList[index] = med.copy(slotEvening = it) }
                                )
                                Text("Eve", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = med.slotNight,
                                    onCheckedChange = { medsList[index] = med.copy(slotNight = it) }
                                )
                                Text("Night", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    val duration = durationDaysStr.toIntOrNull() ?: 14
                    if (doctor.isBlank()) {
                        Toast.makeText(context, "Please enter doctor name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (diagnosis.isBlank()) {
                        Toast.makeText(context, "Please enter diagnosis", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val validMeds = medsList.filter { it.medicineName.isNotBlank() }
                    onSave(
                        prescription?.id ?: 0L,
                        memberId,
                        doctor,
                        specialty,
                        clinic,
                        diagnosis,
                        prescription?.datePrescribed ?: System.currentTimeMillis(),
                        duration,
                        isOngoing,
                        prescription?.followUpDate,
                        advice,
                        status,
                        validMeds,
                        attachmentUri,
                        attachmentName,
                        aiExtractedNotes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_prescription_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (prescription == null) "Save Prescription" else "Update Prescription",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// ADD / EDIT FAMILY MEMBER DIALOG
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditMemberDialog(
    member: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        relationship: String,
        age: Int,
        bloodGroup: String,
        allergies: String,
        emergencyContact: String,
        colorHex: String
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(member?.name ?: "") }
    var relationship by remember { mutableStateOf(member?.relationship ?: "Self") }
    var ageStr by remember { mutableStateOf(if (member != null) member.age.toString() else "30") }
    var bloodGroup by remember { mutableStateOf(member?.bloodGroup ?: "O+") }
    var allergies by remember { mutableStateOf(member?.allergies ?: "") }
    var emergencyContact by remember { mutableStateOf(member?.emergencyContact ?: "") }
    var selectedColor by remember { mutableStateOf(member?.avatarColorHex ?: "#00897B") }

    val relationships = listOf("Self", "Spouse", "Son", "Daughter", "Father", "Mother", "Grandfather", "Grandmother", "Sibling", "Other")
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val colorPalettes = listOf("#00897B", "#00ACC1", "#43A047", "#7CB342", "#FB8C00", "#E53935", "#8E24AA", "#3949AB")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_member_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (member == null) "Add Family Member" else "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_name")
                )

                Text(text = "Relationship:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (rel in relationships) {
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "Blood Group:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (bg in bloodGroups) {
                        FilterChip(
                            selected = bloodGroup == bg,
                            onClick = { bloodGroup = bg },
                            label = { Text(bg) }
                        )
                    }
                }

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies / Conditions") },
                    placeholder = { Text("e.g. Penicillin, Peanuts, Asthma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact Phone") },
                    placeholder = { Text("+91 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Avatar Color
                Text(text = "Profile Avatar Color:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cHex in colorPalettes) {
                        val c = try { Color(android.graphics.Color.parseColor(cHex)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = cHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == cHex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val age = ageStr.toIntOrNull() ?: 30
                        onSave(
                            member?.id ?: 0L,
                            name,
                            relationship,
                            age,
                            bloodGroup,
                            allergies,
                            emergencyContact,
                            selectedColor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_member_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (member == null) "Add Member" else "Save Changes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REFILL MEDICATION DIALOG
// -------------------------------------------------------------

@Composable
fun RefillMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onRefill: (count: Int) -> Unit
) {
    var refillCountStr by remember { mutableStateOf("30") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Refill Stock: ${medication.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Currently remaining: ${medication.pillsRemaining} pills. Add new stock purchased from pharmacy:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = refillCountStr,
                    onValueChange = { refillCountStr = it },
                    label = { Text("Pills / Units to Add") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = refillCountStr.toIntOrNull() ?: 30
                            onRefill(count)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Stock")
                    }
                }
            }
        }
    }
}
