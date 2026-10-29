package com.creazzy.novakeys.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.widget.TextView

/**
 * Keyboard service scaffold. Credential autofill is provided by Android's AutofillService.
 * Inline suggestion UI is not claimed here until InlinePresentation rendering is implemented
 * and tested against real apps; Android's standard Autofill UI remains the fallback.
 */
class NovaInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View = TextView(this).apply {
        text = "NovaKeys • ABC   العربية   123"
        textSize = 18f
        setPadding(24, 28, 24, 28)
    }
}
