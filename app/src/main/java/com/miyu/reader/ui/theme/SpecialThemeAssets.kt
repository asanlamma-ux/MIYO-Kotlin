package com.miyu.reader.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin

enum class ThemeBackdropVariant {
    SHELL,
    LOADING,
    CARD,
}

fun specialThemeSceneCopy(theme: ReaderThemeColors): String =
    when (theme.assetPack) {
        SpecialThemeAsset.BLOSSOM -> "Sweeping petals into place and restoring chapter ambience."
        SpecialThemeAsset.COFFEE -> "Warming the coffeehouse palette and preparing the next pour of chapters."
        SpecialThemeAsset.PARCHMENT -> "Settling vellum grain, highlights, and chapter markers."
        SpecialThemeAsset.MATCHA -> "Laying out the tea-paper wash and restoring your place."
        SpecialThemeAsset.NONE -> "Preparing chapters, highlights, and reading position."
    }

@Composable
fun SpecialThemeBackdrop(
    readerThemeId: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    variant: ThemeBackdropVariant = ThemeBackdropVariant.SHELL,
    reducedMotion: Boolean = false,
) {
    SpecialThemeBackdrop(
        theme = ReaderColors.shellThemeFor(readerThemeId, darkTheme),
        modifier = modifier,
        variant = variant,
        reducedMotion = reducedMotion,
    )
}

@Composable
fun SpecialThemeBackdrop(
    theme: ReaderThemeColors,
    modifier: Modifier = Modifier,
    variant: ThemeBackdropVariant = ThemeBackdropVariant.SHELL,
    reducedMotion: Boolean = false,
) {
    if (theme.assetPack == SpecialThemeAsset.NONE) return

    val transition = rememberInfiniteTransition(label = "special-theme-backdrop")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (variant) {
                    ThemeBackdropVariant.LOADING -> 5200
                    ThemeBackdropVariant.CARD -> 6400
                    ThemeBackdropVariant.SHELL -> 9200
                },
                easing = LinearEasing,
            ),
        ),
        label = "special-theme-phase",
    )
    val phase = if (reducedMotion) 0.15f else animatedPhase

    Canvas(modifier = modifier) {
        val baseAlpha = when (variant) {
            ThemeBackdropVariant.LOADING -> 0.18f
            ThemeBackdropVariant.CARD -> 0.20f
            ThemeBackdropVariant.SHELL -> if (theme.isDark) 0.10f else 0.16f
        }
        drawThemeGlow(theme, baseAlpha, variant)

        when (theme.assetPack) {
            SpecialThemeAsset.BLOSSOM -> drawBlossomPack(theme, phase, variant)
            SpecialThemeAsset.COFFEE -> drawCoffeePack(theme, phase, variant)
            SpecialThemeAsset.PARCHMENT -> drawParchmentPack(theme, phase, variant)
            SpecialThemeAsset.MATCHA -> drawMatchaPack(theme, phase, variant)
            SpecialThemeAsset.NONE -> Unit
        }
    }
}

@Composable
fun SpecialThemePreviewArt(
    theme: ReaderThemeColors,
    modifier: Modifier = Modifier,
) {
    SpecialThemeBackdrop(
        theme = theme,
        modifier = modifier,
        variant = ThemeBackdropVariant.CARD,
        reducedMotion = true,
    )
}

private data class FloatParticle(
    val x: Float,
    val y: Float,
    val drift: Float,
    val scale: Float,
    val delay: Float,
    val rotation: Float,
)

private val floatingParticles = listOf(
    FloatParticle(0.12f, 0.82f, 0.06f, 0.72f, 0.00f, -22f),
    FloatParticle(0.32f, 0.76f, -0.05f, 0.52f, 0.19f, 18f),
    FloatParticle(0.70f, 0.88f, 0.04f, 0.64f, 0.34f, -8f),
    FloatParticle(0.86f, 0.66f, -0.07f, 0.48f, 0.48f, 26f),
    FloatParticle(0.48f, 0.94f, 0.08f, 0.42f, 0.62f, -30f),
    FloatParticle(0.18f, 0.62f, -0.03f, 0.36f, 0.76f, 14f),
    FloatParticle(0.78f, 0.48f, 0.05f, 0.40f, 0.88f, -18f),
)

private fun DrawScope.drawThemeGlow(
    theme: ReaderThemeColors,
    alpha: Float,
    variant: ThemeBackdropVariant,
) {
    val topRadius = when (variant) {
        ThemeBackdropVariant.LOADING -> size.minDimension * 0.34f
        ThemeBackdropVariant.CARD -> size.minDimension * 0.46f
        ThemeBackdropVariant.SHELL -> size.minDimension * 0.38f
    }
    drawCircle(
        color = theme.accent.copy(alpha = alpha),
        radius = topRadius,
        center = Offset(size.width * 0.86f, size.height * 0.12f),
    )
    drawCircle(
        color = theme.secondaryText.copy(alpha = alpha * 0.55f),
        radius = topRadius * 0.76f,
        center = Offset(size.width * 0.08f, size.height * 0.86f),
    )
}

