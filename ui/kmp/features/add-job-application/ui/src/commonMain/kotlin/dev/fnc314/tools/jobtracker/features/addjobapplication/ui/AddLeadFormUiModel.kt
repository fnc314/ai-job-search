package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class AddLeadFormUiModel(
    public val company: String = "",
    public val contact: String = "",
    public val role: String = "",
    public val channel: String = "",
    public val notes: ImmutableList<String> = persistentListOf(),
    public val tags: String = "",
    public val validationError: String? = null,
)
