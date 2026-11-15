package dev.fnc314.tools.jobtracker.core.network

import io.ktor.client.HttpClientConfig

/**
 * Installs the network traffic inspector plugin into [HttpClientConfig] on supported platforms.
 */
public expect fun HttpClientConfig<*>.installInspector()
