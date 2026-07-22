package com.nerdwatch.format

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatterTest {

    @Test
    fun `zero reads NOW`() {
        assertEquals("NOW", CountdownFormatter.format(0))
    }

    @Test
    fun `a past event still reads NOW rather than going negative`() {
        assertEquals("NOW", CountdownFormatter.format(-30))
    }

    @Test
    fun `under an hour drops the hour part`() {
        assertEquals("37M", CountdownFormatter.format(37))
        assertEquals("59M", CountdownFormatter.format(59))
    }

    @Test
    fun `an hour or more shows hours and minutes`() {
        assertEquals("1H 0M", CountdownFormatter.format(60))
        assertEquals("2H 37M", CountdownFormatter.format(157))
    }
}
