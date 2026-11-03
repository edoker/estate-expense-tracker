package com.example.data.repository

import android.content.Context
import android.util.Patterns
import com.example.R
import com.example.data.model.Invite
import com.example.data.model.Project
import com.example.data.model.ProjectMember
import com.example.data.model.User
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProjectRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing projects.")
    }

    fun observeUserProjects(userId: String): Flow<List<Project>> = flow {
        if (auth.currentUser == null || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "projects"
        db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val name = doc.getString("name") ?: ""
                        val adminUid = doc.getString("adminUid") ?: ""
                        val memberUids = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val rawMembers = doc.get("members") as? Map<*, *> ?: emptyMap<Any, Any>()
                        val membersMap = rawMembers.mapNotNull { (key, value) ->
                            val uid = key as? String ?: return@mapNotNull null
                            val memberData = value as? Map<*, *> ?: return@mapNotNull null
                            val memName = memberData["name"] as? String ?: ""
                            val memEmail = memberData["email"] as? String ?: ""
                            val memRole = memberData["role"] as? String ?: "member"
                            uid to ProjectMember(name = memName, email = memEmail, role = memRole)
                        }.toMap()
                        val createdAt = doc.getTimestamp("createdAt")

                        Project(
                            id = doc.id,
                            name = name,
                            adminUid = adminUid,
                            memberUids = memberUids,
                            members = membersMap,
                            createdAt = createdAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .catch { error ->
                if (auth.currentUser != null && error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeProject(projectId: String): Flow<Project?> = flow {
        if (auth.currentUser == null || projectId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "projects/$projectId"
        db.collection("projects").document(projectId)
            .snapshots()
            .map { doc ->
                if (!doc.exists()) return@map null
                try {
                    val name = doc.getString("name") ?: ""
                    val adminUid = doc.getString("adminUid") ?: ""
                    val memberUids = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val rawMembers = doc.get("members") as? Map<*, *> ?: emptyMap<Any, Any>()
                    val membersMap = rawMembers.mapNotNull { (key, value) ->
                        val uid = key as? String ?: return@mapNotNull null
                        val memberData = value as? Map<*, *> ?: return@mapNotNull null
                        val memName = memberData["name"] as? String ?: ""
                        val memEmail = memberData["email"] as? String ?: ""
                        val memRole = memberData["role"] as? String ?: "member"
                        uid to ProjectMember(name = memName, email = memEmail, role = memRole)
                    }.toMap()
                    val createdAt = doc.getTimestamp("createdAt")

                    Project(
                        id = doc.id,
                        name = name,
                        adminUid = adminUid,
                        memberUids = memberUids,
                        members = membersMap,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .catch { error ->
                if (auth.currentUser != null && error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun createProject(name: String, user: User): Result<String> {
        val uid = requireUserId()
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Project name cannot be empty"))
        }

        val projectsRef = db.collection("projects")
        val newDoc = projectsRef.document()
        val data = hashMapOf<String, Any?>(
            "name" to trimmedName,
            "adminUid" to uid,
            "memberUids" to listOf(uid),
            "members" to mapOf(
                uid to mapOf(
                    "name" to user.displayName.ifBlank { "Partner" },
                    "email" to user.email.lowercase().trim(),
                    "role" to "admin"
                )
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            newDoc.set(data).await()
            Result.success(newDoc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, newDoc.path)
            Result.failure(e)
        }
    }

    fun observePendingInvitesForEmail(email: String): Flow<List<Invite>> = flow {
        val cleanEmail = email.lowercase().trim()
        if (auth.currentUser == null || cleanEmail.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "invites"
        db.collection(path)
            .whereEqualTo("invitedEmail", cleanEmail)
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Invite::class.java)
            }
            .catch { error ->
                if (auth.currentUser != null && error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observePendingInvitesForProject(projectId: String): Flow<List<Invite>> = flow {
        if (auth.currentUser == null || projectId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "invites"
        db.collection(path)
            .whereEqualTo("projectId", projectId)
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Invite::class.java)
            }
            .catch { error ->
                if (auth.currentUser != null && error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun invitePartner(
        projectId: String,
        projectName: String,
        invitedEmail: String,
        inviter: User,
        currentMembers: Map<String, ProjectMember>
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanEmail = invitedEmail.lowercase().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        // Check if already a member
        val isAlreadyMember = currentMembers.values.any { it.email.lowercase().trim() == cleanEmail }
        if (isAlreadyMember) {
            return Result.failure(IllegalStateException("User is already a member of this project"))
        }

        val inviteDocId = "${projectId}_${cleanEmail}"
        val inviteRef = db.collection("invites").document(inviteDocId)

        return try {
            val inviteData = hashMapOf<String, Any?>(
                "projectId" to projectId,
                "projectName" to projectName,
                "invitedEmail" to cleanEmail,
                "invitedByUid" to uid,
                "invitedByName" to inviter.displayName.ifBlank { "Partner" },
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )

            inviteRef.set(inviteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(invite: Invite, user: User): Result<Unit> {
        val uid = requireUserId()
        val projectRef = db.collection("projects").document(invite.projectId)
        val inviteRef = db.collection("invites").document(invite.id)

        return try {
            // Update project with new member
            val memberData = mapOf(
                "name" to user.displayName.ifBlank { "Partner" },
                "email" to user.email.lowercase().trim(),
                "role" to "member"
            )

            val batch = db.batch()
            batch.update(
                projectRef,
                "memberUids", FieldValue.arrayUnion(uid),
                "members.$uid", memberData
            )
            batch.update(inviteRef, "status", "accepted")
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, projectRef.path)
            Result.failure(e)
        }
    }

    suspend fun declineInvite(inviteId: String): Result<Unit> {
        requireUserId()
        val inviteRef = db.collection("invites").document(inviteId)
        return try {
            inviteRef.update("status", "declined").await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelInvite(inviteId: String): Result<Unit> {
        requireUserId()
        val inviteRef = db.collection("invites").document(inviteId)
        return try {
            inviteRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeMember(projectId: String, targetUid: String): Result<Unit> {
        val uid = requireUserId()
        if (targetUid == uid) {
            return Result.failure(IllegalStateException("Admins cannot remove themselves."))
        }
        val projectRef = db.collection("projects").document(projectId)
        return try {
            val snapshot = projectRef.get().await()
            val adminUid = snapshot.getString("adminUid")
            if (adminUid != uid) {
                return Result.failure(IllegalStateException("Only the project admin can remove members."))
            }
            if (targetUid == adminUid) {
                return Result.failure(IllegalStateException("Cannot remove the project admin."))
            }

            projectRef.update(
                "memberUids", FieldValue.arrayRemove(targetUid),
                "members.$targetUid", FieldValue.delete()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projectRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val projectRef = db.collection("projects").document(projectId)
        return try {
            val snapshot = projectRef.get().await()
            val adminUid = snapshot.getString("adminUid")
            if (adminUid != uid) {
                return Result.failure(IllegalStateException("Only the project admin can delete this project."))
            }
            projectRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, projectRef.path)
            Result.failure(e)
        }
    }
}
