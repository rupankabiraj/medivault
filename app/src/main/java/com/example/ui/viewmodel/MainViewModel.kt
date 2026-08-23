package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.DocumentOcrService
import com.example.data.local.AppDatabase
import com.example.data.model.BillCategory
import com.example.data.model.Biomarker
import com.example.data.model.CategorySpending
import com.example.data.model.DashboardSummary
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.FamilyMember
import com.example.data.model.InferredBillData
import com.example.data.model.InferredPrescriptionData
import com.example.data.model.InferredReportData
import com.example.data.model.MedicalBill
import com.example.data.model.MedicalRecordType
import com.example.data.model.MedicalReport
import com.example.data.model.MedicalTimelineItem
import com.example.data.model.Medication
import com.example.data.model.PaymentStatus
import com.example.data.model.Prescription
import com.example.data.model.PrescriptionStatus
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.ScheduledDoseItem
import com.example.data.model.TimeSlot
import com.example.data.repository.MedicalRecordsRepository
import com.example.data.util.AttachmentUtils
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MedicalRecordsRepository(database)

    private val ocrService = DocumentOcrService(application.applicationContext)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayString: String
        get() = dateFormat.format(Date())

    // -------------------------------------------------------------
    // NAVIGATION & UI CONTROLS
    // -------------------------------------------------------------
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Bill Filters
    private val _billCategoryFilter = MutableStateFlow<String?>(null)
    val billCategoryFilter: StateFlow<String?> = _billCategoryFilter.asStateFlow()

    private val _billStatusFilter = MutableStateFlow<String?>(null)
    val billStatusFilter: StateFlow<String?> = _billStatusFilter.asStateFlow()

    // Report Filters
    private val _reportCategoryFilter = MutableStateFlow<String?>(null)
    val reportCategoryFilter: StateFlow<String?> = _reportCategoryFilter.asStateFlow()

    private val _reportStatusFilter = MutableStateFlow<String?>(null)
    val reportStatusFilter: StateFlow<String?> = _reportStatusFilter.asStateFlow()

    // Timeline Filters
    private val _timelineFilterType = MutableStateFlow<MedicalRecordType?>(null)
    val timelineFilterType: StateFlow<MedicalRecordType?> = _timelineFilterType.asStateFlow()

    private val _timelineOnlyAttachments = MutableStateFlow(false)
    val timelineOnlyAttachments: StateFlow<Boolean> = _timelineOnlyAttachments.asStateFlow()

    // AI OCR Status
    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    private val _ocrStatusMessage = MutableStateFlow<String?>(null)
    val ocrStatusMessage: StateFlow<String?> = _ocrStatusMessage.asStateFlow()

    // Full Screen Attachment Viewer State
    val viewingAttachmentImageUri = MutableStateFlow<String?>(null)
    val viewingAttachmentTitle = MutableStateFlow<String?>(null)

    // Active Dialog States
    val showAddBillSheet = MutableStateFlow(false)
    val editingBill = MutableStateFlow<MedicalBill?>(null)
    val viewingBill = MutableStateFlow<MedicalBill?>(null)

    val showAddReportSheet = MutableStateFlow(false)
    val editingReport = MutableStateFlow<MedicalReport?>(null)
    val viewingReport = MutableStateFlow<MedicalReport?>(null)

    val showAddPrescriptionSheet = MutableStateFlow(false)
    val editingPrescription = MutableStateFlow<Prescription?>(null)
    val viewingPrescription = MutableStateFlow<Prescription?>(null)

    val showAddMemberDialog = MutableStateFlow(false)
    val editingMember = MutableStateFlow<FamilyMember?>(null)

    val showRefillDialog = MutableStateFlow<Medication?>(null)
    val showExportSummary = MutableStateFlow(false)

    // -------------------------------------------------------------
    // DATA STREAMS
    // -------------------------------------------------------------
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

    // Unified Date-Wise Attachments & Timeline Stream
    val filteredTimeline: StateFlow<List<MedicalTimelineItem>> = combine(
        allBills,
        allReports,
        allPrescriptions,
        allMembers,
        selectedMemberId
    ) { bills, reports, rxs, members, memberId ->
        buildTimelineList(bills, reports, rxs, members, memberId, _timelineFilterType.value, _timelineOnlyAttachments.value, _searchQuery.value)
    }.combine(_timelineFilterType) { currentList, typeFilter ->
        if (typeFilter == null) currentList else currentList.filter { it.recordType == typeFilter }
    }.combine(_timelineOnlyAttachments) { currentList, onlyAttachments ->
        if (!onlyAttachments) currentList else currentList.filter { it.hasAttachment }
    }.combine(_searchQuery) { currentList, query ->
        if (query.isBlank()) currentList else currentList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true) ||
                    it.patientName.contains(query, ignoreCase = true) ||
                    (it.aiExtractedNotes?.contains(query, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildTimelineList(
        bills: List<MedicalBill>,
        reports: List<MedicalReport>,
        rxs: List<Prescription>,
        members: List<FamilyMember>,
        memberId: Long?,
        typeFilter: MedicalRecordType?,
        onlyAttachments: Boolean,
        query: String
    ): List<MedicalTimelineItem> {
        val memberMap = members.associateBy { it.id }
        val items = mutableListOf<MedicalTimelineItem>()

        // 1. Map Bills
        if (typeFilter == null || typeFilter == MedicalRecordType.BILL) {
            for (bill in bills) {
                if (memberId != null && bill.memberId != memberId) continue
                if (onlyAttachments && bill.attachmentUri.isNullOrBlank()) continue
                if (query.isNotBlank() &&
                    !bill.providerName.contains(query, ignoreCase = true) &&
                    !bill.doctorName.contains(query, ignoreCase = true) &&
                    !bill.notes.contains(query, ignoreCase = true)
                ) continue

                val member = memberMap[bill.memberId]
                items.add(
                    MedicalTimelineItem(
                        id = "bill_${bill.id}",
                        recordType = MedicalRecordType.BILL,
                        title = bill.providerName,
                        subtitle = if (bill.doctorName.isNotBlank()) "Dr. ${bill.doctorName} • ${bill.category}" else bill.category,
                        timestamp = bill.billDate,
                        formattedDate = AttachmentUtils.formatDate(bill.billDate),
                        memberId = bill.memberId,
                        patientName = member?.name ?: "Self",
                        patientAvatarColor = member?.avatarColorHex ?: "#00897B",
                        statusText = bill.paymentStatus,
                        amountOrHighlight = String.format(Locale.US, "₹%.2f", bill.totalAmount),
                        attachmentUri = bill.attachmentUri,
                        attachmentName = bill.attachmentName,
                        aiExtractedNotes = bill.aiExtractedNotes,
                        billSource = bill
                    )
                )
            }
        }

        // 2. Map Reports
        if (typeFilter == null || typeFilter == MedicalRecordType.REPORT) {
            for (report in reports) {
                if (memberId != null && report.memberId != memberId) continue
                if (onlyAttachments && report.attachmentUri.isNullOrBlank()) continue
                if (query.isNotBlank() &&
                    !report.testName.contains(query, ignoreCase = true) &&
                    !report.labOrFacility.contains(query, ignoreCase = true) &&
                    !report.summaryFindings.contains(query, ignoreCase = true)
                ) continue

                val member = memberMap[report.memberId]
                items.add(
                    MedicalTimelineItem(
                        id = "report_${report.id}",
                        recordType = MedicalRecordType.REPORT,
                        title = report.testName,
                        subtitle = "${report.labOrFacility} • ${report.category}",
                        timestamp = report.reportDate,
                        formattedDate = AttachmentUtils.formatDate(report.reportDate),
                        memberId = report.memberId,
                        patientName = member?.name ?: "Self",
                        patientAvatarColor = member?.avatarColorHex ?: "#00ACC1",
                        statusText = report.status,
                        amountOrHighlight = report.summaryFindings.take(45),
                        attachmentUri = report.attachmentUri,
                        attachmentName = report.attachmentName,
                        aiExtractedNotes = report.aiExtractedNotes,
                        reportSource = report
                    )
                )
            }
        }

        // 3. Map Prescriptions
        if (typeFilter == null || typeFilter == MedicalRecordType.PRESCRIPTION) {
            for (rx in rxs) {
                if (memberId != null && rx.memberId != memberId) continue
                if (onlyAttachments && rx.attachmentUri.isNullOrBlank()) continue
                if (query.isNotBlank() &&
                    !rx.diagnosis.contains(query, ignoreCase = true) &&
                    !rx.doctorName.contains(query, ignoreCase = true) &&
                    !rx.clinicOrHospital.contains(query, ignoreCase = true)
                ) continue

                val member = memberMap[rx.memberId]
                items.add(
                    MedicalTimelineItem(
                        id = "rx_${rx.id}",
                        recordType = MedicalRecordType.PRESCRIPTION,
                        title = rx.diagnosis,
                        subtitle = "Dr. ${rx.doctorName} (${rx.specialty}) • ${rx.clinicOrHospital}",
                        timestamp = rx.datePrescribed,
                        formattedDate = AttachmentUtils.formatDate(rx.datePrescribed),
                        memberId = rx.memberId,
                        patientName = member?.name ?: "Self",
                        patientAvatarColor = member?.avatarColorHex ?: "#FB8C00",
                        statusText = rx.status,
                        amountOrHighlight = "${rx.durationDays} Days Course",
                        attachmentUri = rx.attachmentUri,
                        attachmentName = rx.attachmentName,
                        aiExtractedNotes = rx.aiExtractedNotes,
                        prescriptionSource = rx
                    )
                )
            }
        }

        return items.sortedByDescending { it.timestamp }
    }

    // Dashboard Summary Statistics
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        combine(allBills, allReports, allPrescriptions) { bills, reports, rxs ->
            Triple(bills, reports, rxs)
        },
        combine(allMedications, todayDoseLogs, selectedMemberId) { meds, logs, memberId ->
            Triple(meds, logs, memberId)
        }
    ) { (bills, reports, rxs), (meds, logs, memberId) ->
        val mBills = bills.filter { memberId == null || it.memberId == memberId }
        val mReports = reports.filter { memberId == null || it.memberId == memberId }
        val mRxs = rxs.filter { memberId == null || it.memberId == memberId }
        val mMeds = meds.filter { it.isActive && (memberId == null || it.memberId == memberId) }

        val totalBilled = mBills.sumOf { it.totalAmount }
        val totalInsurance = mBills.sumOf { it.insuranceCoveredAmount }
        val totalOutOfPocket = mBills.sumOf { it.outOfPocketAmount }
        val pendingBills = mBills.count { it.paymentStatus == PaymentStatus.PENDING.name }

        val totalActivePrescriptions = mRxs.count { it.status == PrescriptionStatus.ACTIVE.name }
        val lowStockMeds = mMeds.count { it.pillsRemaining <= it.refillThreshold }
        val attentionReports = mReports.count { it.status == ReportStatus.ATTENTION_REQUIRED.name || it.status == ReportStatus.CRITICAL.name }

        // Category spendings
        val spendMap = mBills.groupBy { it.category }
        val catSpendings = spendMap.map { (cat, bList) ->
            val sum = bList.sumOf { it.totalAmount }
            val pct = if (totalBilled > 0) ((sum / totalBilled) * 100).toFloat() else 0f
            CategorySpending(
                category = cat,
                totalAmount = sum,
                percentage = pct,
                colorHex = when (cat) {
                    BillCategory.CONSULTATION.name -> "#00897B"
                    BillCategory.PHARMACY.name -> "#00ACC1"
                    BillCategory.DIAGNOSTIC_LAB.name -> "#43A047"
                    BillCategory.SURGERY_PROCEDURE.name -> "#E53935"
                    BillCategory.HOSPITAL_STAY.name -> "#FB8C00"
                    else -> "#8E24AA"
                }
            )
        }.sortedByDescending { it.totalAmount }

        val catExpensesMap = mutableMapOf<String, Double>()
        catSpendings.forEach { catExpensesMap[it.category] = it.totalAmount }

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthlyExpensesMap = mutableMapOf<String, Double>()
        mBills.sortedBy { it.billDate }.forEach { b ->
            val monthKey = monthFormat.format(Date(b.billDate))
            monthlyExpensesMap[monthKey] = (monthlyExpensesMap[monthKey] ?: 0.0) + b.totalAmount
        }

        val totalDosesToday = mMeds.sumOf {
            (if (it.slotMorning) 1 else 0) +
                    (if (it.slotAfternoon) 1 else 0) +
                    (if (it.slotEvening) 1 else 0) +
                    (if (it.slotNight) 1 else 0)
        }
        val takenCount = logs.count { it.status == DoseStatus.TAKEN.name }
        val adherenceRate = if (totalDosesToday > 0) ((takenCount.toFloat() / totalDosesToday.toFloat()) * 100).toInt().coerceAtMost(100) else 100

        DashboardSummary(
            totalBilledAmount = totalBilled,
            totalInsuranceCovered = totalInsurance,
            totalOutOfPocket = totalOutOfPocket,
            pendingBillsCount = pendingBills,
            totalActivePrescriptions = totalActivePrescriptions,
            lowStockMedsCount = lowStockMeds,
            totalReportsCount = mReports.size,
            attentionReportsCount = attentionReports,
            todayDosesCount = totalDosesToday,
            todayDosesTakenCount = takenCount,
            adherenceRatePercent = adherenceRate,
            categorySpendingList = catSpendings,
            categoryExpenses = catExpensesMap,
            monthlyExpenses = monthlyExpensesMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // -------------------------------------------------------------
    // CONTROLLER & EVENT METHODS
    // -------------------------------------------------------------
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
        _billCategoryFilter.value = category
    }

    fun setBillStatusFilter(status: String?) {
        _billStatusFilter.value = status
    }

    fun setReportCategoryFilter(category: String?) {
        _reportCategoryFilter.value = category
    }

    fun setReportStatusFilter(status: String?) {
        _reportStatusFilter.value = status
    }

    fun setTimelineFilterType(type: MedicalRecordType?) {
        _timelineFilterType.value = type
    }

    fun setTimelineOnlyAttachments(onlyAttachments: Boolean) {
        _timelineOnlyAttachments.value = onlyAttachments
    }

    // Attachment Viewer Actions
    fun openAttachmentViewer(uri: String?, title: String?) {
        if (!uri.isNullOrBlank()) {
            viewingAttachmentImageUri.value = uri
            viewingAttachmentTitle.value = title ?: "Attached Document"
        }
    }

    fun closeAttachmentViewer() {
        viewingAttachmentImageUri.value = null
        viewingAttachmentTitle.value = null
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
        lineItems: String,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        aiExtractedNotes: String? = null
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
                lineItemsRaw = lineItems.trim(),
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes
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
        notes: String,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        aiExtractedNotes: String? = null
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
                notes = notes.trim(),
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes
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
        medicationsList: List<Medication>,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        aiExtractedNotes: String? = null
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
                status = status,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                aiExtractedNotes = aiExtractedNotes
            )
            val updatedMeds = medicationsList.map { it.copy(memberId = memberId) }
            repository.savePrescriptionWithMedications(rx, updatedMeds)
            showAddPrescriptionSheet.value = false
            editingPrescription.value = null
        }
    }

    fun deletePrescription(prescription: Prescription) {
        viewModelScope.launch {
            repository.deletePrescription(prescription)
            if (viewingPrescription.value?.id == prescription.id) {
                viewingPrescription.value = null
            }
        }
    }

    fun refillMedication(medicationId: Long, addedCount: Int) {
        viewModelScope.launch {
            repository.refillMedication(medicationId, addedCount)
            showRefillDialog.value = null
        }
    }

    fun toggleDose(doseItem: ScheduledDoseItem) {
        viewModelScope.launch {
            repository.logDose(
                medicationId = doseItem.medication.id,
                memberId = doseItem.medication.memberId,
                dateDayString = todayString,
                slot = doseItem.slot.name,
                isTaken = !doseItem.isTaken
            )
        }
    }

    // Family Member Actions
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
                relationship = relationship,
                age = age,
                bloodGroup = bloodGroup,
                allergies = allergies.trim(),
                emergencyContact = emergencyContact.trim(),
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

    fun parseBiomarkers(raw: String): List<Biomarker> {
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<Biomarker>()
        for (line in raw.lines()) {
            val parts = line.split("|")
            if (parts.size >= 2) {
                val name = parts[0].trim()
                val value = parts.getOrNull(1)?.trim() ?: ""
                val unit = parts.getOrNull(2)?.trim() ?: ""
                val range = parts.getOrNull(3)?.trim() ?: ""
                val flag = parts.getOrNull(4)?.trim() ?: "Normal"
                val isAbnormal = !flag.equals("Normal", ignoreCase = true)
                list.add(
                    Biomarker(
                        name = name,
                        value = value,
                        unit = unit,
                        referenceRange = range,
                        isAbnormal = isAbnormal,
                        interpretation = flag
                    )
                )
            }
        }
        return list
    }
}
