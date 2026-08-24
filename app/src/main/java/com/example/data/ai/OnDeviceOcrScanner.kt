package com.example.data.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.BillCategory
import com.example.data.model.InferredBillData
import com.example.data.model.InferredBiomarker
import com.example.data.model.InferredMedication
import com.example.data.model.InferredPrescriptionData
import com.example.data.model.InferredReportData
import com.example.data.model.MedicineForm
import com.example.data.model.MedicineFrequency
import com.example.data.model.MedicineTiming
import com.example.data.model.PaymentStatus
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import kotlin.coroutines.resume

/**
 * Tier 1: Offline On-Device OCR Scanner powered by Google ML Kit.
 * Extracts text and parses amounts, dates, doctor names, invoice numbers,
 * biomarkers, and medicine schedules with zero cloud setup or API keys.
 */
class OnDeviceOcrScanner(private val context: Context) {

    companion object {
        private const val TAG = "OnDeviceOcrScanner"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeTextFromUri(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val recognized = visionText.text
                        Log.d(TAG, "ML Kit text recognized: ${recognized.take(100)}")
                        continuation.resume(Result.success(recognized))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "ML Kit OCR failed", e)
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for ML Kit OCR", e)
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Extracts bill details from raw recognized text using intelligent heuristics and regexes.
     */
    fun parseBillFromText(rawText: String): InferredBillData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        var provider = "Medical Provider"
        var doctor = ""
        var invoiceNum = ""
        var totalAmount = 0.0
        var category = BillCategory.CONSULTATION.name

        // Find provider name (usually in the first 3 lines)
        if (lines.isNotEmpty()) {
            val potentialProvider = lines.take(4).firstOrNull { line ->
                line.contains("Hospital", ignoreCase = true) ||
                line.contains("Clinic", ignoreCase = true) ||
                line.contains("Pharmacy", ignoreCase = true) ||
                line.contains("Diagnostics", ignoreCase = true) ||
                line.contains("Lab", ignoreCase = true) ||
                line.contains("Center", ignoreCase = true) ||
                line.contains("Healthcare", ignoreCase = true)
            }
            provider = potentialProvider ?: lines.firstOrNull { it.length > 3 } ?: "Medical Provider"
        }

        // Detect category
        val lowerText = rawText.lowercase()
        category = when {
            lowerText.contains("pharmacy") || lowerText.contains("chemist") || lowerText.contains("medication") || lowerText.contains("drugs") -> BillCategory.PHARMACY.name
            lowerText.contains("lab") || lowerText.contains("pathology") || lowerText.contains("diagnostic") || lowerText.contains("blood test") -> BillCategory.DIAGNOSTIC_LAB.name
            lowerText.contains("surgery") || lowerText.contains("operation") || lowerText.contains("procedure") -> BillCategory.SURGERY_PROCEDURE.name
            lowerText.contains("hospital") || lowerText.contains("admission") || lowerText.contains("inpatient") || lowerText.contains("icu") -> BillCategory.HOSPITAL_STAY.name
            lowerText.contains("dental") || lowerText.contains("tooth") || lowerText.contains("dentist") -> BillCategory.DENTAL.name
            lowerText.contains("eye") || lowerText.contains("optical") || lowerText.contains("vision") -> BillCategory.EYE_CARE.name
            lowerText.contains("physio") || lowerText.contains("therapy") || lowerText.contains("rehab") -> BillCategory.THERAPY.name
            else -> BillCategory.CONSULTATION.name
        }

        // Find doctor name
        val doctorPattern = Pattern.compile("(?i)(?:Dr\\.?|Doctor)\\s+([A-Za-z\\s\\.]{3,30})")
        val doctorMatcher = doctorPattern.matcher(rawText)
        if (doctorMatcher.find()) {
            doctor = "Dr. " + (doctorMatcher.group(1)?.trim()?.removePrefix("Dr.")?.removePrefix("Dr")?.trim() ?: "")
        }

        // Find invoice number
        val invPattern = Pattern.compile("(?i)(?:Invoice|Bill|Receipt|Ref|Rx)\\s*(?:No|#|Num|Number)?\\s*[:\\-]?\\s*([A-Za-z0-9\\-_/]{3,20})")
        val invMatcher = invPattern.matcher(rawText)
        if (invMatcher.find()) {
            invoiceNum = invMatcher.group(1)?.trim() ?: ""
        }

        // Extract highest amount near 'Total', 'Net', 'Grand Total', or Currency
        val amountPattern = Pattern.compile("(?i)(?:Total|Grand Total|Net Amount|Amount Due|Paid|₹|Rs\\.?|USD|\\$)\\s*[:\\-]?\\s*([0-9]+(?:[,.][0-9]{2})?)")
        val amountMatcher = amountPattern.matcher(rawText)
        val amountsFound = mutableListOf<Double>()
        while (amountMatcher.find()) {
            val amtStr = amountMatcher.group(1)?.replace(",", "") ?: ""
            amtStr.toDoubleOrNull()?.let { amountsFound.add(it) }
        }

        if (amountsFound.isNotEmpty()) {
            totalAmount = amountsFound.maxOrNull() ?: 0.0
        } else {
            // General number search
            val generalAmountPattern = Pattern.compile("([0-9]{2,6}(?:\\.[0-9]{2})?)")
            val generalMatcher = generalAmountPattern.matcher(rawText)
            val allNumbers = mutableListOf<Double>()
            while (generalMatcher.find()) {
                generalMatcher.group(1)?.toDoubleOrNull()?.let { allNumbers.add(it) }
            }
            totalAmount = allNumbers.maxOrNull() ?: 0.0
        }

        val cleanLineItems = lines.filter { line ->
            line.any { it.isDigit() } && line.length > 5
        }.take(8).joinToString("\n")

        return InferredBillData(
            providerName = provider,
            doctorName = doctor,
            category = category,
            invoiceNumber = invoiceNum,
            totalAmount = totalAmount,
            insuranceCoveredAmount = 0.0,
            paymentStatus = PaymentStatus.PAID.name,
            lineItemsRaw = cleanLineItems,
            notes = "Auto-scanned via on-device ML Kit OCR",
            aiSummary = "Scanned receipt from $provider (Amount: ₹$totalAmount). Processed offline on-device."
        )
    }

