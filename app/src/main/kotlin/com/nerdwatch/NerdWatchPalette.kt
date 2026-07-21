package com.nerdwatch

/**
 * The colors NerdWatch draws with, as plain ARGB ints.
 *
 * Deliberately free of `android.graphics` so it stays a pure value type that JVM
 * unit tests can exercise without an emulator.
 *
 * Kept as its own class so the look can be swapped wholesale later — the archived
 * Tizen version had a red "night vision" mode driven by the ambient light sensor,
 * and that will become a second palette rather than a pile of if-statements.
 */
class NerdWatchPalette(
    val background: Int,
    val accent: Int,
) {
    companion object {
        /** Default dark palette: near-black dial with a phosphor-green accent. */
        val DAY = NerdWatchPalette(
            background = 0xFF0B0B0B.toInt(),
            accent = 0xFF39FF6A.toInt(),
        )
    }
}
