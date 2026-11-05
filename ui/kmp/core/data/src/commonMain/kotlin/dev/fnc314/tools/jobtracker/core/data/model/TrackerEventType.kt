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

public enum class TrackerEventType(
    public val icon: ImageVector,
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(Icons.Default.CheckCircle, "Unspecified", "event:unspecified"),
    APPLY(Icons.AutoMirrored.Filled.Send, "Application Submitted", "event:applied"),
    ACKNOWLEDGE(Icons.Default.Email, "Acknowledged", "event:acknowledged"),
    RECRUITER_SCREEN(Icons.Default.DateRange, "Recruiter Screen", "event:recruiter-screen"),
    SCHEDULE_INTERVIEW(Icons.Default.DateRange, "Interview Scheduled", "event:interview-scheduled"),
    COMPLETE_INTERVIEW(Icons.Default.DateRange, "Interview Completed", "event:interview-completed"),
    ASSESS(Icons.Default.Edit, "Assessment", "event:assessment"),
    VISIT_ONSITE(Icons.Default.Place, "On-Site Interview", "event:onsite"),
    OFFER(Icons.Default.Star, "Offer Received", "event:offer"),
    ACCEPT_OFFER(Icons.Default.CheckCircle, "Offer Accepted", "event:offer-accepted"),
    DECLINE_OFFER(Icons.Default.Close, "Offer Declined", "event:offer-declined"),
    REJECT(Icons.Default.Close, "Rejected", "event:rejection"),
    GHOST(Icons.Default.Close, "No Response", "event:no-response"),
    WITHDRAW(Icons.Default.Close, "Withdrawn", "event:withdrawn"),
    SEND_FOLLOW_UP(Icons.Default.Email, "Follow-Up Sent", "event:follow-up-sent"),
    TAILOR_DOCUMENT(Icons.Default.Edit, "Document Tailored", "event:document-tailored"),
    RECALCULATE_FIT(Icons.Default.Refresh, "Fit Recalculated", "event:fit-recalculated"),
    ADD_ARTIFACT(Icons.Default.Info, "Artifact Added", "event:artifact-added"),
    ADD_NOTE(Icons.Default.Edit, "Note Added", "event:note-added"),
    IMPORT(Icons.Default.Share, "Imported", "event:imported"),
    CORRECT_STATUS(Icons.Default.Refresh, "Status Corrected", "event:status-corrected");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): TrackerEventType {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
