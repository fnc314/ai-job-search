package dev.fnc314.tools.jobtracker.platform

expect class PlatformAuth() {
    suspend fun signIn(): Boolean
    fun signOut()
    fun isAuthenticated(): Boolean
}
