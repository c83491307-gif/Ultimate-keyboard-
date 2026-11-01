package com.creazzy.novakeys.ime.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardCoreTest {
    @Test fun typingProducesEditorEffectWithoutAndroidDependencies() {
        val transition = KeyboardReducer.reduce(
            KeyboardState(),
            KeyboardAction.TypeText("Nova")
        )
        assertEquals(listOf(EditorEffect.CommitText("Nova")), transition.effects)
    }

    @Test fun sensitiveEditorClosesAndBlocksOptionalFeatures() {
        val opened = KeyboardReducer.reduce(
            KeyboardState(),
            KeyboardAction.OpenFeature(KeyboardFeature.CLIPBOARD)
        )
        val sensitive = KeyboardReducer.reduce(
            opened.state,
            KeyboardAction.EditorChanged(EditorContext(isSensitive = true))
        )
        val attempted = KeyboardReducer.reduce(
            sensitive.state,
            KeyboardAction.OpenFeature(KeyboardFeature.DRAWING)
        )
        assertEquals(KeyboardFeature.NONE, sensitive.state.feature)
        assertEquals(KeyboardFeature.NONE, attempted.state.feature)
        assertTrue(attempted.effects.isEmpty())
    }

    @Test fun languageSwitchResetsNumberModeAndFeature() {
        val initial = KeyboardState(
            mode = KeyboardMode.NUMBERS,
            feature = KeyboardFeature.EMOJI
        )
        val next = KeyboardReducer.reduce(initial, KeyboardAction.SwitchLanguage).state
        assertEquals(KeyboardLanguage.ARABIC, next.language)
        assertEquals(KeyboardMode.LETTERS, next.mode)
        assertEquals(KeyboardFeature.NONE, next.feature)
    }

    @Test fun deleteIsAnEffectNotAPlatformCall() {
        val transition = KeyboardReducer.reduce(KeyboardState(), KeyboardAction.DeleteBackward)
        assertEquals(listOf(EditorEffect.DeleteBackward), transition.effects)
    }
}
