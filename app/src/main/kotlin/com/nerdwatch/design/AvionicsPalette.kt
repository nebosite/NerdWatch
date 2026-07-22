package com.nerdwatch.design

/**
 * A complete set of Avionics color tokens as plain ARGB ints.
 *
 * Free of `android.graphics` on purpose so it stays JVM-unit-testable — and so
 * the LIGHT (flashlight) palette can be swapped wholesale rather than smeared
 * through the UI as conditionals.
 */
data class AvionicsPalette(
    val bgTop: Int,
    val bgBottom: Int,
    val fg: Int,
    val dim: Int,
    val line: Int,
    val chip: Int,
    val accent: Int,
    val warn: Int,
    val timeDigits: Int,
    /** Numerals glow only in dark mode; the flashlight palette must stay flat. */
    val glow: Boolean,
) {
    companion object {
        /** Normal cockpit mode: warm-black panel, amber phosphor readouts. */
        val DARK = AvionicsPalette(
            bgTop = 0xFF14100A.toInt(),
            bgBottom = 0xFF0B0805.toInt(),
            fg = 0xFFFFE9C4.toInt(),
            dim = 0xFF8A7550.toInt(),
            line = 0xFF3A2E18.toInt(),
            chip = 0x12FFB000,          // #FFB000 @ 7% alpha
            accent = 0xFFFFB000.toInt(),
            warn = 0xFFFF4D5E.toInt(),
            timeDigits = 0xFFFFE9C4.toInt(),
            glow = true,
        )

        /**
         * Flashlight mode. Everything stays >= 50% white so the face works as a
         * light; the accent is the dark accent blended 30% into white.
         */
        val LIGHT = AvionicsPalette(
            bgTop = 0xFFFFFFFF.toInt(),
            bgBottom = 0xFFFFFFFF.toInt(),
            fg = 0xFFB4B9BF.toInt(),
            dim = 0xFFCED3D8.toInt(),
            line = 0xFFE0E4E8.toInt(),
            chip = 0xFFF3F5F7.toInt(),
            accent = 0xFFFFE7B3.toInt(),
            warn = 0xFFFF4D5E.toInt(),
            timeDigits = 0xFFA8ADB3.toInt(),
            glow = false,
        )
    }
}
