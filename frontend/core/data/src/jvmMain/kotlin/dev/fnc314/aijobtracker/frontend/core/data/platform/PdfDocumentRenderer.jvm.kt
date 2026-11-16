package dev.fnc314.aijobtracker.frontend.core.data.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage

public actual class PdfDocumentRenderer actual constructor(documentBytes: ByteArray) {

    private val document: PDDocument = Loader.loadPDF(documentBytes)
    private val renderer: PDFRenderer = PDFRenderer(document)

    public actual val pageCount: Int = document.numberOfPages

    public actual fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): ImageBitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        return try {
            val bufferedImage: BufferedImage = renderer.renderImageWithDPI(pageIndex, 150f)
            bufferedImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("JVM PDFBox render error: ${e.message}")
            null
        }
    }

    public actual fun close() {
        try {
            document.close()
        } catch (e: Exception) {
            println("Error closing JVM PDDocument: ${e.message}")
        }
    }
}
