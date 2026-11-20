package dev.fnc314.aijobtracker.frontend.core.widgets

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

private val URL_REGEX = Regex("(https?://[^\\s]+|mailto:[^\\s]+)")

/**
 * Universal text composable providing selectable, copiable text rendering with automatic
 * hyperlink recognition, styling, and navigation.
 *
 * @param text The plain text content to display.
 * @param modifier Composable modifier chain.
 * @param color Text color; defaults to [Color.Unspecified].
 * @param style Typography style token.
 * @param selectable When `true`, enables user text selection and clipboard copying. Defaults to `true`.
 * @param autoLink When `true`, auto-detects `https://`, `http://`, and `mailto:` links, applying
 *                 hyperlink styling and interactive click handlers. Defaults to `true`.
 */
@Composable
public fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    selectable: Boolean = true,
    autoLink: Boolean = true,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedContent = remember(text, autoLink, linkColor) {
        if (!autoLink) {
            AnnotatedString(text)
        } else {
            buildLinkedAnnotatedString(text, linkColor)
        }
    }

    AppText(
        text = annotatedContent,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
        selectable = selectable,
    )
}

/**
 * Overload of [AppText] accepting an [AnnotatedString] directly.
 */
@Composable
public fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    selectable: Boolean = true,
) {
    if (selectable) {
        SelectionContainer(modifier = modifier) {
            if (onTextLayout != null) {
                Text(
                    text = text,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    onTextLayout = onTextLayout,
                    style = style,
                )
            } else {
                Text(
                    text = text,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    style = style,
                )
            }
        }
    } else {
        if (onTextLayout != null) {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = style,
            )
        } else {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                style = style,
            )
        }
    }
}

/**
 * Builds an [AnnotatedString] that recognizes URLs and mailto addresses, wrapping each match in a
 * [LinkAnnotation.Url] with an underlined [SpanStyle].
 */
internal fun buildLinkedAnnotatedString(
    text: String,
    linkColor: Color,
): AnnotatedString {
    val matches = URL_REGEX.findAll(text).toList()
    if (matches.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        var cursor = 0
        for (match in matches) {
            var rawUrl = match.value
            // Strip trailing punctuation like '.', ',', ')', ']' that often follows URLs in sentences
            var trimmedUrl = rawUrl
            var trailingPunctuation = ""
            while (trimmedUrl.isNotEmpty() && (trimmedUrl.endsWith(".") || trimmedUrl.endsWith(",") || trimmedUrl.endsWith(")") || trimmedUrl.endsWith("]"))) {
                trailingPunctuation = trimmedUrl.last() + trailingPunctuation
                trimmedUrl = trimmedUrl.dropLast(1)
            }

            if (trimmedUrl.isEmpty()) {
                append(text.substring(cursor, match.range.last + 1))
                cursor = match.range.last + 1
                continue
            }

            val linkStart = match.range.first
            if (linkStart > cursor) {
                append(text.substring(cursor, linkStart))
            }

            val linkAnnotation = LinkAnnotation.Url(
                url = trimmedUrl,
                styles = androidx.compose.ui.text.TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            )
            val annotationStart = length
            append(trimmedUrl)
            addLink(linkAnnotation, annotationStart, length)

            if (trailingPunctuation.isNotEmpty()) {
                append(trailingPunctuation)
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
