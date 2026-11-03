package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseAttachment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AttachmentManager {

    private const val MAX_IMAGE_SIDE = 1600
    private const val JPEG_QUALITY = 80
    const val MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L // 10 MB

    data class PreparedFile(
        val fileName: String,
        val contentType: String,
        val bytes: ByteArray,
        val sizeBytes: Long
    )

    /**
     * Reads all bytes from a Uri using ContentResolver stream, asset file descriptor,
     * file descriptor, or direct file path fallback.
     */
    private fun readBytesFromUri(context: Context, uri: Uri): Result<ByteArray> {
        val resolver = context.contentResolver

        // 1. Standard openInputStream
        try {
            resolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    return Result.success(bytes)
                }
            }
        } catch (_: Exception) {}

        // 2. OpenAssetFileDescriptor fallback
        try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream()?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        return Result.success(bytes)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. OpenFileDescriptor fallback
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        return Result.success(bytes)
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. File fallback for file:// or direct path
        try {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val bytes = file.readBytes()
                    if (bytes.isNotEmpty()) {
                        return Result.success(bytes)
                    }
                }
            }
        } catch (_: Exception) {}

        return Result.failure(
            IllegalArgumentException("Unable to read image or file data. Please make sure the file exists on the device or pick from gallery/downloads.")
        )
    }

    /**
     * Resolves display name and prepares either an image (compressed) or PDF from a Uri.
     */
    suspend fun prepareAttachment(context: Context, uri: Uri, isImage: Boolean): Result<PreparedFile> =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var originalName: String? = null
                var originalSize: Long = 0L

                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                originalName = cursor.getString(nameIndex)
                            }
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex != -1) {
                                originalSize = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (isImage) {
                    val fallbackName = originalName ?: "image_${System.currentTimeMillis()}.jpg"
                    val cleanName = if (fallbackName.contains(".")) fallbackName else "$fallbackName.jpg"
                    val compressedResult = compressImage(context, uri)
                    compressedResult.fold(
                        onSuccess = { compressedBytes ->
                            if (compressedBytes.size > MAX_FILE_SIZE_BYTES) {
                                Result.failure(IllegalArgumentException("Compressed image exceeds 10 MB limit"))
                            } else {
                                Result.success(
                                    PreparedFile(
                                        fileName = cleanName,
                                        contentType = "image/jpeg",
                                        bytes = compressedBytes,
                                        sizeBytes = compressedBytes.size.toLong()
                                    )
                                )
                            }
                        },
                        onFailure = { Result.failure(it) }
                    )
                } else {
                    val fallbackName = originalName ?: "document_${System.currentTimeMillis()}.pdf"
                    val cleanName = if (fallbackName.lowercase().endsWith(".pdf")) fallbackName else "$fallbackName.pdf"

                    val readResult = readBytesFromUri(context, uri)
                    val bytes = readResult.getOrElse { return@withContext Result.failure(it) }

                    if (bytes.size > MAX_FILE_SIZE_BYTES) {
                        return@withContext Result.failure(IllegalArgumentException("PDF exceeds 10 MB limit (${bytes.size / (1024 * 1024)} MB)"))
                    }

                    Result.success(
                        PreparedFile(
                            fileName = cleanName,
                            contentType = "application/pdf",
                            bytes = bytes,
                            sizeBytes = bytes.size.toLong()
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Compresses image to max side ~1600px with 80% JPEG quality, preserving EXIF orientation.
     */
    private fun compressImage(context: Context, uri: Uri): Result<ByteArray> {
        return try {
            val readResult = readBytesFromUri(context, uri)
            val rawBytes = readResult.getOrElse { return Result.failure(it) }

            if (rawBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Selected image file is empty"))
            }

            // 1. Decode bounds from byte array
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return Result.failure(IllegalArgumentException("Invalid image dimensions. Please select a valid JPEG, PNG, or WEBP image."))
            }

            // 2. Compute sample size
            var sampleSize = 1
            val maxSide = maxOf(origWidth, origHeight)
            while (maxSide / (sampleSize * 2) >= MAX_IMAGE_SIDE) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap from byte array
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return Result.failure(IllegalArgumentException("Failed to decode image from selected file"))

            // 4. Handle EXIF rotation from byte array stream
            try {
                ByteArrayInputStream(rawBytes).use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        if (rotatedBitmap != bitmap) {
                            bitmap.recycle()
                            bitmap = rotatedBitmap
                        }
                    }
                }
            } catch (_: Exception) {
                // Non-fatal if EXIF is unreadable
            }

            // 5. Downscale precisely if still larger than MAX_IMAGE_SIDE
            val currentMax = maxOf(bitmap.width, bitmap.height)
            if (currentMax > MAX_IMAGE_SIDE) {
                val scale = MAX_IMAGE_SIDE.toFloat() / currentMax.toFloat()
                val targetWidth = (bitmap.width * scale).toInt()
                val targetHeight = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            // 6. Compress to JPEG 80%
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            bitmap.recycle()

            Result.success(outputStream.toByteArray())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads bytes to Storage at projects/{projectId}/expenses/{expenseId}/{randomId}_{fileName}
     * with progress callback.
     */
    suspend fun uploadAttachment(
        projectId: String,
        expenseId: String,
        preparedFile: PreparedFile,
        uploadedByUid: String,
        onProgress: (Int) -> Unit = {}
    ): Result<ExpenseAttachment> = withContext(Dispatchers.IO) {
        try {
            val randomId = UUID.randomUUID().toString().take(8)
            val cleanFileName = preparedFile.fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val storagePath = "projects/$projectId/expenses/$expenseId/${randomId}_$cleanFileName"

            val storage = Firebase.storage
            val storageRef = storage.reference.child(storagePath)

            val metadata = StorageMetadata.Builder()
                .setContentType(preparedFile.contentType)
                .setCustomMetadata("uploadedByUid", uploadedByUid)
                .setCustomMetadata("originalName", preparedFile.fileName)
                .build()

            val uploadTask = storageRef.putBytes(preparedFile.bytes, metadata)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                if (total > 0) {
                    val progress = ((100.0 * taskSnapshot.bytesTransferred) / total).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }

            uploadTask.await()
            onProgress(100)

            val attachment = ExpenseAttachment(
                name = preparedFile.fileName,
                contentType = preparedFile.contentType,
                sizeBytes = preparedFile.sizeBytes,
                storagePath = storagePath,
                uploadedByUid = uploadedByUid,
                uploadedAt = Timestamp.now()
            )

            Result.success(attachment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a file from Storage.
     */
    suspend fun deleteAttachment(storagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (storagePath.isNotBlank()) {
                val storage = Firebase.storage
                storage.reference.child(storagePath).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtains download URL for an attachment in Storage.
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storage = Firebase.storage
            val uri = storage.reference.child(storagePath).downloadUrl.await()
            Result.success(uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a PDF file to app cache and opens it with system PDF viewer via FileProvider.
     */
    suspend fun openPdf(context: Context, attachment: ExpenseAttachment): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val storage = Firebase.storage
                val storageRef = storage.reference.child(attachment.storagePath)

                val cacheDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
                val cleanName = attachment.name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val localFile = File(cacheDir, cleanName)

                // Download bytes from Firebase Storage
                val maxDownloadBytes = 15L * 1024L * 1024L
                val bytes = storageRef.getBytes(maxDownloadBytes).await()
                FileOutputStream(localFile).use { it.write(bytes) }

                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        localFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // Some modern Android devices return null for resolveActivity with ACTION_VIEW,
                        // so we try starting activity inside a try/catch
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            throw IllegalStateException("No application found to view PDF. Please install a PDF reader.")
                        }
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
