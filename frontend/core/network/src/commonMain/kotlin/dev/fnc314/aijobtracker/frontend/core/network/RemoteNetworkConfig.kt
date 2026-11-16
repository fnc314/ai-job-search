package dev.fnc314.aijobtracker.frontend.core.network

/**
 * Global network environment configuration for remote service endpoints.
 */
public object RemoteNetworkConfig {
    /**
     * Default base URL for the Cloud Run backend service.
     */
    public const val DEFAULT_REMOTE_BACKEND_URL: String = "https://dev-fnc314-ai-job-tracker-backend-375389268913.us-east1.run.app"

    /**
     * Header key used for authenticating with the backend API.
     */
    public const val API_KEY_HEADER: String = "X-Api-Key"

    /**
     * Active backend base URL.
     */
    public var baseUrl: String = DEFAULT_REMOTE_BACKEND_URL

    /**
     * Active API key passed with requests.
     */
    public var apiKey: String? = null
}
