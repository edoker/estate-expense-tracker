package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ExpenseAttachment(
    val name: String = "",
    val contentType: String = "",
    val sizeBytes: Long = 0L,
    val storagePath: String = "",
    val uploadedByUid: String = "",
    val uploadedAt: Timestamp? = null
) {
    val isImage: Boolean
        get() = contentType.startsWith("image/")

    val isPdf: Boolean
        get() = contentType.equals("application/pdf", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true)

    val readableSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = sizeBytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return String.format("%.1f %s", size, units[unitIndex])
        }

    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "name" to name,
            "contentType" to contentType,
            "sizeBytes" to sizeBytes,
            "storagePath" to storagePath,
            "uploadedByUid" to uploadedByUid,
            "uploadedAt" to (uploadedAt ?: Timestamp.now())
        )
    }

    companion object {
        fun fromMap(map: Map<*, *>): ExpenseAttachment? {
            return try {
                ExpenseAttachment(
                    name = map["name"] as? String ?: "Attachment",
                    contentType = map["contentType"] as? String ?: "application/octet-stream",
                    sizeBytes = (map["sizeBytes"] as? Number)?.toLong() ?: 0L,
                    storagePath = map["storagePath"] as? String ?: "",
                    uploadedByUid = map["uploadedByUid"] as? String ?: "",
                    uploadedAt = map["uploadedAt"] as? Timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