private fun DrawScope.drawBlossomPack(
    theme: ReaderThemeColors,
    phase: Float,
    variant: ThemeBackdropVariant,
) {
    val hero = heroCenter(variant)
    val scale = heroScale(variant)
    drawBlossom(
        center = hero,
        scale = scale,
        accent = theme.accent.copy(alpha = if (variant == ThemeBackdropVariant.SHELL) 0.20f else 0.34f),
        core = Color(0xFFF0D178).copy(alpha = if (theme.isDark) 0.40f else 0.64f),
        rotation = -5f + phase * 10f,
    )
    drawFloatingParticles(theme, phase, variant) { center, particleScale, rotation, alpha ->
        drawPetal(
            center = center,
            scale = particleScale,
            color = theme.accent.copy(alpha = alpha),
            rotation = rotation,
        )
    }
}

private fun DrawScope.drawCoffeePack(
    theme: ReaderThemeColors,
    phase: Float,
    variant: ThemeBackdropVariant,
) {
    val center = heroCenter(variant)
    val scale = heroScale(variant)
    val steamColor = theme.accent.copy(alpha = if (theme.isDark) 0.28f else 0.36f)
    repeat(3) { index ->
        val offset = (index - 1) * 26f * scale
        val wave = sin((phase + index * 0.18f) * 6.28318f) * 10f * scale
        val path = Path().apply {
            moveTo(center.x + offset, center.y - 78f * scale)
            cubicTo(
                center.x + offset + wave,
                center.y - 126f * scale,
                center.x + offset - 28f * scale,
                center.y - 154f * scale,
                center.x + offset + 16f * scale,
                center.y - 198f * scale,
            )
        }
        drawPath(
            path = path,
            color = steamColor.copy(alpha = steamColor.alpha * (0.52f + index * 0.16f)),
            style = Stroke(width = 8f * scale, cap = StrokeCap.Round),
        )
    }
    drawRoundRect(
        color = theme.accent.copy(alpha = if (variant == ThemeBackdropVariant.SHELL) 0.18f else 0.36f),
        topLeft = Offset(center.x - 70f * scale, center.y - 48f * scale),
        size = Size(140f * scale, 98f * scale),
        cornerRadius = CornerRadius(28f * scale),
    )
    drawOval(
        color = theme.text.copy(alpha = if (theme.isDark) 0.18f else 0.14f),
        topLeft = Offset(center.x - 58f * scale, center.y - 54f * scale),
        size = Size(116f * scale, 34f * scale),
    )
    drawCircle(
        color = theme.accent.copy(alpha = 0.22f),
        radius = 13f * scale,
        center = Offset(center.x + 88f * scale, center.y - 8f * scale),
        style = Stroke(width = 9f * scale),
    )
    drawFloatingParticles(theme, phase, variant) { centerPoint, particleScale, _, alpha ->
        drawCircle(
            color = theme.accent.copy(alpha = alpha * 0.72f),
            radius = 10f * particleScale,
            center = centerPoint,
        )
    }
}

private fun DrawScope.drawParchmentPack(
    theme: ReaderThemeColors,
    phase: Float,
    variant: ThemeBackdropVariant,
) {
    val center = heroCenter(variant)
    val scale = heroScale(variant)
    val paperAlpha = if (variant == ThemeBackdropVariant.SHELL) 0.14f else 0.34f
    drawRoundRect(
        color = theme.accent.copy(alpha = paperAlpha),
        topLeft = Offset(center.x - 82f * scale, center.y - 122f * scale),
        size = Size(164f * scale, 214f * scale),
        cornerRadius = CornerRadius(18f * scale),
    )
    drawLine(theme.text.copy(alpha = paperAlpha * 1.6f), Offset(center.x - 48f * scale, center.y - 54f * scale), Offset(center.x + 48f * scale, center.y - 54f * scale), 7f * scale, StrokeCap.Round)
    drawLine(theme.text.copy(alpha = paperAlpha), Offset(center.x - 48f * scale, center.y - 22f * scale), Offset(center.x + 62f * scale, center.y - 22f * scale), 5f * scale, StrokeCap.Round)
    drawLine(theme.text.copy(alpha = paperAlpha), Offset(center.x - 48f * scale, center.y + 8f * scale), Offset(center.x + 38f * scale, center.y + 8f * scale), 5f * scale, StrokeCap.Round)
    drawFloatingParticles(theme, phase, variant) { centerPoint, particleScale, _, alpha ->
        drawCircle(
            color = theme.accent.copy(alpha = alpha * 0.60f),
            radius = 4.5f * particleScale,
            center = centerPoint,
        )
    }
}

