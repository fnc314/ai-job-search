package dev.fnc314.aijobtracker.frontend.core.data.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Multiplatform PDF document renderer.
 *
 * Provides page count and page rasterization into [ImageBitmap] instances
 * for native Compose rendering without external viewing applications.
 */
public expect class PdfDocumentRenderer(documentBytes: ByteArray) {

    /** Total number of pages in the PDF document. */
    public val pageCount: Int

    /**
     * Renders a specific page index to an [ImageBitmap].
     *
     * @param pageIndex Zero-based page number.
     * @param targetWidth Desired rasterized pixel width.
     * @param targetHeight Desired rasterized pixel height.
     * @return Rendered [ImageBitmap] suitable for Compose Image display.
     */
    public fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): ImageBitmap?

    /** Closes underlying native resources and document handles. */
    public fun close()
}
