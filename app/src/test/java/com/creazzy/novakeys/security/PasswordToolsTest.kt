package com.creazzy.novakeys.security

import org.junit.Assert.*
import org.junit.Test

class PasswordToolsTest {
    @Test fun generatedPasswordHasRequestedLengthAndCharacterClasses() {
        val password = PasswordTools.generate(24, true)
        assertEquals(24, password.length)
        assertTrue(password.any(Char::isLowerCase))
        assertTrue(password.any(Char::isUpperCase))
        assertTrue(password.any(Char::isDigit))
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test fun generatorRejectsUnsafeShortLengths() {
        assertThrows(IllegalArgumentException::class.java) { PasswordTools.generate(8) }
    }

    @Test fun duplicatePasswordsAreFlagged() {
        val findings = PasswordTools.weakOrRepeated(listOf("short", "short", "LongerPassword!"))
        assertEquals("Repeated password", findings["short"])
    }
}
