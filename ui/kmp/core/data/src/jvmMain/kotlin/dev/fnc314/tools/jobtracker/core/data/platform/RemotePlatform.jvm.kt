package dev.fnc314.tools.jobtracker.core.data.platform

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File
import java.io.FileInputStream

/**
 * Initializes remote backend SDK for Desktop.
 * Resolves credentials in canonical order:
 * 1. TRACKER_SA_KEY environment variable
 * 2. ~/.config/job-tracker/sa.json canonical file
 * 3. GOOGLE_APPLICATION_CREDENTIALS environment variable
 * 4. Application Default Credentials (ADC)
 */
internal object RemotePlatform {
    private var initialized: Boolean = false

    fun initialize(): Unit {
        if (initialized) return
        try {
            val saKeyEnv = System.getenv("TRACKER_SA_KEY")
            val canonicalSaFile = File(System.getProperty("user.home"), ".config/job-tracker/sa.json")
            val gAppCredsEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")

            val credentials = when {
                !saKeyEnv.isNullOrBlank() && File(saKeyEnv).exists() -> {
                    FileInputStream(File(saKeyEnv)).use { GoogleCredentials.fromStream(it) }
                }
                canonicalSaFile.exists() -> {
                    FileInputStream(canonicalSaFile).use { GoogleCredentials.fromStream(it) }
                }
                !gAppCredsEnv.isNullOrBlank() && File(gAppCredsEnv).exists() -> {
                    FileInputStream(File(gAppCredsEnv)).use { GoogleCredentials.fromStream(it) }
                }
                else -> {
                    GoogleCredentials.getApplicationDefault()
                }
            }.createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

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
