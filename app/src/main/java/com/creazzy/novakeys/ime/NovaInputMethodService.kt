package com.creazzy.novakeys.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.inputmethod.InlinePresentationSpec
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.widget.TextView

/**
 * IME-side request hook for Android 11+ inline autofill. Actual credential matching
 * and consent are handled by the system Autofill framework and NovaAutofillService.
 */
class NovaInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View = TextView(this).apply {
        text = "NovaKeys • ABC   العربية   123"
        textSize = 18f
        setPadding(24, 28, 24, 28)
    }

    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val spec = InlinePresentationSpec.Builder(Size(240, 48), Bundle()).build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(3)
            .build()
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        // Returning true allows the framework to deliver inline suggestions to the host.
        // Apps that do not support inline UI fall back to the platform autofill UI.
        return true
    }
}
