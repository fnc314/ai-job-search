package dev.fnc314.aijobtracker.frontend.core.data.platform

import androidx.compose.ui.graphics.ImageBitmap

public actual class PdfDocumentRenderer actual constructor(documentBytes: ByteArray) {

    public actual val pageCount: Int = 1

    public actual fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): ImageBitmap? {
        // PDF.js / canvas worker rendering
        return null
    }

    public actual fun close() {
    }
}
