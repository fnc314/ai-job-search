package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import kotlin.time.Instant

/**
 * User interactions and events emitted from the Job Application Detail UI screen.
 */
public sealed interface JobApplicationDetailUiIntent {

    /**
     * User requested loading or reloading application details.
     *
     * @property id Application slug or UUID identifier.
     */
    public data class Load(public val id: String) : JobApplicationDetailUiIntent

    /**
     * User initiated a lifecycle status change.
     *
     * @property newStatus Desired new [TrackerStatus].
     * @property date Optional effective date string.
     * @property note Optional explanatory note.
     */
    public data class UpdateStatus(
        public val newStatus: TrackerStatus,
        public val date: String? = null,
        public val note: String? = null,
    ) : JobApplicationDetailUiIntent

    /**
     * User updated the numerical alignment fit score.
     *
     * @property newScore New fit rating (0..100).
     */
    public data class UpdateFitScore(public val newScore: Int) : JobApplicationDetailUiIntent

    /**
     * User recorded a milestone event to the application timeline.
     *
     * @property type Categorical event kind [TrackerEventType].
     * @property notes Contextual notes or recruiter feedback.
     * @property scheduledAt Optional structured date-time for scheduled activities (e.g. interviews, on-sites).
     */
    public data class RecordEvent(
        public val type: TrackerEventType,
        public val notes: String = "",
        public val scheduledAt: Instant? = null,
    ) : JobApplicationDetailUiIntent

    /**
     * User clicked to open a document artifact (CV, cover letter, or outcome).
     *
     * @property artifactId Target artifact identifier.
     */
    public data class OpenArtifact(public val artifactId: String) : JobApplicationDetailUiIntent

    /** User clicked back button to return to dashboard. */
    public data object ClickBack : JobApplicationDetailUiIntent
}
