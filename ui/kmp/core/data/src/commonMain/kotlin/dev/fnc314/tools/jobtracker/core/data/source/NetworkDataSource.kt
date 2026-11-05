package dev.fnc314.tools.jobtracker.core.data.source

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

public interface NetworkDataSource {
    public val client: HttpClient
    public suspend fun get(url: String): String
}

internal class NetworkDataSourceImpl(
    override val client: HttpClient,
) : NetworkDataSource {
    override suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }
}
