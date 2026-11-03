package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.repository.UserRepository
import com.example.data.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PartnerLedgerMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token received: $token")
        saveCachedToken(applicationContext, token)

        val currentUid = Firebase.auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            serviceScope.launch {
                val userRepo = UserRepository(applicationContext)
                userRepo.addFcmToken(currentUid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Payment Update"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["summary"]
            ?: "New activity in your project."

        val projectId = remoteMessage.data["projectId"]

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            projectId = projectId
        )
    }

    companion object {
        private const val PREFS_NAME = "partner_ledger_fcm"
        private const val KEY_TOKEN = "cached_fcm_token"

        fun saveCachedToken(context: Context, token: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply()
        }

        fun getCachedToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null)
        }

        suspend fun syncCurrentToken(context: Context, uid: String): String? {
            return try {
                val token = FirebaseMessaging.getInstance().token.await()
                saveCachedToken(context, token)
                val userRepo = UserRepository(context)
                userRepo.addFcmToken(uid, token)
                token
            } catch (e: Exception) {
                Log.w("FCM", "Could not fetch or sync FCM token: ${e.message}")
                null
            }
        }

        suspend fun removeCurrentToken(context: Context, uid: String) {
            try {
                val token = getCachedToken(context) ?: try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (_: Exception) {
                    null
                }

                if (!token.isNullOrBlank() && uid.isNotBlank()) {
                    val userRepo = UserRepository(context)
                    userRepo.removeFcmToken(uid, token)
                }
            } catch (e: Exception) {
                Log.w("FCM", "Error removing FCM token on sign-out: ${e.message}")
            }
        }
    }
}
