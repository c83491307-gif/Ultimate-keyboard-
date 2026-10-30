package com.creazzy.novakeys.testing

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Debug-only host screen used by CI to prove that NovaKeys binds to and displays
 * inside a real editable field. This activity is excluded from release APKs.
 */
class ImeSmokeTestActivity : Activity() {
    private var editor: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            contentDescription = "novakeys-ime-smoke-host"
        }
        container.addView(TextView(this).apply {
            text = "NovaKeys emulator input test"
            textSize = 20f
        })

        val input = EditText(this).apply {
            hint = "Keyboard test field"
            contentDescription = "novakeys-ime-test-editor"
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            showSoftInputOnFocus = true
        }
        editor = input
        container.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(container)
        input.requestFocus()
        input.postDelayed({ requestKeyboard() }, 700L)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) editor?.post { requestKeyboard() }
    }

    private fun requestKeyboard() {
        val target = editor ?: return
        if (!target.hasWindowFocus()) return
        target.requestFocus()
        val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }
}
