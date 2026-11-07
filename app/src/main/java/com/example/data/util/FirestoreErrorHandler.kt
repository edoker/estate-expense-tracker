package com.example.data.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val currentUser = try {
        Firebase.auth.currentUser
    } catch (e: Exception) {
        null
    }

    // If currentUser is null, this is an unauthenticated query attempt or a listener cancelled on sign-out.
    // Logging at ERROR level with tag "FirestoreError" triggers automated test/monitor alarms.
    if (currentUser == null) {
        Log.w("FirestoreError", "Ignored Firestore operation on '$path' while unauthenticated: ${exception.message}")
        return exception.message ?: "Unauthenticated"
    }

    val providerInfoList = currentUser.providerData.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    }

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser.uid)
        put("email", currentUser.email)
        put("emailVerified", currentUser.isEmailVerified)
        put("tenantId", currentUser.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    val isSnapshotListenerCancellation = (operationType == OperationType.GET || operationType == OperationType.LIST) &&
        (exception.message?.contains("snapshot", ignoreCase = true) == true ||
         exception.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
         exception.message?.contains("NOT_FOUND", ignoreCase = true) == true)

    if (isSnapshotListenerCancellation) {
        Log.w("FirestoreError", "Firestore Snapshot Closed: $jsonString")
    } else {
        Log.e("FirestoreError", "Firestore Error: $jsonString")
    }
    return jsonString
}
