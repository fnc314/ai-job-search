package dev.fnc314.aijobtracker.frontend.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

public actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
