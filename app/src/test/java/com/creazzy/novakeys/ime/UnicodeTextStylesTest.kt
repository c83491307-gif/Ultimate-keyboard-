package com.creazzy.novakeys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnicodeTextStylesTest {
    @Test fun boldMapsLatinAndKeepsSpaces() {
        assertEquals("𝐇𝐢 𝐀𝐁𝐂", UnicodeTextStyles.transform("Hi ABC", UnicodeTextStyles.Style.BOLD))
    }

    @Test fun arabicRemainsReadableAndCanBeFramed() {
        assertEquals("مرحبا", UnicodeTextStyles.transform("مرحبا", UnicodeTextStyles.Style.BOLD))
        assertEquals("꧁ مرحبا ꧂", UnicodeTextStyles.decorate("مرحبا", "frame"))
    }

    @Test fun circledAndSmallCapsProduceCopyableUnicode() {
        assertEquals("Ⓗⓘ ①⓪", UnicodeTextStyles.transform("Hi 10", UnicodeTextStyles.Style.CIRCLED))
        assertEquals("ʜᴇʟʟᴏ", UnicodeTextStyles.transform("hello", UnicodeTextStyles.Style.SMALL_CAPS))
    }

    @Test fun glitchAddsCombiningMarksWithoutRemovingText() {
        val result = UnicodeTextStyles.transform("Hello", UnicodeTextStyles.Style.GLITCH)
        assertTrue(result.startsWith("H"))
        assertTrue(result.contains("\u0301") || result.contains("\u030d") || result.contains("\u0336"))
    }
}
