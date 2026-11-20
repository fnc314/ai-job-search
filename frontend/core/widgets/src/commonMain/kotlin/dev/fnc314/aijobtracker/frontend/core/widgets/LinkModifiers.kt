package dev.fnc314.aijobtracker.frontend.core.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString

/**
 * Modifier that configures an element as an accessible, clickable web/app hyperlink.
 *
 * When clicked, dispatches the [url] to Compose's [LocalUriHandler].
 * On desktop and web targets, sets the hover cursor icon to [PointerIcon.Hand].
 *
 * @param url The URI destination to open when tapped/clicked.
 * @param role Accessibility role override, defaults to [Role.Button].
 */
@Composable
public fun Modifier.clickableUrl(
    url: String,
    role: Role = Role.Button,
): Modifier {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }

    return this
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = role,
            onClick = {
                if (url.isNotBlank()) {
                    try {
                        uriHandler.openUri(url)
                    } catch (_: Exception) {
                        // Fallback or ignore if platform cannot resolve URI
                    }
                }
            },
        )
}

/**
 * Modifier that copies the specified [text] to the system clipboard when clicked.
 *
 * @param text The string content to place on the clipboard upon click.
 */
@Suppress("DEPRECATION")
@Composable
public fun Modifier.copyableOnClick(
    text: String,
): Modifier {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }

    return this
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = {
                if (text.isNotBlank()) {
                    clipboardManager.setText(AnnotatedString(text))
                }
            },
        )
}
