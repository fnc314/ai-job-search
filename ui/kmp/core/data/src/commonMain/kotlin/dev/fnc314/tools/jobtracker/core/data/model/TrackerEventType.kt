package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Event classification defining the nature of a milestone or touchpoint in a [TrackerEvent].
 *
 * Couples semantic event definitions with Material Design iconography for timeline rendering.
 *
 * @property icon Material Design vector icon [ImageVector] representing this event visually.
 * @property displayName Human-readable label for timeline entries.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"event:applied"`).
 */
public enum class TrackerEventType(
    public val icon: ImageVector,
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when event kind is unspecified. */
    UNSPECIFIED(Icons.Default.CheckCircle, "Unspecified", "event:unspecified"),

    /** Initial application submission to company. */
    APPLY(Icons.AutoMirrored.Filled.Send, "Application Submitted", "event:applied"),

    /** Automated or recruiter confirmation of application receipt. */
    ACKNOWLEDGE(Icons.Default.Email, "Acknowledged", "event:acknowledged"),

    /** Initial conversational phone or video screen with a technical recruiter. */
    RECRUITER_SCREEN(Icons.Default.DateRange, "Recruiter Screen", "event:recruiter-screen"),

    /** Technical or behavioral interview scheduled. */
    SCHEDULE_INTERVIEW(Icons.Default.DateRange, "Interview Scheduled", "event:interview-scheduled"),

    /** Scheduled interview round concluded. */
    COMPLETE_INTERVIEW(Icons.Default.DateRange, "Interview Completed", "event:interview-completed"),

    /** Take-home technical coding assessment or architectural assignment. */
    ASSESS(Icons.Default.Edit, "Assessment", "event:assessment"),

    /** Full loop or final onsite interview round. */
    VISIT_ONSITE(Icons.Default.Place, "On-Site Interview", "event:onsite"),

    /** Formal offer of employment extended. */
    OFFER(Icons.Default.Star, "Offer Received", "event:offer"),

    /** Offer formally accepted by candidate. */
    ACCEPT_OFFER(Icons.Default.CheckCircle, "Offer Accepted", "event:offer-accepted"),

    /** Candidate declined the employment offer. */
    DECLINE_OFFER(Icons.Default.Close, "Offer Declined", "event:offer-declined"),

    /** Candidate rejected by company. */
    REJECT(Icons.Default.Close, "Rejected", "event:rejection"),

    /** Application marked inactive after 30+ days of silence. */
    GHOST(Icons.Default.Close, "No Response", "event:no-response"),

    /** Candidate voluntarily withdrew from consideration. */
    WITHDRAW(Icons.Default.Close, "Withdrawn", "event:withdrawn"),

    /** Follow-up message sent by candidate or agent. */
    SEND_FOLLOW_UP(Icons.Default.Email, "Follow-Up Sent", "event:follow-up-sent"),

    /** Tailored LaTeX CV or Cover Letter compiled for this role. */
    TAILOR_DOCUMENT(Icons.Default.Edit, "Document Tailored", "event:document-tailored"),

    /** Candidate fit score recalculated against updated requirements. */
    RECALCULATE_FIT(Icons.Default.Refresh, "Fit Recalculated", "event:fit-recalculated"),

    /** New document or specification artifact attached. */
    ADD_ARTIFACT(Icons.Default.Info, "Artifact Added", "event:artifact-added"),

    /** Contextual strategy note added. */
    ADD_NOTE(Icons.Default.Edit, "Note Added", "event:note-added"),

    /** Record imported from external system or CSV archive. */
    IMPORT(Icons.Default.Share, "Imported", "event:imported"),

    /** Manual correction applied to status lifecycle. */
    CORRECT_STATUS(Icons.Default.Refresh, "Status Corrected", "event:status-corrected");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced event string into a [TrackerEventType] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [TrackerEventType] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): TrackerEventType {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("event:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
