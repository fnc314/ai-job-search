package dev.fnc314.tools.jobtracker.core.data.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Query
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * JVM implementation of JobApplicationRepository using Firebase Admin SDK.
 * This runs on the Desktop (JVM) target.
 */
public class JVMJobApplicationRepository : JobApplicationRepository {

    private val db = FirestoreClient.getFirestore(FirebaseApp.getInstance())

    override suspend fun getAllJobApplications(): List<JobApplication> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("applications")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .get()

            snapshot.getDocuments().mapNotNull { doc ->
                parseApplicationDocument(doc)
            }
        } catch (e: Exception) {
            println("Error fetching applications: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getJobApplication(id: String): JobApplication? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("applications").document(id).get().get()
            if (!doc.exists()) return@withContext null
            parseApplicationDocument(doc)
        } catch (e: Exception) {
            println("Error fetching application $id: ${e.message}")
            null
        }
    }

    override suspend fun getCompanies(): List<Company> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("companies").get().get()
            snapshot.getDocuments().mapNotNull { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                Factory.createCompany(
                    id = id,
                    name = name,
                )
            }
        } catch (e: Exception) {
            println("Error fetching companies: ${e.message}")
            emptyList()
        }
    }

    override suspend fun saveJobApplication(app: JobApplication): String = withContext(Dispatchers.IO) {
        app.id
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication = withContext(Dispatchers.IO) {
        Factory.createJobApplication(id = id, company = "", status = status)
    }

    override suspend fun recordEvent(event: TrackerEvent): String = withContext(Dispatchers.IO) {
        event.id
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("applications").document(id)
            docRef.update("fitRating", score).get()
            getJobApplication(id) ?: Factory.createJobApplication(
                id = id,
                company = "",
                fitRating = score,
                fitEvaluation = Factory.createTrackerFit(score = score),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Factory.createJobApplication(
                id = id,
                company = "",
                fitRating = score,
                fitEvaluation = Factory.createTrackerFit(score = score),
            )
        }
    }

    private fun parseApplicationDocument(doc: DocumentSnapshot): JobApplication? {
        val id = doc.id
        val company = doc.getString("company") ?: return null
        val statusRaw = doc.getString("status")
        val status = EnumParsing.parseTrackerStatus(statusRaw)
        val channelRaw = doc.getString("channel")
        val channel = EnumParsing.parseApplicationChannel(channelRaw)

        val role = doc.getString("role")
        val source = doc.getString("source")
        val notesRendered = doc.getString("notesRendered")
        val fitRating = doc.getLong("fitRating")?.toInt()

        // Parse timestamps
        val createdAt = parseTimestampValue(doc.get("createdAt"))
        val updatedAt = parseTimestampValue(doc.get("updatedAt"))

        // Parse tags
        val tags = doc.get("tags") as? List<String> ?: emptyList()

        // Parse contactIds
        val contactIds = doc.get("contactIds") as? List<String> ?: emptyList()

        // Parse counters
        val countersMap = doc.get("counters") as? Map<*, *>
        val counters = countersMap?.let {
            Factory.createTrackerCounts(
                events = (it["events"] as? Number)?.toInt() ?: 0,
                followUpsSent = (it["followUpsSent"] as? Number)?.toInt() ?: 0,
                interviews = (it["interviews"] as? Number)?.toInt() ?: 0,
                rejections = (it["rejections"] as? Number)?.toInt() ?: 0,
            )
        }

        // Parse timeline
        val timelineMap = doc.get("timeline") as? Map<*, *>
        val timeline = timelineMap?.let {
            val firstActivityAt = parseTimestampValue(it["firstActivityAt"])
            val lastActivityAt = parseTimestampValue(it["lastActivityAt"])
            Factory.createTimeline(
                firstActivityAt = firstActivityAt,
                lastActivityAt = lastActivityAt,
                daysSinceActivity = (it["daysSinceActivity"] as? Number)?.toInt() ?: 0,
            )
        }

        // Parse location
        val locationMap = doc.get("location") as? Map<*, *>
        val locationDetail = locationMap?.let {
            val workModeRaw = it["workMode"] as? String
            Factory.createLocation(
                city = it["city"] as? String,
                region = it["region"] as? String,
                country = it["country"] as? String,
                workMode = EnumParsing.parseWorkMode(workModeRaw),
                relocation = it["relocation"] as? Boolean ?: false,
                superCommuter = it["superCommuter"] as? Boolean ?: false,
            )
        }

        // Parse compensation
        val compMap = doc.get("compensation") as? Map<*, *>
        val compensation = compMap?.let {
            Factory.createCompensation(
                min = it["min"] as? Double,
                max = it["max"] as? Double,
                currency = it["currency"] as? String,
                period = it["period"] as? String,
            )
        }

        // Parse fit evaluation
        val fitMap = doc.get("fitEvaluation") as? Map<*, *>
        val fitEvaluation = fitMap?.let {
            Factory.createTrackerFit(
                rubricVersion = (it["rubricVersion"] as? Number)?.toInt() ?: 1,
                score = (it["score"] as? Number)?.toInt() ?: fitRating,
                gaps = (it["gaps"] as? List<String>) ?: emptyList(),
            )
        } ?: fitRating?.let {
            Factory.createTrackerFit(
                rubricVersion = 1,
                score = it,
            )
        }

        return Factory.createJobApplication(
            id = id,
            company = company,
            role = role,
            location = locationMap?.let {
                val city = it["city"] as? String
                val country = it["country"] as? String
                buildString {
                    if (city != null) append(city)
                    if (city != null && country != null) append(", ")
                    if (country != null) append(country)
                }.takeIf { s -> s.isNotBlank() }
            },
            status = status,
            channel = channel,
            jobId = doc.getString("jobId"),
            jobUrl = source,
            description = null,
            tags = tags,
            contactIds = contactIds,
            compensation = compensation,
            locationDetail = locationDetail,
            fitRating = fitRating ?: fitEvaluation?.score,
            fitEvaluation = fitEvaluation,
            counters = counters,
            timeline = timeline,
            notesRendered = notesRendered,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun parseTimestampValue(value: Any?): Instant? {
        // Firestore timestamps come as com.google.protobuf.Timestamp
        if (value is Map<*, *>) {
            // Timestamp map format: {"_seconds": ..., "_nanoseconds": ...}
            val seconds = (value["_seconds"] as? Number)?.toLong()
                ?: (value["seconds"] as? Number)?.toLong()
            if (seconds != null) {
                return Instant.fromEpochSeconds(seconds)
            }
        }
        // Direct long (epoch ms)
        if (value is Number) {
            return Instant.fromEpochMilliseconds(value.toLong())
        }
        return null
    }
}
