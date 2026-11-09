package dev.fnc314.tools.jobtracker.core.data.json

import kotlinx.serialization.json.Json

/**
 * Shared preconfigured [Json] serializer instance used across the multiplatform client.
 *
 * Configured with:
 * - `ignoreUnknownKeys = true` for forward-compatible schema migrations.
 * - `isLenient = true` for resilient JSON parsing.
 * - `encodeDefaults = true` ensuring all explicit default properties are written out.
 * - `coerceInputValues = true` fallback-mapping malformed nulls or enum names.
 */
public val globalJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
    prettyPrint = false
}
