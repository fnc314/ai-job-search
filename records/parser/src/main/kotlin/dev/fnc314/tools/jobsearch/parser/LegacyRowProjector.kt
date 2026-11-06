package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.JobApplication
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifact

/**
 * Transforms typed [JobApplication] protobuf messages and associated [TrackerArtifact] collections
 * into flat key-value dictionaries matching legacy table column layouts.
 *
 * Used by the curses-based terminal UI (TUI) and reporting tools that consume tabular row representations.
 */
public object LegacyRowProjector {

    /**
     * The canonical list of legacy column keys supported in exported tabular rows and CSV archives.
     */
    public val LEGACY_KEYS: List<String> = listOf(
        "id", "company", "role", "location", "status", "status_label",
        "channel", "job_url", "description", "tags", "contact_ids",
        "compensation_min", "compensation_max", "compensation_currency",
        "work_mode", "role_location", "relocation", "fit_rating", "fit_gaps",
        "events", "follow_ups_sent", "interviews", "rejections",
        "first_activity", "last_activity", "days_since_activity",
        "next_follow_up", "notes", "created_at", "updated_at",
        "cv_file", "cover_letter_file",
    )

    /**
     * Converts a status string (e.g. `"INTERVIEW"` or `"status:interview"`) into its legacy display label (e.g. `"interview"`).
     *
     * @param value The raw status string or null.
     * @return The legacy presentation label string, defaulting to `"applied"` if null or unmapped.
     */
    public fun legacyStatus(value: String?): String {
        if (value.isNullOrBlank()) return "applied"
        return when (value) {
            "LEAD", "status:lead" -> "lead"
            "DRAFTING", "status:drafting" -> "drafting"
            "APPLIED", "status:applied" -> "applied"
            "INTERVIEW", "status:interview" -> "interview"
            "OFFER_EXPECTED", "status:offer" -> "offer"
            "HIRED", "status:hired" -> "hired"
            "REJECTED", "status:rejected" -> "rejected"
            "NO_RESPONSE", "status:no-response" -> "no-response"
            "OFFER_DECLINED", "status:offer-declined" -> "offer-declined"
            "APPLICATION_WITHDRAWN", "status:withdrawn" -> "withdrawn"
            else -> value.lowercase().removePrefix("status:")
        }
    }

    /**
     * Extracts the path or external URL of an artifact matching a specific kind from an artifact list.
     *
     * @param artifacts The collection of [TrackerArtifact] items to query.
     * @param kind The string name of the artifact kind (e.g. `"CV"` or `"ARTIFACT_CV"`).
     * @return The local file path or external URL if found, or null otherwise.
     */
    public fun artifactValue(artifacts: List<TrackerArtifact>, kind: String): String? {
        val normalizedKind = kind.removePrefix("ARTIFACT_")
        return artifacts
            .firstOrNull { it.kind.name.removePrefix("ARTIFACT_") == normalizedKind }
            ?.let { if (it.localPath.isNotBlank()) it.localPath else it.externalUrl.ifBlank { null } }
    }

    /**
     * Projects a strongly typed protobuf [JobApplication] message and optional artifact list into a flat string-keyed row dictionary.
     *
     * Extracts scalar properties, formats nested compensation boundaries, serializes timeline dates to ISO strings,
     * computes legacy status labels, and embeds CV/cover letter file references.
     *
     * @param app The [JobApplication] protobuf instance to project.
     * @param artifacts The list of [TrackerArtifact] instances linked to this application.
     * @return A map associating column names to stringified values.
     */
    public fun rowFromStore(
        app: JobApplication,
        artifacts: List<TrackerArtifact> = emptyList(),
    ): Map<String, String> {
        val row = mutableMapOf<String, String>()

        row["id"] = app.id
        row["company"] = app.company
        row["role"] = app.role
        row["location"] = app.location
        row["status"] = app.status.wireKey
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
            val loc = app.locationDetail
            val roleLoc = loc.roleLocation.name.lowercase().replace("_", "-")
            row["work_mode"] = roleLoc
            row["role_location"] = roleLoc
            row["relocation"] = loc.relocation.toString()
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
        row["cv_file"] = artifactValue(artifacts, "CV") ?: ""
        row["cover_letter_file"] = artifactValue(artifacts, "COVER_LETTER") ?: ""

        return row
    }
}
