package com.creazzy.novakeys.testing

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Debug-only host screen used by CI to prove that the NovaKeys IME can attach
 * to a real editable field in an Android emulator. This activity is not in
 * release source sets and is not shipped in production APKs.
 */
class ImeSmokeTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            contentDescription = "novakeys-ime-smoke-host"
        }
        container.addView(TextView(this).apply {
            text = "NovaKeys emulator input test"
            textSize = 20f
        })

        val editor = EditText(this).apply {
            hint = "Keyboard test field"
            contentDescription = "novakeys-ime-test-editor"
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusableInTouchMode = true
        }
        container.addView(editor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(container)
        editor.requestFocus()
        window.decorView.post {
            editor.requestFocus()
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }
}
