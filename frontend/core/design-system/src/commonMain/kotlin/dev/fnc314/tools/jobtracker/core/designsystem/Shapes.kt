package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standard Material Design 3 shape definitions applied across surfaces, cards, and dialogs.
 */
public val JobTrackerShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Specialized corner shape tokens for high-level UI component variants.
 *
 * @property full Full pill or circle shape.
 * @property card Standard application list card shape (`12.dp`).
 * @property chip Filter chip or badge shape (`8.dp`).
 * @property button Interactive action button shape (`20.dp`).
 * @property dialog Modal alert dialog container shape (`28.dp`).
 * @property bottomSheet Modal bottom sheet top-rounded shape (`28.dp`).
 */
public data class ExtendedShapes(
    public val full: CornerBasedShape = CircleShape,
    public val card: CornerBasedShape = RoundedCornerShape(12.dp),
    public val chip: CornerBasedShape = RoundedCornerShape(8.dp),
    public val button: CornerBasedShape = RoundedCornerShape(20.dp),
    public val dialog: CornerBasedShape = RoundedCornerShape(28.dp),
    public val bottomSheet: CornerBasedShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
)
