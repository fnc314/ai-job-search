package dev.fnc314.tools.jobtracker.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

public actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js)
