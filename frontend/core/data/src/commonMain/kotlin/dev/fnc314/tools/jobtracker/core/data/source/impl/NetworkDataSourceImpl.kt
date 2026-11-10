package dev.fnc314.tools.jobtracker.core.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class NetworkDataSourceImpl(
    override val client: HttpClient,
) : NetworkDataSource {
    override suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }

    override suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String>,
    ): String {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
            headers.forEach { (k, v) -> header(k, v) }
        }.bodyAsText()
    }
}
