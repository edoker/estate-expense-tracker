package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.UserRepository
import com.example.data.util.NotificationHelper
import com.example.navigation.AppNavigation
import com.example.ui.auth.AuthViewModel
import com.example.ui.theme.PartnerLedgerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val targetProjectIdState = MutableStateFlow<String?>(null)

    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val userRepository = UserRepository(applicationContext)
                return AuthViewModel(userRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel
        NotificationHelper.createNotificationChannel(this)

        // Capture any deep-linked project ID from incoming notification
        intent?.getStringExtra("projectId")?.let {
            targetProjectIdState.value = it
        }

        val credentialManager = CredentialManager.create(this)

        setContent {
            PartnerLedgerTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    credentialManager = credentialManager,
                    targetProjectIdFlow = targetProjectIdState
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("projectId")?.let {
            targetProjectIdState.value = it
        }
    }
}
