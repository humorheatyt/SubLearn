package com.sublearn.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.sublearn.design.SubLearnAlpha
import com.sublearn.design.SubLearnMotion
import com.sublearn.design.SubLearnColors
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.design.SubLearnStroke
import com.sublearn.domain.AppSettings
import com.sublearn.domain.Cue
import com.sublearn.domain.SubtitleLayer
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.TextDirection as DomainTextDirection
import com.sublearn.domain.WordMarkStyle
import com.sublearn.domain.WordStyleSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import kotlin.math.abs

internal enum class SubtitleTapKind { WORD, LINE, BLOCK, PHRASE }

@Composable
internal fun SubtitleOverlay(
    cue: Cue?,
    layer: SubtitleLayer,
    settings: AppSettings,
    knownWords: Set<String>,
    myWords: Set<String>,
    visible: Boolean,
    temporarilyInverted: Boolean,
    layoutMode: Boolean,
    onTranslate: (String, SubtitleTapKind) -> Unit,
    onLayoutDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLearning = layer == SubtitleLayer.LEARNING
    val shown = visible xor (temporarilyInverted && layer == SubtitleLayer.LEARNING)
    val nativeShown = visible xor (temporarilyInverted && layer == SubtitleLayer.NATIVE)
    if (cue == null || (if (isLearning) !shown else !nativeShown)) return
    val font = settings.surfaceFonts[if (isLearning) "subtitle.learning" else "subtitle.native"] ?: SurfaceFontSettings()
    val textColor = font.colorArgb?.let(::Color) ?: if (isLearning) SubLearnColors.Learning else SubLearnColors.Native
    val weight = FontWeight(font.weight.coerceIn(100, 900))
    val family = when (font.family) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val isRtl = if (font.direction == DomainTextDirection.AUTO) !isLearning else font.direction == DomainTextDirection.RTL
    val pointerScope = rememberCoroutineScope()
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    var layoutResult by remember(cue.id, layer) { mutableStateOf<TextLayoutResult?>(null) }
    val styledRanges = remember(cue.text, cue.tokens, knownWords, myWords, settings.knownWordStyle, settings.myWordsStyle) {
        cue.tokens.mapNotNull { token ->
            val key = token.text.lowercase(Locale.ROOT)
            val style = when {
                key in knownWords && settings.knownWordStyle.enabled -> settings.knownWordStyle
                key in myWords && settings.myWordsStyle.enabled -> settings.myWordsStyle
                else -> null
            }
            style?.let { StyledWordRange(token.startOffset until token.endOffset, it) }
        }
    }
    val annotated = remember(cue.text, styledRanges) { createAnnotated(cue.text, styledRanges) }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier.fillMaxWidth().background(SubLearnColors.PlayerSubtitleScrim, RoundedCornerShape(SubLearnRadii.small))
                .padding(horizontal = SubLearnSpacing.md, vertical = SubLearnSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = annotated,
                modifier = Modifier.fillMaxWidth()
                    .subtitleTapInput(cue, layoutResult, layoutMode, pointerScope, onTranslate, onLayoutDrag)
                    .drawBehind { drawKnownMarks(layoutResult, styledRanges, textColor) }
                    .semantics {
                        role = Role.Button
                        contentDescription = cue.text
                        onClick {
                            onTranslate(cue.text, SubtitleTapKind.LINE)
                            true
                        }
                    },
                style = TextStyle(
                    color = textColor.copy(alpha = if (isLearning) settings.learningSubtitleAlpha else settings.nativeSubtitleAlpha),
                    fontSize = (if (isLearning) settings.learningSubtitleSizeSp else settings.nativeSubtitleSizeSp).sp,
                    fontWeight = weight,
                    fontFamily = family,
                    textAlign = TextAlign.Center,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    lineHeight = ((if (isLearning) settings.learningSubtitleSizeSp else settings.nativeSubtitleSizeSp) * 1.24f).sp,
                ),
                maxLines = 4,
                onTextLayout = { layoutResult = it },
            )
        }
    }
}

