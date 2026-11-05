package dev.fnc314.tools.jobtracker.features.signin.ui

/**
 * User-initiated intents dispatched to [SignInViewModel].
 */
public sealed interface SignInUiIntent {
    /** Initiates Google Sign-In or platform OAuth credential flow. */
    public data object ClickSignIn : SignInUiIntent

    /** Terminates the active authenticated user session. */
    public data object ClickSignOut : SignInUiIntent

    /** Dismisses active authentication error alerts. */
    public data object ClearError : SignInUiIntent
}
