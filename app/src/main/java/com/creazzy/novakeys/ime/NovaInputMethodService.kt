package com.creazzy.novakeys.ime

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight functional NovaKeys keyboard.
 * Credential autofill remains handled by NovaAutofillService / Android's Autofill UI.
 * InlineSuggestions are intentionally not advertised until implemented with Android's
 * InlinePresentation APIs and verified against real autofill clients.
 */
class NovaInputMethodService : InputMethodService() {
    private var language = "en"
    private var numeric = false
    private var shifted = false
    private var root: LinearLayout? = null

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        numeric = false
        shifted = attribute?.inputType?.and(0x0000000f) == 2
        if (root != null) rebuild()
    }

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(6), dp(5), dp(8))
            setBackgroundColor(Color.rgb(235, 239, 245))
        }
        rebuild()
        return root!!
    }

    private fun rebuild() {
        val panel = root ?: return
        panel.removeAllViews()
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(label("NovaKeys", 15f, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        toolbar.addView(toolButton(if (language == "en") "EN · العربية" else "عربي · EN") {
            language = if (language == "en") "ar" else "en"; rebuild()
        }, LinearLayout.LayoutParams(0, dp(38), 1.4f))
        toolbar.addView(toolButton(if (numeric) "ABC" else "123") {
            numeric = !numeric; rebuild()
        }, LinearLayout.LayoutParams(dp(62), dp(38)))
        toolbar.addView(toolButton("⌄") { requestHideSelf(0) }, LinearLayout.LayoutParams(dp(42), dp(38)))
        panel.addView(toolbar)

        val rows = when {
            numeric -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("-","0",",","."))
            language == "ar" -> listOf(
                listOf("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"),
                listOf("ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط"),
                listOf("ذ","ئ","ء","ؤ","ر","لا","ى","ة","و","ز","ظ")
            )
            else -> listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("⇧","z","x","c","v","b","n","m","⌫")
            )
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            if (!numeric && language == "en" && index == 1) line.setPadding(dp(12), 0, dp(12), 0)
            row.forEach { key ->
                val weight = if (key == "⇧" || key == "⌫") 1.35f else 1f
                line.addView(keyButton(displayKey(key)) { handleKey(key) },
                    LinearLayout.LayoutParams(0, dp(45), weight).apply {
                        setMargins(dp(2), dp(3), dp(2), dp(3))
                    })
            }
            panel.addView(line)
        }
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(keyButton("?123") { numeric = !numeric; rebuild() }, LinearLayout.LayoutParams(dp(62), dp(46)).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton(if (language == "ar") "،" else ",") { commit(if (language == "ar") "،" else ",") }, LinearLayout.LayoutParams(0, dp(46), 0.7f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton(if (language == "ar") "العربية" else "English") { commit(if (language == "ar") " " else " ") }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton("Space") { commit(" ") }, LinearLayout.LayoutParams(0, dp(46), 3f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton(if (isLastField()) "✓" else "↵") { handleEnter() }, LinearLayout.LayoutParams(dp(54), dp(46)).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        panel.addView(bottom)
    }

    private fun handleKey(key: String) {
        when (key) {
            "⇧" -> { shifted = !shifted; rebuild() }
            "⌫" -> currentInputConnection?.let { connection ->
                if (!connection.deleteSurroundingText(1, 0)) connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            }
            else -> commit(if (shifted && key.length == 1) key.uppercase() else key).also { if (shifted) { shifted = false; rebuild() } }
        }
    }

    private fun handleEnter() {
        val connection = currentInputConnection ?: return
        if (isLastField()) connection.performEditorAction(currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_DONE)
        else connection.commitText("\n", 1)
    }

    private fun isLastField(): Boolean {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        return action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_SEND
    }

    private fun commit(value: String) { currentInputConnection?.commitText(value, 1) }

    private fun displayKey(key: String): String =
        if (shifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key

    private fun label(text: String, size: Float, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.rgb(39, 50, 67))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, dp(4), 0)
    }

    private fun toolButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 11f
        isAllCaps = false
        setPadding(dp(2), 0, dp(2), 0)
        setOnClickListener { action() }
    }

    private fun keyButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = if (text.length > 4) 11f else 16f
        isAllCaps = false
        setTextColor(Color.rgb(32, 42, 57))
        background = GradientDrawable().apply {
            setColor(if (text == "Space" || text == "English" || text == "العربية") Color.WHITE else Color.rgb(255, 255, 255))
            cornerRadius = dp(7).toFloat()
            setStroke(dp(1), Color.rgb(215, 221, 230))
        }
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(1), 0, dp(1), 0)
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
