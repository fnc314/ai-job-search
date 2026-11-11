package dev.fnc314.tools.jobsearch.parser

import java.io.File
import java.time.Instant

/**
 * Exception thrown when cache operations fail.
 */
public class CacheError(message: String) : RuntimeException(message)

/**
 * Local read cache utilities mirroring Python `cache.py`.
 *
 * Handles reading, writing, and validating the offline cache file.
 * Uses plain JSON strings rather than typed serialization since the cache
 * contains arbitrary Firestore document maps.
 */
public object CacheUtils {

    /**
     * Load a cache from a JSON file as a raw string. Returns null if the file doesn't exist.
     */
    public fun loadRaw(path: File): String? {
        if (!path.exists()) return null
        try {
            return path.readText()
        } catch (e: Exception) {
            throw CacheError("${path.absolutePath} cannot be read: ${e.message}")
        }
    }

    /**
     * Save cache JSON atomically: write to a temp file, then rename.
     * Prevents partial writes from corrupting the cache.
     */
    public fun saveRaw(json: String, path: File): File {
        path.parentFile?.mkdirs()
        val temp = File(path.absolutePath + ".tmp")
        temp.writeText(json + "\n")
        temp.renameTo(path)
        return path
    }

    /**
     * Calculate the age of a cache in seconds given the generatedAt timestamp.
     */
    public fun ageSeconds(generatedAt: String, now: Instant? = null): Double? {
        if (generatedAt.isBlank()) return null
        val moment = try {
            Instant.parse(generatedAt.replace("Z", "+00:00"))
        } catch (e: Exception) {
            return null
        }
        val reference = now ?: Instant.now()
        return maxOf(0.0, java.time.Duration.between(moment, reference).toSeconds().toDouble())
    }

    /**
     * Check whether a cache file exists and is non-empty.
     */
    public fun isFresh(path: File, maxAgeSeconds: Long = 300): Boolean {
        if (!path.exists()) return false
        val content = loadRaw(path) ?: return false
        // Extract generatedAt from JSON using simple string search
        val generatedAt = extractGeneratedAt(content) ?: return false
        val age = ageSeconds(generatedAt) ?: return false
        return age <= maxAgeSeconds
    }

    private fun extractGeneratedAt(json: String): String? {
        val key = "\"generatedAt\""
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val rest = json.substring(idx + key.length)
        val colonIdx = rest.indexOf(':')
        if (colonIdx == -1) return null
        val afterColon = rest.substring(colonIdx + 1).trimStart()
        if (!afterColon.startsWith("\"")) return null
        val endQuote = afterColon.indexOf('"', 1)
        if (endQuote == -1) return null
        return afterColon.substring(1, endQuote)
    }
}
