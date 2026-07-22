package com.nerdwatch.design

import com.nerdwatch.ui.FaceSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvionicsPaletteTest {

    private fun channels(argb: Int) = Triple(
        (argb shr 16) and 0xFF,
        (argb shr 8) and 0xFF,
        argb and 0xFF,
    )

    @Test
    fun `both palettes are fully opaque`() {
        listOf(AvionicsPalette.DARK, AvionicsPalette.LIGHT).forEach { palette ->
            listOf(palette.bgTop, palette.bgBottom, palette.fg, palette.accent).forEach {
                assertTrue("expected opaque", ((it ushr 24) and 0xFF) == 0xFF)
            }
        }
    }

    @Test
    fun `light mode stays bright enough to work as a flashlight`() {
        // Spec: everything >= 50% white so the face actually emits light.
        val opaqueTokens = with(AvionicsPalette.LIGHT) {
            listOf(bgTop, bgBottom, fg, dim, line, chip, accent, timeDigits)
        }

        opaqueTokens.forEach { token ->
            val (r, g, b) = channels(token)
            assertTrue("token $token too dark for light mode", r >= 128 && g >= 128 && b >= 128)
        }
    }

    @Test
    fun `numerals glow only in dark mode`() {
        assertTrue(AvionicsPalette.DARK.glow)
        assertFalse(AvionicsPalette.LIGHT.glow)
    }

    @Test
    fun `battery is low below twenty percent`() {
        assertTrue(FaceSnapshot.PREVIEW.copy(batteryPercent = 19).batteryIsLow)
        assertFalse(FaceSnapshot.PREVIEW.copy(batteryPercent = 20).batteryIsLow)
    }
}
