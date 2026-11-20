package dev.fnc314.aijobtracker.frontend.features.signin.data

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredSession(
    val user: UserAccount,
    val customToken: String? = null,
)

internal interface CredentialStore {
    fun saveSession(session: StoredSession)
    fun loadSession(): StoredSession?
    fun clearSession()
}

internal class KeychainCredentialStore : CredentialStore {
    private val serviceName = "dev.fnc314.aijobtracker.auth"
    private val accountName = "currentUserSession"
    private val isMac = System.getProperty("os.name")?.lowercase()?.contains("mac") == true
    private val fallbackStore by lazy { EncryptedFileCredentialStore() }

    override fun saveSession(session: StoredSession) {
        if (!isMac) {
            fallbackStore.saveSession(session)
            return
        }

        val jsonString = Json.encodeToString(session)
        val deleteProcess = ProcessBuilder(
            "/usr/bin/security",
            "delete-generic-password",
            "-s", serviceName,
            "-a", accountName
        ).start()
        deleteProcess.waitFor()

        val addProcess = ProcessBuilder(
            "/usr/bin/security",
            "add-generic-password",
            "-s", serviceName,
            "-a", accountName,
            "-w", jsonString,
            "-U"
        ).start()
        val exitCode = addProcess.waitFor()
        if (exitCode != 0) {
            val error = addProcess.errorStream.bufferedReader().readText()
            System.err.println("KeychainCredentialStore save warning ($exitCode): $error. Falling back to encrypted file store.")
            fallbackStore.saveSession(session)
        }
    }

    override fun loadSession(): StoredSession? {
        if (!isMac) {
            return fallbackStore.loadSession()
        }

        val process = ProcessBuilder(
            "/usr/bin/security",
            "find-generic-password",
            "-s", serviceName,
            "-a", accountName,
            "-w"
        ).start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            return fallbackStore.loadSession()
        }

        val output = process.inputStream.bufferedReader().readText().trim()
        if (output.isBlank()) return fallbackStore.loadSession()

        return try {
            Json.decodeFromString<StoredSession>(output)
        } catch (e: Exception) {
            fallbackStore.loadSession()
        }
    }

    override fun clearSession() {
        if (isMac) {
            val process = ProcessBuilder(
                "/usr/bin/security",
                "delete-generic-password",
                "-s", serviceName,
                "-a", accountName
            ).start()
            process.waitFor()
        }
        fallbackStore.clearSession()
    }
}

/**
 * Fallback credential store for non-macOS systems using AES-GCM encryption with local key protection.
 */
internal class EncryptedFileCredentialStore : CredentialStore {
    private val vaultFile = File(System.getProperty("user.home"), ".config/ai-job-tracker/auth.vault")
    private val secretKey: SecretKey by lazy { getOrCreateKey() }

    private fun getOrCreateKey(): SecretKey {
        val keyFile = File(System.getProperty("user.home"), ".config/ai-job-tracker/.auth.key")
        keyFile.parentFile.mkdirs()
        if (keyFile.exists() && keyFile.length() == 32L) {
            val keyBytes = keyFile.readBytes()
            return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
        }
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        keyFile.writeBytes(key.encoded)
        try {
            keyFile.setReadable(false, false)
            keyFile.setReadable(true, true)
            keyFile.setWritable(false, false)
            keyFile.setWritable(true, true)
        } catch (_: Exception) {}
        return key
    }

    override fun saveSession(session: StoredSession) {
        val jsonString = Json.encodeToString(session)
        val plainBytes = jsonString.toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)

        vaultFile.parentFile.mkdirs()
        vaultFile.outputStream().use { os ->
            os.write(iv.size)
            os.write(iv)
            os.write(encrypted)
        }
    }

    override fun loadSession(): StoredSession? {
        if (!vaultFile.exists() || vaultFile.length() == 0L) return null
        return try {
            val bytes = vaultFile.readBytes()
            val ivSize = bytes[0].toInt()
            val iv = bytes.copyOfRange(1, 1 + ivSize)
            val cipherText = bytes.copyOfRange(1 + ivSize, bytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val plainBytes = cipher.doFinal(cipherText)
            val jsonString = String(plainBytes, StandardCharsets.UTF_8)
            Json.decodeFromString<StoredSession>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    override fun clearSession() {
        if (vaultFile.exists()) {
            vaultFile.delete()
        }
    }
}
