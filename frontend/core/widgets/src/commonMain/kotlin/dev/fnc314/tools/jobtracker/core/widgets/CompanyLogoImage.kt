package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.di.extractHttpStatusCode
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.logo_error_generic
import fnc314jobsearchtracker.core_widgets.generated.resources.logo_http_error_code
import org.jetbrains.compose.resources.stringResource

private const val MIN_LOGO_DIMENSION_PX = 64f
private const val MIN_SQUARE_ASPECT_RATIO = 0.8f
private const val MAX_SQUARE_ASPECT_RATIO = 1.25f
private const val VISIBLE_ALPHA = 1f
private const val HIDDEN_ALPHA = 0f
private const val SHIMMER_DURATION_MS = 1100
private const val SHIMMER_START_OFFSET = -200f
private const val SHIMMER_END_OFFSET = 400f
private const val SHIMMER_HIGHLIGHT_ALPHA = 0.65f
private const val SHIMMER_BASE_ALPHA = 0.25f
private const val LOADING_INDICATOR_RATIO = 0.4f
private val DEFAULT_CORNER_RADIUS = 8.dp
private val PROGRESS_STROKE_WIDTH = 2.dp
private val ERROR_BANNER_VERTICAL_PADDING = 1.dp

/**
 * Predefined sizing tiers for [CompanyLogoImage] across all application surfaces.
 */
public enum class CompanyLogoSize {
    /** Compact inline logo size (`48.dp`) used in detail sections. */
    Small,

    /** Standard list card logo size (`64.dp`) used on compact cards. */
    Medium,

    /** Prominent logo size (`72.dp`) used on wide cards and company overview headers. */
    Large,
}

/**
 * Resolves the [Dp] dimension corresponding to this [CompanyLogoSize].
 */
@Composable
public fun CompanyLogoSize.toDp(): Dp = when (this) {
    CompanyLogoSize.Small -> MaterialTheme.componentSizes.companyLogoSmall
    CompanyLogoSize.Medium -> MaterialTheme.componentSizes.companyLogoMedium
    CompanyLogoSize.Large -> MaterialTheme.componentSizes.companyLogoLarge
}

/**
 * Resolves the monogram [TextStyle] corresponding to this [CompanyLogoSize].
 */
@Composable
public fun CompanyLogoSize.toTextStyle(): TextStyle = when (this) {
    CompanyLogoSize.Small -> MaterialTheme.typography.titleMedium
    CompanyLogoSize.Medium -> MaterialTheme.typography.titleLarge
    CompanyLogoSize.Large -> MaterialTheme.typography.headlineSmall
}

/**
 * Resolves a default hunter.io logo URL given an employer's company name.
 *
 * @param companyName The raw employer name.
 * @return A default hunter.io logo URL, or `null` if the company name is blank or unknown.
 */
