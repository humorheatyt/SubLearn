package com.sublearn.subtitles

import com.sublearn.domain.Cue

/** Immutable subtitle timeline. Lookup remains logarithmic even with overlapping intervals. */
class CueTimelineIndex(cues: List<Cue>) {
    val cues: List<Cue> = cues.sortedWith(compareBy<Cue> { it.startMs }.thenBy { it.endMs })
    private val size: Int = generateSequence(1) { it shl 1 }.first { it >= this.cues.size.coerceAtLeast(1) }
    private val maxEnd = LongArray(size * 2) { Long.MIN_VALUE }

    init {
        this.cues.forEachIndexed { index, cue -> maxEnd[size + index] = cue.endMs }
        for (node in size - 1 downTo 1) maxEnd[node] = maxOf(maxEnd[node * 2], maxEnd[node * 2 + 1])
    }

    /** Half-open intervals are used: a cue ending at [timeMs] is no longer current. */
    fun cueAt(timeMs: Long): Cue? {
        if (timeMs < 0 || cues.isEmpty()) return null
        val rightExclusive = upperBoundStart(timeMs)
        if (rightExclusive == 0) return null
        val index = findRightmost(1, 0, size, rightExclusive, timeMs)
        return index?.let(cues::get)
    }

    fun indexAt(timeMs: Long): Int? {
        val cue = cueAt(timeMs) ?: return null
        return cues.indexOf(cue)
    }

    private fun upperBoundStart(timeMs: Long): Int {
        var low = 0
        var high = cues.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (cues[middle].startMs <= timeMs) low = middle + 1 else high = middle
        }
        return low
    }

    private fun findRightmost(
        node: Int,
        nodeStart: Int,
        nodeEnd: Int,
        queryEnd: Int,
        timeMs: Long,
    ): Int? {
        if (nodeStart >= queryEnd || maxEnd[node] <= timeMs) return null
        if (nodeEnd - nodeStart == 1) return nodeStart.takeIf { it < cues.size }
        val middle = (nodeStart + nodeEnd) ushr 1
        findRightmost(node * 2 + 1, middle, nodeEnd, queryEnd, timeMs)?.let { return it }
        return findRightmost(node * 2, nodeStart, middle, queryEnd, timeMs)
    }
}
