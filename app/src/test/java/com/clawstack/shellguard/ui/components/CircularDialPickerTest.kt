package com.clawstack.shellguard.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CircularDialPickerTest {

    @Test
    fun `cardinal clock positions map to expected seconds`() {
        // 12 o'clock (dx=0, dy=-100) -> 0 deg -> 60s
        assertEquals(60, polarOffsetToSeconds(dx = 0f, dy = -100f))
        // 3 o'clock (dx=100, dy=0) -> 90 deg -> 15s
        assertEquals(15, polarOffsetToSeconds(dx = 100f, dy = 0f))
        // 6 o'clock (dx=0, dy=100) -> 180 deg -> 30s
        assertEquals(30, polarOffsetToSeconds(dx = 0f, dy = 100f))
        // 9 o'clock (dx=-100, dy=0) -> 270 deg -> 45s
        assertEquals(45, polarOffsetToSeconds(dx = -100f, dy = 0f))
    }

    @Test
    fun `tap near 12 oclock clockwise clamps to minimum 5 seconds`() {
        // ~6 degrees clockwise from 12 o'clock (~1s -> clamped to 5s)
        assertEquals(5, polarOffsetToSeconds(dx = 10f, dy = -100f, previousSeconds = null))
    }

    @Test
    fun `dragging clockwise past 12 oclock holds at 60s without wrapping to 5s`() {
        val next = polarOffsetToSeconds(
            dx = 15f,
            dy = -100f,
            previousSeconds = 59
        )
        assertEquals(60, next)
    }

    @Test
    fun `dragging counter-clockwise past 12 oclock holds at 5s without wrapping to 60s`() {
        val next = polarOffsetToSeconds(
            dx = -15f,
            dy = -100f,
            previousSeconds = 6
        )
        assertEquals(5, next)
    }

    @Test
    fun `normal drag across non-boundary regions moves smoothly`() {
        assertEquals(30, polarOffsetToSeconds(dx = 0f, dy = 100f, previousSeconds = 29))
        assertEquals(45, polarOffsetToSeconds(dx = -100f, dy = 0f, previousSeconds = 44))
    }
}
