package dev.fnc314.aijobtracker.frontend.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_application_withdrawn
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_drafted
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_drafting
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_hired
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_interview
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_lead
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_no_response
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_none
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_offer_declined
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_offer_expected
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_ranked
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_rejected
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_submitted
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_unknown
import dev.fnc314.aijobtracker.frontend.core_data.generated.resources.status_unspecified
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val SECTION_INTERVIEW_OR_OFFER = 1
private const val SECTION_ACTIVE_PIPELINE = 2
private const val SECTION_SUBMITTED = 3
private const val SECTION_CLOSED = 4

/**
 * Lifecycle states of a job application tracked within the system.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates all compile-time closed-vocabulary funnel states, while [Unrecognized]
 * captures arbitrary or forward-compatible wire strings without data loss.
 */
public sealed interface TrackerStatus : WireContract {

    /** Returns the human-readable display label. */
    public fun displayName(): String = displayName

    /** Resolves the localized user-facing label for this status from Compose resources. */
    @Composable
    public fun localizedDisplayName(): String = when (this) {
        is Known -> stringResource(labelRes)
        is Unrecognized -> stringResource(Res.string.status_unknown, wireKey)
    }

    /** Returns the canonical remote document string representation. */
    public fun toRemote(): String = wireKey

    /** Indicates whether this status represents a final terminal state where the pipeline has closed. */
    public val isTerminal: Boolean

    /** Indicates whether the application is currently active and requires ongoing monitoring or engagement. */
    public val isActive: Boolean

    /** Resolves the category section group for list organization: 1=Interviewing, 2=Drafting/Pre-submission, 3=Submitted, 4=Rejected. */
    public val sectionOrder: Int

    /** Resolves whether the card should render with subdued/disabled alpha and styling. */
    public val isSubduedCard: Boolean

    /** Resolves whether the card should present an action-required pestering flair. */
    public val isActionRequiredCard: Boolean

    /** Resolves the theme-aware Material 3 container background color for this status. */
    @Composable
    public fun containerColor(): Color

    /** Resolves the theme-aware Material 3 content on-container text/icon color for this status. */
    @Composable
    public fun contentColor(): Color

