package com.creazzy.novakeys.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSetupStateTest {
    private val ownId = "com.creazzy.novakeys/com.creazzy.novakeys.ime.NovaInputMethodService"

    @Test fun detectsEnabledKeyboardAmongSeveralMethods() {
        assertTrue(KeyboardSetupState.isEnabled("other/.Ime:$ownId:third/.Ime", ownId))
    }

    @Test fun disabledOrEmptyListIsNotEnabled() {
        assertFalse(KeyboardSetupState.isEnabled(null, ownId))
        assertFalse(KeyboardSetupState.isEnabled("other/.Ime:third/.Ime", ownId))
    }

    @Test fun detectsDefaultKeyboardExactly() {
        assertTrue(KeyboardSetupState.isDefault(ownId, ownId))
        assertFalse(KeyboardSetupState.isDefault("other/.Ime", ownId))
        assertFalse(KeyboardSetupState.isDefault(null, ownId))
    }
}
