package com.nerdwatch.solar

/**
 * Maps a Kp index to a background color that fades evenly between pegged values:
 *
 * ```
 * 3 → transparent   4 → green   5 → yellow   7 → orange   8 → red   9 → magenta
 * ```
 *
 * Below 3 is fully transparent; above 9 stays magenta. Between pegs every ARGB
 * channel is interpolated linearly, so e.g. Kp 6 is halfway between yellow and
 * orange, and Kp 3.5 is half-faded green. Pure integer math — unit-testable.
 */
object SolarPalette {

    private const val GREEN = 0xFF2ECC40.toInt()
    private const val YELLOW = 0xFFFFDC00.toInt()
    private const val ORANGE = 0xFFFF851B.toInt()
    private const val RED = 0xFFFF4136.toInt()
    private const val MAGENTA = 0xFFE040FB.toInt()

    /** Peg 3 is green with zero alpha, so 3→4 is a clean fade-in of green. */
    private val PEGS: List<Pair<Double, Int>> = listOf(
        3.0 to (GREEN and 0x00FFFFFF),
        4.0 to GREEN,
        5.0 to YELLOW,
        7.0 to ORANGE,
        8.0 to RED,
        9.0 to MAGENTA,
    )

    /** Background ARGB for a Kp value. */
    fun backgroundArgb(kp: Double): Int {
        if (kp <= PEGS.first().first) return 0x00000000
        if (kp >= PEGS.last().first) return PEGS.last().second

        for (i in 0 until PEGS.size - 1) {
            val (lowKp, lowColor) = PEGS[i]
            val (highKp, highColor) = PEGS[i + 1]
            if (kp in lowKp..highKp) {
                val t = (kp - lowKp) / (highKp - lowKp)
                return lerpArgb(lowColor, highColor, t)
            }
        }
        return PEGS.last().second
    }

    private fun lerpArgb(from: Int, to: Int, t: Double): Int {
        val a = channel(from, 24, to, t)
        val r = channel(from, 16, to, t)
        val g = channel(from, 8, to, t)
        val b = channel(from, 0, to, t)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun channel(from: Int, shift: Int, to: Int, t: Double): Int {
        val lo = (from shr shift) and 0xFF
        val hi = (to shr shift) and 0xFF
        return (lo + (hi - lo) * t).toInt().coerceIn(0, 255)
    }
}
