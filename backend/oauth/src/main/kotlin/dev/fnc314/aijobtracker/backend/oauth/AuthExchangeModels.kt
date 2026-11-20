package dev.fnc314.aijobtracker.backend.oauth

import kotlinx.serialization.Serializable

/**
 * Authentication exchange request model supporting multiple identity providers.
 *
 * @property provider Identity provider name (e.g. "google", "apple"). Defaults to "google".
 * @property code Authorization code received from the OAuth 2.0 PKCE flow.
 * @property idToken Direct identity token from providers supporting native client SDK handshakes.
 * @property codeVerifier PKCE code verifier generated on the client.
 * @property redirectUri Loopback or callback URI registered with the OAuth provider.
 */
@Serializable
public data class AuthExchangeRequest(
    public val provider: String = "google",
    public val code: String? = null,
    public val idToken: String? = null,
    public val codeVerifier: String? = null,
    public val redirectUri: String? = null,
)

/**
 * Authenticated user profile information returned to clients.
 *
 * @property uid Unique identity identifier.
 * @property email Primary email address of the account.
 * @property displayName User's public display name.
 * @property photoUrl Avatar image URL.
 */
@Serializable
public data class AuthUser(
    public val uid: String,
    public val email: String? = null,
    public val displayName: String? = null,
    public val photoUrl: String? = null,
)

/**
 * Successful authentication session containing a Firebase custom auth token and user profile.
 *
 * @property status Operation status, defaults to "SUCCESS".
 * @property customToken Firebase Custom Auth Token minted for the user.
 * @property user Authenticated user details.
 */
@Serializable
public data class AuthSession(
    public val status: String = "SUCCESS",
    public val customToken: String,
    public val user: AuthUser,
)