    /**
     * Extracts report details from raw recognized text.
     */
    fun parseReportFromText(rawText: String): InferredReportData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val lowerText = rawText.lowercase()

        var testName = "Diagnostic Lab Report"
        var facility = "Diagnostic Laboratory"
        var doctor = ""
        var category = ReportCategory.PATHOLOGY_BLOOD.name
        var status = ReportStatus.NORMAL.name

        // Detect test type
        when {
            lowerText.contains("blood") || lowerText.contains("cbc") || lowerText.contains("hemoglobin") || lowerText.contains("lipid") || lowerText.contains("thyroid") || lowerText.contains("glucose") || lowerText.contains("hba1c") -> {
                category = ReportCategory.PATHOLOGY_BLOOD.name
                testName = when {
                    lowerText.contains("lipid") -> "Lipid Profile Test"
                    lowerText.contains("thyroid") || lowerText.contains("tsh") -> "Thyroid Panel (T3, T4, TSH)"
                    lowerText.contains("cbc") || lowerText.contains("complete blood") -> "Complete Blood Count (CBC)"
                    lowerText.contains("hba1c") || lowerText.contains("glucose") -> "Blood Glucose & HbA1c"
                    else -> "Routine Pathology Blood Panel"
                }
            }
            lowerText.contains("x-ray") || lowerText.contains("mri") || lowerText.contains("ct scan") || lowerText.contains("ultrasound") || lowerText.contains("radiology") -> {
                category = ReportCategory.RADIOLOGY_IMAGING.name
                testName = when {
                    lowerText.contains("mri") -> "MRI Scan"
                    lowerText.contains("ct") -> "CT Scan"
                    lowerText.contains("ultrasound") || lowerText.contains("usg") -> "Ultrasound Examination"
                    else -> "Radiology / X-Ray Imaging"
                }
            }
            lowerText.contains("ecg") || lowerText.contains("echo") || lowerText.contains("cardio") -> {
                category = ReportCategory.CARDIOLOGY.name
                testName = "Electrocardiogram (ECG) / Cardiology Report"
            }
            lowerText.contains("urine") || lowerText.contains("stool") -> {
                category = ReportCategory.URINE_STOOL.name
                testName = "Routine Urine & Stool Analysis"
            }
            else -> {
                category = ReportCategory.SPECIALTY_LAB.name
                testName = lines.firstOrNull { it.length > 5 } ?: "Diagnostic Medical Report"
            }
        }

        // Find facility
        val facilityLine = lines.take(5).firstOrNull {
            it.contains("Lab", ignoreCase = true) ||
            it.contains("Diagnostics", ignoreCase = true) ||
            it.contains("Imaging", ignoreCase = true) ||
            it.contains("Hospital", ignoreCase = true) ||
            it.contains("Pathology", ignoreCase = true)
        }
        if (facilityLine != null) facility = facilityLine

        // Find doctor
        val doctorPattern = Pattern.compile("(?i)(?:Dr\\.?|Ref By|Doctor)\\s*[:\\-]?\\s*([A-Za-z\\s\\.]{3,30})")
        val doctorMatcher = doctorPattern.matcher(rawText)
        if (doctorMatcher.find()) {
            doctor = "Dr. " + (doctorMatcher.group(1)?.trim()?.removePrefix("Dr.")?.removePrefix("Dr")?.trim() ?: "")
        }

        // Biomarkers extraction from tabular lines
        val biomarkers = mutableListOf<InferredBiomarker>()
        for (line in lines) {
            val parts = line.split(Regex("[\\t,;:|]"))
            if (parts.size >= 2) {
                val name = parts[0].trim()
                val valStr = parts[1].trim()
                if (name.length in 3..25 && valStr.any { it.isDigit() }) {
                    val unit = parts.getOrNull(2)?.trim() ?: ""
                    val ref = parts.getOrNull(3)?.trim() ?: ""
                    biomarkers.add(
                        InferredBiomarker(
                            name = name,
                            value = valStr,
                            unit = unit,
                            referenceRange = ref,
                            flag = "Normal"
                        )
                    )
                }
            }
        }

