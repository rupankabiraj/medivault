package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Family Member Profile for Multi-User Management
 */
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String, // e.g. "Self", "Spouse", "Father", "Mother", "Child"
    val age: Int = 30,
    val bloodGroup: String = "O+",
    val allergies: String = "None known",
    val emergencyContact: String = "",
    val avatarColorHex: String = "#00897B", // Hex color for avatar display
    val isPrimary: Boolean = false
)

enum class BillCategory(val displayName: String) {
    CONSULTATION("Doctor Consultation"),
    PHARMACY("Pharmacy / Medicines"),
    DIAGNOSTIC_LAB("Diagnostic & Lab Tests"),
    SURGERY_PROCEDURE("Surgery & Procedures"),
    HOSPITAL_STAY("Hospitalization / IPD"),
    DENTAL("Dental Care"),
    EYE_CARE("Ophthalmology & Optical"),
    THERAPY("Physiotherapy & Rehab"),
    OTHER("Other Healthcare Expense")
}

enum class PaymentStatus(val label: String) {
    PAID("Paid in Full"),
    PENDING("Pending / Unpaid"),
    CLAIM_SUBMITTED("Claim Submitted"),
    CLAIM_SETTLED("Claim Settled"),
    CLAIM_REJECTED("Claim Rejected"),
    INSURANCE_CLAIM_IN_PROGRESS("Claim In Progress"),
    PARTIALLY_REIMBURSED("Partially Reimbursed"),
    DISPUTED("Disputed / Review");

    val displayName: String get() = label
}

/**
 * Medical Expense & Bill
 */
@Entity(tableName = "medical_bills")
data class MedicalBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val providerName: String, // Hospital, Clinic, or Pharmacy
    val doctorName: String = "",
    val billDate: Long,
    val category: String, // BillCategory name
    val invoiceNumber: String = "",
    val totalAmount: Double = 0.0,
    val insuranceCoveredAmount: Double = 0.0,
    val outOfPocketAmount: Double = 0.0,
    val paymentStatus: String = PaymentStatus.PAID.name,
    val dueDate: Long? = null,
    val notes: String = "",
    val lineItemsRaw: String = "", // e.g. "Consultation: ₹500\nBlood Test: ₹350"
    val receiptTag: String = "Bill Receipt",
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val aiExtractedNotes: String? = null
)

enum class ReportCategory(val displayName: String) {
    PATHOLOGY_BLOOD("Blood & Pathology"),
    RADIOLOGY_IMAGING("Radiology (X-Ray / MRI / CT / USG)"),
    CARDIOLOGY("Cardiology (ECG / Echo)"),
    URINE_STOOL("Urine & Stool Analysis"),
    BIOPSY("Biopsy & Histology"),
    ANNUAL_CHECKUP("Annual Health Checkup"),
    SPECIALTY_LAB("Specialty Lab Tests"),
    OTHER("Other Medical Report")
}

enum class ReportStatus(val label: String) {
    NORMAL("Normal / Healthy"),
    ATTENTION_REQUIRED("Attention Required"),
    CRITICAL("Critical / Elevated"),
    PENDING_DOCTOR_REVIEW("Pending Doctor Review");

    val displayName: String get() = label
}

data class Biomarker(
    val name: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    val isAbnormal: Boolean = false,
    val interpretation: String = "Normal" // e.g. "Normal", "High", "Low"
)

/**
 * Medical Diagnostic & Lab Report
 */
@Entity(tableName = "medical_reports")
data class MedicalReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val testName: String,
    val category: String, // ReportCategory name
    val reportDate: Long,
    val labOrFacility: String,
    val orderingDoctor: String = "",
    val summaryFindings: String = "",
    val status: String = ReportStatus.NORMAL.name,
    val biomarkersRaw: String = "", // serialized biomarker list e.g. "Hemoglobin|14.2|g/dL|13.5-17.5|Normal"
    val followUpDate: Long? = null,
    val notes: String = "",
    val documentTag: String = "Report Document",
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val aiExtractedNotes: String? = null
)

