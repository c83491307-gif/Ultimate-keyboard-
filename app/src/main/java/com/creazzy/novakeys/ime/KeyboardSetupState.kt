package com.creazzy.novakeys.ime

/** Pure helpers for showing accurate keyboard setup status without privileged settings access. */
object KeyboardSetupState {
    fun isEnabled(enabledInputMethods: String?, componentId: String): Boolean =
        enabledInputMethods.orEmpty().split(':').any { it.equals(componentId, ignoreCase = true) }

    fun isDefault(defaultInputMethod: String?, componentId: String): Boolean =
        defaultInputMethod.orEmpty().equals(componentId, ignoreCase = true)
}
