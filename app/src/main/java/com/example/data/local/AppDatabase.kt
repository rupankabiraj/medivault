package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BillCategory
import com.example.data.model.DoseLog
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FamilyMember::class,
        MedicalBill::class,
        MedicalReport::class,
        Prescription::class,
        Medication::class,
        DoseLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun medicalBillDao(): MedicalBillDao
    abstract fun medicalReportDao(): MedicalReportDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medivault_health.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateSampleData(database)
                    }
                }
            }
        }

        private suspend fun populateSampleData(db: AppDatabase) {
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000

            // 1. Family Members
            val memberDao = db.familyMemberDao()
            val alexId = memberDao.insertMember(
                FamilyMember(
                    id = 1,
                    name = "Alex Johnson",
                    relationship = "Self",
                    age = 34,
                    bloodGroup = "O+",
                    allergies = "Penicillin, Shellfish",
                    emergencyContact = "+1 (555) 234-5678",
                    isPrimary = true,
                    avatarColorHex = "#00897B"
                )
            )
            val sarahId = memberDao.insertMember(
                FamilyMember(
                    id = 2,
                    name = "Sarah Johnson",
                    relationship = "Spouse",
                    age = 32,
                    bloodGroup = "A+",
                    allergies = "None",
                    emergencyContact = "+1 (555) 345-6789",
                    isPrimary = false,
                    avatarColorHex = "#0288D1"
                )
            )
            val leoId = memberDao.insertMember(
                FamilyMember(
                    id = 3,
                    name = "Leo Johnson",
                    relationship = "Child",
                    age = 6,
                    bloodGroup = "O+",
                    allergies = "Peanuts",
                    emergencyContact = "+1 (555) 234-5678",
                    isPrimary = false,
                    avatarColorHex = "#7C3AED"
                )
            )

            // 2. Prescriptions & Medications
            val rxDao = db.prescriptionDao()
            val medDao = db.medicationDao()

            val rx1 = rxDao.insertPrescription(
                Prescription(
                    id = 1,
                    memberId = alexId,
                    doctorName = "Dr. Emily Chen, MD",
                    specialty = "Cardiologist & Internal Med",
                    clinicOrHospital = "Metro Health Heart Center",
                    diagnosis = "Stage 1 Hypertension & Lipid Management",
                    datePrescribed = now - (14 * dayMillis),
                    durationDays = 90,
                    isOngoing = true,
                    followUpDate = now + (76 * dayMillis),
                    doctorAdvice = "Maintain low sodium diet, 30 min daily brisk walking, monitor BP weekly.",
                    status = PrescriptionStatus.ACTIVE.name
                )
            )

            medDao.insertMedications(
                listOf(
                    Medication(
                        prescriptionId = rx1,
                        memberId = alexId,
                        medicineName = "Amlodipine Besylate",
                        dosage = "5 mg",
                        form = MedicineForm.TABLET.name,
                        frequency = MedicineFrequency.ONCE_DAILY.name,
                        timing = MedicineTiming.AFTER_FOOD.name,
                        pillsRemaining = 16,
                        totalPrescribedPills = 30,
                        refillThreshold = 7,
                        instructions = "Take 1 tablet every morning with water after breakfast.",
                        isActive = true,
                        slotMorning = true,
                        slotAfternoon = false,
                        slotEvening = false,
                        slotNight = false
                    ),
                    Medication(
                        prescriptionId = rx1,
                        memberId = alexId,
                        medicineName = "Atorvastatin Calcium",
                        dosage = "20 mg",
                        form = MedicineForm.TABLET.name,
                        frequency = MedicineFrequency.ONCE_DAILY.name,
                        timing = MedicineTiming.AT_BEDTIME.name,
                        pillsRemaining = 4, // low stock alert!
                        totalPrescribedPills = 30,
                        refillThreshold = 6,
                        instructions = "Take 1 tablet at night before sleep. Avoid grapefruit juice.",
                        isActive = true,
                        slotMorning = false,
                        slotAfternoon = false,
                        slotEvening = false,
                        slotNight = true
                    )
                )
            )

            val rx2 = rxDao.insertPrescription(
                Prescription(
                    id = 2,
                    memberId = sarahId,
                    doctorName = "Dr. Robert Miller, DO",
                    specialty = "Endocrinologist",
                    clinicOrHospital = "St. Jude Wellness Clinic",
                    diagnosis = "Mild Hypothyroidism & Vitamin D Deficiency",
                    datePrescribed = now - (25 * dayMillis),
                    durationDays = 60,
                    isOngoing = true,
                    followUpDate = now + (35 * dayMillis),
                    doctorAdvice = "Take thyroid medication first thing in the morning with plain water.",
                    status = PrescriptionStatus.ACTIVE.name
                )
            )

            medDao.insertMedications(
                listOf(
                    Medication(
                        prescriptionId = rx2,
                        memberId = sarahId,
                        medicineName = "Levothyroxine Sodium",
                        dosage = "50 mcg",
                        form = MedicineForm.TABLET.name,
                        frequency = MedicineFrequency.ONCE_DAILY.name,
                        timing = MedicineTiming.BEFORE_FOOD.name,
                        pillsRemaining = 35,
                        totalPrescribedPills = 60,
                        refillThreshold = 10,
                        instructions = "Strictly 30-60 min before breakfast on an empty stomach.",
                        isActive = true,
                        slotMorning = true,
                        slotAfternoon = false,
                        slotEvening = false,
                        slotNight = false
                    ),
                    Medication(
                        prescriptionId = rx2,
                        memberId = sarahId,
                        medicineName = "Vitamin D3 (Cholecalciferol)",
                        dosage = "60,000 IU",
                        form = MedicineForm.CAPSULE.name,
                        frequency = MedicineFrequency.WEEKLY.name,
                        timing = MedicineTiming.WITH_FOOD.name,
                        pillsRemaining = 5,
                        totalPrescribedPills = 8,
                        refillThreshold = 2,
                        instructions = "Take once a week on Sundays with a fatty meal.",
                        isActive = true,
                        slotMorning = false,
                        slotAfternoon = true,
                        slotEvening = false,
                        slotNight = false
                    )
                )
            )

            // 3. Medical Bills
            val billDao = db.medicalBillDao()
            billDao.insertBill(
                MedicalBill(
                    memberId = alexId,
                    providerName = "City General Hospital",
                    doctorName = "Dr. Emily Chen, MD",
                    billDate = now - (14 * dayMillis),
                    category = BillCategory.CONSULTATION.name,
                    invoiceNumber = "INV-2026-8891",
                    totalAmount = 2400.0,
                    insuranceCoveredAmount = 1800.0,
                    outOfPocketAmount = 600.0,
                    paymentStatus = PaymentStatus.CLAIM_SETTLED.name,
                    dueDate = null,
                    notes = "Routine cardiology consultation + ECG assessment.",
                    lineItemsRaw = "Doctor Specialist Consultation: ₹1800.00\n12-Lead Resting ECG: ₹600.00",
                    receiptTag = "Receipt #8891-CGH"
                )
            )

            billDao.insertBill(
                MedicalBill(
                    memberId = alexId,
                    providerName = "Quest Diagnostics Lab",
                    doctorName = "Dr. Emily Chen, MD",
                    billDate = now - (12 * dayMillis),
                    category = BillCategory.DIAGNOSTIC_LAB.name,
                    invoiceNumber = "QST-77120",
                    totalAmount = 3800.0,
                    insuranceCoveredAmount = 3000.0,
                    outOfPocketAmount = 800.0,
                    paymentStatus = PaymentStatus.PAID.name,
                    dueDate = null,
                    notes = "Comprehensive Metabolic Panel, Lipid Panel, and HbA1c.",
                    lineItemsRaw = "Lipid Profile Panel: ₹1500.00\nComprehensive Metabolic Panel: ₹1400.00\nHbA1c Glycated Hemoglobin: ₹900.00",
                    receiptTag = "Lab Invoice QST-77120"
                )
            )

            billDao.insertBill(
                MedicalBill(
                    memberId = sarahId,
                    providerName = "Advanced Imaging MRI Center",
                    doctorName = "Dr. Kevin Scott",
                    billDate = now - (5 * dayMillis),
                    category = BillCategory.DIAGNOSTIC_LAB.name,
                    invoiceNumber = "AIM-99412",
                    totalAmount = 8500.0,
                    insuranceCoveredAmount = 6800.0,
                    outOfPocketAmount = 1700.0,
                    paymentStatus = PaymentStatus.CLAIM_SUBMITTED.name,
                    dueDate = now + (20 * dayMillis),
                    notes = "Cervical Spine MRI scan for chronic neck strain.",
                    lineItemsRaw = "MRI Cervical Spine without contrast: ₹7500.00\nRadiologist Interpretation Fee: ₹1000.00",
                    receiptTag = "AIM-Receipt-99412"
                )
            )

            billDao.insertBill(
                MedicalBill(
                    memberId = leoId,
                    providerName = "Sunshine Pediatric Care",
                    doctorName = "Dr. Sarah Jenkins",
                    billDate = now - (2 * dayMillis),
                    category = BillCategory.PHARMACY.name,
                    invoiceNumber = "PHARM-3312",
                    totalAmount = 650.0,
                    insuranceCoveredAmount = 450.0,
                    outOfPocketAmount = 200.0,
                    paymentStatus = PaymentStatus.PENDING.name,
                    dueDate = now + (10 * dayMillis),
                    notes = "Amoxicillin suspension & fever drops for ear infection.",
                    lineItemsRaw = "Amoxicillin 250mg/5ml: ₹350.00\nChildren's Ibuprofen Liquid: ₹300.00",
                    receiptTag = "Pharmacy Rx Bill"
                )
            )

            // 4. Medical Reports & Biomarkers
            val reportDao = db.medicalReportDao()
            reportDao.insertReport(
                MedicalReport(
                    memberId = alexId,
                    testName = "Comprehensive Lipid & Cardiovascular Panel",
                    category = ReportCategory.PATHOLOGY_BLOOD.name,
                    reportDate = now - (12 * dayMillis),
                    labOrFacility = "Quest Diagnostics Lab",
                    orderingDoctor = "Dr. Emily Chen, MD",
                    summaryFindings = "Total cholesterol and LDL slightly elevated compared to target threshold. Triglycerides within optimal limits.",
                    status = ReportStatus.ATTENTION_REQUIRED.name,
                    biomarkersRaw = "Total Cholesterol|215|mg/dL|< 200|High\nLDL Cholesterol|138|mg/dL|< 100|High\nHDL Cholesterol|52|mg/dL|> 40|Normal\nTriglycerides|125|mg/dL|< 150|Normal\nCholesterol/HDL Ratio|4.1|ratio|< 5.0|Normal",
                    followUpDate = now + (76 * dayMillis),
                    notes = "Advised to continue Atorvastatin 20mg and repeat lipid test in 3 months.",
                    documentTag = "PDF Quest_Lipid_Panel_2026.pdf"
                )
            )

            reportDao.insertReport(
                MedicalReport(
                    memberId = alexId,
                    testName = "Complete Blood Count (CBC) with Differential",
                    category = ReportCategory.PATHOLOGY_BLOOD.name,
                    reportDate = now - (12 * dayMillis),
                    labOrFacility = "Quest Diagnostics Lab",
                    orderingDoctor = "Dr. Emily Chen, MD",
                    summaryFindings = "All hematology markers within ideal reference ranges. No signs of anemia or active infection.",
                    status = ReportStatus.NORMAL.name,
                    biomarkersRaw = "Hemoglobin|15.2|g/dL|13.8 - 17.2|Normal\nWhite Blood Cells (WBC)|6.8|x10^3/uL|4.5 - 11.0|Normal\nPlatelet Count|260|x10^3/uL|150 - 450|Normal\nRed Blood Cells (RBC)|4.9|x10^6/uL|4.3 - 5.9|Normal\nHematocrit|45.1|%|40.7 - 50.3|Normal",
                    followUpDate = null,
                    notes = "Normal healthy baseline report.",
                    documentTag = "PDF Quest_CBC_Differential.pdf"
                )
            )

            reportDao.insertReport(
                MedicalReport(
                    memberId = sarahId,
                    testName = "Thyroid Function Panel (TSH, Free T3, Free T4)",
                    category = ReportCategory.PATHOLOGY_BLOOD.name,
                    reportDate = now - (20 * dayMillis),
                    labOrFacility = "LabCorp Diagnostics",
                    orderingDoctor = "Dr. Robert Miller, DO",
                    summaryFindings = "TSH mildly elevated at 4.9 uIU/mL indicating borderline subclinical hypothyroidism. Free T4 within normal limits.",
                    status = ReportStatus.ATTENTION_REQUIRED.name,
                    biomarkersRaw = "TSH (Thyroid Stimulating Hormone)|4.90|uIU/mL|0.45 - 4.50|High\nFree T4 (Thyroxine)|1.15|ng/dL|0.82 - 1.77|Normal\nFree T3 (Triiodothyronine)|2.9|pg/mL|2.0 - 4.4|Normal\nVitamin D (25-OH)|18.4|ng/mL|30.0 - 100.0|Low",
                    followUpDate = now + (35 * dayMillis),
                    notes = "Prescribed Levothyroxine 50 mcg and high potency Vitamin D3 weekly capsule.",
                    documentTag = "PDF LabCorp_Thyroid_Panel.pdf"
                )
            )

            reportDao.insertReport(
                MedicalReport(
                    memberId = sarahId,
                    testName = "Cervical Spine MRI Scan",
                    category = ReportCategory.RADIOLOGY_IMAGING.name,
                    reportDate = now - (5 * dayMillis),
                    labOrFacility = "Advanced Imaging MRI Center",
                    orderingDoctor = "Dr. Kevin Scott",
                    summaryFindings = "Mild C5-C6 disc bulge without central canal stenosis or cord compression. Normal vertebral alignment.",
                    status = ReportStatus.NORMAL.name,
                    biomarkersRaw = "Canal Diameter|14.2|mm|> 12.0|Normal\nDisc Alignment|Intact|status|Intact|Normal\nNeural Foramina|Patent|status|Patent|Normal",
                    followUpDate = null,
                    notes = "Physiotherapy recommended twice a week. Ergonomic chair setup advised.",
                    documentTag = "DICOM / PDF MRI_Cervical_Spine.pdf"
                )
            )
        }
    }
}
