package dev.fnc314.tools.jobtracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform