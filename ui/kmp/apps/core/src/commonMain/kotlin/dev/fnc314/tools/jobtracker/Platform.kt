package dev.fnc314.tools.jobtracker

internal interface Platform {
    val name: String
}

internal expect fun getPlatform(): Platform