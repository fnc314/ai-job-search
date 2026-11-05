package dev.fnc314.tools.jobtracker.core.data.source

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * HTTP client abstraction for remote REST queries and cloud function invocations.
 */
public interface NetworkDataSource {
    /** Configured Ktor [HttpClient] instance with JSON content negotiation. */
    public val client: HttpClient

    /**
     * Executes an asynchronous HTTP GET request, returning the body text.
     *
     * @param url Fully-qualified HTTP/HTTPS endpoint URL.
     * @return Response body as a string.
     */
    public suspend fun get(url: String): String
}

internal class NetworkDataSourceImpl(
    override val client: HttpClient,
) : NetworkDataSource {
    override suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }
}
