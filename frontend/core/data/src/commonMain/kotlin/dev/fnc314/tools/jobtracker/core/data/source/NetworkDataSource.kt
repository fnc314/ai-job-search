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

    /**
     * Executes an asynchronous HTTP POST request with a JSON body and custom headers, returning the response body text.
     *
     * @param url Fully-qualified HTTP/HTTPS endpoint URL.
     * @param jsonBody Serialized JSON string payload.
     * @param headers Optional HTTP headers to include in the request.
     * @return Response body as a string.
     */
    public suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap(),
    ): String
}

