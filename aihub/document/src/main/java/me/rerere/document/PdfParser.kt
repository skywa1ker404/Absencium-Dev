package me.rerere.document

import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import java.io.File

data class PdfPageContent(
    val pageNumber: Int,
    val text: String,
)

object PdfParser {
    private const val DEFAULT_RENDER_DPI = 150


    fun extractPages(file: File): List<PdfPageContent> {
        val document = PDFDocument.openDocument(file.absolutePath).asPDF()
        return try {
            val pages = document.countPages()
            buildList(capacity = pages) {
                for (pageIndex in 0 until pages) {
                    val page = document.loadPage(pageIndex)
                    try {
                        val structuredText = page.toStructuredText()
                        try {
                            add(
                                PdfPageContent(
                                    pageNumber = pageIndex + 1,
                                    text = structuredText.asText()
                                )
                            )
                        } finally {
                            structuredText.destroy()
                        }
                    } finally {
                        page.destroy()
                    }
                }
            }
        } finally {
            document.destroy()
        }
    }

    fun renderPageAsPng(
        file: File,
        pageIndex: Int,
        outputFile: File,
        dpi: Int = DEFAULT_RENDER_DPI,
    ): File {
        outputFile.parentFile?.mkdirs()

        val document = PDFDocument.openDocument(file.absolutePath).asPDF()
        return try {
            val page = document.loadPage(pageIndex)
            try {
                val scale = dpi / 72f
                val pixmap = page.toPixmap(
                    Matrix.Scale(scale, scale),
                    ColorSpace.DeviceRGB,
                    false
                )
                try {
                    pixmap.saveAsPNG(outputFile.absolutePath)
                } finally {
                    pixmap.destroy()
                }
            } finally {
                page.destroy()
            }
            outputFile
        } finally {
            document.destroy()
        }
    }
}
