package com.creazzy.novakeys.cloud

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.creazzy.novakeys.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Google identity plus opaque, client-encrypted backups in Firestore. */
object CloudBackupManager {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        if (listOf(BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_APP_ID,
                BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.GOOGLE_WEB_CLIENT_ID).any { it.isBlank() }) return false
        return runCatching {
            FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).build())
            true
        }.getOrDefault(false)
    }
    fun currentUser(context: Context): FirebaseUser? =
        if (initialize(context)) FirebaseAuth.getInstance().currentUser else null

    suspend fun signIn(activity: Activity): FirebaseUser {
        check(initialize(activity)) { "Firebase and Google OAuth are not configured by the app owner yet." }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google sign-in did not return a supported credential."
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .await().user ?: error("Google sign-in returned no user.")
    }
    fun signOut(context: Context) { if (initialize(context)) FirebaseAuth.getInstance().signOut() }
    suspend fun uploadEncryptedBackup(uid: String, encryptedBackup: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).set(
            mapOf("encryptedBackup" to encryptedBackup, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }
    suspend fun downloadEncryptedBackup(uid: String): String? =
        FirebaseFirestore.getInstance().collection("users").document(uid).get().await().getString("encryptedBackup")
}
