package com.creazzy.novakeys.ime.domain

/**
 * Owns IME state and executes platform effects through an injected boundary.
 * The domain remains independent of Android and can be tested on the JVM.
 */
class KeyboardController(
    initialState: KeyboardState = KeyboardState(),
    private val effectHandler: (EditorEffect) -> Unit
) {
    var state: KeyboardState = initialState
        private set

    fun dispatch(action: KeyboardAction): KeyboardState {
        val transition = KeyboardReducer.reduce(state, action)
        state = transition.state
        transition.effects.forEach(effectHandler)
        return state
    }

    fun resetForEditor(context: EditorContext, initialMode: KeyboardMode = KeyboardMode.LETTERS): KeyboardState {
        state = state.copy(
            mode = initialMode,
            shifted = false,
            feature = KeyboardFeature.NONE,
            editor = context
        )
        return state
    }
}
