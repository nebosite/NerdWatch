package com.nerdwatch.design

import org.junit.Assert.assertEquals
import org.junit.Test

class DesignScaleTest {

    @Test
    fun `reference face scales one to one`() {
        assertEquals(1f, DesignScale.REFERENCE.factor, 0.0001f)
        assertEquals(110f, DesignScale.REFERENCE.px(110f), 0.0001f)
    }

    @Test
    fun `44mm watch scales the design up`() {
        val scale = DesignScale(480f)

        assertEquals(480f / 450f, scale.factor, 0.0001f)
        // The 352px button bar must grow proportionally, not stay 352px.
        assertEquals(375.46f, scale.px(352f), 0.01f)
    }

    @Test
    fun `40mm watch scales the design down`() {
        val scale = DesignScale(432f)

        assertEquals(0.96f, scale.factor, 0.0001f)
        assertEquals(105.6f, scale.px(110f), 0.01f)
    }

    @Test
    fun `em converts a font-relative fraction to device pixels`() {
        // A digit cell is 0.52em of the 110px time font.
        assertEquals(57.2f, DesignScale.REFERENCE.em(0.52f, 110f), 0.01f)
    }
}
