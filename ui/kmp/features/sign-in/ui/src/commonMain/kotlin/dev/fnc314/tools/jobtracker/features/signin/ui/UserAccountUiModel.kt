package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

/**
 * Presentation model displaying active user identity and profile metadata.
 *
 * @property email Primary authenticated user email address.
 * @property displayName User display or full name.
 * @property photoUrl URL pointing to user profile avatar or photo.
 */
@Immutable
public data class UserAccountUiModel(
    public val email: String,
    public val displayName: String?,
    public val photoUrl: String?,
)

/**
 * Transforms a domain [UserAccount] into a presentation [UserAccountUiModel].
 *
 * @return Mapped [UserAccountUiModel] ready for UI binding.
 */
public fun UserAccount.toUiModel(): UserAccountUiModel = UserAccountUiModel(
    email = email.orEmpty(),
    displayName = displayName,
    photoUrl = photoUrl,
)
