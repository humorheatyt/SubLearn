package com.sublearn.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.TextDirection as DomainTextDirection

/** Applies only the selected surface's typography; callers keep each language role isolated. */
fun TextStyle.withSurfaceFont(settings: SurfaceFontSettings): TextStyle = copy(
    fontFamily = when (settings.family) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    },
    fontSize = settings.sizeSp.sp,
    fontWeight = FontWeight(settings.weight.coerceIn(100, 900)),
    color = settings.colorArgb?.let(::Color) ?: color,
    textDirection = settings.direction.toComposeTextDirection(),
)

fun DomainTextDirection.toComposeTextDirection(): TextDirection = when (this) {
    DomainTextDirection.LTR -> TextDirection.Ltr
    DomainTextDirection.RTL -> TextDirection.Rtl
    DomainTextDirection.AUTO -> TextDirection.ContentOrLtr
}
