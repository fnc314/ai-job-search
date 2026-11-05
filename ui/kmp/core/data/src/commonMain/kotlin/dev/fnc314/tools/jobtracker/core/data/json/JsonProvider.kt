package dev.fnc314.tools.jobtracker.core.data.json

import kotlinx.serialization.json.Json

public val globalJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
    prettyPrint = false
}
