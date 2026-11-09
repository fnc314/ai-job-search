package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Form presentation model capturing input values for creating a new job application.
 *
 * @property company Name of the hiring organization.
 * @property role Position or job title.
 * @property sector Business domain or market sector.
 * @property roleType Level or specialty classification.
 * @property workMode Expected work format ([WorkMode.REMOTE], [WorkMode.HYBRID], [WorkMode.ONSITE]).
 * @property channel Application submission method ([ApplicationChannel]).
 * @property status Initial lifecycle state ([TrackerStatus]).
 * @property jobUrl Web link to the public job posting.
 * @property jobApplicationUrl Direct link to the submission or confirmation portal.
 * @property jobId External job opening reference code.
 * @property location Geographic city, state, or regional scope.
 * @property notes Chronological or contextual comments.
 * @property tags Comma-delimited classification labels.
 * @property validationError User-facing validation message, or `null` if valid.
 */
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
