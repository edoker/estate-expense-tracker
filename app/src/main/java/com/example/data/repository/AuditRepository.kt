package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Audit
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AuditRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing audit trail.")
    }

    // Read-only observation of audit records, sorted newest first
    fun observeProjectAudit(projectId: String): Flow<List<Audit>> = flow {
        if (auth.currentUser == null || projectId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "projects/$projectId/audit"
        db.collection("projects").document(projectId).collection("audit")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val action = doc.getString("action") ?: "created"
                        val entityType = doc.getString("entityType") ?: "expense"
                        val entityId = doc.getString("entityId") ?: ""
                        val actorUid = doc.getString("actorUid") ?: ""
                        val actorName = doc.getString("actorName") ?: "Partner"
                        val timestamp = doc.getTimestamp("timestamp")
                        val summary = doc.getString("summary") ?: ""

                        @Suppress("UNCHECKED_CAST")
                        val changes = (doc.get("changes") as? Map<String, *>)?.mapNotNull { (key, value) ->
                            val changeMap = value as? Map<String, Any?>
                            if (changeMap != null) key to changeMap else null
                        }?.toMap() ?: emptyMap()

                        @Suppress("UNCHECKED_CAST")
                        val snapshotData = (doc.get("snapshot") as? Map<String, Any?>) ?: emptyMap()

                        Audit(
                            id = doc.id,
                            action = action,
                            entityType = entityType,
                            entityId = entityId,
                            actorUid = actorUid,
                            actorName = actorName,
                            timestamp = timestamp,
                            summary = summary,
                            changes = changes,
                            snapshot = snapshotData
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp?.seconds ?: 0L }
            }
            .catch { error ->
                if (!ProjectRepository.deletedProjectIds.contains(projectId) && auth.currentUser != null && error is Exception) {
                    val msg = error.message.orEmpty()
                    if (!msg.contains("PERMISSION_DENIED", ignoreCase = true) && !msg.contains("snapshot", ignoreCase = true)) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                }
                emit(emptyList())
            }
            .collect { emit(it) }
    }
}