public fun resolveCompanyLogoUrl(companyName: String): String? {
    val trimmed = companyName.trim()
    if (trimmed.isEmpty() || trimmed.equals("Unknown Company", ignoreCase = true)) {
        return null
    }
    val clean = trimmed.lowercase()
        .replace(" financial services", "")
        .replace(" technologies", "")
        .replace(" motor company", "")
        .replace(" platforms", "")
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
 * Unifies logo presentation across all list cards and detail screens:
 * - Renders a transparent background (`Color.Transparent`) when a valid square logo loads.
 * - Crops and scales loaded logos to predefined [CompanyLogoSize] dimensions.
 * - Displays a dynamic shimmer and progress indicator while loading (`AsyncImagePainter.State.Loading`).
 * - Displays a top red banner with the HTTP error code when loading fails (`AsyncImagePainter.State.Error`),
 *   while detailed error logging is handled by the Coil `ImageLoader` setup.
 *
 * @param companyName Name of the company used for monogram fallback and content description.
 * @param modifier Layout modifier.
 * @param logoUrl Remote image URL or `null` (falls back to [resolveCompanyLogoUrl] if `null`).
 * @param companyInitial Initial character for monogram fallback.
 * @param logoSize Predefined [CompanyLogoSize] tier controlling dimension and monogram typography.
 * @param size Explicit box dimension override in [Dp] (defaults to `logoSize.toDp()`).
 * @param shape Corner radius shape for the logo container.
 * @param contentScale Scaling strategy applied to the loaded logo image.
 * @param textStyle Typography style for the monogram initial text.
 */
@Composable
public fun CompanyLogoImage(
    companyName: String,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    companyInitial: String = companyName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
        ?: "?",
    logoSize: CompanyLogoSize = CompanyLogoSize.Small,
    size: Dp = logoSize.toDp(),
    shape: Shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
    contentScale: ContentScale = ContentScale.Crop,
    textStyle: TextStyle = logoSize.toTextStyle(),
): Unit {
    val effectiveUrl = remember(logoUrl, companyName) { logoUrl ?: resolveCompanyLogoUrl(companyName) }
    var isValidLogoLoaded by remember(effectiveUrl) { mutableStateOf(false) }
    var isLoading by remember(effectiveUrl) { mutableStateOf(effectiveUrl != null) }
    var isLogoRejected by remember(effectiveUrl) { mutableStateOf(effectiveUrl == null) }
    var hasLoadError by remember(effectiveUrl) { mutableStateOf(false) }
    var errorHttpCode by remember(effectiveUrl) { mutableStateOf<Int?>(null) }

    val containerColor = if (isValidLogoLoaded) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val boxModifier = modifier
        .size(size)
        .clip(shape)
        .background(containerColor)
        .let { base ->
            if (isValidLogoLoaded) {
                base
            } else {
                base.border(
                    width = MaterialTheme.spacing.hairline,
                    color = if (hasLoadError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = shape,
                )
            }
        }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading && !isValidLogoLoaded) {
            LogoLoadingShimmer(size = size)
        } else if (!isValidLogoLoaded) {
            Text(
                text = companyInitial,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (effectiveUrl != null && !isLogoRejected) {
            JobApplicationTrackerImage(
                model = effectiveUrl,
                contentDescription = "$companyName logo",
                contentScale = contentScale,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .alpha(if (isValidLogoLoaded) VISIBLE_ALPHA else HIDDEN_ALPHA),
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            isLoading = true
                            isValidLogoLoaded = false
                        }

                        is AsyncImagePainter.State.Success -> {
                            isLoading = false
                            if (isAcceptableSquareLogo(state.painter.intrinsicSize)) {
                                isValidLogoLoaded = true
                                hasLoadError = false
                                errorHttpCode = null
                            } else {
                                isValidLogoLoaded = false
                                isLogoRejected = true
                            }
                        }

                        is AsyncImagePainter.State.Error -> {
                            isLoading = false
                            isValidLogoLoaded = false
                            isLogoRejected = true
                            hasLoadError = true
                            errorHttpCode = extractHttpStatusCode(state.result.throwable)
                        }

                        is AsyncImagePainter.State.Empty -> {
                            isLoading = false
                            isValidLogoLoaded = false
                        }
                    }
                },
            )
        }

        if (hasLoadError && !isValidLogoLoaded) {
            val bannerText = errorHttpCode?.let { code ->
                stringResource(Res.string.logo_http_error_code, code)
            } ?: stringResource(Res.string.logo_error_generic)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(
                        horizontal = MaterialTheme.spacing.micro,
                        vertical = ERROR_BANNER_VERTICAL_PADDING,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun LogoLoadingShimmer(size: Dp) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = SHIMMER_START_OFFSET,
        targetValue = SHIMMER_END_OFFSET,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SHIMMER_BASE_ALPHA)
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA)
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + (SHIMMER_END_OFFSET / 2f), translateAnim + (SHIMMER_END_OFFSET / 2f)),
    )

    Box(
        modifier = Modifier
            .size(size)
            .background(brush = brush),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * LOADING_INDICATOR_RATIO),
            strokeWidth = PROGRESS_STROKE_WIDTH,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Convenience overload for [CompanyLogoImage] with [logoUrl] as first parameter.
 *
 * @param logoUrl Remote image URL or `null`.
 * @param companyName Name of the company used for monogram fallback and content description.
 * @param modifier Layout modifier.
 * @param logoSize Predefined [CompanyLogoSize] tier.
 * @param size Explicit box dimension override in [Dp].
 * @param shape Corner radius shape for the logo container.
 */
@Composable
public fun CompanyLogoImage(
    logoUrl: String?,
    companyName: String,
    modifier: Modifier = Modifier,
    logoSize: CompanyLogoSize = CompanyLogoSize.Small,
    size: Dp = logoSize.toDp(),
    shape: Shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
): Unit {
    CompanyLogoImage(
        companyName = companyName,
        modifier = modifier,
        logoUrl = logoUrl,
        logoSize = logoSize,
        size = size,
        shape = shape,
    )
}
