package com.creazzy.novakeys.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.os.Bundle
import android.widget.TextView

/**
 * Minimal IME host for inline autofill. The platform AutofillService owns credential
 * matching; Android decides whether inline presentation is supported by the focused app.
 */
class NovaInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View = TextView(this).apply {
        text = "NovaKeys • ABC   العربية   123"
        textSize = 18f
        setPadding(24, 28, 24, 28)
    }

    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (android.os.Build.VERSION.SDK_INT < 30) return null
        return InlineSuggestionsRequest.Builder(
            android.view.inputmethod.InlinePresentationSpec(
                android.util.Size(220, 48),
                android.view.inputmethod.InlineSuggestionUi.getStyle(this)
            )
        ).setMaxSuggestionCount(3).build()
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        // Android's inline UI is rendered by the system/host app; returning true means
        // the IME accepts the response. A full production keyboard should render its own
        // suggestions strip for platforms/apps without inline support.
        return true
    }
}
