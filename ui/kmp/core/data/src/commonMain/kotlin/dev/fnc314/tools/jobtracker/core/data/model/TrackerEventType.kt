package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Event classification defining the nature of a milestone or touchpoint in a [TrackerEvent].
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical lifecycle events with Material iconography, while [Unrecognized]
 * captures arbitrary or forward-compatible event strings without data loss.
 */
public sealed interface TrackerEventType : WireContract {

    /** Material Design vector icon representing this event visually. */
    public val icon: ImageVector

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known timeline milestone events.
     */
    public enum class Known(
        override val icon: ImageVector,
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerEventType {
        /** Fallback when event kind is unspecified. */
        UNSPECIFIED(Icons.Default.CheckCircle, "Unspecified", "UNSPECIFIED", wireTag = 0),

        /** Initial application submission to company. */
        APPLY(Icons.AutoMirrored.Filled.Send, "Application Submitted", "APPLY", wireTag = 1),

        /** Automated or recruiter confirmation of application receipt. */
        ACKNOWLEDGE(Icons.Default.Email, "Acknowledged", "ACKNOWLEDGE", wireTag = 2),

        /** Initial conversational phone or video screen with a technical recruiter. */
        RECRUITER_SCREEN(Icons.Default.DateRange, "Recruiter Screen", "RECRUITER_SCREEN", wireTag = 3),

        /** Technical or behavioral interview scheduled. */
        SCHEDULE_INTERVIEW(Icons.Default.DateRange, "Interview Scheduled", "SCHEDULE_INTERVIEW", wireTag = 4),

        /** Scheduled interview round concluded. */
        COMPLETE_INTERVIEW(Icons.Default.DateRange, "Interview Completed", "COMPLETE_INTERVIEW", wireTag = 5),

        /** Take-home technical coding assessment or architectural assignment. */
        ASSESS(Icons.Default.Edit, "Assessment", "ASSESS", wireTag = 6),

        /** Full loop or final onsite interview round. */
        VISIT_ONSITE(Icons.Default.Place, "On-Site Interview", "VISIT_ONSITE", wireTag = 7),

        /** Formal offer of employment extended. */
        OFFER(Icons.Default.Star, "Offer Received", "OFFER", wireTag = 8),

        /** Offer formally accepted by candidate. */
        ACCEPT_OFFER(Icons.Default.CheckCircle, "Offer Accepted", "ACCEPT_OFFER", wireTag = 9),

        /** Candidate declined the employment offer. */
        DECLINE_OFFER(Icons.Default.Close, "Offer Declined", "DECLINE_OFFER", wireTag = 10),

        /** Candidate rejected by company. */
        REJECT(Icons.Default.Close, "Rejected", "REJECT", wireTag = 11),

        /** Application marked inactive after 30+ days of silence. */
        GHOST(Icons.Default.Close, "No Response", "GHOST", wireTag = 12),

        /** Candidate voluntarily withdrew from consideration. */
        WITHDRAW(Icons.Default.Close, "Withdrawn", "WITHDRAW", wireTag = 13),

        /** Follow-up message sent by candidate or agent. */
        SEND_FOLLOW_UP(Icons.Default.Email, "Follow-Up Sent", "SEND_FOLLOW_UP", wireTag = 14),

        /** Tailored LaTeX CV or Cover Letter compiled for this role. */
        TAILOR_DOCUMENT(Icons.Default.Edit, "Document Tailored", "TAILOR_DOCUMENT", wireTag = 15),

        /** Candidate fit score recalculated against updated requirements. */
        RECALCULATE_FIT(Icons.Default.Refresh, "Fit Recalculated", "RECALCULATE_FIT", wireTag = 16),

        /** New document or specification artifact attached. */
        ADD_ARTIFACT(Icons.Default.Info, "Artifact Added", "ADD_ARTIFACT", wireTag = 17),

        /** Contextual strategy note added. */
        ADD_NOTE(Icons.Default.Edit, "Note Added", "ADD_NOTE", wireTag = 18),

        /** Record imported from external system or CSV archive. */
        IMPORT(Icons.Default.Share, "Imported", "IMPORT", wireTag = 19),

        /** Manual correction applied to status lifecycle. */
        CORRECT_STATUS(Icons.Default.Refresh, "Status Corrected", "CORRECT_STATUS", wireTag = 20),

        /** Direct outreach initiated to recruiter or hiring team member. */
        RECRUITER_OUTREACH(Icons.Default.Person, "Recruiter Outreach", "RECRUITER_OUTREACH");
    }

    /**
     * Associated-value carrier for unmapped, future, or custom event strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerEventType {
        override val icon: ImageVector get() = Icons.Default.Info
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: TrackerEventType = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val legacy = when (entry) {
                Known.UNSPECIFIED -> "event:unspecified"
                Known.APPLY -> "event:applied"
                Known.ACKNOWLEDGE -> "event:acknowledged"
                Known.RECRUITER_SCREEN -> "event:recruiter-screen"
                Known.SCHEDULE_INTERVIEW -> "event:interview-scheduled"
                Known.COMPLETE_INTERVIEW -> "event:interview-completed"
                Known.ASSESS -> "event:assessment"
                Known.VISIT_ONSITE -> "event:onsite"
                Known.OFFER -> "event:offer"
                Known.ACCEPT_OFFER -> "event:offer-accepted"
                Known.DECLINE_OFFER -> "event:offer-declined"
                Known.REJECT -> "event:rejection"
                Known.GHOST -> "event:no-response"
                Known.WITHDRAW -> "event:withdrawn"
                Known.SEND_FOLLOW_UP -> "event:follow-up-sent"
                Known.TAILOR_DOCUMENT -> "event:document-tailored"
                Known.RECALCULATE_FIT -> "event:fit-recalculated"
                Known.ADD_ARTIFACT -> "event:artifact-added"
                Known.ADD_NOTE -> "event:note-added"
                Known.IMPORT -> "event:imported"
                Known.CORRECT_STATUS -> "event:status-corrected"
                Known.RECRUITER_OUTREACH -> "event:recruiter-outreach"
            }
            listOf(legacy, legacy.removePrefix("event:"), entry.name.lowercase().replace("_", "-"))
        }

        public val UNSPECIFIED: TrackerEventType = Known.UNSPECIFIED
        public val APPLY: TrackerEventType = Known.APPLY
        public val ACKNOWLEDGE: TrackerEventType = Known.ACKNOWLEDGE
        public val RECRUITER_SCREEN: TrackerEventType = Known.RECRUITER_SCREEN
        public val SCHEDULE_INTERVIEW: TrackerEventType = Known.SCHEDULE_INTERVIEW
        public val COMPLETE_INTERVIEW: TrackerEventType = Known.COMPLETE_INTERVIEW
        public val ASSESS: TrackerEventType = Known.ASSESS
        public val VISIT_ONSITE: TrackerEventType = Known.VISIT_ONSITE
        public val OFFER: TrackerEventType = Known.OFFER
        public val ACCEPT_OFFER: TrackerEventType = Known.ACCEPT_OFFER
        public val DECLINE_OFFER: TrackerEventType = Known.DECLINE_OFFER
        public val REJECT: TrackerEventType = Known.REJECT
        public val GHOST: TrackerEventType = Known.GHOST
        public val WITHDRAW: TrackerEventType = Known.WITHDRAW
        public val SEND_FOLLOW_UP: TrackerEventType = Known.SEND_FOLLOW_UP
        public val TAILOR_DOCUMENT: TrackerEventType = Known.TAILOR_DOCUMENT
        public val RECALCULATE_FIT: TrackerEventType = Known.RECALCULATE_FIT
        public val ADD_ARTIFACT: TrackerEventType = Known.ADD_ARTIFACT
        public val ADD_NOTE: TrackerEventType = Known.ADD_NOTE
        public val IMPORT: TrackerEventType = Known.IMPORT
        public val CORRECT_STATUS: TrackerEventType = Known.CORRECT_STATUS
        public val RECRUITER_OUTREACH: TrackerEventType = Known.RECRUITER_OUTREACH

        public val entries: List<TrackerEventType> = Known.entries

        public fun fromWireKey(raw: String?): TrackerEventType {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): TrackerEventType = fromWireKey(raw)
    }
}
