package dev.fnc314.tools.jobtracker.core.widgets.di

import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.HttpException
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dev.fnc314.tools.jobtracker.core.logging.AppLogger
import org.koin.core.module.Module
import org.koin.dsl.module

private const val NANOS_PER_MILLISECOND = 1_000_000L
private val HTTP_CODE_REGEX = Regex("""\b([45]\d{2})\b""")

/**
 * Extracts an HTTP status code from a Coil or network [Throwable] when available.
 *
 * Inspects [HttpException.response] first, then falls back to scanning the exception message
 * and its cause chain for a 4xx or 5xx status code.
 *
 * @param throwable The exception raised during image loading, or `null`.
 * @return The integer HTTP status code (e.g., `404`, `500`), or `null` if non-HTTP.
 */
public fun extractHttpStatusCode(throwable: Throwable?): Int? {
    var current: Throwable? = throwable
    while (current != null) {
        if (current is HttpException) {
            return current.response.code
        }
        val messageMatch = current.message?.let { HTTP_CODE_REGEX.find(it) }
        if (messageMatch != null) {
            val code = messageMatch.groupValues[1].toIntOrNull()
            if (code != null) {
                return code
            }
        }
        current = current.cause
    }
    return null
}

/** Called when an image load request starts. */
public fun interface OnImageLoadStart {
    /**
     * Invoked when an image request begins loading.
     *
     * @param url The target image URL string.
     */
    public operator fun invoke(url: String)
}

/** Called when an image load request completes successfully. [durationMs] is the elapsed time in milliseconds. */
public fun interface OnImageLoadSuccess {
    /**
     * Invoked when an image request succeeds.
     *
     * @param url The target image URL string.
     * @param durationMs Elapsed load time in milliseconds.
     */
    public operator fun invoke(url: String, durationMs: Long)
}

/** Called when an image load request fails. */
public fun interface OnImageLoadError {
    /**
     * Invoked when an image request fails with an error.
     *
     * @param url The target image URL string.
     * @param throwable The failure cause.
     */
    public operator fun invoke(url: String, throwable: Throwable)
}

/** Called when an image load request is cancelled. */
public fun interface OnImageLoadCancel {
    /**
     * Invoked when an image request is cancelled.
     *
     * @param url The target image URL string.
     */
    public operator fun invoke(url: String)
}

/**
 * Unified interface for observing image load lifecycle events.
 *
 * Aggregates the four [OnImageLoadStart], [OnImageLoadSuccess], [OnImageLoadError], and
 * [OnImageLoadCancel] callbacks so that a single instance can be attached to the [ImageLoader].
 */
public interface ImageLoadEventListener {
    /**
     * Called when an image request starts.
     *
     * @param url The target image URL string.
     */
    public fun onLoadStart(url: String)

    /**
     * Called when an image request succeeds.
     *
     * @param url The target image URL string.
     * @param durationMs Elapsed time in milliseconds.
     */
    public fun onLoadSuccess(url: String, durationMs: Long)

    /**
     * Called when an image request fails.
     *
     * @param url The target image URL string.
     * @param throwable The failure cause.
     */
    public fun onLoadError(url: String, throwable: Throwable)

    /**
     * Called when an image request is cancelled.
     *
     * @param url The target image URL string.
     */
    public fun onLoadCancel(url: String)
}

/**
 * Default [ImageLoadEventListener] that delegates each lifecycle method to the corresponding
 * [OnImageLoadStart], [OnImageLoadSuccess], [OnImageLoadError], and [OnImageLoadCancel] callbacks.
 *
 * All callbacks default to no-op lambdas, so callers can supply only the events they care about.
 *
 * @param onStart Callback invoked when an image load starts.
 * @param onSuccess Callback invoked when an image load succeeds.
 * @param onError Callback invoked when an image load fails.
 * @param onCancel Callback invoked when an image load is cancelled.
 */
public class DefaultImageLoadEventListener(
    private val onStart: OnImageLoadStart = OnImageLoadStart {},
    private val onSuccess: OnImageLoadSuccess = OnImageLoadSuccess { _, _ -> },
    private val onError: OnImageLoadError = OnImageLoadError { url, throwable ->
        val httpCode = extractHttpStatusCode(throwable)
        val statusDetail = httpCode?.let { "HTTP $it" } ?: "NON_HTTP_ERROR"
        AppLogger.e { "Image load failed [$statusDetail] for url=$url: ${throwable.message}" }
    },
    private val onCancel: OnImageLoadCancel = OnImageLoadCancel {},
) : ImageLoadEventListener {
    override fun onLoadStart(url: String): Unit = onStart(url)
    override fun onLoadSuccess(url: String, durationMs: Long): Unit = onSuccess(url, durationMs)
    override fun onLoadError(url: String, throwable: Throwable): Unit = onError(url, throwable)
    override fun onLoadCancel(url: String): Unit = onCancel(url)
}

/**
 * Bridges [ImageLoadEventListener] to Coil's [EventListener] so the Koin-injected listener
 * can be attached to the [ImageLoader] and emit detailed HTTP error diagnostics via [AppLogger].
 */
internal class CoilEventListenerBridge(
    private val delegate: ImageLoadEventListener,
) : EventListener() {
    private var startTimeNanos: Long = 0

    override fun onStart(request: ImageRequest) {
        startTimeNanos = System.nanoTime()
        val url = request.data.toString()
        delegate.onLoadStart(url)
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        val url = request.data.toString()
        val durationMs = (System.nanoTime() - startTimeNanos) / NANOS_PER_MILLISECOND
        delegate.onLoadSuccess(url, durationMs)
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        val url = request.data.toString()
        delegate.onLoadError(url, result.throwable)
    }

    override fun onCancel(request: ImageRequest) {
        val url = request.data.toString()
        delegate.onLoadCancel(url)
    }
}

/**
 * Creates a configured [ImageLoader] with Ktor3 network fetcher,
 * wired to the given [eventListener] for observability.
 */
internal fun createImageLoader(
    context: PlatformContext,
    eventListener: ImageLoadEventListener,
): ImageLoader {
    return ImageLoader.Builder(context)
        .eventListenerFactory { CoilEventListenerBridge(eventListener) }
        .components {
            add(KtorNetworkFetcherFactory())
        }
        .build()
}

/**
 * Koin DI module for core widgets.
 *
 * Registers:
 * - [ImageLoadEventListener] (default no-op implementation)
 *
 * The [ImageLoader] is created at the app root via [createImageLoader] inside
 * [dev.fnc314.tools.jobtracker.core.widgets.ProvideImageLoader], which calls
 * `setSingletonImageLoaderFactory` so all `AsyncImage` composables use the configured loader.
 */
public val coreWidgetsModule: Module = module {
    single<ImageLoadEventListener> { DefaultImageLoadEventListener() }
}
