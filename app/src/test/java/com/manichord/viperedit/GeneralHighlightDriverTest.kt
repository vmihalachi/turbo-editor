package com.manichord.viperedit

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.manichord.viperedit.home.HighlightColorProvider
import com.manichord.viperedit.home.HighlightDriver

class GeneralHighlightDriverTest {

    private lateinit var mockColorProvider: HighlightColorProvider
    private lateinit var highlightDriver: HighlightDriver

    @Before
    fun setUp() {
        mockColorProvider = MockColorProvider()
        highlightDriver = HighlightDriver(mockColorProvider, "java")
    }

    @Test
    fun testKeyword() {
        val highlights = highlightDriver.highlightText("public", 0)

        assertEquals(1, highlights.size)
        assertEquals(mockColorProvider.keywordColor, highlights[0].color)
        assertEquals(0, highlights[0].start)
        assertEquals(6, highlights[0].end)
    }

    @Test
    fun testMultipleKeywords() {
        val highlights = highlightDriver.highlightText("public void", 0)

        assertEquals(2, highlights.size)
        assertEquals(mockColorProvider.keywordColor, highlights[0].color)
        assertEquals(mockColorProvider.keywordColor, highlights[1].color)
        assertEquals(0, highlights[0].start)
        assertEquals(6, highlights[0].end)
        assertEquals(7, highlights[1].start)
        assertEquals(11, highlights[1].end)
    }
}

class MockColorProvider : HighlightColorProvider {
    override val keywordColor: Int
        get() = 1
    override val attrColor: Int
        get() = 2
    override val attrValueColor: Int
        get() = 3
    override val commentColor: Int
        get() = 4
    override val stringColor: Int
        get() = 5
    override val numberColor: Int
        get() = 6
    override val variableColor: Int
        get() = 7
}