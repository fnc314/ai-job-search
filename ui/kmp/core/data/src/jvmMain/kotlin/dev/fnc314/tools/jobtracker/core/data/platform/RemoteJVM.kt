package dev.fnc314.tools.jobtracker.core.data.platform

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initializes remote backend SDK for JVM (Desktop).
 * Uses Application Default Credentials (ADC).
 */
internal object RemoteJVM {
    private var initialized: Boolean = false

    fun initialize(): Unit {
        if (initialized) return
        try {
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
        } catch (e: Exception) {
            println("Failed to initialize Remote JVM client: ${e.message}")
        }
    }
}
