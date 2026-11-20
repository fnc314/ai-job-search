package dev.fnc314.aijobtracker.frontend.features.signin.data

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount
import dev.fnc314.aijobtracker.frontend.core.network.RemoteNetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class AuthExchangePayload(
    val provider: String = "google",
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)

internal actual class PlatformSignInHandler {
    private val credentialStore by lazy { KeychainCredentialStore() }
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy { HttpClient(CIO) }

    actual suspend fun signIn(): Result<UserAccount> = runCatching {
        val existingSession = credentialStore.loadSession()
        if (existingSession != null) {
            return@runCatching existingSession.user
        }

        val codeVerifier = PkceUtil.generateCodeVerifier()
        val codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier)
        val state = PkceUtil.generateState()

        val server = DesktopOAuthServer()
        val port = server.start()
        val redirectUri = "http://127.0.0.1:$port/callback"

        val clientId = "375389268913-83qulec8m6ap7pgrtgk2sg64v9l9ket2.apps.googleusercontent.com"
        val authUrl = StringBuilder("https://accounts.google.com/o/oauth2/v2/auth")
            .append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
            .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
            .append("&response_type=code")
            .append("&scope=").append(URLEncoder.encode("openid email profile", StandardCharsets.UTF_8))
            .append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8))
            .append("&code_challenge_method=S256")
            .append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8))
            .append("&access_type=offline")
            .append("&prompt=consent")
            .toString()

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                val os = System.getProperty("os.name")?.lowercase() ?: ""
                when {
                    os.contains("mac") -> ProcessBuilder("open", authUrl).start()
                    os.contains("linux") -> ProcessBuilder("xdg-open", authUrl).start()
                    os.contains("windows") -> ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", authUrl).start()
                    else -> throw UnsupportedOperationException("Unable to open browser on this platform")
                }
            }

            // Await loopback callback with 2 minute timeout
            val callbackResult = withTimeout(120_000L) {
                server.callbackDeferred.await()
            }

            if (callbackResult.error != null) {
                throw IllegalStateException("Google OAuth authentication error: ${callbackResult.error}")
            }

            val code = callbackResult.code ?: throw IllegalStateException("Authorization code missing in OAuth callback")
            if (callbackResult.state != state) {
                throw IllegalStateException("Invalid OAuth state parameter returned")
            }

            // Exchange with backend service
            val backendBaseUrl = RemoteNetworkConfig.baseUrl.trimEnd('/')
            val endpoint = "$backendBaseUrl/api/auth/sign-in"
            val requestPayload = AuthExchangePayload(
                provider = "google",
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri,
            )

            val httpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                RemoteNetworkConfig.apiKey?.takeIf { it.isNotBlank() }?.let { key ->
                    header(RemoteNetworkConfig.API_KEY_HEADER, key)
                }
                setBody(json.encodeToString(requestPayload))
            }

            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                throw IllegalStateException("Backend auth exchange failed (${httpResponse.status.value}): $responseBody")
            }

            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val status = responseJson["status"]?.jsonPrimitive?.content ?: "SUCCESS"
            if (status != "SUCCESS") {
                val msg = responseJson["message"]?.jsonPrimitive?.content ?: "Authentication failed"
                throw IllegalStateException(msg)
            }

            val userJson = responseJson["user"]?.jsonObject
                ?: throw IllegalStateException("Backend response missing user object")
            val customToken = responseJson["customToken"]?.jsonPrimitive?.content

            val user = UserAccount(
                uid = userJson["uid"]?.jsonPrimitive?.content ?: "",
                email = userJson["email"]?.jsonPrimitive?.content,
                displayName = userJson["displayName"]?.jsonPrimitive?.content,
                photoUrl = userJson["photoUrl"]?.jsonPrimitive?.content,
            )

            credentialStore.saveSession(StoredSession(user = user, customToken = customToken))
            return@runCatching user
        } finally {
            server.stop()
        }
    }

    actual suspend fun getCurrentUser(): UserAccount? {
        return credentialStore.loadSession()?.user
    }

    actual suspend fun signOut(): Result<Unit> = runCatching {
        credentialStore.clearSession()
    }
}

internal actual fun createPlatformSignInHandler(): PlatformSignInHandler = PlatformSignInHandler()
