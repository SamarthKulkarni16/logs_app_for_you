package com.samarth.logsapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter both email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { authRepository.signInWithEmail(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign in failed") }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter both email and password")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { authRepository.signUpWithEmail(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                val result = googleSignInHelper.requestGoogleIdToken()
                authRepository.signInWithGoogleIdToken(result.idToken, result.rawNonce)
            }
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in failed") }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