private fun Modifier.subtitleTapInput(
    cue: Cue,
    layout: TextLayoutResult?,
    layoutMode: Boolean,
    pointerScope: kotlinx.coroutines.CoroutineScope,
    onTranslate: (String, SubtitleTapKind) -> Unit,
    onLayoutDrag: (Float) -> Unit,
): Modifier =
    pointerInput(cue.id, layout, layoutMode) {
        if (layoutMode) return@pointerInput
        var tapCount = 0
        var lastTap = 0L
        var pending: kotlinx.coroutines.Job? = null
        detectTapGestures(onTap = { point ->
            val textLayout = layout ?: return@detectTapGestures
            val offset = textLayout.getOffsetForPosition(point).coerceIn(0, cue.text.length)
            val now = System.currentTimeMillis()
            tapCount = if (now - lastTap <= SubLearnMotion.subtitleTapWindowMs) tapCount + 1 else 1
            lastTap = now
            val count = tapCount
            pending?.cancel()
            pending = pointerScope.launch {
                delay(SubLearnMotion.subtitleTapWindowMs)
                val selected = when {
                    count >= 3 -> cue.text to SubtitleTapKind.BLOCK
                    count == 2 -> {
                        val line = textLayout.getLineForOffset(offset)
                        val start = textLayout.getLineStart(line).coerceIn(0, cue.text.length)
                        val end = textLayout.getLineEnd(line, visibleEnd = true).coerceIn(start, cue.text.length)
                        cue.text.substring(start, end).trim() to SubtitleTapKind.LINE
                    }
                    else -> {
                        val token = cue.tokens.firstOrNull { offset in it.startOffset until it.endOffset }
                        token?.text to SubtitleTapKind.WORD
                    }
                }
                if (!selected.first.isNullOrBlank()) onTranslate(selected.first!!, selected.second)
                tapCount = 0
            }
        })
    }.pointerInput(cue.id, layoutMode) {
        var startOffset: Int? = null
        var endOffset: Int? = null
        detectDragGestures(
            onDragStart = { point ->
                val textLayout = layout
                startOffset = textLayout?.getOffsetForPosition(point)?.coerceIn(0, cue.text.length)
                endOffset = startOffset
            },
            onDrag = { change, amount ->
                if (layoutMode) {
                    onLayoutDrag(amount.y)
                } else {
                    endOffset = layout?.getOffsetForPosition(change.position)?.coerceIn(0, cue.text.length)
                }
                change.consume()
            },
            onDragEnd = {
                if (!layoutMode) {
                    val start = startOffset
                    val end = endOffset
                    if (start != null && end != null && abs(end - start) > 1) {
                        val low = minOf(start, end)
                        val high = maxOf(start, end).coerceAtMost(cue.text.length)
                        val selected = cue.text.substring(low, high).trim()
                        if (selected.isNotBlank()) onTranslate(selected, SubtitleTapKind.PHRASE)
                    }
                }
            },
        )
    }

private data class StyledWordRange(val range: IntRange, val settings: WordStyleSettings)

private fun createAnnotated(text: String, ranges: List<StyledWordRange>): AnnotatedString =
    AnnotatedString.Builder(text).apply {
        ranges.forEach { marked ->
            val range = marked.range
            val settings = marked.settings
            val color = Color(settings.colorArgb)
            val span = when (settings.style) {
                WordMarkStyle.UNDERLINE -> SpanStyle(color = color, textDecoration = TextDecoration.Underline)
                WordMarkStyle.DOTTED_UNDERLINE -> SpanStyle(color = color)
                WordMarkStyle.OUTLINE -> SpanStyle(color = color, background = color.copy(alpha = SubLearnAlpha.wordOutlineFill))
                WordMarkStyle.BACKGROUND -> SpanStyle(background = color.copy(alpha = SubLearnAlpha.wordMarkFill))
                WordMarkStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold, color = color)
                WordMarkStyle.COLOR -> SpanStyle(color = color)
            }
            if (range.first >= 0 && range.last < text.length) addStyle(span, range.first, range.last + 1)
        }
    }.toAnnotatedString()

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKnownMarks(
    layout: TextLayoutResult?,
    ranges: List<StyledWordRange>,
    fallback: Color,
) {
    if (layout == null || ranges.isEmpty()) return
    ranges.forEach { marked ->
        val range = marked.range
        val settings = marked.settings
        if (!settings.enabled || (settings.style != WordMarkStyle.DOTTED_UNDERLINE && settings.style != WordMarkStyle.OUTLINE)) return@forEach
        val color = Color(settings.colorArgb).takeIf { settings.colorArgb != 0L } ?: fallback
        val boxes = (range.first..range.last).mapNotNull { index ->
            if (index in 0 until layout.layoutInput.text.length) layout.getBoundingBox(index) else null
        }
        boxes.groupBy { it.top }.values.forEach { lineBoxes ->
            val left = lineBoxes.minOf { it.left }
            val right = lineBoxes.maxOf { it.right }
            val top = lineBoxes.minOf { it.top }
            val bottom = lineBoxes.maxOf { it.bottom }
            if (settings.style == WordMarkStyle.OUTLINE) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left - SubLearnStroke.inset.toPx(), top),
                    size = androidx.compose.ui.geometry.Size(right - left + SubLearnStroke.inset.toPx() * 2f, bottom - top),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(SubLearnStroke.corner.toPx()),
                    style = Stroke(width = SubLearnStroke.outline.toPx()),
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(left, bottom - SubLearnStroke.outline.toPx()),
                    end = Offset(right, bottom - SubLearnStroke.outline.toPx()),
                    strokeWidth = SubLearnStroke.underline.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(SubLearnStroke.dash.toPx(), SubLearnStroke.dash.toPx())),
                )
            }
        }
    }
}