    /**
     * Closed vocabulary of known application lifecycle states.
     */
    public enum class Known(
        /** Localized string resource for displaying this status in Compose UI. */
        public val labelRes: StringResource,
        override val displayName: String,
        override val wireTag: Int = -1,
    ) : TrackerStatus {
        /** Sentinel value representing an unfiltered filter state matching all applications. */
        NONE(labelRes = Res.string.status_none, displayName = "All"),

        /** Fallback status when the remote status string is missing or unspecified. */
        UNSPECIFIED(labelRes = Res.string.status_unspecified, displayName = "Unspecified", wireTag = 0),

        /** Pre-funnel opportunity lead captured prior to active drafting. */
        LEAD(labelRes = Res.string.status_lead, displayName = "Lead", wireTag = 10),

        /** Async artifact generation or active drafting currently in progress. */
        DRAFTING(labelRes = Res.string.status_drafting, displayName = "Drafting", wireTag = 11),

        /** Application draft and artifacts prepared; pending formal submission. */
        DRAFTED(labelRes = Res.string.status_drafted, displayName = "Drafted", wireTag = 12),

        /** Job posting evaluated by fit scorer; awaiting decision to draft assets. */
        RANKED(labelRes = Res.string.status_ranked, displayName = "Ranked", wireTag = 13),

        /** Formal application submitted to company or hiring platform. */
        SUBMITTED(labelRes = Res.string.status_submitted, displayName = "Submitted", wireTag = 2),

        /** Active interview process underway (screens, technical rounds, onsites). */
        INTERVIEW(labelRes = Res.string.status_interview, displayName = "Interview", wireTag = 3),

        /** Candidate expecting or negotiating a formal compensation offer. */
        OFFER_EXPECTED(labelRes = Res.string.status_offer_expected, displayName = "Offer Expected", wireTag = 4),

        /** Candidate accepted offer and successfully completed hiring. */
        HIRED(labelRes = Res.string.status_hired, displayName = "Hired", wireTag = 5),

        /** Application was rejected by employer. */
        REJECTED(labelRes = Res.string.status_rejected, displayName = "Rejected", wireTag = 6),

        /** Employer did not respond within standard follow-up horizon (>30 days). */
        NO_RESPONSE(labelRes = Res.string.status_no_response, displayName = "No Response", wireTag = 7),

        /** Candidate declined an offer extended by the company. */
        OFFER_DECLINED(labelRes = Res.string.status_offer_declined, displayName = "Offer Declined", wireTag = 8),

        /** Candidate voluntarily withdrew application from consideration. */
        APPLICATION_WITHDRAWN(
            labelRes = Res.string.status_application_withdrawn,
            displayName = "Application Withdrawn",
            wireTag = 9,
        );

        override val wireKey: String
            get() = name

        override val isTerminal: Boolean
            get() = when (this) {
                HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> true
                NONE, UNSPECIFIED, LEAD, DRAFTING, DRAFTED, RANKED, SUBMITTED, INTERVIEW, OFFER_EXPECTED -> false
            }

        override val isActive: Boolean
            get() = when (this) {
                LEAD, DRAFTING, DRAFTED, RANKED, SUBMITTED, INTERVIEW, OFFER_EXPECTED -> true
                NONE, UNSPECIFIED, HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> false
            }

        override val sectionOrder: Int
            get() = when (this) {
                INTERVIEW, OFFER_EXPECTED, HIRED -> SECTION_INTERVIEW_OR_OFFER
                LEAD, DRAFTING, DRAFTED, RANKED -> SECTION_ACTIVE_PIPELINE
                SUBMITTED -> SECTION_SUBMITTED
                REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> SECTION_CLOSED
                NONE, UNSPECIFIED -> SECTION_SUBMITTED
            }

        override val isSubduedCard: Boolean
            get() = when (this) {
                REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> true
                else -> false
            }

        override val isActionRequiredCard: Boolean
            get() = when (this) {
                LEAD, DRAFTING, DRAFTED, RANKED -> true
                else -> false
            }

        @Composable
        override fun containerColor(): Color = when (this) {
            LEAD, DRAFTING, DRAFTED, RANKED -> MaterialTheme.colorScheme.surfaceContainerLow
            SUBMITTED -> MaterialTheme.colorScheme.tertiaryFixed
            INTERVIEW -> MaterialTheme.colorScheme.secondaryFixed
            OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.primaryFixed
            REJECTED -> MaterialTheme.colorScheme.errorContainer
            NO_RESPONSE -> MaterialTheme.colorScheme.surfaceContainerLow
            OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.error
            NONE, UNSPECIFIED -> MaterialTheme.colorScheme.primaryFixedDim
        }

        @Composable
        override fun contentColor(): Color = when (this) {
            LEAD, DRAFTING, DRAFTED, RANKED -> MaterialTheme.colorScheme.onSurfaceVariant
            SUBMITTED -> MaterialTheme.colorScheme.onTertiaryFixed
            INTERVIEW -> MaterialTheme.colorScheme.onSecondaryFixed
            OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.onPrimaryFixed
            REJECTED -> MaterialTheme.colorScheme.onErrorContainer
            NO_RESPONSE -> MaterialTheme.colorScheme.onSurface
            OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.onError
            NONE, UNSPECIFIED -> MaterialTheme.colorScheme.onPrimaryFixedVariant
        }
    }

    /**
     * Associated-value carrier for unmapped, future, or custom status strings.
     * Preserves the verbatim incoming [wireKey] without loss of fidelity.
     */
    public data class Unrecognized(
        override val wireKey: String,
    ) : TrackerStatus {
        override val displayName: String get() = "Unknown ($wireKey)"
        override val isTerminal: Boolean get() = false
        override val isActive: Boolean get() = false
        override val sectionOrder: Int get() = SECTION_SUBMITTED
        override val isSubduedCard: Boolean get() = false
        override val isActionRequiredCard: Boolean get() = false

        @Composable
        override fun containerColor(): Color = MaterialTheme.colorScheme.surfaceVariant

        @Composable
        override fun contentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
    }

