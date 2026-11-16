package dev.fnc314.aijobtracker.frontend.core.data.platform

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

private const val TAG = "AIJT"

public actual class PdfDocumentRenderer actual constructor(documentBytes: ByteArray) {

    private val tempFile: File = File.createTempFile("pdf_render_", ".pdf").apply {
        deleteOnExit()
        FileOutputStream(this).use { it.write(documentBytes) }
    }

    private val fileDescriptor: ParcelFileDescriptor = ParcelFileDescriptor.open(
        tempFile,
        ParcelFileDescriptor.MODE_READ_ONLY
    )

    private val renderer: PdfRenderer = PdfRenderer(fileDescriptor)

    public actual val pageCount: Int = renderer.pageCount

    public actual fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): ImageBitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        return try {
            val page = renderer.openPage(pageIndex)
            val width = if (targetWidth > 0) targetWidth else page.width
            val height = if (targetHeight > 0) targetHeight else page.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Android PdfRenderer error: ${e.message}", e)
            null
        }
    }

    public actual fun close() {
        try {
            renderer.close()
            fileDescriptor.close()
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Android PdfRenderer: ${e.message}", e)
        }
    }
}
