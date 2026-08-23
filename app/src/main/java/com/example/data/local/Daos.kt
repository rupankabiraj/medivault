package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoseLog
import com.example.data.model.FamilyMember
import com.example.data.model.MedicalBill
import com.example.data.model.MedicalReport
import com.example.data.model.Medication
import com.example.data.model.Prescription
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY isPrimary DESC, id ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("DELETE FROM family_members WHERE isPrimary = 0")
    suspend fun deleteNonPrimaryMembers()

    @Query("DELETE FROM family_members")
    suspend fun deleteAllMembers()
}

@Dao
interface MedicalBillDao {
    @Query("SELECT * FROM medical_bills ORDER BY billDate DESC")
    fun getAllBills(): Flow<List<MedicalBill>>

    @Query("SELECT * FROM medical_bills WHERE memberId = :memberId ORDER BY billDate DESC")
    fun getBillsByMember(memberId: Long): Flow<List<MedicalBill>>

    @Query("SELECT * FROM medical_bills WHERE id = :id")
    suspend fun getBillById(id: Long): MedicalBill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: MedicalBill): Long

    @Update
    suspend fun updateBill(bill: MedicalBill)

    @Delete
    suspend fun deleteBill(bill: MedicalBill)

    @Query("DELETE FROM medical_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM medical_bills")
    suspend fun deleteAllBills()
}

@Dao
interface MedicalReportDao {
    @Query("SELECT * FROM medical_reports ORDER BY reportDate DESC")
    fun getAllReports(): Flow<List<MedicalReport>>

    @Query("SELECT * FROM medical_reports WHERE memberId = :memberId ORDER BY reportDate DESC")
    fun getReportsByMember(memberId: Long): Flow<List<MedicalReport>>

    @Query("SELECT * FROM medical_reports WHERE id = :id")
    suspend fun getReportById(id: Long): MedicalReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MedicalReport): Long

    @Update
    suspend fun updateReport(report: MedicalReport)

    @Delete
    suspend fun deleteReport(report: MedicalReport)

    @Query("DELETE FROM medical_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM medical_reports")
    suspend fun deleteAllReports()
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY datePrescribed DESC")
    fun getAllPrescriptions(): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE memberId = :memberId ORDER BY datePrescribed DESC")
    fun getPrescriptionsByMember(memberId: Long): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Long): Prescription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription): Long

    @Update
    suspend fun updatePrescription(prescription: Prescription)

    @Delete
    suspend fun deletePrescription(prescription: Prescription)

    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: Long)

    @Query("DELETE FROM prescriptions")
    suspend fun deleteAllPrescriptions()
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY isActive DESC, medicineName ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE memberId = :memberId ORDER BY isActive DESC, medicineName ASC")
    fun getMedicationsByMember(memberId: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE prescriptionId = :prescriptionId")
    fun getMedicationsByPrescription(prescriptionId: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE prescriptionId = :prescriptionId")
    suspend fun deleteMedicationsByPrescription(prescriptionId: Long)

    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE dateDayString = :dayString")
    fun getDoseLogsForDay(dayString: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND dateDayString = :dayString AND slot = :slot LIMIT 1")
    suspend fun getDoseLog(medicationId: Long, dayString: String, slot: String): DoseLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(doseLog: DoseLog): Long

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND dateDayString = :dayString AND slot = :slot")
    suspend fun deleteDoseLog(medicationId: Long, dayString: String, slot: String)

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAllDoseLogs()
}
