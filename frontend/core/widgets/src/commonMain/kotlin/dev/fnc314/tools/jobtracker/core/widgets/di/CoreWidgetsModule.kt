package dev.fnc314.tools.jobtracker.core.widgets.di

import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.util.DebugLogger
import org.koin.core.module.Module
import org.koin.dsl.module

/** Called when an image load request starts. */
public fun interface OnImageLoadStart {
    public operator fun invoke(url: String)
}

/** Called when an image load request completes successfully. [durationMs] is the elapsed time in milliseconds. */
public fun interface OnImageLoadSuccess {
    public operator fun invoke(url: String, durationMs: Long)
}

/** Called when an image load request fails. */
public fun interface OnImageLoadError {
    public operator fun invoke(url: String, throwable: Throwable)
}

/** Called when an image load request is cancelled. */
public fun interface OnImageLoadCancel {
    public operator fun invoke(url: String)
}

/**
 * Unified interface for observing image load lifecycle events.
 *
 * Aggregates the four [OnImageLoadStart], [OnImageLoadSuccess], [OnImageLoadError], and
 * [OnImageLoadCancel] callbacks so that a single instance can be attached to the [ImageLoader].
 */
public interface ImageLoadEventListener {
    public fun onLoadStart(url: String)
    public fun onLoadSuccess(url: String, durationMs: Long)
    public fun onLoadError(url: String, throwable: Throwable)
    public fun onLoadCancel(url: String)
}

/**
 * Default [ImageLoadEventListener] that delegates each lifecycle method to the corresponding
 * [OnImageLoad*] callback.
 *
 * All callbacks default to no-op lambdas, so callers can supply only the events they care about.
 */
public class DefaultImageLoadEventListener(
    private val onStart: OnImageLoadStart = OnImageLoadStart {},
    private val onSuccess: OnImageLoadSuccess = OnImageLoadSuccess { _, _ -> },
    private val onError: OnImageLoadError = OnImageLoadError { _, _ -> },
    private val onCancel: OnImageLoadCancel = OnImageLoadCancel {},
) : ImageLoadEventListener {
    override fun onLoadStart(url: String): Unit = onStart(url)
    override fun onLoadSuccess(url: String, durationMs: Long): Unit = onSuccess(url, durationMs)
    override fun onLoadError(url: String, throwable: Throwable): Unit = onError(url, throwable)
    override fun onLoadCancel(url: String): Unit = onCancel(url)
}

/**
 * Bridges [ImageLoadEventListener] to Coil's [EventListener] so the Koin-injected listener
 * can be attached to the [ImageLoader].
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
        val durationMs = (System.nanoTime() - startTimeNanos) / 1_000_000
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
 * Creates a configured [ImageLoader] with [DebugLogger] and Ktor3 network fetcher,
 * wired to the given [eventListener] for observability.
 */
internal fun createImageLoader(
    context: PlatformContext,
    eventListener: ImageLoadEventListener,
): ImageLoader = ImageLoader.Builder(context)
    .logger(DebugLogger())
    .eventListenerFactory { CoilEventListenerBridge(eventListener) }
    .components {
        add(KtorNetworkFetcherFactory())
    }
    .build()

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
