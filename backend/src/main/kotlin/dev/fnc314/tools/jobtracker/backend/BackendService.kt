package dev.fnc314.tools.jobtracker.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

public object BackendService {
    private var initialized = false

    public val firestore: Firestore by lazy {
        initFirebase()
        FirestoreClient.getFirestore(FirebaseApp.getInstance())
    }

    private fun initFirebase() {
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

            val projectId = System.getenv("TRACKER_PROJECT")
                ?: System.getenv("REMOTE_GCP_PROJECT")
                ?: "job-search-fnc314-dev"

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

            if (FirebaseApp.getApps().none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                FirebaseApp.initializeApp(options)
            }
            initialized = true
        } catch (e: Exception) {
            println("Warning: Firebase initialization in BackendService: ${e.message}")
        }
    }

    public fun expireStaleProposals(expirationDays: Long = 14): Int {
        val db = firestore
        val cutoff = Timestamp.ofTimeSecondsAndNanos(
            Instant.now().minus(expirationDays, ChronoUnit.DAYS).epochSecond,
            0
        )
        val proposals = db.collection("pending-proposals")
            .whereEqualTo("state", "PENDING")
            .whereLessThan("createdAt", cutoff)
            .get()
            .get()

        var count = 0
        val now = Timestamp.now()
        for (doc in proposals.documents) {
            doc.reference.update(
                mapOf(
                    "state" to "EXPIRED",
                    "updatedAt" to now
                )
            ).get()
            count++
        }
        return count
    }

    public fun processDueSyncs(): Int {
        val db = firestore
        val now = Timestamp.now()
        val syncs = db.collection("sync-requests")
            .whereEqualTo("state", "PENDING")
            .whereLessThanOrEqualTo("dueAt", now)
            .get()
            .get()

        var count = 0
        for (doc in syncs.documents) {
            doc.reference.update(
                mapOf(
                    "state" to "DONE",
                    "processedAt" to now
                )
            ).get()
            count++
        }
        return count
    }

    public fun performRoutineSweep(): Map<String, Any> {
        val expiredCount = expireStaleProposals()
        val syncCount = processDueSyncs()

        val db = firestore
        val sweepTimestamp = Timestamp.now()
        db.collection("meta").document("sync").set(
            mapOf(
                "lastSweepAt" to sweepTimestamp,
                "staleProposalsExpired" to expiredCount,
                "dueSyncsProcessed" to syncCount,
                "actor" to "CLOUD_RUN_SWEEP"
            )
        ).get()

        return mapOf(
            "status" to "SUCCESS",
            "message" to "Routine sweep completed successfully",
            "sweepAt" to sweepTimestamp.toString(),
            "staleProposalsExpired" to expiredCount,
            "dueSyncsProcessed" to syncCount
        )
    }
}
