package dev.fnc314.tools.jobtracker.features.signin.ui

public sealed interface SignInUiIntent {
    public data object ClickSignIn : SignInUiIntent
    public data object ClickSignOut : SignInUiIntent
    public data object ClearError : SignInUiIntent
}
