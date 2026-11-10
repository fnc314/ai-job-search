package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import dev.fnc314.tools.jobtracker.core.designsystem.spacing

private const val MIN_LOGO_DIMENSION_PX = 64f
private const val MIN_SQUARE_ASPECT_RATIO = 0.8f
private const val MAX_SQUARE_ASPECT_RATIO = 1.25f
private const val VISIBLE_ALPHA = 1f
private const val HIDDEN_ALPHA = 0f

/**
 * Resolves a default hunter.io logo URL given an employer's company name.
 */
public fun resolveCompanyLogoUrl(companyName: String): String? {
    val trimmed = companyName.trim()
    if (trimmed.isEmpty() || trimmed.equals("Unknown Company", ignoreCase = true)) {
        return null
    }
    val clean = trimmed.lowercase()
        .replace(" financial services", "")
        .replace(" technologies", "")
        .replace(" inc", "")
        .replace(" corp", "")
        .replace(" llc", "")
        .replace(" l.p.", "")
        .replace(Regex("[^a-z0-9]"), "")
    return clean.takeIf { it.isNotEmpty() }?.let { "https://logos.hunter.io/$it.com" }
}

private fun isAcceptableSquareLogo(intrinsicSize: Size): Boolean {
    if (!intrinsicSize.isSpecified || intrinsicSize.width < MIN_LOGO_DIMENSION_PX || intrinsicSize.height < MIN_LOGO_DIMENSION_PX) {
        return false
    }
    val aspectRatio = intrinsicSize.width / intrinsicSize.height
    return aspectRatio in MIN_SQUARE_ASPECT_RATIO..MAX_SQUARE_ASPECT_RATIO
}

/**
 * Reusable company logo rendering component.
 *
 * Displays the employer's logo fetched via Coil [AsyncImage] inside a shaped, bordered container.
 * Falls back to an initials monogram block when [logoUrl] is null or blank.
 *
 * @param companyName Name of the company used for monogram fallback and content description.
 * @param modifier Layout modifier.
 * @param logoUrl Remote image URL or null (falls back to [resolveCompanyLogoUrl] if null).
 * @param companyInitial Initial character for monogram fallback.
 * @param size Box dimension size in [Dp].
 * @param shape Corner radius shape for the logo container.
 * @param textStyle Typography style for the monogram initial text.
 */
@Composable
public fun CompanyLogoImage(
    companyName: String,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    companyInitial: String = companyName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val effectiveUrl = remember(logoUrl, companyName) { logoUrl ?: resolveCompanyLogoUrl(companyName) }
    var isValidLogoLoaded by remember(effectiveUrl) { mutableStateOf(false) }
    var isLogoRejected by remember(effectiveUrl) { mutableStateOf(effectiveUrl == null) }

    val containerColor = if (isValidLogoLoaded) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .border(
                width = MaterialTheme.spacing.hairline,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isValidLogoLoaded) {
            Text(
                text = companyInitial,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (effectiveUrl != null && !isLogoRejected) {
            AsyncImage(
                model = effectiveUrl,
                contentDescription = "$companyName logo",
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .alpha(if (isValidLogoLoaded) VISIBLE_ALPHA else HIDDEN_ALPHA),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            if (isAcceptableSquareLogo(state.painter.intrinsicSize)) {
                                isValidLogoLoaded = true
                            } else {
                                isValidLogoLoaded = false
                                isLogoRejected = true
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            isValidLogoLoaded = false
                            isLogoRejected = true
                        }
                        is AsyncImagePainter.State.Loading,
                        is AsyncImagePainter.State.Empty -> {
                            isValidLogoLoaded = false
                        }
                    }
                },
            )
        }
    }
}

/**
 * Convenience overload for [CompanyLogoImage] with [logoUrl] as first parameter.
 */
@Composable
public fun CompanyLogoImage(
    logoUrl: String?,
    companyName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    CompanyLogoImage(
        companyName = companyName,
        modifier = modifier,
        logoUrl = logoUrl,
        size = size,
        shape = shape,
    )
}
