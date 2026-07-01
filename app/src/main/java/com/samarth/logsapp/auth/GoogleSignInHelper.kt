package com.samarth.logsapp.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.security.SecureRandom

/** Result of a successful Google sign-in request via Credential Manager. */
data class GoogleIdTokenResult(
    val idToken: String,
    /** The *raw* (un-hashed) nonce — pass this to Supabase's signInWith(IDToken). */
    val rawNonce: String
)

/**
 * Requests a Google ID token via Android's Credential Manager.
 *
 * Uses the GOOGLE_WEB_CLIENT_ID (not the Android client ID) as the server
 * client ID — this is the same Web client ID already configured as the
 * Google provider on the shared Supabase project (used by Flow Timer too).
 * Google resolves the Android client ID automatically from the app's
 * package name + signing certificate, so it never needs to appear in code.
 *
 * Per Supabase's own integration example: Google's GetGoogleIdOption needs
 * the SHA-256 *hash* of the nonce, but Supabase verifies the ID token
 * against the *raw* nonce — so both values are generated here and the raw
 * one is returned for the caller to pass along to signInWith(IDToken).
 */
class GoogleSignInHelper(private val context: Context, private val webClientId: String) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun requestGoogleIdToken(): GoogleIdTokenResult {
        val rawNonce = generateRawNonce()
        val hashedNonce = sha256Hash(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw IllegalStateException("Failed to parse Google ID token", e)
            }
            return GoogleIdTokenResult(idToken = idToken, rawNonce = rawNonce)
        }
        throw IllegalStateException("Unexpected credential type returned")
    }

    private fun generateRawNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

