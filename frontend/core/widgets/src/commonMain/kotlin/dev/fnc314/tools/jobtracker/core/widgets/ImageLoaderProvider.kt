package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.ImageLoader
import dev.fnc314.tools.jobtracker.core.widgets.di.ImageLoadEventListener
import dev.fnc314.tools.jobtracker.core.widgets.di.createImageLoader
import org.koin.compose.koinInject

/**
 * CompositionLocal providing the application-configured [ImageLoader].
 *
 * Populated by [ProvideImageLoader] at the app root. Falls back to Coil's default
 * singleton if used outside the provider (should not happen in normal usage).
 */
public val LocalJobTrackerImageLoader: ProvidableCompositionLocal<ImageLoader> = compositionLocalOf {
    error("No ImageLoader provided. Wrap your app content with ProvideImageLoader.")
}

/**
 * App-level wrapper that configures Coil's singleton [ImageLoader] for the entire composition.
 *
 * Calls [setSingletonImageLoaderFactory] so that all [coil3.compose.AsyncImage] calls — including
 * those in third-party libraries — use the configured loader with observability hooks.
 *
 * This composable follows the same pattern as [JobTrackerTheme] and [ProvideWindowSizeClass]:
 * it wraps the app content and sets up a global resource.
 *
 * @param content Child composable content that uses the configured image loader.
 */
@Composable
public fun ProvideImageLoader(
    content: @Composable () -> Unit,
) {
    val context = LocalPlatformContext.current
    val eventListener: ImageLoadEventListener = koinInject()

    val imageLoader = remember(context) {
        createImageLoader(context, eventListener)
    }

    setSingletonImageLoaderFactory { imageLoader }

    CompositionLocalProvider(LocalJobTrackerImageLoader provides imageLoader) {
        content()
    }
}
