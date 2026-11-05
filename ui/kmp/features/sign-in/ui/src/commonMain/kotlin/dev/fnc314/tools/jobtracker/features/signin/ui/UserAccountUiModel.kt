package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

@Immutable
public data class UserAccountUiModel(
    public val email: String,
    public val displayName: String?,
    public val photoUrl: String?,
)

public fun UserAccount.toUiModel(): UserAccountUiModel = UserAccountUiModel(
    email = email.orEmpty(),
    displayName = displayName,
    photoUrl = photoUrl,
)
