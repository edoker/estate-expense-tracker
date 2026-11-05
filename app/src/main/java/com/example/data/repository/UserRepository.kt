package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.User
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun syncUserProfile(firebaseUser: FirebaseUser): Result<User> {
        val uid = firebaseUser.uid
        val docRef = db.collection("users").document(uid)

        val emailLower = (firebaseUser.email ?: "").lowercase().trim()
        val defaultGoogleName = firebaseUser.displayName?.ifBlank { null }
            ?: (if (emailLower.contains("@")) emailLower.substringBefore("@") else "Partner")
        val photoUrl = firebaseUser.photoUrl?.toString()

        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                // First login: pre-populate displayName with Google auth, but marked as not yet confirmed
                val initialData = hashMapOf<String, Any?>(
                    "displayName" to defaultGoogleName,
                    "email" to emailLower,
                    "photoUrl" to photoUrl,
                    "isNameConfirmed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "fcmTokens" to emptyList<String>()
                )
                docRef.set(initialData).await()
            } else {
                // Subsequent login: preserve existing/confirmed displayName, do not overwrite with Google auth
                val existingName = snapshot.getString("displayName")
                val isNameConfirmed = snapshot.getBoolean("isNameConfirmed") ?: false

                val updates = hashMapOf<String, Any?>(
                    "email" to emailLower,
                    "photoUrl" to photoUrl,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )

                // Only set displayName if it was completely blank
                if (existingName.isNullOrBlank()) {
                    updates["displayName"] = defaultGoogleName
                    updates["isNameConfirmed"] = false
                }

                docRef.update(updates).await()
            }

            // Fetch the updated document
            val updatedSnapshot = docRef.get().await()
            val user = updatedSnapshot.toObject(User::class.java)
                ?: User(
                    uid = uid,
                    displayName = defaultGoogleName,
                    email = emailLower,
                    photoUrl = photoUrl,
                    isNameConfirmed = false
                )
            Result.success(user)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(uid: String, newName: String): Result<Unit> {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update(
                "displayName", cleanName,
                "isNameConfirmed", true
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<User?> = flow {
        if (auth.currentUser == null || uid.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users").document(uid)
        docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(User::class.java) else null
            }
            .catch { e ->
                if (auth.currentUser != null && e is Exception) {
                    handleFirestoreError(e, OperationType.GET, docRef.path)
                }
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun addFcmToken(uid: String, token: String): Result<Unit> {
        if (uid.isBlank() || token.isBlank()) return Result.success(Unit)
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update("fcmTokens", FieldValue.arrayUnion(token)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFcmToken(uid: String, token: String): Result<Unit> {
        if (uid.isBlank() || token.isBlank()) return Result.success(Unit)
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update("fcmTokens", FieldValue.arrayRemove(token)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
