package com.creazzy.novakeys.testing

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Debug-only host screen used by CI to prove that NovaKeys binds to and displays
 * inside a real editable field. This activity is excluded from release APKs.
 *
 * The emulator can take several seconds to transfer window focus after launching
 * this activity. Retry only while this activity owns window focus so the test
 * exercises Android's real IME lifecycle instead of relying on a coordinate tap.
 */
class ImeSmokeTestActivity : Activity() {
    private var editor: EditText? = null
    private var status: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var requestAttempts = 0
    private val requestKeyboardAgain = object : Runnable {
        override fun run() {
            val target = editor ?: return
            if (isFinishing || isDestroyed || !target.isAttachedToWindow) return
            if (!hasWindowFocus()) {
                handler.postDelayed(this, RETRY_INTERVAL_MS)
                return
            }

            target.requestFocus()
            val focused = target.hasFocus() && currentFocus === target
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val accepted = if (focused) {
                manager.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
            } else {
                false
            }
            window.insetsController?.show(WindowInsets.Type.ime())
            requestAttempts++
            status?.text = "windowFocus=${hasWindowFocus()} editorFocus=${target.hasFocus()} " +
                "showAccepted=$accepted attempts=$requestAttempts"
            Log.i(TAG, "IME smoke request #$requestAttempts: windowFocus=${hasWindowFocus()}, " +
                "editorFocus=${target.hasFocus()}, currentFocus=${currentFocus?.javaClass?.simpleName}, " +
                "showAccepted=$accepted")

            // Continue briefly: the IME service may still be starting on a cold emulator.
            if (requestAttempts < MAX_REQUEST_ATTEMPTS) {
                handler.postDelayed(this, RETRY_INTERVAL_MS)
            }
        }
    }

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
            isFocusableInTouchMode = true
        }
        container.addView(TextView(this).apply {
            text = "NovaKeys emulator input test"
            textSize = 20f
        })
        status = TextView(this).apply {
            text = "Waiting for editor focus…"
            textSize = 12f
        }
        container.addView(status)

        val input = EditText(this).apply {
            hint = "Keyboard test field"
            contentDescription = "novakeys-ime-test-editor"
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            showSoftInputOnFocus = true
            setSingleLine(true)
        }
        editor = input
        container.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(container)
        container.requestFocus()
        input.setOnFocusChangeListener { _, hasFocus ->
            Log.i(TAG, "Smoke editor focus changed: $hasFocus")
            if (hasFocus) scheduleKeyboardRequest()
        }
    }

    override fun onResume() {
        super.onResume()
        scheduleKeyboardRequest()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.i(TAG, "Smoke host window focus changed: $hasFocus")
        if (hasFocus) scheduleKeyboardRequest() else handler.removeCallbacks(requestKeyboardAgain)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        editor = null
        status = null
        super.onDestroy()
    }

    private fun scheduleKeyboardRequest() {
        handler.removeCallbacks(requestKeyboardAgain)
        requestAttempts = 0
        handler.post {
            val target = editor ?: return@post
            target.requestFocus()
            handler.postDelayed(requestKeyboardAgain, INITIAL_REQUEST_DELAY_MS)
        }
    }

    companion object {
        private const val TAG = "NovaKeysImeSmoke"
        private const val INITIAL_REQUEST_DELAY_MS = 350L
        private const val RETRY_INTERVAL_MS = 500L
        private const val MAX_REQUEST_ATTEMPTS = 24
    }
}
