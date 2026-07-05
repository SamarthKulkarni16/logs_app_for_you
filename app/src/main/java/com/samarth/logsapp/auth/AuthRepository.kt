package com.samarth.logsapp.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper around Supabase Auth. Screens/ViewModels call into this
 * instead of touching the SupabaseClient directly, so the auth mechanism
 * (email/password vs Google ID token) can change without UI changes.
 */
class AuthRepository(private val supabase: SupabaseClient) {

    /** Current session state — emits new values automatically on sign-in/out. */
    val sessionStatus: StateFlow<SessionStatus>
        get() = supabase.auth.sessionStatus

    suspend fun signUpWithEmail(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Signs in using a Google ID token obtained via Android's Credential
     * Manager (see GoogleSignInHelper). `rawNonce` must be the same
     * un-hashed nonce whose SHA-256 hash was passed to GetGoogleIdOption —
     * Supabase verifies the token against the *raw* nonce, while Google's
     * Credential Manager API requires the *hashed* nonce. Supabase verifies
     * the token itself against the Google Web client ID configured on the
     * Auth > Providers > Google page of the Supabase project.
     */
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String) {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = rawNonce
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    fun currentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    fun currentUser() = supabase.auth.currentUserOrNull()
}

