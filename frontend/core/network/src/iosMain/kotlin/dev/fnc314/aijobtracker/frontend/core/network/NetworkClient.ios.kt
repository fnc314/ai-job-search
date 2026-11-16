package dev.fnc314.aijobtracker.frontend.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin)
