package dev.fnc314.aijobtracker.backend.oauth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service orchestrating OAuth authorization code verification, token exchange with upstream
 * identity providers (e.g. Google), and minting Firebase custom tokens.
 */
public object OAuthExchangeService {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Executes OAuth code exchange or token verification for the specified identity provider.
     *
     * @param request Authentication payload from the client.
     * @param firebaseAuth Instance of [FirebaseAuth] for verifying tokens or minting custom tokens.
     * @return [AuthSession] containing user details and custom token.
     */
    public fun exchange(
        request: AuthExchangeRequest,
        firebaseAuth: FirebaseAuth,
    ): AuthSession {
        val provider = request.provider.lowercase()
        require(provider == "google") { "Unsupported identity provider: ${request.provider}" }

        val authUser = when {
            !request.idToken.isNullOrBlank() -> {
                val decodedToken = firebaseAuth.verifyIdToken(request.idToken)
                val userRecord = firebaseAuth.getUser(decodedToken.uid)
                AuthUser(
                    uid = userRecord.uid,
                    email = userRecord.email,
                    displayName = userRecord.displayName,
                    photoUrl = userRecord.photoUrl,
                )
            }
            !request.code.isNullOrBlank() && !request.redirectUri.isNullOrBlank() -> {
                exchangeGoogleCode(
                    code = request.code,
                    codeVerifier = request.codeVerifier,
                    redirectUri = request.redirectUri,
                    firebaseAuth = firebaseAuth,
                )
            }
            else -> {
                throw IllegalArgumentException("Missing authorization code or idToken with redirectUri")
            }
        }

        val claims = mutableMapOf<String, Any>()
        if (!authUser.email.isNullOrBlank()) {
            claims["email"] = authUser.email
        }
        val customToken = firebaseAuth.createCustomToken(authUser.uid, claims)

        return AuthSession(
            customToken = customToken,
            user = authUser,
        )
    }

    private fun exchangeGoogleCode(
        code: String,
        codeVerifier: String?,
        redirectUri: String,
        firebaseAuth: FirebaseAuth,
    ): AuthUser {
        val clientId = System.getenv("GOOGLE_OAUTH_CLIENT_ID")
            ?: "375389268913-83qulec8m6ap7pgrtgk2sg64v9l9ket2.apps.googleusercontent.com"
        val clientSecret = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET")

        val formData = mutableMapOf(
            "code" to code,
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "grant_type" to "authorization_code",
        )
        if (!codeVerifier.isNullOrBlank()) {
            formData["code_verifier"] = codeVerifier
        }
        if (!clientSecret.isNullOrBlank()) {
            formData["client_secret"] = clientSecret
        }

        val bodyString = formData.map { (k, v) ->
            URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)
        }.joinToString("&")

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(bodyString))
            .build()

        val httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (httpResponse.statusCode() !in 200..299) {
            throw IllegalStateException("Google token exchange failed: HTTP ${httpResponse.statusCode()} - ${httpResponse.body()}")
        }

        val responseJson = json.parseToJsonElement(httpResponse.body()).jsonObject
        val idToken = responseJson["id_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Token response missing id_token")

        val jwtParts = idToken.split(".")
        if (jwtParts.size < 2) {
            throw IllegalArgumentException("Malformed JWT id_token received from Google")
        }
        val payloadString = String(Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)
        val claims = json.parseToJsonElement(payloadString).jsonObject

        val googleSub = claims["sub"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Google id_token missing 'sub' claim")
        val email = claims["email"]?.jsonPrimitive?.content
        val name = claims["name"]?.jsonPrimitive?.content
        val picture = claims["picture"]?.jsonPrimitive?.content

        val userRecord = try {
            if (!email.isNullOrBlank()) {
                try {
                    firebaseAuth.getUserByEmail(email)
                } catch (e: Exception) {
                    firebaseAuth.getUser(googleSub)
                }
            } else {
                firebaseAuth.getUser(googleSub)
            }
        } catch (e: Exception) {
            val createRequest = UserRecord.CreateRequest()
                .setUid(googleSub)
                .setEmail(email)
                .setDisplayName(name)
                .setPhotoUrl(picture)
                .setEmailVerified(true)
            firebaseAuth.createUser(createRequest)
        }

        return AuthUser(
            uid = userRecord.uid,
            email = userRecord.email ?: email,
            displayName = userRecord.displayName ?: name,
            photoUrl = userRecord.photoUrl ?: picture,
        )
    }
}