private fun DrawScope.drawMatchaPack(
    theme: ReaderThemeColors,
    phase: Float,
    variant: ThemeBackdropVariant,
) {
    val center = heroCenter(variant)
    val scale = heroScale(variant)
    drawLeaf(
        center = center + Offset(-18f * scale, -10f * scale),
        scale = scale * 1.12f,
        color = theme.accent.copy(alpha = if (variant == ThemeBackdropVariant.SHELL) 0.20f else 0.40f),
        rotation = -22f + phase * 8f,
    )
    drawLeaf(
        center = center + Offset(48f * scale, 22f * scale),
        scale = scale * 0.82f,
        color = theme.secondaryText.copy(alpha = if (variant == ThemeBackdropVariant.SHELL) 0.16f else 0.32f),
        rotation = 32f - phase * 7f,
    )
    drawFloatingParticles(theme, phase, variant) { centerPoint, particleScale, rotation, alpha ->
        drawLeaf(
            center = centerPoint,
            scale = particleScale * 0.24f,
            color = theme.accent.copy(alpha = alpha * 0.72f),
            rotation = rotation,
        )
    }
}

private fun DrawScope.drawFloatingParticles(
    theme: ReaderThemeColors,
    phase: Float,
    variant: ThemeBackdropVariant,
    drawParticle: DrawScope.(Offset, Float, Float, Float) -> Unit,
) {
    val travel = when (variant) {
        ThemeBackdropVariant.LOADING -> 0.34f
        ThemeBackdropVariant.CARD -> 0.18f
        ThemeBackdropVariant.SHELL -> 0.24f
    }
    val count = when (variant) {
        ThemeBackdropVariant.LOADING -> 7
        ThemeBackdropVariant.CARD -> 4
        ThemeBackdropVariant.SHELL -> 6
    }
    floatingParticles.take(count).forEach { spec ->
        val p = ((phase + spec.delay) % 1f)
        val fadeIn = (p / 0.18f).coerceIn(0f, 1f)
        val fadeOut = ((1f - p) / 0.16f).coerceIn(0f, 1f)
        val alpha = minOf(fadeIn, fadeOut) * (if (theme.isDark) 0.20f else 0.26f)
        val x = size.width * (spec.x + spec.drift * sin(p * 6.28318f))
        val y = size.height * (spec.y - p * travel)
        val particleScale = spec.scale * when (variant) {
            ThemeBackdropVariant.LOADING -> 1.10f
            ThemeBackdropVariant.CARD -> 0.45f
            ThemeBackdropVariant.SHELL -> 0.90f
        }
        drawParticle(Offset(x, y), particleScale, spec.rotation + p * 80f, alpha)
    }
}

private fun DrawScope.heroCenter(variant: ThemeBackdropVariant): Offset =
    when (variant) {
        ThemeBackdropVariant.LOADING -> Offset(size.width * 0.50f, size.height * 0.36f)
        ThemeBackdropVariant.CARD -> Offset(size.width * 0.74f, size.height * 0.30f)
        ThemeBackdropVariant.SHELL -> Offset(size.width * 0.82f, size.height * 0.18f)
    }

private fun DrawScope.heroScale(variant: ThemeBackdropVariant): Float =
    when (variant) {
        ThemeBackdropVariant.LOADING -> (size.minDimension / 420f).coerceIn(0.55f, 1.4f)
        ThemeBackdropVariant.CARD -> (size.minDimension / 220f).coerceIn(0.28f, 0.8f)
        ThemeBackdropVariant.SHELL -> (size.minDimension / 520f).coerceIn(0.55f, 1.2f)
    }

private fun DrawScope.drawBlossom(
    center: Offset,
    scale: Float,
    accent: Color,
    core: Color,
    rotation: Float,
) {
    repeat(5) { index ->
        drawPetal(
            center = center,
            scale = 2.4f * scale,
            color = accent,
            rotation = rotation + index * 72f,
        )
    }
    drawCircle(core, radius = 34.dp.toPx() * scale, center = center)
    drawCircle(accent.copy(alpha = 0.46f), radius = 10.dp.toPx() * scale, center = center)
}

private fun DrawScope.drawPetal(
    center: Offset,
    scale: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - 10f * scale)
            cubicTo(
                center.x + 11f * scale,
                center.y - 22f * scale,
                center.x + 27f * scale,
                center.y - 4f * scale,
                center.x + 4f * scale,
                center.y + 30f * scale,
            )
            cubicTo(
                center.x - 22f * scale,
                center.y + 8f * scale,
                center.x - 12f * scale,
                center.y - 10f * scale,
                center.x,
                center.y - 10f * scale,
            )
            close()
        }
        drawPath(path, color)
    }
}

private fun DrawScope.drawLeaf(
    center: Offset,
    scale: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - 56f * scale)
            cubicTo(
                center.x + 72f * scale,
                center.y - 28f * scale,
                center.x + 58f * scale,
                center.y + 50f * scale,
                center.x,
                center.y + 74f * scale,
            )
            cubicTo(
                center.x - 58f * scale,
                center.y + 50f * scale,
                center.x - 72f * scale,
                center.y - 28f * scale,
                center.x,
                center.y - 56f * scale,
            )
            close()
        }
        drawPath(path, color)
        drawLine(
            color = color.copy(alpha = color.alpha * 0.70f),
            start = Offset(center.x, center.y - 34f * scale),
            end = Offset(center.x, center.y + 52f * scale),
            strokeWidth = 4f * scale,
            cap = StrokeCap.Round,
        )
    }
}
