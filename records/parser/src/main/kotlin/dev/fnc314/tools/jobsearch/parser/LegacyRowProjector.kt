package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.JobApplication
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifact

/**
 * Migrated from reader.py: rowFromStore() and legacyStatus().
 * Projects a typed JobApplication to the legacy flat row shape for TUI display.
 */
object LegacyRowProjector {

    val LEGACY_KEYS = listOf(
        "id", "company", "role", "location", "status", "status_label",
        "channel", "job_url", "description", "tags", "contact_ids",
        "compensation_min", "compensation_max", "compensation_currency",
        "work_mode", "relocation", "fit_rating", "fit_gaps",
        "events", "follow_ups_sent", "interviews", "rejections",
        "first_activity", "last_activity", "days_since_activity",
        "next_follow_up", "notes", "created_at", "updated_at",
        "cv_file", "cover_letter_file",
    )

    /**
     * Convert namespaced status back to legacy label for display.
     */
    fun legacyStatus(value: String?): String {
        if (value.isNullOrBlank()) return "applied"
        return when (value) {
            "status:drafting" -> "drafting"
            "status:applied" -> "applied"
            "status:interview" -> "interview"
            "status:offer" -> "offer"
            "status:hired" -> "hired"
            "status:rejected" -> "rejected"
            "status:no-response" -> "no-response"
            "status:offer-declined" -> "offer-declined"
            "status:withdrawn" -> "withdrawn"
            else -> "applied"
        }
    }

    /**
     * Find an artifact by kind and return its path.
     */
    fun artifactValue(artifacts: List<TrackerArtifact>, kind: String): String? {
        return artifacts
            .firstOrNull { it.kind.name == kind }
            .let { it?.localPath ?: it?.externalUrl }
    }

    /**
     * Project a JobApplication to the legacy flat row shape.
     */
    fun rowFromStore(
        app: JobApplication,
        artifacts: List<TrackerArtifact> = emptyList(),
    ): Map<String, String> {
        val row = mutableMapOf<String, String>()

        row["id"] = app.id
        row["company"] = app.company
        row["role"] = app.role
        row["location"] = app.location
        row["status"] = EnumMapping.trackerStatusToString(app.status)
        row["status_label"] = legacyStatus(row["status"])
        row["channel"] = app.channel.name.lowercase().replace("_", "-")
        row["job_url"] = app.jobUrl
        row["description"] = app.description
        row["tags"] = app.tagsList.joinToString(",")
        row["contact_ids"] = app.contactIdsList.joinToString(",")

        // Compensation
        if (app.hasCompensation()) {
            row["compensation_min"] = app.compensation.min.toInt().toString()
            row["compensation_max"] = app.compensation.max.toInt().toString()
            row["compensation_currency"] = app.compensation.currency
        }

        // Location
        if (app.hasLocationDetail()) {
            row["work_mode"] = app.locationDetail.workMode.name.lowercase().replace("_", "-")
            row["relocation"] = app.locationDetail.relocation.toString()
        }

        // Fit
        if (app.hasFitEvaluation()) {
            row["fit_gaps"] = app.fitEvaluation.gapsList.joinToString(",")
        }

        // Counters
        if (app.hasCounters()) {
            row["events"] = app.counters.events.toString()
            row["follow_ups_sent"] = app.counters.followUpsSent.toString()
            row["interviews"] = app.counters.interviews.toString()
            row["rejections"] = app.counters.rejections.toString()
        }

        // Timeline
        if (app.hasTimeline()) {
            val tl = app.timeline
            if (tl.hasFirstActivityAt()) {
                row["first_activity"] = TimestampConverter.timestampToIso(tl.firstActivityAt)
            }
            if (tl.hasLastActivityAt()) {
                row["last_activity"] = TimestampConverter.timestampToIso(tl.lastActivityAt)
            }
            row["days_since_activity"] = tl.daysSinceActivity.toString()
            if (tl.hasNextFollowUpAt()) {
                row["next_follow_up"] = TimestampConverter.timestampToIso(tl.nextFollowUpAt)
            }
        }

        row["notes"] = app.notesRendered
        if (app.hasCreatedAt()) {
            row["created_at"] = TimestampConverter.timestampToIso(app.createdAt)
        }
        if (app.hasUpdatedAt()) {
            row["updated_at"] = TimestampConverter.timestampToIso(app.updatedAt)
        }

        // Artifacts
        row["cv_file"] = artifactValue(artifacts, "ARTIFACT_CV") ?: ""
        row["cover_letter_file"] = artifactValue(artifacts, "ARTIFACT_COVER_LETTER") ?: ""

        return row
    }
}
