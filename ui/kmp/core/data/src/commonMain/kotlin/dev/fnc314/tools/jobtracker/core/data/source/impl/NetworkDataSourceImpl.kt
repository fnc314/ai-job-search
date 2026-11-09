package dev.fnc314.tools.jobtracker.core.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

internal class NetworkDataSourceImpl(
    override val client: HttpClient,
) : NetworkDataSource {
    override suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }
}
