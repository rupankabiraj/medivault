package com.example.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttachmentUtils {

    /**
     * Resolves a stored URI string (which might be file://, content://, or an absolute path) to a File
     */
    fun resolveFile(context: Context, uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val cleanPath = when {
                uriString.startsWith("file://") -> Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                uriString.startsWith("content://") -> null // Handled via contentResolver
                else -> uriString
            }
            if (cleanPath != null) {
                val f = File(cleanPath)
                if (f.exists()) f else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
     * Downloads/Exports the attachment to the device's public Downloads directory
     * so user can access it in their Files/Downloads app or Gallery.
     */
    fun downloadAttachmentToDevice(context: Context, uriString: String?, fileName: String? = null): Boolean {
        if (uriString.isNullOrBlank()) {
            Toast.makeText(context, "No attachment available to download", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val targetName = if (!fileName.isNullOrBlank()) {
                if (fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".png", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)) {
                    fileName
                } else {
                    "$fileName.jpg"
                }
            } else {
                "MediVault_Doc_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
            }

            val sourceFile = resolveFile(context, uriString)
            val inputStream: InputStream? = if (sourceFile != null && sourceFile.exists()) {
                FileInputStream(sourceFile)
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriString))
            }

            if (inputStream == null) {
                Toast.makeText(context, "Could not open attachment file", Toast.LENGTH_SHORT).show()
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MediVault")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads/MediVault/$targetName", Toast.LENGTH_LONG).show()
                    true
                } else {
                    // Fallback to MediaStore Images
                    val imageValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, targetName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MediVault")
                    }
                    val imgUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)
                    if (imgUri != null) {
                        context.contentResolver.openOutputStream(imgUri)?.use { output ->
                            inputStream.use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, "Saved to Pictures/MediVault/$targetName", Toast.LENGTH_LONG).show()
                        true
                    } else {
                        Toast.makeText(context, "Failed to save file to Downloads", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            } else {
                // Pre-Android 10
                @Suppress("DEPRECATION")
                val downloadsFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MediVault").apply {
                    if (!exists()) mkdirs()
                }
                val destFile = File(downloadsFolder, targetName)
                FileOutputStream(destFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Saved to Downloads: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Shares the attachment via Android system share sheet with WhatsApp, Email, Drive, etc.
     */
    fun shareAttachment(context: Context, uriString: String?, title: String = "Medical Document") {
        if (uriString.isNullOrBlank()) {
            Toast.makeText(context, "No attachment available to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sourceFile = resolveFile(context, uriString)
            val contentUri: Uri = if (sourceFile != null && sourceFile.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    sourceFile
                )
            } else {
                Uri.parse(uriString)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Shared from MediVault: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to share attachment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

