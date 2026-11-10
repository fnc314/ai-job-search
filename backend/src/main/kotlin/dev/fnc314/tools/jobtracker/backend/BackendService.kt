package dev.fnc314.tools.jobtracker.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobsearch.parser.CompanyMetricsParser
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.CompanyMetrics
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

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

    /**
     * Pure function to calculate [CompanyMetrics] from an array of application document maps.
     */
    @Suppress("UNCHECKED_CAST", "MagicNumber")
    public fun calculateMetricsForApplications(applications: List<Map<String, Any?>>): Map<String, Any> {
        var draft = 0
        var submitted = 0
        var interviewing = 0
        var offer = 0
        var rejected = 0

        val fitScores = mutableListOf<Int>()
        var minSalary: Double? = null
        var maxSalary: Double? = null
        var salaryCurrency = "USD"
        val channelCounts = mutableMapOf<String, Int>()
        val locations = linkedSetOf<String>()

        for (app in applications) {
            val status = (app["status"] as? String)?.uppercase() ?: ""
            when {
                status in listOf("DRAFTING", "LEAD") -> draft++
                status in listOf("APPLIED", "NO_RESPONSE") -> submitted++
                status in listOf("INTERVIEW") -> interviewing++
                status in listOf("OFFER_EXPECTED", "HIRED", "OFFER_DECLINED") -> offer++
                status in listOf("REJECTED", "APPLICATION_WITHDRAWN") -> rejected++
                else -> draft++ // Default unmapped to draft category
            }

            // Fit Rating
            val fitRating = (app["fitRating"] as? Number)?.toInt()
                ?: (app["fitScore"] as? Number)?.toInt()
            if (fitRating != null && fitRating >= 0) {
                fitScores.add(fitRating)
            }

            // Compensation
            val compMap = app["compensation"] as? Map<String, Any?>
            val appMin = (compMap?.get("min") as? Number)?.toDouble()
                ?: (app["compensationMin"] as? Number)?.toDouble()
            val appMax = (compMap?.get("max") as? Number)?.toDouble()
                ?: (app["compensationMax"] as? Number)?.toDouble()
            val appCurrency = (compMap?.get("currency") as? String)
                ?: (app["compensationCurrency"] as? String)
            if (!appCurrency.isNullOrBlank()) {
                salaryCurrency = appCurrency
            }

            if (appMin != null && appMin > 0) {
                minSalary = if (minSalary == null) appMin else minOf(minSalary, appMin)
            }
            if (appMax != null && appMax > 0) {
                maxSalary = if (maxSalary == null) appMax else maxOf(maxSalary, appMax)
            }

            // Channel
            val channelStr = (app["channel"] as? String)?.uppercase()
            if (!channelStr.isNullOrBlank()) {
                val cleanChannel = channelStr.removePrefix("CHANNEL:")
                channelCounts[cleanChannel] = (channelCounts[cleanChannel] ?: 0) + 1
            }

            // Locations
            val loc = (app["location"] as? String)?.trim()
            if (!loc.isNullOrBlank()) {
                locations.add(loc)
            }
        }

        val total = draft + submitted + interviewing + offer + rejected
        val percentages = if (total > 0) {
            mapOf(
                "draft" to (draft.toFloat() / total),
                "submitted" to (submitted.toFloat() / total),
                "interviewing" to (interviewing.toFloat() / total),
                "offer" to (offer.toFloat() / total),
                "rejected" to (rejected.toFloat() / total)
            )
        } else {
            mapOf(
                "draft" to 0f,
                "submitted" to 0f,
                "interviewing" to 0f,
                "offer" to 0f,
                "rejected" to 0f
            )
        }

        val avgFit = if (fitScores.isNotEmpty()) {
            fitScores.average().toInt()
        } else {
            null
        }

        val salaryRangeMap = mutableMapOf<String, Any>(
            "currency" to salaryCurrency,
            "display" to formatSalaryDisplay(minSalary, maxSalary, salaryCurrency)
        )
        if (minSalary != null) salaryRangeMap["min"] = minSalary
        if (maxSalary != null) salaryRangeMap["max"] = maxSalary

        val primaryChannel = channelCounts.maxByOrNull { it.value }?.key ?: "DIRECT"

        val protoBuilder = CompanyMetrics.newBuilder()
            .setCounts(
                CompanyMetrics.Counts.newBuilder()
                    .setDraft(draft)
                    .setSubmitted(submitted)
                    .setInterviewing(interviewing)
                    .setOffer(offer)
                    .setRejected(rejected)
                    .setTotal(total)
                    .build()
            )
            .setPercentages(
                CompanyMetrics.Percentages.newBuilder()
                    .setDraft(percentages["draft"] ?: 0f)
                    .setSubmitted(percentages["submitted"] ?: 0f)
                    .setInterviewing(percentages["interviewing"] ?: 0f)
                    .setOffer(percentages["offer"] ?: 0f)
                    .setRejected(percentages["rejected"] ?: 0f)
                    .build()
            )
            .setSalaryRange(
                CompanyMetrics.SalaryRange.newBuilder()
                    .setCurrency(salaryCurrency)
                    .setDisplay(salaryRangeMap["display"] as String)
                    .apply {
                        if (minSalary != null) setMin(minSalary)
                        if (maxSalary != null) setMax(maxSalary)
                    }
                    .build()
            )
            .addAllLocations(locations)
            .setLastCalculatedAt(
                com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(Instant.now().epochSecond)
                    .setNanos(0)
                    .build()
            )

        if (avgFit != null) {
            protoBuilder.setAverageFitScore(avgFit)
        }

        val channelEnum = try {
            ApplicationChannel.valueOf(primaryChannel)
        } catch (_: IllegalArgumentException) {
            ApplicationChannel.DIRECT
        }
        protoBuilder.setPrimaryChannel(channelEnum)

        return CompanyMetricsParser.toMap(protoBuilder.build())
    }

    private fun formatSalaryDisplay(min: Double?, max: Double?, currency: String): String {
        val symbol = if (currency.equals("USD", ignoreCase = true)) "$" else "$currency "
        return when {
            min != null && max != null && min != max -> {
                "${symbol}${formatNumber(min)} - ${symbol}${formatNumber(max)}"
            }
            min != null -> "${symbol}${formatNumber(min)}"
            max != null -> "${symbol}${formatNumber(max)}"
            else -> ""
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num >= 1000.0) {
            val inK = num / 1000.0
            if (inK == inK.toLong().toDouble()) {
                "${inK.toLong()}k"
            } else {
                String.format(Locale.ROOT, "%.1fk", inK)
            }
        } else {
            num.toLong().toString()
        }
    }

    /**
     * Recalculates metrics for all companies (or a specific [targetCompanyId])
     * and atomically writes them to Firestore `companies/{companyId}.metrics`.
     */
    public fun recalculateCompanyMetrics(targetCompanyId: String? = null): Map<String, Any> {
        val db = firestore
        var query = db.collection("applications") as com.google.cloud.firestore.Query
        if (!targetCompanyId.isNullOrBlank()) {
            query = query.whereEqualTo("companyId", targetCompanyId)
        }

        val snapshot = query.get().get()
        val appsByCompany = snapshot.documents.groupBy { doc ->
            doc.getString("companyId")?.takeIf { it.isNotBlank() }
                ?: doc.getString("company")?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')
                ?: "unknown"
        }

        var companiesUpdated = 0
        for ((companyId, docs) in appsByCompany) {
            if (companyId == "unknown") continue
            val appMaps = docs.map { it.data }
            val metricsMap = calculateMetricsForApplications(appMaps)

            val companyDoc = db.collection("companies").document(companyId)
            val companyName = docs.firstOrNull()?.getString("company") ?: companyId

            val updatePayload = mapOf(
                "companyId" to companyId,
                "name" to companyName,
                "metrics" to metricsMap,
                "updatedAt" to Timestamp.now()
            )

            companyDoc.set(updatePayload, SetOptions.merge()).get()
            companiesUpdated++
        }

        return mapOf(
            "status" to "SUCCESS",
            "companiesUpdated" to companiesUpdated,
            "targetCompanyId" to (targetCompanyId ?: "ALL"),
            "timestamp" to Timestamp.now().toString()
        )
    }

    public fun performRoutineSweep(): Map<String, Any> {
        val expiredCount = try {
            expireStaleProposals()
        } catch (e: Exception) {
            println("Warning: expireStaleProposals in routine sweep: ${e.message}")
            0
        }
        val syncCount = try {
            processDueSyncs()
        } catch (e: Exception) {
            println("Warning: processDueSyncs in routine sweep: ${e.message}")
            0
        }
        val metricsResult = recalculateCompanyMetrics()

        val db = firestore
        val sweepTimestamp = Timestamp.now()
        db.collection("meta").document("sync").set(
            mapOf(
                "lastSweepAt" to sweepTimestamp,
                "staleProposalsExpired" to expiredCount,
                "dueSyncsProcessed" to syncCount,
                "companiesMetricsUpdated" to metricsResult["companiesUpdated"],
                "actor" to "CLOUD_RUN_SWEEP"
            ),
            SetOptions.merge()
        ).get()

        return mapOf(
            "status" to "SUCCESS",
            "message" to "Routine sweep completed successfully",
            "sweepAt" to sweepTimestamp.toString(),
            "staleProposalsExpired" to expiredCount,
            "dueSyncsProcessed" to syncCount,
            "companiesMetricsUpdated" to (metricsResult["companiesUpdated"] ?: 0)
        )
    }
}
