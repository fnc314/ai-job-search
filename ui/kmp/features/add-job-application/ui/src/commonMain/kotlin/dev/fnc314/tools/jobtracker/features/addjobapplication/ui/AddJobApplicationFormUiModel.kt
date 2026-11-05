package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class AddJobApplicationFormUiModel(
    public val company: String = "",
    public val role: String = "",
    public val sector: String = "",
    public val roleType: String = "",
    public val workMode: WorkMode = WorkMode.UNSPECIFIED,
    public val channel: ApplicationChannel = ApplicationChannel.UNSPECIFIED,
    public val status: TrackerStatus = TrackerStatus.APPLIED,
    public val jobUrl: String = "",
    public val jobApplicationUrl: String = "",
    public val jobId: String = "",
    public val location: String = "",
    public val notes: ImmutableList<String> = persistentListOf(),
    public val tags: String = "",
    public val validationError: String? = null,
)
