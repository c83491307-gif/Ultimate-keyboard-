package com.creazzy.novakeys.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.inputmethodservice.InputMethodService
import com.creazzy.novakeys.clipboard.ClipboardImage
import com.creazzy.novakeys.clipboard.ScreenshotClipboardActivity
import com.creazzy.novakeys.clipboard.ScreenshotClipboardStore
import com.creazzy.novakeys.clipboard.TextToImageActivity
import java.io.File

/**
 * Bilingual keyboard with a persistent local image strip.
 * The image clipboard works in compatible editors via InputConnection.commitContent;
 * for editors that reject images, the image URI is placed on the system clipboard.
 */
class NovaInputMethodService : InputMethodService() {
    private var language = "en"
    private var numeric = false
    private var shifted = false
    private var root: LinearLayout? = null
    private var screenshotThread: HandlerThread? = null
    private var screenshotHandler: Handler? = null
    private var screenshotObserver: ContentObserver? = null

    private val pendingScreenshotScan = Runnable {
        if (ScreenshotClipboardStore.isAutoSyncEnabled(this)) {
            val count = runCatching { ScreenshotClipboardStore.scanNewScreenshots(this) }.getOrDefault(0)
            if (count > 0) Handler(mainLooper).post {
                Toast.makeText(this, "Saved $count new screenshot(s) to NovaKeys image clipboard", Toast.LENGTH_SHORT).show()
                if (root?.isShown == true) rebuild()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        screenshotThread = HandlerThread("NovaKeysScreenshotObserver").apply { start() }
        screenshotHandler = Handler(screenshotThread!!.looper)
        screenshotObserver = object : ContentObserver(screenshotHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                screenshotHandler?.removeCallbacks(pendingScreenshotScan)
                screenshotHandler?.postDelayed(pendingScreenshotScan, 1100L)
            }
        }
        runCatching {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, screenshotObserver!!
            )
        }
        // Resume any opt-in sync that was enabled before the keyboard service started.
        screenshotHandler?.postDelayed(pendingScreenshotScan, 500L)
    }

    override fun onDestroy() {
        runCatching { screenshotObserver?.let { contentResolver.unregisterContentObserver(it) } }
        screenshotHandler?.removeCallbacks(pendingScreenshotScan)
        screenshotThread?.quitSafely()
        screenshotObserver = null
        screenshotHandler = null
        screenshotThread = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        numeric = false
        shifted = attribute?.inputType?.and(0x0000000f) == 2
        if (root != null) rebuild()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        rebuild()
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
        toolbar.addView(label("NovaKeys", 14f, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        toolbar.addView(toolButton(if (language == "en") "EN · عربي" else "عربي · EN") {
            language = if (language == "en") "ar" else "en"; rebuild()
        }, LinearLayout.LayoutParams(0, dp(38), 1.25f))
        toolbar.addView(toolButton("🖼") { openImageLibrary() }, LinearLayout.LayoutParams(dp(42), dp(38)))
        toolbar.addView(toolButton("T→IMG") { openTextImage() }, LinearLayout.LayoutParams(dp(62), dp(38)))
        toolbar.addView(toolButton(if (numeric) "ABC" else "123") {
            numeric = !numeric; rebuild()
        }, LinearLayout.LayoutParams(dp(48), dp(38)))
        toolbar.addView(toolButton("⌄") { requestHideSelf(0) }, LinearLayout.LayoutParams(dp(34), dp(38)))
        panel.addView(toolbar)
        addRecentImages(panel)

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
                    LinearLayout.LayoutParams(0, dp(43), weight).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    })
            }
            panel.addView(line)
        }
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(keyButton("?123") { numeric = !numeric; rebuild() }, LinearLayout.LayoutParams(dp(60), dp(44)).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton(if (language == "ar") "،" else ",") { commit(if (language == "ar") "،" else ",") }, LinearLayout.LayoutParams(0, dp(44), 0.7f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton("Space") { commit(" ") }, LinearLayout.LayoutParams(0, dp(44), 3f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        bottom.addView(keyButton(if (isLastField()) "✓" else "↵") { handleEnter() }, LinearLayout.LayoutParams(dp(54), dp(44)).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        panel.addView(bottom)
    }

    private fun addRecentImages(panel: LinearLayout) {
        val images = ScreenshotClipboardStore.list(this, 6)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(2), dp(3), dp(2))
        }
        row.addView(label(if (images.isEmpty()) "Images +" else "Recent", 10f, true),
            LinearLayout.LayoutParams(dp(54), dp(48)))
        images.forEach { item ->
            val thumbnail = ImageView(this).apply {
                contentDescription = "Paste ${item.fileName}"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(6).toFloat()
                    setStroke(dp(1), Color.rgb(206, 215, 226))
                }
                clipToOutline = true
                setPadding(dp(2), dp(2), dp(2), dp(2))
                val file = File(ScreenshotClipboardStore.imageDirectory(this@NovaInputMethodService), item.fileName)
                val bitmap = runCatching {
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 10 })
                }.getOrNull()
                if (bitmap != null) setImageBitmap(bitmap)
                setOnClickListener { commitImage(item) }
            }
            row.addView(thumbnail, LinearLayout.LayoutParams(dp(48), dp(44)).apply {
                setMargins(dp(2), 0, dp(3), 0)
            })
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun commitImage(image: ClipboardImage) {
        val uri = runCatching { ScreenshotClipboardStore.uriFor(this, image) }.getOrElse {
            Toast.makeText(this, "Image is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        val mime = ScreenshotClipboardStore.mimeType(image)
        val supportedTypes = currentInputEditorInfo?.contentMimeTypes
        val editorSupportsImage = supportedTypes?.any { type ->
            ClipDescription.compareMimeTypes(mime, type) || ClipDescription.compareMimeTypes(type, mime)
        } == true
        val connection = currentInputConnection
        if (editorSupportsImage && connection != null) {
            val description = ClipDescription(image.fileName, arrayOf(mime))
            val content = InputContentInfo(uri, description, null)
            val accepted = runCatching {
                connection.commitContent(content, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, Bundle())
            }.getOrDefault(false)
            if (accepted) {
                Toast.makeText(this, "Image sent to the current app", Toast.LENGTH_SHORT).show()
                return
            }
        }
        runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(ClipData.newUri(contentResolver, image.fileName, uri))
            Toast.makeText(this, "Image copied. Use Paste if this app supports image pasting.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, "This app does not accept pasted images", Toast.LENGTH_LONG).show()
        }
    }

    private fun openImageLibrary() {
        startActivity(Intent(this, ScreenshotClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openTextImage() {
        startActivity(Intent(this, TextToImageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
        setPadding(dp(4), 0, dp(2), 0)
    }

    private fun toolButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = if (text.length > 5) 10f else 12f
        isAllCaps = false
        setPadding(dp(1), 0, dp(1), 0)
        setOnClickListener { action() }
    }

    private fun keyButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = if (text.length > 4) 11f else 16f
        isAllCaps = false
        setTextColor(Color.rgb(32, 42, 57))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
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
