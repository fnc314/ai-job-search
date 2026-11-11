package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * Single public image loading composable for the Job Tracker application.
 *
 * Delegates to Coil's [AsyncImage] using the application-configured [ImageLoader]
 * provided by [ProvideImageLoader]. All image loading within the app should flow
 * through this composable to ensure centralized observability and configuration.
 *
 * @param model The image data to load (URL string, file path, byte array, etc.).
 * @param contentDescription Text used by accessibility services to describe the image.
 * @param modifier Layout modifier.
 * @param contentScale How the image should be scaled to fit the target size.
 * @param onState Optional callback invoked when the image loading state changes.
 */
@Composable
public fun JobApplicationTrackerImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onState: ((AsyncImagePainter.State) -> Unit) = {},
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onState = onState,
    )
}