enum class PrescriptionStatus(val label: String) {
    ACTIVE("Active Prescription"),
    COMPLETED("Completed Course"),
    PAUSED("Paused / On-Hold");

    val displayName: String get() = label
}

/**
 * Doctor's Prescription
 */
@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val doctorName: String,
    val specialty: String = "General Physician",
    val clinicOrHospital: String,
    val diagnosis: String,
    val datePrescribed: Long,
    val durationDays: Int = 30,
    val isOngoing: Boolean = false,
    val followUpDate: Long? = null,
    val doctorAdvice: String = "",
    val status: String = PrescriptionStatus.ACTIVE.name,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val aiExtractedNotes: String? = null
)

enum class MedicineForm(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    SYRUP("Syrup / Liquid"),
    INJECTION("Injection"),
    INHALER("Inhaler"),
    DROPS("Eye/Ear Drops"),
    CREAM("Cream / Ointment"),
    OTHER("Other")
}

enum class MedicineFrequency(val displayName: String, val timesPerDay: Int) {
    ONCE_DAILY("Once Daily", 1),
    TWICE_DAILY("Twice Daily", 2),
    THRICE_DAILY("3 Times Daily", 3),
    FOUR_TIMES_DAILY("4 Times Daily", 4),
    AS_NEEDED("As Needed (SOS)", 0),
    EVERY_OTHER_DAY("Every Other Day", 1),
    WEEKLY("Once a Week", 1)
}

enum class MedicineTiming(val displayName: String) {
    AFTER_FOOD("After Meals"),
    BEFORE_FOOD("Before Meals (Empty Stomach)"),
    WITH_FOOD("With Meals"),
    AT_BEDTIME("At Bedtime"),
    ANYTIME("Anytime / Flexible")
}

/**
 * Individual Medication item linked to a Prescription
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prescriptionId: Long = 0,
    val memberId: Long = 0,
    val medicineName: String = "",
    val dosage: String = "500 mg", // e.g. "500 mg", "10 ml"
    val form: String = MedicineForm.TABLET.name,
    val frequency: String = MedicineFrequency.TWICE_DAILY.name,
    val timing: String = MedicineTiming.AFTER_FOOD.name,
    val pillsRemaining: Int = 30,
    val totalPrescribedPills: Int = 30,
    val refillThreshold: Int = 5,
    val instructions: String = "",
    val isActive: Boolean = true,
    val slotMorning: Boolean = true,
    val slotAfternoon: Boolean = false,
    val slotEvening: Boolean = true,
    val slotNight: Boolean = false
) {
    val name: String get() = medicineName
}

enum class DoseStatus {
    TAKEN,
    SKIPPED
}

enum class TimeSlot(val title: String, val timeDisplay: String) {
    MORNING("Morning", "8:00 AM"),
    AFTERNOON("Afternoon", "1:00 PM"),
    EVENING("Evening", "7:00 PM"),
    NIGHT("Night", "10:00 PM")
}

/**
 * Daily medication intake logger
 */
@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val memberId: Long,
    val dateDayString: String, // Format: "YYYY-MM-DD"
    val slot: String, // TimeSlot name
    val timestamp: Long,
    val status: String = DoseStatus.TAKEN.name
)

data class ScheduledDoseItem(
    val medication: Medication,
    val member: FamilyMember?,
    val slot: TimeSlot,
    val isTaken: Boolean,
    val prescription: Prescription? = null
)

data class CategorySpending(
    val category: String,
    val totalAmount: Double,
    val percentage: Float,
    val colorHex: String
)

