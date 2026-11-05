package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.User
import com.example.data.repository.UserRepository
import com.example.data.service.PartnerLedgerMessagingService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    data object Loading : AuthState
    data class Unauthenticated(val errorMessage: String? = null) : AuthState
    data class Authenticated(val user: User) : AuthState
}

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            if (_authState.value !is AuthState.Unauthenticated) {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        checkCurrentSession()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun checkCurrentSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val result = userRepository.syncUserProfile(currentUser)
                result.onSuccess { user ->
                    _authState.value = AuthState.Authenticated(user)
                }.onFailure { error ->
                    Log.e("AuthViewModel", "Failed to sync user on resume: ${error.message}", error)
                    // Even if sync has network lag, show authenticated with fallback info
                    val fallbackUser = User(
                        uid = currentUser.uid,
                        displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Partner",
                        email = currentUser.email?.lowercase() ?: "",
                        photoUrl = currentUser.photoUrl?.toString()
                    )
                    _authState.value = AuthState.Authenticated(fallbackUser)
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun attemptAutoSignIn(context: Context, credentialManager: CredentialManager) {
        if (auth.currentUser != null) {
            checkCurrentSession()
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("AuthViewModel", "default_web_client_id not found: ${e.message}")
            _authState.value = AuthState.Unauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val syncResult = userRepository.syncUserProfile(firebaseUser)
                        syncResult.onSuccess { user ->
                            _authState.value = AuthState.Authenticated(user)
                        }.onFailure {
                            _authState.value = AuthState.Unauthenticated("Failed to sync profile: ${it.localizedMessage}")
                        }
                    } else {
                        _authState.value = AuthState.Unauthenticated("Sign in failed")
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated()
                }
            } catch (e: Exception) {
                // Auto sign-in silent fail is normal for first time / unlinked account
                Log.d("AuthViewModel", "Auto sign-in silent fallback: ${e.message}")
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        _authState.value = AuthState.Loading

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val activity = context as? Activity
        if (activity == null) {
            _authState.value = AuthState.Unauthenticated("UI Context is not an Activity")
            return
        }

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val syncResult = userRepository.syncUserProfile(firebaseUser)
                        syncResult.onSuccess { user ->
                            _authState.value = AuthState.Authenticated(user)
                        }.onFailure {
                            _authState.value = AuthState.Unauthenticated("Profile sync failed: ${it.localizedMessage}")
                        }
                    } else {
                        _authState.value = AuthState.Unauthenticated("Google authentication returned empty user")
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated("Unexpected credential type returned")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                _authState.value = AuthState.Unauthenticated(e.localizedMessage ?: "Sign in failed. Please try again.")
            }
        }
    }

    fun syncFcmToken(context: Context, uid: String) {
        viewModelScope.launch {
            PartnerLedgerMessagingService.syncCurrentToken(context.applicationContext, uid)
        }
    }

    fun signOut(context: Context, credentialManager: CredentialManager) {
        val currentUid = auth.currentUser?.uid
        _authState.value = AuthState.Unauthenticated()
        viewModelScope.launch {
            if (!currentUid.isNullOrBlank()) {
                PartnerLedgerMessagingService.removeCurrentToken(context.applicationContext, currentUid)
            }
            auth.signOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            }
        }
    }

    fun updateDisplayName(
        newName: String,
        projectRepository: com.example.data.repository.ProjectRepository? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val currentAuth = _authState.value
        if (currentAuth !is AuthState.Authenticated) {
            onComplete?.invoke(false, "Not signed in")
            return
        }
        val cleanName = newName.trim()
        if (cleanName.isBlank()) {
            onComplete?.invoke(false, "Name cannot be empty")
            return
        }

        viewModelScope.launch {
            val result = userRepository.updateDisplayName(currentAuth.user.uid, cleanName)
            result.onSuccess {
                val updatedUser = currentAuth.user.copy(
                    displayName = cleanName,
                    isNameConfirmed = true
                )
                _authState.value = AuthState.Authenticated(updatedUser)

                // Sync new name across all projects in Firestore
                try {
                    projectRepository?.updateMemberNameAcrossProjects(currentAuth.user.uid, cleanName)
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Failed to sync name to projects: ${e.message}")
                }

                onComplete?.invoke(true, null)
            }.onFailure { error ->
                Log.e("AuthViewModel", "Failed to update display name", error)
                onComplete?.invoke(false, error.localizedMessage ?: "Failed to update name")
            }
        }
    }

    fun dismissError() {
        if (_authState.value is AuthState.Unauthenticated) {
            _authState.value = AuthState.Unauthenticated(null)
        }
    }
}
