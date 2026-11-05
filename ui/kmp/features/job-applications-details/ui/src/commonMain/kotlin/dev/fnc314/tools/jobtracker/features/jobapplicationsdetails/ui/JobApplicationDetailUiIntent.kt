package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

public sealed interface JobApplicationDetailUiIntent {
    public data class Load(public val id: String) : JobApplicationDetailUiIntent
    public data class UpdateStatus(
        public val newStatus: TrackerStatus,
        public val date: String? = null,
        public val note: String? = null,
    ) : JobApplicationDetailUiIntent
    public data class UpdateFitScore(public val newScore: Int) : JobApplicationDetailUiIntent
    public data class RecordEvent(
        public val type: TrackerEventType,
        public val notes: String = "",
    ) : JobApplicationDetailUiIntent
    public data class OpenArtifact(public val artifactId: String) : JobApplicationDetailUiIntent
    public data object ClickBack : JobApplicationDetailUiIntent
}