data class DashboardSummary(
    val totalBilledAmount: Double = 0.0,
    val totalInsuranceCovered: Double = 0.0,
    val totalOutOfPocket: Double = 0.0,
    val pendingBillsCount: Int = 0,
    val totalActivePrescriptions: Int = 0,
    val lowStockMedsCount: Int = 0,
    val totalReportsCount: Int = 0,
    val attentionReportsCount: Int = 0,
    val todayDosesCount: Int = 0,
    val todayDosesTakenCount: Int = 0,
    val adherenceRatePercent: Int = 100,
    val categorySpendingList: List<CategorySpending> = emptyList(),
    val categoryExpenses: Map<String, Double> = emptyMap(),
    val monthlyExpenses: Map<String, Double> = emptyMap()
) {
    val totalSpent: Double get() = totalBilledAmount
    val insuranceCovered: Double get() = totalInsuranceCovered
    val outOfPocket: Double get() = totalOutOfPocket
}

// -------------------------------------------------------------
// AI INFERENCE & OCR DATA MODELS
// -------------------------------------------------------------

data class InferredBillData(
    val providerName: String = "",
    val doctorName: String = "",
    val invoiceNumber: String = "",
    val billDate: String = "",
    val category: String = "CONSULTATION",
    val totalAmount: Double = 0.0,
    val insuranceCoveredAmount: Double = 0.0,
    val outOfPocketAmount: Double = 0.0,
    val lineItemsRaw: String = "",
    val notes: String = "",
    val paymentStatus: String = "PAID",
    val aiSummary: String = ""
) {
    val lineItems: String get() = lineItemsRaw
}

data class InferredBiomarker(
    val name: String = "",
    val value: String = "",
    val unit: String = "",
    val referenceRange: String = "",
    val isAbnormal: Boolean = false,
    val flag: String = "Normal"
) {
    val interpretation: String get() = flag
    val range: String get() = referenceRange
}

typealias InferredReportBiomarker = InferredBiomarker

data class InferredReportData(
    val testName: String = "",
    val category: String = "PATHOLOGY_BLOOD",
    val labOrFacility: String = "",
    val orderingDoctor: String = "",
    val reportDate: String = "",
    val clinicalFindings: String = "",
    val summaryFindings: String = "",
    val status: String = "NORMAL",
    val biomarkers: List<InferredBiomarker> = emptyList(),
    val doctorAdvice: String = "",
    val aiSummary: String = ""
)

data class InferredMedication(
    val name: String = "",
    val dosage: String = "500 mg",
    val form: String = "TABLET",
    val frequency: String = "TWICE_DAILY",
    val timing: String = "After Meals",
    val pillsCount: Int = 30,
    val slotMorning: Boolean = true,
    val slotAfternoon: Boolean = false,
    val slotEvening: Boolean = true,
    val slotNight: Boolean = false
)

data class InferredPrescriptionData(
    val doctorName: String = "",
    val specialty: String = "General Physician",
    val clinicOrHospital: String = "",
    val diagnosis: String = "",
    val doctorAdvice: String = "",
    val durationDays: Int = 30,
    val medications: List<InferredMedication> = emptyList(),
    val aiSummary: String = ""
)

enum class MedicalRecordType {
    BILL,
    REPORT,
    PRESCRIPTION
}

/**
 * Unified timeline record for date-wise grouping of all health documents & attachments
 */
data class MedicalTimelineItem(
    val id: String,
    val recordType: MedicalRecordType,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val formattedDate: String,
    val memberId: Long,
    val patientName: String,
    val patientAvatarColor: String,
    val statusText: String,
    val amountOrHighlight: String?,
    val attachmentUri: String?,
    val attachmentName: String?,
    val aiExtractedNotes: String?,
    val billSource: MedicalBill? = null,
    val reportSource: MedicalReport? = null,
    val prescriptionSource: Prescription? = null
) {
    val hasAttachment: Boolean get() = !attachmentUri.isNullOrBlank()
    val dateMillis: Long get() = timestamp
    val amountOrMetric: String get() = amountOrHighlight ?: ""
    val type: MedicalRecordType get() = recordType
    val memberName: String get() = patientName
    val memberAvatarColor: String get() = patientAvatarColor
    val aiInsights: String? get() = aiExtractedNotes
    val billRef: MedicalBill? get() = billSource
    val reportRef: MedicalReport? get() = reportSource
    val rxRef: Prescription? get() = prescriptionSource
}
