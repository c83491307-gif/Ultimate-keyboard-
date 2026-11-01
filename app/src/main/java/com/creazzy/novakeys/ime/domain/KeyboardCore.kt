package com.creazzy.novakeys.ime.domain

/**
 * Platform-independent keyboard state and reducer.
 *
 * This file deliberately has no Android imports so its behavior can be tested
 * without booting an emulator.
 */
enum class KeyboardLanguage { ENGLISH, ARABIC }
enum class KeyboardMode { LETTERS, NUMBERS }
enum class KeyboardFeature { NONE, CLIPBOARD, EMOJI, DRAWING, TYPOGRAPHY }

data class EditorContext(
    val isSensitive: Boolean = false,
    val isMultiline: Boolean = false
)

data class KeyboardState(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val mode: KeyboardMode = KeyboardMode.LETTERS,
    val shifted: Boolean = false,
    val feature: KeyboardFeature = KeyboardFeature.NONE,
    val editor: EditorContext = EditorContext()
)

sealed interface KeyboardAction {
    data class TypeText(val text: String) : KeyboardAction
    data object DeleteBackward : KeyboardAction
    data object ToggleShift : KeyboardAction
    data object ToggleNumberMode : KeyboardAction
    data object SwitchLanguage : KeyboardAction
    data class OpenFeature(val feature: KeyboardFeature) : KeyboardAction
    data object CloseFeature : KeyboardAction
    data class EditorChanged(val context: EditorContext) : KeyboardAction
}

sealed interface EditorEffect {
    data class CommitText(val text: String) : EditorEffect
    data object DeleteBackward : EditorEffect
}

data class KeyboardTransition(
    val state: KeyboardState,
    val effects: List<EditorEffect> = emptyList()
)

/**
 * The reducer is the single owner of keyboard state transitions.
 * Android InputConnection calls are represented as effects and executed only
 * by the Android platform adapter.
 */
object KeyboardReducer {
    fun reduce(state: KeyboardState, action: KeyboardAction): KeyboardTransition =
        when (action) {
            is KeyboardAction.TypeText -> {
                if (action.text.isEmpty()) KeyboardTransition(state)
                else KeyboardTransition(state, listOf(EditorEffect.CommitText(action.text)))
            }

            KeyboardAction.DeleteBackward ->
                KeyboardTransition(state, listOf(EditorEffect.DeleteBackward))

            KeyboardAction.ToggleShift ->
                KeyboardTransition(state.copy(shifted = !state.shifted, feature = KeyboardFeature.NONE))

            KeyboardAction.ToggleNumberMode ->
                KeyboardTransition(
                    state.copy(
                        mode = if (state.mode == KeyboardMode.LETTERS) KeyboardMode.NUMBERS else KeyboardMode.LETTERS,
                        feature = KeyboardFeature.NONE
                    )
                )

            KeyboardAction.SwitchLanguage ->
                KeyboardTransition(
                    state.copy(
                        language = if (state.language == KeyboardLanguage.ENGLISH) KeyboardLanguage.ARABIC else KeyboardLanguage.ENGLISH,
                        mode = KeyboardMode.LETTERS,
                        feature = KeyboardFeature.NONE
                    )
                )

            is KeyboardAction.OpenFeature -> {
                val blockedForSensitiveEditor = state.editor.isSensitive && action.feature != KeyboardFeature.NONE
                if (blockedForSensitiveEditor) KeyboardTransition(state.copy(feature = KeyboardFeature.NONE))
                else KeyboardTransition(state.copy(feature = action.feature))
            }

            KeyboardAction.CloseFeature ->
                KeyboardTransition(state.copy(feature = KeyboardFeature.NONE))

            is KeyboardAction.EditorChanged -> {
                val nextFeature = if (action.context.isSensitive) KeyboardFeature.NONE else state.feature
                KeyboardTransition(state.copy(editor = action.context, feature = nextFeature))
            }
        }
}
