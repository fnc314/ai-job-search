package dev.fnc314.tools.jobtracker.platform

actual class PlatformAuth actual constructor() {
    actual suspend fun signIn(): Boolean = false
    actual fun signOut() {}
    actual fun isAuthenticated(): Boolean = false
}
