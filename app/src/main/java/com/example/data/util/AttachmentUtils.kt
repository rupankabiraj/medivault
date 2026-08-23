package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttachmentUtils {

    /**
     * Saves a chosen or captured document image to app-private internal storage
     * Returns the permanent file path / URI string.
     */
    fun saveAttachmentToInternalStorage(context: Context, sourceUri: Uri, prefix: String = "doc"): Pair<String, String>? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${prefix}_${timestamp}.jpg"
            val attachmentsDir = File(context.filesDir, "attachments").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(attachmentsDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            Pair(Uri.fromFile(destinationFile).toString(), fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads, downsamples, and converts a Uri to a Base64-encoded JPEG string for Gemini API.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 1200): Pair<String, String>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Scale down if larger than maxDimension to conserve memory & bandwidth
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val max = maxOf(width, height).toFloat()
                maxDimension / max
            } else {
                1.0f
            }

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Pair(base64, "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatDate(millis: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun formatDateMonthGroup(millis: Long): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}
