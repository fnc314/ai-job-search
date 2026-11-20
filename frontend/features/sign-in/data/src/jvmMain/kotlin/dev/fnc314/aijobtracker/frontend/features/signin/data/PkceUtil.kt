package dev.fnc314.aijobtracker.frontend.features.signin.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal object PkceUtil {
    private val secureRandom = SecureRandom()

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun generateState(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