    /** Companion object providing lookup and shortcut accessors for [TrackerStatus]. */
    public companion object {
        /** Default fallback value for [TrackerStatus]. */
        public val DEFAULT: TrackerStatus = Known.UNSPECIFIED

        // Static O(1) multi-key lookup map built using stdlib extension with legacy aliases
        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val legacy = when (entry) {
                Known.LEAD -> listOf("status:lead", "lead")
                Known.DRAFTING -> listOf("status:drafting", "drafting", "draft")
                Known.DRAFTED -> listOf("status:drafted", "drafted")
                Known.RANKED -> listOf("status:ranked", "ranked")
                Known.SUBMITTED -> listOf("status:submitted", "submitted", "screening", "applied", "status:applied")
                Known.INTERVIEW -> listOf("status:interview", "interview", "interviewing", "onsite", "recruiter_screened", "recruiter-screened", "status:recruiter-screened")
                Known.OFFER_EXPECTED -> listOf("status:offer", "offer", "negotiating_pre_offer", "negotiating-pre-offer", "negotiating_post_offer", "negotiating-post-offer")
                Known.HIRED -> listOf("status:hired", "hired")
                Known.REJECTED -> listOf("status:rejected", "rejected")
                Known.NO_RESPONSE -> listOf("status:no-response", "no-response", "no_response")
                Known.OFFER_DECLINED -> listOf("status:offer-declined", "offer-declined", "offer_declined")
                Known.APPLICATION_WITHDRAWN -> listOf("status:withdrawn", "withdrawn")
                Known.NONE -> listOf("status:none", "all")
                Known.UNSPECIFIED -> listOf("status:unspecified", "unspecified")
            }
            legacy + listOf(entry.name.lowercase().replace("_", "-"))
        }

        // Backward-compatible shortcut properties matching standard enum names
        /** Sentinel value representing an unfiltered filter state matching all applications. */
        public val NONE: TrackerStatus = Known.NONE
        /** Fallback status when the remote status string is missing or unspecified. */
        public val UNSPECIFIED: TrackerStatus = Known.UNSPECIFIED
        /** Pre-funnel opportunity lead captured prior to active drafting. */
        public val LEAD: TrackerStatus = Known.LEAD
        /** Async artifact generation or active drafting currently in progress. */
        public val DRAFTING: TrackerStatus = Known.DRAFTING
        /** Application draft and artifacts prepared; pending formal submission. */
        public val DRAFTED: TrackerStatus = Known.DRAFTED
        /** Job posting evaluated by fit scorer; awaiting decision to draft assets. */
        public val RANKED: TrackerStatus = Known.RANKED
        /** Formal application submitted to company or hiring platform. */
        public val SUBMITTED: TrackerStatus = Known.SUBMITTED
        /** Backward-compatible alias for [SUBMITTED]. */
        @Deprecated("Use SUBMITTED instead", ReplaceWith("SUBMITTED"))
        public val APPLIED: TrackerStatus = Known.SUBMITTED
        /** Active interview process underway (screens, technical rounds, onsites). */
        public val INTERVIEW: TrackerStatus = Known.INTERVIEW
        /** Candidate expecting or negotiating a formal compensation offer. */
        public val OFFER_EXPECTED: TrackerStatus = Known.OFFER_EXPECTED
        /** Candidate accepted offer and successfully completed hiring. */
        public val HIRED: TrackerStatus = Known.HIRED
        /** Application was rejected by employer. */
        public val REJECTED: TrackerStatus = Known.REJECTED
        /** Employer did not respond within standard follow-up horizon (>30 days). */
        public val NO_RESPONSE: TrackerStatus = Known.NO_RESPONSE
        /** Candidate declined an offer extended by the company. */
        public val OFFER_DECLINED: TrackerStatus = Known.OFFER_DECLINED
        /** Candidate voluntarily withdrew application from consideration. */
        public val APPLICATION_WITHDRAWN: TrackerStatus = Known.APPLICATION_WITHDRAWN

        /** All known canonical entries for pickers, dropdowns, and filters. */
        public val entries: List<TrackerStatus> = Known.entries

        /**
         * Resolves a raw wire string into its matching [TrackerStatus] instance.
         *
         * @param raw The raw string representation.
         * @return The matched [Known] status or [Unrecognized] containing the raw string.
         */
        public fun fromWireKey(raw: String?): TrackerStatus {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /**
         * Resolves a raw Firestore document status string into its matching [TrackerStatus] constant.
         *
         * @param raw The raw string from remote or local storage.
         * @return The resolved [TrackerStatus], or [UNSPECIFIED] if null/blank, or [Unrecognized] if unmapped.
         */
        public fun fromRemote(raw: String?): TrackerStatus = fromWireKey(raw)
    }
}
