package com.nerdwatch.ui

/**
 * Everything the face displays, resolved to strings, at one instant.
 *
 * Keeping this a plain value type means the layout can be rendered from test
 * data or a preview without a battery, a step counter, or a calendar — and it
 * keeps formatting decisions out of the composables.
 */
data class FaceSnapshot(
    val batteryPercent: Int,
    val dateText: String,
    /** `HH:MM` in 24h, or `MM:SS` while the chronometer is engaged. */
    val timeText: String,
    /** `:SS`, or `.hh` hundredths while the chronometer is engaged. */
    val secondsText: String,
    val steps: String,
    val temperature: String,
    val nextEventName: String,
    val nextEventCountdown: String,
    val chronoEngaged: Boolean = false,
) {
    /** Spec: battery readout turns `warn` red below 20%. */
    val batteryIsLow: Boolean get() = batteryPercent < LOW_BATTERY_PERCENT

    companion object {
        const val LOW_BATTERY_PERCENT = 20

        /** Reference data matching the design mock, for previews and tests. */
        val PREVIEW = FaceSnapshot(
            batteryPercent = 63,
            dateText = "TUE · JUL 21",
            timeText = "14:32",
            secondsText = ":07",
            steps = "8,432",
            temperature = "78°",
            nextEventName = "STANDUP",
            nextEventCountdown = "T-2H 37M",
        )
    }
}
