package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.FamilyMember
import com.example.data.model.MedicalBill
import com.example.data.model.MedicalReport
import com.example.data.model.Medication
import com.example.data.model.PaymentStatus
import com.example.data.model.Prescription
import kotlinx.coroutines.flow.Flow

class MedicalRecordsRepository(private val db: AppDatabase) {

    // Family Members
    val allMembers: Flow<List<FamilyMember>> = db.familyMemberDao().getAllMembers()

    suspend fun insertMember(member: FamilyMember): Long =
        db.familyMemberDao().insertMember(member)

    suspend fun updateMember(member: FamilyMember) =
        db.familyMemberDao().updateMember(member)

    suspend fun deleteMember(member: FamilyMember) =
        db.familyMemberDao().deleteMember(member)

    // Medical Bills
    val allBills: Flow<List<MedicalBill>> = db.medicalBillDao().getAllBills()

    fun getBillsByMember(memberId: Long): Flow<List<MedicalBill>> =
        db.medicalBillDao().getBillsByMember(memberId)

    suspend fun insertBill(bill: MedicalBill): Long =
        db.medicalBillDao().insertBill(bill)

    suspend fun updateBill(bill: MedicalBill) =
        db.medicalBillDao().updateBill(bill)

    suspend fun deleteBill(bill: MedicalBill) =
        db.medicalBillDao().deleteBill(bill)

    suspend fun deleteBillById(id: Long) =
        db.medicalBillDao().deleteBillById(id)

    suspend fun updateBillPaymentStatus(bill: MedicalBill, newStatus: PaymentStatus) {
        val updated = bill.copy(
            paymentStatus = newStatus.name,
            insuranceCoveredAmount = if (newStatus == PaymentStatus.CLAIM_SETTLED && bill.insuranceCoveredAmount == 0.0) {
                bill.totalAmount * 0.8
            } else {
                bill.insuranceCoveredAmount
            },
            outOfPocketAmount = if (newStatus == PaymentStatus.CLAIM_SETTLED && bill.insuranceCoveredAmount == 0.0) {
                bill.totalAmount * 0.2
            } else {
                bill.outOfPocketAmount
            }
        )
        db.medicalBillDao().updateBill(updated)
    }

    // Medical Reports
    val allReports: Flow<List<MedicalReport>> = db.medicalReportDao().getAllReports()

    fun getReportsByMember(memberId: Long): Flow<List<MedicalReport>> =
        db.medicalReportDao().getReportsByMember(memberId)

    suspend fun insertReport(report: MedicalReport): Long =
        db.medicalReportDao().insertReport(report)

    suspend fun updateReport(report: MedicalReport) =
        db.medicalReportDao().updateReport(report)

    suspend fun deleteReport(report: MedicalReport) =
        db.medicalReportDao().deleteReport(report)

    suspend fun deleteReportById(id: Long) =
        db.medicalReportDao().deleteReportById(id)

    // Prescriptions & Medications
    val allPrescriptions: Flow<List<Prescription>> = db.prescriptionDao().getAllPrescriptions()
    val allMedications: Flow<List<Medication>> = db.medicationDao().getAllMedications()

    fun getPrescriptionsByMember(memberId: Long): Flow<List<Prescription>> =
        db.prescriptionDao().getPrescriptionsByMember(memberId)

    fun getMedicationsByMember(memberId: Long): Flow<List<Medication>> =
        db.medicationDao().getMedicationsByMember(memberId)

    fun getMedicationsByPrescription(prescriptionId: Long): Flow<List<Medication>> =
        db.medicationDao().getMedicationsByPrescription(prescriptionId)

    suspend fun insertPrescription(prescription: Prescription): Long =
        db.prescriptionDao().insertPrescription(prescription)

    suspend fun updatePrescription(prescription: Prescription) =
        db.prescriptionDao().updatePrescription(prescription)

    suspend fun deletePrescription(prescription: Prescription) {
        db.medicationDao().deleteMedicationsByPrescription(prescription.id)
        db.prescriptionDao().deletePrescription(prescription)
    }

    suspend fun deletePrescriptionById(id: Long) {
        db.medicationDao().deleteMedicationsByPrescription(id)
        db.prescriptionDao().deletePrescriptionById(id)
    }

    suspend fun insertMedication(medication: Medication): Long =
        db.medicationDao().insertMedication(medication)

    suspend fun insertMedications(medications: List<Medication>) =
        db.medicationDao().insertMedications(medications)

    suspend fun updateMedication(medication: Medication) =
        db.medicationDao().updateMedication(medication)

    suspend fun deleteMedication(medication: Medication) =
        db.medicationDao().deleteMedication(medication)

    suspend fun refillMedication(medicationId: Long, addPillCount: Int) {
        val med = db.medicationDao().getMedicationById(medicationId) ?: return
        val updated = med.copy(
            pillsRemaining = med.pillsRemaining + addPillCount,
            totalPrescribedPills = maxOf(med.totalPrescribedPills, med.pillsRemaining + addPillCount)
        )
        db.medicationDao().updateMedication(updated)
    }

    // Dose Logs
    fun getDoseLogsForDay(dayString: String): Flow<List<DoseLog>> =
        db.doseLogDao().getDoseLogsForDay(dayString)

    suspend fun logDose(
        medicationId: Long,
        memberId: Long,
        dayString: String,
        slot: String,
        status: DoseStatus
    ) {
        val existing = db.doseLogDao().getDoseLog(medicationId, dayString, slot)
        if (existing == null) {
            db.doseLogDao().insertDoseLog(
                DoseLog(
                    medicationId = medicationId,
                    memberId = memberId,
                    dateDayString = dayString,
                    slot = slot,
                    timestamp = System.currentTimeMillis(),
                    status = status.name
                )
            )
            // If taken, reduce pill count
            if (status == DoseStatus.TAKEN) {
                val med = db.medicationDao().getMedicationById(medicationId)
                if (med != null && med.pillsRemaining > 0) {
                    db.medicationDao().updateMedication(med.copy(pillsRemaining = med.pillsRemaining - 1))
                }
            }
        } else {
            // Toggle off
            db.doseLogDao().deleteDoseLog(medicationId, dayString, slot)
            // If it was taken, restore pill count
            if (existing.status == DoseStatus.TAKEN.name) {
                val med = db.medicationDao().getMedicationById(medicationId)
                if (med != null) {
                    db.medicationDao().updateMedication(med.copy(pillsRemaining = med.pillsRemaining + 1))
                }
            }
        }
    }
}
