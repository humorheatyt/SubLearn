package com.sublearn.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AppSettings
import com.sublearn.domain.GestureAction
import kotlin.math.abs
import kotlin.math.hypot

internal fun Modifier.videoTapGestures(
    settings: AppSettings,
    onSingleTap: () -> Unit,
    onDoubleTap: (GestureAction) -> Unit,
): Modifier = pointerInput(settings.gestureBindings, onSingleTap, onDoubleTap) {
    detectTapGestures(
        onTap = { onSingleTap() },
        onDoubleTap = { onDoubleTap(settings.gestureBindings["double-tap-video"] ?: settings.doubleTapAction) },
    )
}

@Composable
internal fun Modifier.videoDragGestures(
    settings: AppSettings,
    onAction: (GestureAction, Float, Float, IntSize) -> Unit,
): Modifier {
    val density = LocalDensity.current
    return pointerInput(settings.gestureBindings) {
        val edgeGuard = with(density) { SubLearnSpacing.gestureEdgeGuard.toPx() }
        val touchSlop = with(density) { SubLearnSpacing.gestureTouchSlop.toPx() }
        var start = Offset.Zero
        var total = Offset.Zero
        var permitted = true
        var classified = false
        detectDragGestures(
            onDragStart = { position ->
                start = position
                total = Offset.Zero
                permitted = position.x >= edgeGuard && position.x <= size.width - edgeGuard
                classified = false
            },
            onDragEnd = { },
            onDragCancel = { },
            onDrag = { change, delta ->
                total += delta
                if (!permitted) return@detectDragGestures
                if (!classified && hypot(total.x, total.y) >= touchSlop) classified = true
                if (!classified) return@detectDragGestures
                val action = when {
                    abs(total.x) > abs(total.y) -> settings.gestureBindings["swipe-video-horizontal"] ?: GestureAction.NONE
                    start.x < size.width / 2f -> settings.gestureBindings["swipe-video-left-vertical"] ?: GestureAction.NONE
                    else -> settings.gestureBindings["swipe-video-right-vertical"] ?: GestureAction.NONE
                }
                change.consume()
                onAction(action, delta.x, delta.y, size)
            },
        )
    }
}

@Composable
internal fun Modifier.videoTwoFingerUp(
    settings: AppSettings,
    onAction: (GestureAction) -> Unit,
): Modifier {
    val density = LocalDensity.current
    return pointerInput(settings.gestureBindings) {
        val touchSlop = with(density) { SubLearnSpacing.twoFingerTouchSlop.toPx() }
        awaitEachGesture {
            var initialCentroid: Offset? = null
            var triggered = false
            var lastPointerCount = 0
            do {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val centroid = Offset(
                        pressed.map { it.position.x }.average().toFloat(),
                        pressed.map { it.position.y }.average().toFloat(),
                    )
                    if (lastPointerCount < 2) initialCentroid = centroid
                    val origin = initialCentroid
                    if (!triggered && origin != null && origin.y - centroid.y > touchSlop) {
                        triggered = true
                        val action = settings.gestureBindings["two-finger-up"] ?: GestureAction.NONE
                        if (action != GestureAction.NONE) onAction(action)
                        event.changes.forEach { it.consume() }
                    }
                }
                lastPointerCount = pressed.size
            } while (event.changes.any { it.pressed })
        }
    }
}
