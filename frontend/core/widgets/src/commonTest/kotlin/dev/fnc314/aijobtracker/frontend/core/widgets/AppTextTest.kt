package dev.fnc314.aijobtracker.frontend.core.widgets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTextTest {

    @Test
    fun buildLinkedAnnotatedString_withoutUrls_returnsExactPlainText() {
        val input = "Simple text without any hyperlinks."
        val result = buildLinkedAnnotatedString(input, Color.Blue)

        assertEquals(input, result.text)
        assertTrue(result.getLinkAnnotations(0, result.length).isEmpty())
    }

    @Test
    fun buildLinkedAnnotatedString_withHttpsUrl_createsLinkAnnotation() {
        val input = "Visit https://motorolasolutions.com for more info"
        val result = buildLinkedAnnotatedString(input, Color.Blue)

        assertEquals(input, result.text)
        val links = result.getLinkAnnotations(0, result.length)
        assertEquals(1, links.size)
        val link = links.first().item
        assertTrue(link is LinkAnnotation.Url)
        assertEquals("https://motorolasolutions.com", link.url)
    }

    @Test
    fun buildLinkedAnnotatedString_withTrailingPunctuation_stripsPunctuationFromUrl() {
        val input = "Check out https://google.com, and email mailto:user@test.com."
        val result = buildLinkedAnnotatedString(input, Color.Blue)

        assertEquals(input, result.text)
        val links = result.getLinkAnnotations(0, result.length)
        assertEquals(2, links.size)

        val firstLink = links[0].item as LinkAnnotation.Url
        assertEquals("https://google.com", firstLink.url)

        val secondLink = links[1].item as LinkAnnotation.Url
        assertEquals("mailto:user@test.com", secondLink.url)
    }
}
