package com.nerdwatch.moon

import org.junit.Assert.assertEquals
import org.junit.Test

class MoonDataTest {

    @Test
    fun `clear skies draw no cloud`() {
        assertEquals(0, MoonData.cloudCountForCover(0.0))
        assertEquals(0, MoonData.cloudCountForCover(30.0)) // boundary: not over 30
    }

    @Test
    fun `over thirty percent draws one cloud`() {
        assertEquals(1, MoonData.cloudCountForCover(30.1))
        assertEquals(1, MoonData.cloudCountForCover(45.0))
        assertEquals(1, MoonData.cloudCountForCover(60.0)) // boundary: not over 60
    }

    @Test
    fun `over sixty percent draws two clouds`() {
        assertEquals(2, MoonData.cloudCountForCover(60.1))
        assertEquals(2, MoonData.cloudCountForCover(100.0))
    }
}
