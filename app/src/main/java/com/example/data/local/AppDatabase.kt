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
    version = 2,
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

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medivault_health.db"
                )
                    .fallbackToDestructiveMigration()
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
                        populateInitialDefaultProfile(database)
                    }
                }
            }
        }

        private suspend fun populateInitialDefaultProfile(db: AppDatabase) {
            // Initial clean install: Create only the default primary user profile
            // No sample bills, reports, or prescriptions are populated.
            val memberDao = db.familyMemberDao()
            memberDao.insertMember(
                FamilyMember(
                    id = 1,
                    name = "Primary User",
                    relationship = "Self",
                    age = 30,
                    bloodGroup = "O+",
                    allergies = "None",
                    emergencyContact = "",
                    isPrimary = true,
                    avatarColorHex = "#00897B"
                )
            )
        }
    }
}
