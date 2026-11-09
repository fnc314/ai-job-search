package dev.fnc314.tools.jobtracker.core.data.source

import io.ktor.client.HttpClient

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

