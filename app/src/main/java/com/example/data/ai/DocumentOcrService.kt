package com.example.data.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
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
import com.example.data.util.AttachmentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiKeyManager {
    private const val PREFS_NAME = "medivault_ai_settings"
    private const val KEY_CUSTOM_GEMINI_API = "custom_gemini_api_key"

    fun getApiKey(context: Context): String {
        val customKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_GEMINI_API, "")?.trim() ?: ""
        if (customKey.isNotBlank()) {
            return customKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun setApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_GEMINI_API, key.trim())
            .apply()
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_GEMINI_API)
            .apply()
    }

    fun hasCustomKey(context: Context): Boolean {
        val customKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_GEMINI_API, "")?.trim() ?: ""
        return customKey.isNotBlank()
    }

    fun isConfigured(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun getMaskedKey(context: Context): String {
        val key = getApiKey(context)
        if (key.isBlank()) return "Not configured"
        if (key.length <= 8) return "••••••••"
        return "${key.take(6)}••••••••${key.takeLast(4)}"
    }
}

class DocumentOcrService(private val context: Context) {

    companion object {
        private const val TAG = "DocumentOcrService"
        // Model for multimodal text and image reasoning as per guidelines
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Executes a multimodal Gemini request with image Base64 and text prompt.
     */
    private suspend fun callGeminiMultimodal(
        imageBase64: String,
        mimeType: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please tap 'Configure API Key' or set it in the Settings/About dialog.")
            )
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            // Construct Gemini Request JSON
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Part 1: Text instruction
                            put(JSONObject().put("text", prompt))
                            // Part 2: Inline document image
                            val inlineData = JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", imageBase64)
                            }
                            put(JSONObject().put("inlineData", inlineData))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.95)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API Error code ${response.code}: $responseBody")
                return@withContext Result.failure(
                    RuntimeException("API Error (${response.code}): ${response.message}")
                )
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(RuntimeException("No analysis candidates returned by Gemini"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            if (text.isBlank()) {
                return@withContext Result.failure(RuntimeException("Gemini returned empty text"))
            }

            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            Result.failure(e)
        }
    }

    private val onDeviceScanner = OnDeviceOcrScanner(context)

    /**
     * Extracts structured details from a medical bill/invoice image.
     * Uses Gemini 2.5 Flash if configured; falls back automatically to On-Device ML Kit OCR.
     */
    suspend fun extractBillDetails(imageUri: Uri): Result<InferredBillData> = withContext(Dispatchers.IO) {
        val isCloudAiAvailable = ApiKeyManager.isConfigured(context)
        val base64Data = AttachmentUtils.uriToBase64(context, imageUri)

        if (isCloudAiAvailable && base64Data != null) {
            val prompt = """
                You are a specialized Medical Document and Financial OCR system.
                Carefully inspect this attached medical bill, hospital receipt, pharmacy invoice, or insurance statement image.
                
                Extract all relevant financial and clinical information into a single clean JSON object with the following schema:
                ```json
                {
                  "providerName": "Hospital, Clinic, Diagnostic Center, or Pharmacy name",
                  "doctorName": "Doctor or physician name if present (or empty string)",
                  "category": "CONSULTATION or DIAGNOSTIC_LAB or PHARMACY or SURGERY_PROCEDURE or HOSPITAL_STAY or EMERGENCY or DENTAL or THERAPY or OTHER",
                  "invoiceNumber": "Invoice, receipt, or bill reference number if visible",
                  "totalAmount": 1500.0,
                  "insuranceCoveredAmount": 0.0,
                  "paymentStatus": "PAID or PENDING or CLAIM_SUBMITTED",
                  "lineItemsRaw": "Item 1: ₹500\nItem 2: ₹1000",
                  "notes": "Brief notes on charges, payment mode, or claim submission details",
                  "aiSummary": "2-3 sentences explaining this bill, items charged, and overall financial summary"
                }
                ```
                Return ONLY the valid JSON object inside markdown json code block.
            """.trimIndent()

            val rawResult = callGeminiMultimodal(base64Data.first, base64Data.second, prompt)
            if (rawResult.isSuccess) {
                return@withContext rawResult.mapCatching { parseBillJson(it) }
            }
            Log.w(TAG, "Gemini bill extraction failed, falling back to ML Kit OCR: ${rawResult.exceptionOrNull()?.message}")
        }

        // Tier 1 Fallback: ML Kit On-Device Scanner
        val textResult = onDeviceScanner.recognizeTextFromUri(imageUri)
        textResult.mapCatching { recognizedText ->
            onDeviceScanner.parseBillFromText(recognizedText)
        }
    }

    /**
     * Extracts structured findings, biomarkers, and health status from a diagnostic report image.
     * Uses Gemini 2.5 Flash if configured; falls back automatically to On-Device ML Kit OCR.
     */
    suspend fun extractReportDetails(imageUri: Uri): Result<InferredReportData> = withContext(Dispatchers.IO) {
        val isCloudAiAvailable = ApiKeyManager.isConfigured(context)
        val base64Data = AttachmentUtils.uriToBase64(context, imageUri)

        if (isCloudAiAvailable && base64Data != null) {
            val prompt = """
                You are a specialized Clinical Pathology, Radiology, and Medical Diagnostic Report Interpreter.
                Carefully examine this medical test report, scan results, or laboratory document image.
                
                Extract the test name, clinical summary, department, health status, and all specific biomarker / test parameter readings into a JSON object:
                ```json
                {
                  "testName": "Name of diagnostic test (e.g. Complete Blood Count, Lipid Profile, Thyroid Panel, MRI Brain)",
                  "category": "PATHOLOGY_BLOOD or RADIOLOGY_IMAGING or CARDIOLOGY or URINE_STOOL or BIOPSY or ANNUAL_CHECKUP or SPECIALTY_LAB or OTHER",
                  "labOrFacility": "Name of laboratory or diagnostic facility",
                  "orderingDoctor": "Physician or doctor who ordered the test (or empty string)",
                  "summaryFindings": "Clear, informative clinical summary of findings and diagnostic interpretation",
                  "status": "NORMAL or ATTENTION_REQUIRED or CRITICAL",
                  "doctorAdvice": "Recommended follow-up, dietary advice, or repeats mentioned in report",
                  "biomarkers": [
                    {
                      "name": "Hemoglobin",
                      "value": "14.2",
                      "unit": "g/dL",
                      "referenceRange": "13.5 - 17.5",
                      "flag": "Normal"
                    }
                  ],
                  "aiSummary": "Comprehensive plain-language health explanation of what these results mean for the patient"
                }
                ```
                Return ONLY the valid JSON object inside markdown json code block.
            """.trimIndent()

            val rawResult = callGeminiMultimodal(base64Data.first, base64Data.second, prompt)
            if (rawResult.isSuccess) {
                return@withContext rawResult.mapCatching { parseReportJson(it) }
            }
            Log.w(TAG, "Gemini report extraction failed, falling back to ML Kit OCR: ${rawResult.exceptionOrNull()?.message}")
        }

        // Tier 1 Fallback: ML Kit On-Device Scanner
        val textResult = onDeviceScanner.recognizeTextFromUri(imageUri)
        textResult.mapCatching { recognizedText ->
            onDeviceScanner.parseReportFromText(recognizedText)
        }
    }

    /**
     * Extracts doctor, diagnosis, medicines, dosages, and daily slots from a prescription image.
     * Uses Gemini 2.5 Flash for deep handwriting deciphering; falls back automatically to On-Device ML Kit OCR.
     */
    suspend fun extractPrescriptionDetails(imageUri: Uri): Result<InferredPrescriptionData> = withContext(Dispatchers.IO) {
        val isCloudAiAvailable = ApiKeyManager.isConfigured(context)
        val base64Data = AttachmentUtils.uriToBase64(context, imageUri)

        if (isCloudAiAvailable && base64Data != null) {
            val prompt = """
                You are an expert Doctor's Prescription Reader and Pharmacological Assistant.
                Carefully decipher the handwriting/text in this medical prescription image.
                
                Extract the doctor's name, clinic, diagnosis/health condition, duration, advice, and all prescribed medications with exact dosage, schedule, timing, and pill counts:
                ```json
                {
                  "doctorName": "Doctor's full name",
                  "specialty": "Doctor specialty e.g. Cardiologist, Physician, Pediatrician",
                  "clinicOrHospital": "Hospital or clinic name",
                  "diagnosis": "Diagnosed condition or symptom (e.g. Hypertension, Acute Bronchitis, Type 2 Diabetes)",
                  "doctorAdvice": "Lifestyle, dietary, or follow-up instructions",
                  "durationDays": 30,
                  "medications": [
                    {
                      "name": "Amoxicillin",
                      "dosage": "500 mg",
                      "form": "TABLET or CAPSULE or SYRUP or INJECTION or INHALER or DROPS or CREAM or OTHER",
                      "frequency": "ONCE_DAILY or TWICE_DAILY or THRICE_DAILY or FOUR_TIMES_DAILY or AS_NEEDED or WEEKLY",
                      "timing": "After Meals or Before Meals or With Meals or At Bedtime",
                      "pillsCount": 30,
                      "slotMorning": true,
                      "slotAfternoon": false,
                      "slotEvening": true,
                      "slotNight": false
                    }
                  ],
                  "aiSummary": "Concise summary of this treatment plan, medications prescribed, and key patient instructions"
                }
                ```
                Return ONLY the valid JSON object inside markdown json code block.
            """.trimIndent()

            val rawResult = callGeminiMultimodal(base64Data.first, base64Data.second, prompt)
            if (rawResult.isSuccess) {
                return@withContext rawResult.mapCatching { parsePrescriptionJson(it) }
            }
            Log.w(TAG, "Gemini rx extraction failed, falling back to ML Kit OCR: ${rawResult.exceptionOrNull()?.message}")
        }

        // Tier 1 Fallback: ML Kit On-Device Scanner
        val textResult = onDeviceScanner.recognizeTextFromUri(imageUri)
        textResult.mapCatching { recognizedText ->
            onDeviceScanner.parsePrescriptionFromText(recognizedText)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.contains("```json")) {
            clean = clean.substringAfter("```json").substringBefore("```").trim()
        } else if (clean.contains("```")) {
            clean = clean.substringAfter("```").substringBefore("```").trim()
        }
        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1)
        }
        return clean
    }

    private fun parseBillJson(rawText: String): InferredBillData {
        val jsonStr = cleanJsonString(rawText)
        val json = JSONObject(jsonStr)

        val totalAmount = json.optDouble("totalAmount", 0.0)
        val insuredAmount = json.optDouble("insuranceCoveredAmount", 0.0)

        var category = json.optString("category", BillCategory.CONSULTATION.name).uppercase()
        if (BillCategory.values().none { it.name == category }) {
            category = BillCategory.CONSULTATION.name
        }

        var status = json.optString("paymentStatus", PaymentStatus.PAID.name).uppercase()
        if (PaymentStatus.values().none { it.name == status }) {
            status = PaymentStatus.PAID.name
        }

        return InferredBillData(
            providerName = json.optString("providerName", "Medical Provider"),
            doctorName = json.optString("doctorName", ""),
            category = category,
            invoiceNumber = json.optString("invoiceNumber", ""),
            totalAmount = totalAmount,
            insuranceCoveredAmount = insuredAmount,
            paymentStatus = status,
            lineItemsRaw = json.optString("lineItemsRaw", ""),
            notes = json.optString("notes", ""),
            aiSummary = json.optString("aiSummary", "Inferred details from scanned medical bill.")
        )
    }

    private fun parseReportJson(rawText: String): InferredReportData {
        val jsonStr = cleanJsonString(rawText)
        val json = JSONObject(jsonStr)

        var category = json.optString("category", ReportCategory.PATHOLOGY_BLOOD.name).uppercase()
        if (ReportCategory.values().none { it.name == category }) {
            category = ReportCategory.PATHOLOGY_BLOOD.name
        }

        var status = json.optString("status", ReportStatus.NORMAL.name).uppercase()
        if (ReportStatus.values().none { it.name == status }) {
            status = ReportStatus.NORMAL.name
        }

        val biomarkersList = mutableListOf<InferredBiomarker>()
        val biomarkersArray = json.optJSONArray("biomarkers")
        if (biomarkersArray != null) {
            for (i in 0 until biomarkersArray.length()) {
                val item = biomarkersArray.optJSONObject(i) ?: continue
                biomarkersList.add(
                    InferredBiomarker(
                        name = item.optString("name", "Marker ${i + 1}"),
                        value = item.optString("value", ""),
                        unit = item.optString("unit", ""),
                        referenceRange = item.optString("referenceRange", ""),
                        flag = item.optString("flag", "Normal")
                    )
                )
            }
        }

        return InferredReportData(
            testName = json.optString("testName", "Diagnostic Lab Report"),
            category = category,
            labOrFacility = json.optString("labOrFacility", "Diagnostic Laboratory"),
            orderingDoctor = json.optString("orderingDoctor", ""),
            summaryFindings = json.optString("summaryFindings", ""),
            status = status,
            doctorAdvice = json.optString("doctorAdvice", ""),
            biomarkers = biomarkersList,
            aiSummary = json.optString("aiSummary", "Inferred diagnostic test findings and health assessment.")
        )
    }

    private fun parsePrescriptionJson(rawText: String): InferredPrescriptionData {
        val jsonStr = cleanJsonString(rawText)
        val json = JSONObject(jsonStr)

        val medicationsList = mutableListOf<InferredMedication>()
        val medsArray = json.optJSONArray("medications")
        if (medsArray != null) {
            for (i in 0 until medsArray.length()) {
                val item = medsArray.optJSONObject(i) ?: continue

                var form = item.optString("form", MedicineForm.TABLET.name).uppercase()
                if (MedicineForm.values().none { it.name == form }) {
                    form = MedicineForm.TABLET.name
                }

                var frequency = item.optString("frequency", MedicineFrequency.TWICE_DAILY.name).uppercase()
                if (MedicineFrequency.values().none { it.name == frequency }) {
                    frequency = MedicineFrequency.TWICE_DAILY.name
                }

                medicationsList.add(
                    InferredMedication(
                        name = item.optString("name", "Prescribed Medicine"),
                        dosage = item.optString("dosage", "500 mg"),
                        form = form,
                        frequency = frequency,
                        timing = item.optString("timing", MedicineTiming.AFTER_FOOD.displayName),
                        pillsCount = item.optInt("pillsCount", 30),
                        slotMorning = item.optBoolean("slotMorning", true),
                        slotAfternoon = item.optBoolean("slotAfternoon", false),
                        slotEvening = item.optBoolean("slotEvening", true),
                        slotNight = item.optBoolean("slotNight", false)
                    )
                )
            }
        }

        return InferredPrescriptionData(
            doctorName = json.optString("doctorName", "Doctor"),
            specialty = json.optString("specialty", "General Physician"),
            clinicOrHospital = json.optString("clinicOrHospital", "Clinic"),
            diagnosis = json.optString("diagnosis", "General Health Consultation"),
            doctorAdvice = json.optString("doctorAdvice", ""),
            durationDays = json.optInt("durationDays", 30),
            medications = medicationsList,
            aiSummary = json.optString("aiSummary", "Inferred prescription and medication regimen.")
        )
    }
}
