package dev.fnc314.aijobtracker.frontend.core.widgets.di

import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlin.time.Clock

private class CoilEventListenerBridge(
    private val delegate: ImageLoadEventListener,
) : EventListener() {
    private var startTimeMs: Long = 0

    override fun onStart(request: ImageRequest) {
        startTimeMs = Clock.System.now().toEpochMilliseconds()
        val url = request.data.toString()
        delegate.onLoadStart(url)
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        val url = request.data.toString()
        val durationMs = Clock.System.now().toEpochMilliseconds() - startTimeMs
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

internal actual fun createImageLoader(
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
