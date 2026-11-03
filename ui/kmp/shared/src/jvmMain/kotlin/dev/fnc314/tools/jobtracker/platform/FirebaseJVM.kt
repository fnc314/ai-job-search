package dev.fnc314.tools.jobtracker.platform

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initializes Firebase Admin SDK for JVM (Desktop).
 * Uses Application Default Credentials (ADC) - the same credentials
 * used by the Firebase CLI (gcloud auth application-default login).
 */
object FirebaseJVM {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        try {
            // Use Application Default Credentials (ADC)
            // This reads from GOOGLE_APPLICATION_CREDENTIALS env var or
            // the default gcloud credentials location
            val credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("job-search-fnc314-dev")
                .build()

            if (FirebaseApp.getApps().none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                FirebaseApp.initializeApp(options)
            }
            initialized = true
            println("Firebase initialized successfully for JVM")
        } catch (e: Exception) {
            println("Failed to initialize Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}
