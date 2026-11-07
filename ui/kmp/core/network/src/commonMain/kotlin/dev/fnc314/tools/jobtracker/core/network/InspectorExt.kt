package dev.fnc314.tools.jobtracker.core.network

import io.ktor.client.HttpClientConfig

public expect fun HttpClientConfig<*>.installInspector()
