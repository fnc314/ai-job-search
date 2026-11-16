package dev.fnc314.aijobtracker.frontend.core.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.data.platform.PdfDocumentRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Universal in-app PDF document viewer composable for Compose Multiplatform.
 *
 * Renders pages lazily with pinch-to-zoom and pan support without requiring
 * external viewing applications.
 *
 * @param documentBytes Raw byte array of the PDF document.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun PdfViewer(
    documentBytes: ByteArray,
    modifier: Modifier = Modifier
) {
    var renderer by remember(documentBytes) { mutableStateOf<PdfDocumentRenderer?>(null) }
    var pageBitmaps by remember(documentBytes) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var isLoading by remember(documentBytes) { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    DisposableEffect(documentBytes) {
        val r = PdfDocumentRenderer(documentBytes)
        renderer = r
        onDispose {
            r.close()
        }
    }

    LaunchedEffect(renderer) {
        val currentRenderer = renderer ?: return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.Default) {
            val pages = mutableListOf<ImageBitmap>()
            for (i in 0 until currentRenderer.pageCount) {
                currentRenderer.renderPage(i, 0, 0)?.let { pages.add(it) }
            }
            pageBitmaps = pages
            isLoading = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pageBitmaps.isEmpty() -> {
                    Text(
                        text = "Unable to render PDF document",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    ) {
                        items(pageBitmaps.size) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = pageBitmaps[index],
                                    contentDescription = "Page ${index + 1} of ${pageBitmaps.size}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
