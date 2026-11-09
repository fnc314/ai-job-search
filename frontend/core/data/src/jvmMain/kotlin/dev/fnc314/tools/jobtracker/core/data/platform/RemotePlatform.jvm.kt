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
public object RemotePlatform {
    private var initialized: Boolean = false

    public fun initialize(): Unit {
        if (initialized) return
        try {
            val credentials = resolveCredentials().createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            val projectId = resolveProjectId()

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

            if (FirebaseApp.getApps().none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                FirebaseApp.initializeApp(options)
            }
            initialized = true
        } catch (e: Exception) {
            println("Failed to initialize Remote JVM client: ${e.message}")
        }
    }

    private fun resolveCredentials(): GoogleCredentials {
        val saKeyEnv = System.getenv("TRACKER_SA_KEY")
        if (!saKeyEnv.isNullOrBlank() && File(saKeyEnv).exists()) {
            return FileInputStream(File(saKeyEnv)).use { GoogleCredentials.fromStream(it) }
        }
        val canonicalSaFile = File(System.getProperty("user.home"), ".config/job-tracker/sa.json")
        if (canonicalSaFile.exists()) {
            return FileInputStream(canonicalSaFile).use { GoogleCredentials.fromStream(it) }
        }
        val gAppCredsEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if (!gAppCredsEnv.isNullOrBlank() && File(gAppCredsEnv).exists()) {
            return FileInputStream(File(gAppCredsEnv)).use { GoogleCredentials.fromStream(it) }
        }
        return GoogleCredentials.getApplicationDefault()
    }

    private fun resolveProjectId(): String {
        return System.getenv("TRACKER_PROJECT")
            ?: System.getenv("REMOTE_GCP_PROJECT")
            ?: "job-search-fnc314-dev"
    }
}
