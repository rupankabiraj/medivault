package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BillCategory
import com.example.data.model.FamilyMember
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
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.util.Locale

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
        lineItems: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                placeholder = { Text("e.g. City General Hospital, Quest Diagnostics") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bill_provider")
            )

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Doctor / Specialist Name") },
                placeholder = { Text("e.g. Dr. Emily Chen") },
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
                label = { Text("Bill Itemization / Breakdown") },
                placeholder = { Text("Doctor Consultation: ₹1200\nLab Tests: ₹800\nPrescription Medicine: ₹450") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Insurance Claim Details") },
                placeholder = { Text("e.g. Follow-up reimbursement submitted via Aetna Portal.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val total = totalAmountStr.toDoubleOrNull() ?: 0.0
                    val insured = insuredAmountStr.toDoubleOrNull() ?: 0.0
                    val outOfPocket = (total - insured).coerceAtLeast(0.0)

                    onSave(
                        bill?.id ?: 0L,
                        memberId,
                        if (provider.isBlank()) "Medical Provider" else provider,
                        doctor,
                        bill?.billDate ?: System.currentTimeMillis(),
                        category,
                        invoiceNum,
                        total,
                        insured,
                        outOfPocket,
                        paymentStatus,
                        null,
                        notes,
                        lineItems
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bill_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Medical Bill", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

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
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var testName by remember { mutableStateOf(report?.testName ?: "") }
    var category by remember { mutableStateOf(report?.category ?: ReportCategory.PATHOLOGY_BLOOD.name) }
    var facility by remember { mutableStateOf(report?.labOrFacility ?: "") }
    var doctor by remember { mutableStateOf(report?.orderingDoctor ?: "") }
    var summary by remember { mutableStateOf(report?.summaryFindings ?: "") }
    var status by remember { mutableStateOf(report?.status ?: ReportStatus.NORMAL.name) }
    var notes by remember { mutableStateOf(report?.notes ?: "") }
    var memberId by remember { mutableLongStateOf(report?.memberId ?: selectedMemberId ?: members.firstOrNull()?.id ?: 1L) }

    // Dynamic Biomarkers list builder
    val biomarkerList = remember {
        mutableStateListOf<BiomarkerBuilderItem>().apply {
            if (report != null && report.biomarkersRaw.isNotBlank()) {
                val lines = report.biomarkersRaw.split("\n")
                for (l in lines) {
                    val p = l.split("|")
                    if (p.size >= 4) {
                        add(BiomarkerBuilderItem(p[0], p[1], p[2], p[3], if (p.size > 4) p[4] else "Normal"))
                    }
                }
            } else {
                add(BiomarkerBuilderItem("Hemoglobin", "14.5", "g/dL", "13.5-17.5", "Normal"))
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
                    text = if (report == null) "Add Diagnostic Lab Report" else "Edit Lab Report",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Patient selector
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

            OutlinedTextField(
                value = testName,
                onValueChange = { testName = it },
                label = { Text("Diagnostic Test / Report Title *") },
                placeholder = { Text("e.g. Complete Blood Count (CBC), Lipid Panel, MRI Spine") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_report_title")
            )

            OutlinedTextField(
                value = facility,
                onValueChange = { facility = it },
                label = { Text("Laboratory / Diagnostic Center *") },
                placeholder = { Text("e.g. Quest Diagnostics, LabCorp, City Radiology") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Ordering Physician / Doctor") },
                placeholder = { Text("e.g. Dr. Emily Chen, MD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Test Category Selection
            Text(text = "Department / Report Category:", style = MaterialTheme.typography.labelMedium)
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

            // Overall Status
            Text(text = "Overall Health Status / Result:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (st in ReportStatus.values()) {
                    FilterChip(
                        selected = status == st.name,
                        onClick = { status = st.name },
                        label = { Text(st.label) }
                    )
                }
            }

            // Clinical Summary Findings
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Key Clinical Findings & Interpretation") },
                placeholder = { Text("e.g. Normal white cell count, slight elevation in total cholesterol.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Biomarker Test Values builder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Biomarkers & Test Values (${biomarkerList.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        biomarkerList.add(BiomarkerBuilderItem("Glucose Fasting", "95", "mg/dL", "70-99", "Normal"))
                    }
                ) {
                    Text("+ Add Marker", fontWeight = FontWeight.Bold)
                }
            }

            for (i in biomarkerList.indices) {
                val item = biomarkerList[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Marker #${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = { biomarkerList.removeAt(i) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Marker",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { item.name = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = item.value,
                                onValueChange = { item.value = it },
                                label = { Text("Value") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.unit,
                                onValueChange = { item.unit = it },
                                label = { Text("Unit") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = item.range,
                                onValueChange = { item.range = it },
                                label = { Text("Ref Range") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = item.flag,
                                onValueChange = { item.flag = it },
                                label = { Text("Flag (Normal/High/Low)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Doctor's Follow-up Instructions") },
                placeholder = { Text("e.g. Repeat test in 3 months. Continue current dosage.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val rawBiomarkers = biomarkerList
                        .filter { it.name.isNotBlank() }
                        .joinToString("\n") { "${it.name}|${it.value}|${it.unit}|${it.range}|${it.flag}" }

                    onSave(
                        report?.id ?: 0L,
                        memberId,
                        if (testName.isBlank()) "Diagnostic Lab Report" else testName,
                        category,
                        report?.reportDate ?: System.currentTimeMillis(),
                        if (facility.isBlank()) "Lab Facility" else facility,
                        doctor,
                        summary,
                        status,
                        rawBiomarkers,
                        null,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_report_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Diagnostic Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

class BiomarkerBuilderItem(
    name: String,
    value: String,
    unit: String,
    range: String,
    flag: String
) {
    var name by mutableStateOf(name)
    var value by mutableStateOf(value)
    var unit by mutableStateOf(unit)
    var range by mutableStateOf(range)
    var flag by mutableStateOf(flag)
}

class MedicationBuilderItem(
    name: String,
    dosage: String,
    form: String,
    frequency: String,
    timing: String,
    pills: Int,
    morning: Boolean,
    afternoon: Boolean,
    evening: Boolean,
    night: Boolean
) {
    var name by mutableStateOf(name)
    var dosage by mutableStateOf(dosage)
    var form by mutableStateOf(form)
    var frequency by mutableStateOf(frequency)
    var timing by mutableStateOf(timing)
    var pills by mutableIntStateOf(pills)
    var morning by mutableStateOf(morning)
    var afternoon by mutableStateOf(afternoon)
    var evening by mutableStateOf(evening)
    var night by mutableStateOf(night)
}

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
        medicationsList: List<Medication>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var doctor by remember { mutableStateOf(prescription?.doctorName ?: "") }
    var specialty by remember { mutableStateOf(prescription?.specialty ?: "General Physician") }
    var clinic by remember { mutableStateOf(prescription?.clinicOrHospital ?: "") }
    var diagnosis by remember { mutableStateOf(prescription?.diagnosis ?: "") }
    var durationDaysStr by remember { mutableStateOf(prescription?.durationDays?.toString() ?: "30") }
    var isOngoing by remember { mutableStateOf(prescription?.isOngoing ?: true) }
    var advice by remember { mutableStateOf(prescription?.doctorAdvice ?: "") }
    var status by remember { mutableStateOf(prescription?.status ?: PrescriptionStatus.ACTIVE.name) }
    var memberId by remember { mutableLongStateOf(prescription?.memberId ?: selectedMemberId ?: members.firstOrNull()?.id ?: 1L) }

    val medsList = remember {
        mutableStateListOf<MedicationBuilderItem>().apply {
            if (medications.isNotEmpty()) {
                for (m in medications) {
                    add(
                        MedicationBuilderItem(
                            m.medicineName,
                            m.dosage,
                            m.form,
                            m.frequency,
                            m.timing,
                            m.pillsRemaining,
                            m.slotMorning,
                            m.slotAfternoon,
                            m.slotEvening,
                            m.slotNight
                        )
                    )
                }
            } else {
                add(
                    MedicationBuilderItem(
                        "Amoxicillin",
                        "500 mg",
                        MedicineForm.TABLET.name,
                        MedicineFrequency.TWICE_DAILY.name,
                        MedicineTiming.AFTER_FOOD.name,
                        30,
                        morning = true,
                        afternoon = false,
                        evening = true,
                        night = false
                    )
                )
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
                    text = if (prescription == null) "Add Prescription & Meds" else "Edit Prescription",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Patient
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

            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Diagnosis / Health Condition *") },
                placeholder = { Text("e.g. Hypertension, Diabetes Type 2, Bronchitis") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rx_diagnosis")
            )

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Doctor's Name *") },
                placeholder = { Text("e.g. Dr. Emily Chen, MD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Doctor Specialty") },
                    placeholder = { Text("e.g. Cardiology") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = clinic,
                    onValueChange = { clinic = it },
                    label = { Text("Hospital / Clinic") },
                    placeholder = { Text("e.g. Metro Health") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Prescribed Medicines Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prescribed Medicines (${medsList.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        medsList.add(
                            MedicationBuilderItem(
                                "New Medicine",
                                "10 mg",
                                MedicineForm.TABLET.name,
                                MedicineFrequency.ONCE_DAILY.name,
                                MedicineTiming.AFTER_FOOD.name,
                                30,
                                morning = true,
                                afternoon = false,
                                evening = false,
                                night = false
                            )
                        )
                    }
                ) {
                    Text("+ Add Medicine", fontWeight = FontWeight.Bold)
                }
            }

            for (i in medsList.indices) {
                val med = medsList[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Medicine #${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (medsList.size > 1) {
                                IconButton(
                                    onClick = { medsList.removeAt(i) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Medicine",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = med.name,
                                onValueChange = { med.name = it },
                                label = { Text("Medicine Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = med.dosage,
                                onValueChange = { med.dosage = it },
                                label = { Text("Dosage") },
                                placeholder = { Text("e.g. 500mg") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = med.timing,
                                onValueChange = { med.timing = it },
                                label = { Text("Timing") },
                                placeholder = { Text("After food") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = med.pills.toString(),
                                onValueChange = { med.pills = it.toIntOrNull() ?: 0 },
                                label = { Text("Pill Count") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Daily Dose Slots:", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = med.morning, onCheckedChange = { med.morning = it })
                                Text("Morn", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = med.afternoon, onCheckedChange = { med.afternoon = it })
                                Text("Aft", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = med.evening, onCheckedChange = { med.evening = it })
                                Text("Eve", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = med.night, onCheckedChange = { med.night = it })
                                Text("Night", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = advice,
                onValueChange = { advice = it },
                label = { Text("Doctor's Advice & Lifestyle Instructions") },
                placeholder = { Text("e.g. Low sodium diet, 30 min daily walking, monitor blood pressure weekly.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val finalMeds = medsList.map {
                        Medication(
                            prescriptionId = prescription?.id ?: 0L,
                            memberId = memberId,
                            medicineName = it.name.trim(),
                            dosage = it.dosage.trim(),
                            form = it.form,
                            frequency = it.frequency,
                            timing = it.timing.trim(),
                            pillsRemaining = it.pills,
                            totalPrescribedPills = it.pills,
                            refillThreshold = 5,
                            instructions = "",
                            isActive = true,
                            slotMorning = it.morning,
                            slotAfternoon = it.afternoon,
                            slotEvening = it.evening,
                            slotNight = it.night
                        )
                    }

                    onSave(
                        prescription?.id ?: 0L,
                        memberId,
                        if (doctor.isBlank()) "Doctor" else doctor,
                        specialty,
                        if (clinic.isBlank()) "Clinic" else clinic,
                        if (diagnosis.isBlank()) "General Treatment" else diagnosis,
                        prescription?.datePrescribed ?: System.currentTimeMillis(),
                        durationDaysStr.toIntOrNull() ?: 30,
                        isOngoing,
                        null,
                        advice,
                        status,
                        finalMeds
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_prescription_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Prescription", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

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
    var name by remember { mutableStateOf(member?.name ?: "") }
    var relationship by remember { mutableStateOf(member?.relationship ?: "Family Member") }
    var ageStr by remember { mutableStateOf(member?.age?.toString() ?: "30") }
    var bloodGroup by remember { mutableStateOf(member?.bloodGroup ?: "O+") }
    var allergies by remember { mutableStateOf(member?.allergies ?: "None") }
    var emergencyContact by remember { mutableStateOf(member?.emergencyContact ?: "") }

    val colorOptions = listOf("#00897B", "#0288D1", "#7C3AED", "#EA580C", "#DB2777", "#059669")
    var selectedColor by remember { mutableStateOf(member?.avatarColorHex ?: colorOptions[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (member == null) "Add Family Member" else "Edit Member Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Relationship") },
                        placeholder = { Text("e.g. Spouse, Child, Parent") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    placeholder = { Text("e.g. O+, A+, B-, AB+") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Known Allergies") },
                    placeholder = { Text("e.g. Penicillin, Peanuts, None") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact Number") },
                    placeholder = { Text("e.g. +1 (555) 019-2831") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Avatar Color
                Text(text = "Profile Theme Color:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (cHex in colorOptions) {
                        val c = Color(android.graphics.Color.parseColor(cHex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = cHex }
                                .padding(if (selectedColor == cHex) 3.dp else 0.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                member?.id ?: 0L,
                                if (name.isBlank()) "Family Member" else name,
                                relationship,
                                ageStr.toIntOrNull() ?: 30,
                                bloodGroup,
                                allergies,
                                emergencyContact,
                                selectedColor
                            )
                        }
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RefillMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onRefill: (count: Int) -> Unit
) {
    var countStr by remember { mutableStateOf("30") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Refill ${medication.medicineName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Current stock: ${medication.pillsRemaining} pills remaining. Add newly purchased pill count to update inventory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = countStr,
                    onValueChange = { countStr = it },
                    label = { Text("Pills / Doses to Add") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 30, 60, 90).forEach { num ->
                        OutlinedButton(
                            onClick = { countStr = num.toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$num", fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = countStr.toIntOrNull() ?: 30
                            onRefill(count)
                        }
                    ) {
                        Text("Add to Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
