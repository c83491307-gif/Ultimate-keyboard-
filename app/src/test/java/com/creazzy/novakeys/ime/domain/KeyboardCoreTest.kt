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
    @Test fun controllerExecutesEffectsAndOwnsUpdatedState() {
        val executed = mutableListOf<EditorEffect>()
        val controller = KeyboardController(effectHandler = executed::add)

        controller.dispatch(KeyboardAction.TypeText("Hi"))
        controller.dispatch(KeyboardAction.ToggleShift)

        assertEquals(listOf(EditorEffect.CommitText("Hi")), executed)
        assertTrue(controller.state.shifted)
    }

    @Test fun editorResetAppliesNumericModeAndSensitivePolicy() {
        val controller = KeyboardController(effectHandler = {})
        controller.dispatch(KeyboardAction.OpenFeature(KeyboardFeature.CLIPBOARD))

        val state = controller.resetForEditor(
            EditorContext(isSensitive = true, isMultiline = false),
            KeyboardMode.NUMBERS
        )

        assertEquals(KeyboardMode.NUMBERS, state.mode)
        assertEquals(KeyboardFeature.NONE, state.feature)
        assertTrue(state.editor.isSensitive)
        assertTrue(!state.shifted)
    }

}