        return InferredReportData(
            testName = testName,
            category = category,
            labOrFacility = facility,
            orderingDoctor = doctor,
            summaryFindings = "Diagnostic report parameters scanned on-device via ML Kit OCR.",
            status = status,
            doctorAdvice = "Please review with your consulting physician.",
            biomarkers = biomarkers.take(8),
            aiSummary = "$testName conducted at $facility. Text extracted directly on-device without internet."
        )
    }

    /**
     * Extracts prescription details from recognized text.
     */
    fun parsePrescriptionFromText(rawText: String): InferredPrescriptionData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var doctor = "Doctor"
        var specialty = "General Physician"
        var clinic = "Clinic"
        var diagnosis = "General Consultation"

        val doctorPattern = Pattern.compile("(?i)(?:Dr\\.?|Doctor)\\s+([A-Za-z\\s\\.]{3,30})")
        val doctorMatcher = doctorPattern.matcher(rawText)
        if (doctorMatcher.find()) {
            doctor = "Dr. " + (doctorMatcher.group(1)?.trim()?.removePrefix("Dr.")?.removePrefix("Dr")?.trim() ?: "")
        }

        val clinicLine = lines.take(4).firstOrNull {
            it.contains("Clinic", ignoreCase = true) ||
            it.contains("Hospital", ignoreCase = true) ||
            it.contains("Health", ignoreCase = true)
        }
        if (clinicLine != null) clinic = clinicLine

        // Extract medicines
        val medications = mutableListOf<InferredMedication>()
        val medPattern = Pattern.compile("(?i)(?:Tab|Cap|Syp|Inj|Tablet|Capsule|Syrup)?\\.?\\s*([A-Za-z0-9\\-]{3,20})\\s*([0-9]+\\s*(?:mg|gm|ml|mcg|IU)?)?")
        for (line in lines) {
            val matcher = medPattern.matcher(line)
            if (matcher.find()) {
                val medName = matcher.group(1)?.trim() ?: ""
                val dosage = matcher.group(2)?.trim() ?: "1 Tablet"
                if (medName.isNotBlank() && medName.length > 3 && !medName.equals("Doctor", ignoreCase = true) && !medName.equals("Patient", ignoreCase = true) && !medName.equals("Date", ignoreCase = true)) {
                    val form = when {
                        line.contains("Cap", ignoreCase = true) -> MedicineForm.CAPSULE.name
                        line.contains("Syp", ignoreCase = true) || line.contains("Syrup", ignoreCase = true) -> MedicineForm.SYRUP.name
                        line.contains("Inj", ignoreCase = true) -> MedicineForm.INJECTION.name
                        line.contains("Drop", ignoreCase = true) -> MedicineForm.DROPS.name
                        else -> MedicineForm.TABLET.name
                    }
                    val timing = if (line.contains("Before", ignoreCase = true) || line.contains("empty stomach", ignoreCase = true)) {
                        MedicineTiming.BEFORE_FOOD.displayName
                    } else {
                        MedicineTiming.AFTER_FOOD.displayName
                    }
                    val slotM = line.contains("1-", ignoreCase = true) || line.contains("Morning", ignoreCase = true) || line.contains("OD", ignoreCase = true) || line.contains("BD", ignoreCase = true) || line.contains("TDS", ignoreCase = true)
                    val slotN = line.contains("-1", ignoreCase = true) || line.contains("Night", ignoreCase = true) || line.contains("BD", ignoreCase = true) || line.contains("TDS", ignoreCase = true) || line.contains("HS", ignoreCase = true)
                    val slotA = line.contains("TDS", ignoreCase = true) || line.contains("Afternoon", ignoreCase = true)

                    medications.add(
                        InferredMedication(
                            name = medName.replaceFirstChar { it.uppercase() },
                            dosage = if (dosage.isNotBlank()) dosage else "500 mg",
                            form = form,
                            frequency = if (slotM && slotN) MedicineFrequency.TWICE_DAILY.name else MedicineFrequency.ONCE_DAILY.name,
                            timing = timing,
                            pillsCount = 30,
                            slotMorning = slotM,
                            slotAfternoon = slotA,
                            slotEvening = false,
                            slotNight = slotN
                        )
                    )
                }
            }
        }

        return InferredPrescriptionData(
            doctorName = doctor,
            specialty = specialty,
            clinicOrHospital = clinic,
            diagnosis = diagnosis,
            doctorAdvice = "Take medicines as directed on prescription.",
            durationDays = 30,
            medications = medications.distinctBy { it.name }.take(6),
            aiSummary = "Prescription from $doctor ($clinic) processed on-device with ML Kit OCR."
        )
    }
}
