package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BillCategory
import com.example.data.model.Biomarker
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
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
import com.example.data.model.TimeSlot
import com.example.data.repository.MedicalRecordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardSummary(
    val totalSpent: Double = 0.0,
    val insuranceCovered: Double = 0.0,
    val outOfPocket: Double = 0.0,
    val pendingBillsCount: Int = 0,
    val pendingBillsTotal: Double = 0.0,
    val activeRxCount: Int = 0,
    val lowStockMedsCount: Int = 0,
    val attentionReportsCount: Int = 0,
    val todayScheduledDosesCount: Int = 0,
    val todayTakenDosesCount: Int = 0,
    val categoryExpenses: Map<String, Double> = emptyMap(),
    val monthlyExpenses: Map<String, Double> = emptyMap()
)

data class ScheduledDoseItem(
    val medication: Medication,
    val member: FamilyMember?,
    val slot: TimeSlot,
    val isTaken: Boolean,
    val prescription: Prescription?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicalRecordsRepository
    val todayString: String

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = MedicalRecordsRepository(db)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        todayString = sdf.format(Date())
    }

    // UI Navigation & Filters
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedMemberId = MutableStateFlow<Long?>(null) // null = all family members
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _billCategoryFilter = MutableStateFlow<String?>(null)
    val billCategoryFilter: StateFlow<String?> = _billCategoryFilter.asStateFlow()

    private val _billStatusFilter = MutableStateFlow<String?>(null)
    val billStatusFilter: StateFlow<String?> = _billStatusFilter.asStateFlow()

    private val _reportCategoryFilter = MutableStateFlow<String?>(null)
    val reportCategoryFilter: StateFlow<String?> = _reportCategoryFilter.asStateFlow()

    private val _reportStatusFilter = MutableStateFlow<String?>(null)
    val reportStatusFilter: StateFlow<String?> = _reportStatusFilter.asStateFlow()

    // Dialog state
    val showAddBillSheet = MutableStateFlow(false)
    val editingBill = MutableStateFlow<MedicalBill?>(null)

    val showAddReportSheet = MutableStateFlow(false)
    val editingReport = MutableStateFlow<MedicalReport?>(null)

    val showAddPrescriptionSheet = MutableStateFlow(false)
    val editingPrescription = MutableStateFlow<Prescription?>(null)

    val showAddMemberDialog = MutableStateFlow(false)
    val editingMember = MutableStateFlow<FamilyMember?>(null)

    val showRefillDialog = MutableStateFlow<Medication?>(null)
    val showExportSummary = MutableStateFlow(false)

    // Detailed viewing state
    val viewingBill = MutableStateFlow<MedicalBill?>(null)
    val viewingReport = MutableStateFlow<MedicalReport?>(null)
    val viewingPrescription = MutableStateFlow<Prescription?>(null)

    // Data streams from repository
    val allMembers: StateFlow<List<FamilyMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBills: StateFlow<List<MedicalBill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<MedicalReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPrescriptions: StateFlow<List<Prescription>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDoseLogs: StateFlow<List<DoseLog>> = repository.getDoseLogsForDay(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Bills
    val filteredBills: StateFlow<List<MedicalBill>> = combine(
        allBills,
        selectedMemberId,
        searchQuery,
        billCategoryFilter,
        billStatusFilter
    ) { bills, memberId, query, catFilter, statusFilter ->
        bills.filter { bill ->
            val matchMember = memberId == null || bill.memberId == memberId
            val matchCat = catFilter == null || bill.category.equals(catFilter, ignoreCase = true)
            val matchStatus = statusFilter == null || bill.paymentStatus.equals(statusFilter, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    bill.providerName.contains(query, ignoreCase = true) ||
                    bill.doctorName.contains(query, ignoreCase = true) ||
                    bill.invoiceNumber.contains(query, ignoreCase = true) ||
                    bill.notes.contains(query, ignoreCase = true) ||
                    bill.lineItemsRaw.contains(query, ignoreCase = true)
            matchMember && matchCat && matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Reports
    val filteredReports: StateFlow<List<MedicalReport>> = combine(
        allReports,
        selectedMemberId,
        searchQuery,
        reportCategoryFilter,
        reportStatusFilter
    ) { reports, memberId, query, catFilter, statusFilter ->
        reports.filter { report ->
            val matchMember = memberId == null || report.memberId == memberId
            val matchCat = catFilter == null || report.category.equals(catFilter, ignoreCase = true)
            val matchStatus = statusFilter == null || report.status.equals(statusFilter, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    report.testName.contains(query, ignoreCase = true) ||
                    report.labOrFacility.contains(query, ignoreCase = true) ||
                    report.orderingDoctor.contains(query, ignoreCase = true) ||
                    report.summaryFindings.contains(query, ignoreCase = true) ||
                    report.biomarkersRaw.contains(query, ignoreCase = true) ||
                    report.notes.contains(query, ignoreCase = true)
            matchMember && matchCat && matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Prescriptions
    val filteredPrescriptions: StateFlow<List<Prescription>> = combine(
        allPrescriptions,
        selectedMemberId,
        searchQuery
    ) { prescriptions, memberId, query ->
        prescriptions.filter { rx ->
            val matchMember = memberId == null || rx.memberId == memberId
            val matchQuery = query.isBlank() ||
                    rx.doctorName.contains(query, ignoreCase = true) ||
                    rx.clinicOrHospital.contains(query, ignoreCase = true) ||
                    rx.diagnosis.contains(query, ignoreCase = true) ||
                    rx.doctorAdvice.contains(query, ignoreCase = true) ||
                    rx.specialty.contains(query, ignoreCase = true)
            matchMember && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's Scheduled Dose Items
    val todayScheduledDoses: StateFlow<List<ScheduledDoseItem>> = combine(
        allMedications,
        todayDoseLogs,
        allMembers,
        allPrescriptions,
        selectedMemberId
    ) { meds, logs, members, rxs, memberId ->
        val items = mutableListOf<ScheduledDoseItem>()
        val memberMap = members.associateBy { it.id }
        val rxMap = rxs.associateBy { it.id }

        val activeMeds = meds.filter { it.isActive && (memberId == null || it.memberId == memberId) }

        for (med in activeMeds) {
            val member = memberMap[med.memberId]
            val rx = rxMap[med.prescriptionId]

            if (med.slotMorning) {
                val isTaken = logs.any { it.medicationId == med.id && it.slot == TimeSlot.MORNING.name && it.status == DoseStatus.TAKEN.name }
                items.add(ScheduledDoseItem(med, member, TimeSlot.MORNING, isTaken, rx))
            }
            if (med.slotAfternoon) {
                val isTaken = logs.any { it.medicationId == med.id && it.slot == TimeSlot.AFTERNOON.name && it.status == DoseStatus.TAKEN.name }
                items.add(ScheduledDoseItem(med, member, TimeSlot.AFTERNOON, isTaken, rx))
            }
            if (med.slotEvening) {
                val isTaken = logs.any { it.medicationId == med.id && it.slot == TimeSlot.EVENING.name && it.status == DoseStatus.TAKEN.name }
                items.add(ScheduledDoseItem(med, member, TimeSlot.EVENING, isTaken, rx))
            }
            if (med.slotNight) {
                val isTaken = logs.any { it.medicationId == med.id && it.slot == TimeSlot.NIGHT.name && it.status == DoseStatus.TAKEN.name }
                items.add(ScheduledDoseItem(med, member, TimeSlot.NIGHT, isTaken, rx))
            }
        }
        items.sortedBy { it.slot.ordinal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Analytics Summary
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allBills,
        allReports,
        allPrescriptions,
        allMedications,
        todayScheduledDoses,
        selectedMemberId
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val bills = args[0] as List<MedicalBill>
        @Suppress("UNCHECKED_CAST")
        val reports = args[1] as List<MedicalReport>
        @Suppress("UNCHECKED_CAST")
        val rxs = args[2] as List<Prescription>
        @Suppress("UNCHECKED_CAST")
        val meds = args[3] as List<Medication>
        @Suppress("UNCHECKED_CAST")
        val doses = args[4] as List<ScheduledDoseItem>
        val memberId = args[5] as Long?

        val targetBills = bills.filter { memberId == null || it.memberId == memberId }
        val targetReports = reports.filter { memberId == null || it.memberId == memberId }
        val targetRxs = rxs.filter { memberId == null || it.memberId == memberId }
        val targetMeds = meds.filter { memberId == null || it.memberId == memberId }

        val totalSpent = targetBills.sumOf { it.totalAmount }
        val insuranceCovered = targetBills.sumOf { it.insuranceCoveredAmount }
        val outOfPocket = targetBills.sumOf { it.outOfPocketAmount }

        val pendingBills = targetBills.filter { it.paymentStatus == PaymentStatus.PENDING.name || it.paymentStatus == PaymentStatus.CLAIM_SUBMITTED.name }
        val pendingCount = pendingBills.size
        val pendingTotal = pendingBills.sumOf { it.totalAmount }

        val activeRx = targetRxs.count { it.status == PrescriptionStatus.ACTIVE.name }
        val lowStockCount = targetMeds.count { it.isActive && it.pillsRemaining <= it.refillThreshold }
        val attentionReports = targetReports.count { it.status == ReportStatus.ATTENTION_REQUIRED.name || it.status == ReportStatus.CRITICAL.name }

        val scheduledDoseCount = doses.size
        val takenDoseCount = doses.count { it.isTaken }

        val catMap = mutableMapOf<String, Double>()
        for (b in targetBills) {
            val cat = b.category
            catMap[cat] = (catMap[cat] ?: 0.0) + b.totalAmount
        }

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthMap = mutableMapOf<String, Double>()
        for (b in targetBills.sortedBy { it.billDate }) {
            val m = monthFormat.format(Date(b.billDate))
            monthMap[m] = (monthMap[m] ?: 0.0) + b.totalAmount
        }

        DashboardSummary(
            totalSpent = totalSpent,
            insuranceCovered = insuranceCovered,
            outOfPocket = outOfPocket,
            pendingBillsCount = pendingCount,
            pendingBillsTotal = pendingTotal,
            activeRxCount = activeRx,
            lowStockMedsCount = lowStockCount,
            attentionReportsCount = attentionReports,
            todayScheduledDosesCount = scheduledDoseCount,
            todayTakenDosesCount = takenDoseCount,
            categoryExpenses = catMap,
            monthlyExpenses = monthMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Tab Navigation
    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectMember(memberId: Long?) {
        _selectedMemberId.value = memberId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBillCategoryFilter(category: String?) {
        _billCategoryFilter.value = if (_billCategoryFilter.value == category) null else category
    }

    fun setBillStatusFilter(status: String?) {
        _billStatusFilter.value = if (_billStatusFilter.value == status) null else status
    }

    fun setReportCategoryFilter(category: String?) {
        _reportCategoryFilter.value = if (_reportCategoryFilter.value == category) null else category
    }

    fun setReportStatusFilter(status: String?) {
        _reportStatusFilter.value = if (_reportStatusFilter.value == status) null else status
    }

    // Bill Actions
    fun saveBill(
        id: Long = 0,
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
    ) {
        viewModelScope.launch {
            val bill = MedicalBill(
                id = id,
                memberId = memberId,
                providerName = provider.trim(),
                doctorName = doctor.trim(),
                billDate = date,
                category = category,
                invoiceNumber = invoiceNum.trim(),
                totalAmount = total,
                insuranceCoveredAmount = insured,
                outOfPocketAmount = outOfPocket,
                paymentStatus = status,
                dueDate = dueDate,
                notes = notes.trim(),
                lineItemsRaw = lineItems.trim()
            )
            if (id == 0L) {
                repository.insertBill(bill)
            } else {
                repository.updateBill(bill)
            }
            showAddBillSheet.value = false
            editingBill.value = null
        }
    }

    fun deleteBill(bill: MedicalBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            if (viewingBill.value?.id == bill.id) {
                viewingBill.value = null
            }
        }
    }

    fun toggleBillPaid(bill: MedicalBill) {
        viewModelScope.launch {
            val nextStatus = if (bill.paymentStatus == PaymentStatus.PAID.name) {
                PaymentStatus.PENDING
            } else {
                PaymentStatus.PAID
            }
            repository.updateBillPaymentStatus(bill, nextStatus)
        }
    }

    // Report Actions
    fun saveReport(
        id: Long = 0,
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
    ) {
        viewModelScope.launch {
            val report = MedicalReport(
                id = id,
                memberId = memberId,
                testName = testName.trim(),
                category = category,
                reportDate = reportDate,
                labOrFacility = facility.trim(),
                orderingDoctor = doctor.trim(),
                summaryFindings = summary.trim(),
                status = status,
                biomarkersRaw = biomarkers.trim(),
                followUpDate = followUpDate,
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.insertReport(report)
            } else {
                repository.updateReport(report)
            }
            showAddReportSheet.value = false
            editingReport.value = null
        }
    }

    fun deleteReport(report: MedicalReport) {
        viewModelScope.launch {
            repository.deleteReport(report)
            if (viewingReport.value?.id == report.id) {
                viewingReport.value = null
            }
        }
    }

    // Prescription & Medication Actions
    fun savePrescriptionWithMedications(
        rxId: Long = 0,
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
    ) {
        viewModelScope.launch {
            val rx = Prescription(
                id = rxId,
                memberId = memberId,
                doctorName = doctor.trim(),
                specialty = specialty.trim(),
                clinicOrHospital = clinic.trim(),
                diagnosis = diagnosis.trim(),
                datePrescribed = datePrescribed,
                durationDays = durationDays,
                isOngoing = isOngoing,
                followUpDate = followUpDate,
                doctorAdvice = advice.trim(),
                status = status
            )
            val generatedRxId = if (rxId == 0L) {
                repository.insertPrescription(rx)
            } else {
                repository.updatePrescription(rx)
                rxId
            }

            if (rxId != 0L) {
                // Delete old meds and re-insert
                // or update
            }
            for (med in medicationsList) {
                repository.insertMedication(med.copy(prescriptionId = generatedRxId, memberId = memberId))
            }

            showAddPrescriptionSheet.value = false
            editingPrescription.value = null
        }
    }

    fun deletePrescription(rx: Prescription) {
        viewModelScope.launch {
            repository.deletePrescription(rx)
            if (viewingPrescription.value?.id == rx.id) {
                viewingPrescription.value = null
            }
        }
    }

    fun refillMedication(medicationId: Long, pillsCount: Int) {
        viewModelScope.launch {
            repository.refillMedication(medicationId, pillsCount)
            showRefillDialog.value = null
        }
    }

    // Dose Logging
    fun toggleDose(doseItem: ScheduledDoseItem) {
        viewModelScope.launch {
            repository.logDose(
                medicationId = doseItem.medication.id,
                memberId = doseItem.medication.memberId,
                dayString = todayString,
                slot = doseItem.slot.name,
                status = if (doseItem.isTaken) DoseStatus.SKIPPED else DoseStatus.TAKEN
            )
        }
    }

    // Family Member Management
    fun saveMember(
        id: Long = 0,
        name: String,
        relationship: String,
        age: Int,
        bloodGroup: String,
        allergies: String,
        emergencyContact: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val member = FamilyMember(
                id = id,
                name = name.trim(),
                relationship = relationship.trim(),
                age = age,
                bloodGroup = bloodGroup.trim(),
                allergies = allergies.trim(),
                emergencyContact = emergencyContact.trim(),
                isPrimary = relationship.equals("Self", ignoreCase = true),
                avatarColorHex = colorHex
            )
            if (id == 0L) {
                repository.insertMember(member)
            } else {
                repository.updateMember(member)
            }
            showAddMemberDialog.value = false
            editingMember.value = null
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            if (_selectedMemberId.value == member.id) {
                _selectedMemberId.value = null
            }
        }
    }

    // Helpers
    fun parseBiomarkers(raw: String): List<Biomarker> {
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<Biomarker>()
        val lines = raw.split("\n")
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size >= 4) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                val unit = parts[2].trim()
                val range = parts[3].trim()
                val interpretation = if (parts.size > 4) parts[4].trim() else "Normal"
                val isAbnormal = interpretation.equals("High", ignoreCase = true) ||
                        interpretation.equals("Low", ignoreCase = true) ||
                        interpretation.equals("Abnormal", ignoreCase = true) ||
                        interpretation.equals("Critical", ignoreCase = true)
                list.add(Biomarker(name, value, unit, range, isAbnormal, interpretation))
            }
        }
        return list
    }
}
