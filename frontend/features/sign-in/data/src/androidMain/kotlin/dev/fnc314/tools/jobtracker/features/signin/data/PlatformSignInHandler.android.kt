package dev.fnc314.tools.jobtracker.features.signin.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dev.fnc314.tools.jobtracker.core.data.ApplicationContextHolder
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import kotlinx.coroutines.tasks.await

private const val TAG = "PlatformSignInHandler"

public actual class PlatformSignInHandler(
    private val context: Context?,
    private val serverClientId: String? = null
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager: CredentialManager? by lazy {
        context?.let { CredentialManager.create(it) }
    }

    public actual suspend fun signIn(): Result<UserAccount> = runCatching {
        val ctx = context ?: throw IllegalStateException("Android application context must be initialized before using PlatformSignInHandler")
        val cm = credentialManager ?: throw IllegalStateException("CredentialManager not initialized")
        Log.d(TAG, "signIn: Starting sign-in flow")

        // Check if user is already signed in to Firebase
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser != null) {
            Log.i(TAG, "signIn: User already authenticated in Firebase: ${currentFirebaseUser.uid}")
            return@runCatching UserAccount(
                uid = currentFirebaseUser.uid,
                email = currentFirebaseUser.email,
                displayName = currentFirebaseUser.displayName,
                photoUrl = currentFirebaseUser.photoUrl?.toString()
            )
        }
        Log.d(TAG, "signIn: No existing Firebase session, proceeding with Google sign-in")

        // Web OAuth client ID (client_type 3) from google-services.json
        val clientId = serverClientId ?: "399944190604-dpasrf0mis9860nvp74m6dc98cp5rphq.apps.googleusercontent.com"
        Log.d(TAG, "signIn: Using server client ID: $clientId")

        // Use GetSignInWithGoogleOption for explicit "Sign in with Google" button flows.
        // This avoids the known TransactionTooLargeException bug on Android 14+ with
        // multiple Google accounts when using GetGoogleIdOption.
        // See: https://issuetracker.google.com/issues/341690734
        val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        Log.d(TAG, "signIn: Requesting credential from Credential Manager")
        val result = try {
            cm.getCredential(
                request = request,
                context = ctx
            ).also { 
                Log.d(TAG, "signIn: Credential Manager returned credential type: ${it.credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "signIn: User cancelled the sign-in flow", e)
            throw e
        } catch (e: NoCredentialException) {
            Log.w(TAG, "signIn: No credentials available on this device", e)
            throw e
        } catch (e: GetCredentialException) {
            Log.e(TAG, "signIn: Credential Manager error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "signIn: Unexpected error during credential retrieval", e)
            throw e
        }

        val credential = result.credential
        exchangeCredentialForAccount(credential)
    }

    private suspend fun exchangeCredentialForAccount(credential: androidx.credentials.Credential): UserAccount {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.d(TAG, "signIn: Received Google ID token credential")
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            Log.d(TAG, "signIn: Exchanging Google ID token for Firebase credential")

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = try {
                auth.signInWithCredential(authCredential).await().also {
                    Log.i(TAG, "signIn: Firebase authentication successful for user: ${it.user?.uid}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "signIn: Firebase authentication failed", e)
                throw e
            }
            val user = checkNotNull(authResult.user) { "Firebase User is null after sign in" }

            return UserAccount(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )
        } else {
            Log.e(TAG, "signIn: Unexpected credential type: ${credential.type}")
            throw IllegalStateException("Unexpected credential type: ${credential.type}")
        }
    }

    public actual suspend fun getCurrentUser(): UserAccount? {
        val user = auth.currentUser
        return if (user != null) {
            Log.d(TAG, "getCurrentUser: Found authenticated user: ${user.uid}")
            UserAccount(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )
        } else {
            Log.d(TAG, "getCurrentUser: No authenticated user")
            null
        }
    }

    public actual suspend fun signOut(): Result<Unit> = runCatching {
        Log.d(TAG, "signOut: Signing out from Firebase")
        auth.signOut()
        val cm = credentialManager
        if (cm != null) {
            Log.d(TAG, "signOut: Clearing credential state")
            cm.clearCredentialState(ClearCredentialStateRequest())
        }
        Log.i(TAG, "signOut: Sign out complete")
    }
}

public actual fun createPlatformSignInHandler(): PlatformSignInHandler {
    val context = ApplicationContextHolder.application
    return PlatformSignInHandler(context)
}
