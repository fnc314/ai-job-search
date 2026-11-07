package dev.fnc314.tools.jobtracker.core.network

import com.waqas028.kmpinspector.ktor.KmpInspectorPlugin
import io.ktor.client.HttpClientConfig

public actual fun HttpClientConfig<*>.installInspector() {
    install(KmpInspectorPlugin)
}
