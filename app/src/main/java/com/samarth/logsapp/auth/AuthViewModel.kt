package com.samarth.logsapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    /** Emitted right after a successful sign-in/sign-up so the UI can show the welcome animation. */
    data class Success(val isNewAccount: Boolean) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    val currentUserEmail: String?
        get() = authRepository.currentUser()?.email

    fun signOut() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
        }
    }

    /**
     * Single entry point for email auth. Tries signing in first; if that fails
     * (most likely because no account exists yet with this email), attempts to
     * create one. There's no separate "sign up" mode - the user just enters
     * their details and continues.
     */
    fun continueWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter both email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { authRepository.signInWithEmail(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Success(isNewAccount = false) }
                .onFailure {
                    if (password.length < 6) {
                        _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                        return@launch
                    }
                    runCatching { authRepository.signUpWithEmail(email, password) }
                        .onSuccess {
                            if (authRepository.currentUserId() != null) {
                                _uiState.value = AuthUiState.Success(isNewAccount = true)
                            } else {
                                _uiState.value = AuthUiState.Error("Check your email to confirm your account, then continue")
                            }
                        }
                        .onFailure {
                            _uiState.value = AuthUiState.Error("Incorrect password")
                        }
                }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                val result = googleSignInHelper.requestGoogleIdToken()
                authRepository.signInWithGoogleIdToken(result.idToken, result.rawNonce)
            }
                .onSuccess { _uiState.value = AuthUiState.Success(isNewAccount = isNewAccount(authRepository.currentUser())) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in failed") }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /** Called once the welcome animation finishes playing. */
    fun completeAuthFlow() {
        if (_uiState.value is AuthUiState.Success) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /** A user is "new" if their account was created within a few seconds of this sign-in. */
    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun isNewAccount(user: UserInfo?): Boolean {
        val created = user?.createdAt ?: return false
        val lastSignIn = user.lastSignInAt ?: return true
        return (lastSignIn - created).inWholeSeconds < 5
    }
}
