package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Family Member profile for patient-based filtering
 */
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String, // "Self", "Spouse", "Child", "Parent", "Other"
    val age: Int,
    val bloodGroup: String = "O+",
    val allergies: String = "None",
    val emergencyContact: String = "",
    val isPrimary: Boolean = false,
    val avatarColorHex: String = "#00897B"
)

enum class BillCategory(val displayName: String, val iconName: String) {
    CONSULTATION("Consultation", "stethoscope"),
    DIAGNOSTIC_LAB("Diagnostic & Lab", "science"),
    PHARMACY("Pharmacy / Medicines", "medication"),
    SURGERY_PROCEDURE("Surgery & Procedure", "local_hospital"),
    HOSPITAL_STAY("Inpatient / Room Rent", "hotel"),
    EMERGENCY("Emergency Care", "emergency"),
    DENTAL("Dental Care", "dentistry"),
    THERAPY("Therapy / Physio", "accessibility_new"),
    OTHER("Other Medical Expense", "receipt_long")
}

enum class PaymentStatus(val label: String, val isSettled: Boolean) {
    PAID("Paid in Full", true),
    PENDING("Payment Pending", false),
    CLAIM_SUBMITTED("Insurance Claim Filed", false),
    CLAIM_SETTLED("Claim Approved & Settled", true),
    CLAIM_REJECTED("Claim Rejected", false)
}

/**
 * Medical Bill item
 */
@Entity(tableName = "medical_bills")
data class MedicalBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val providerName: String, // Clinic or Hospital
    val doctorName: String = "",
    val billDate: Long, // timestamp in millis
    val category: String, // BillCategory name
    val invoiceNumber: String = "",
    val totalAmount: Double,
    val insuranceCoveredAmount: Double = 0.0,
    val outOfPocketAmount: Double = totalAmount,
    val paymentStatus: String = PaymentStatus.PAID.name,
    val dueDate: Long? = null,
    val notes: String = "",
    val lineItemsRaw: String = "", // e.g. "Consultation: ₹500\nBlood Test: ₹350"
    val receiptTag: String = "Bill Receipt"
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
    PENDING_DOCTOR_REVIEW("Pending Doctor Review")
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
    val documentTag: String = "Report Document"
)

enum class PrescriptionStatus(val label: String) {
    ACTIVE("Active Prescription"),
    COMPLETED("Completed Course"),
    PAUSED("Paused / On-Hold")
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
    val status: String = PrescriptionStatus.ACTIVE.name
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
    val memberId: Long,
    val medicineName: String,
    val dosage: String, // e.g. "500 mg", "10 ml"
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
)

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
    val status: String // DoseStatus name
)
