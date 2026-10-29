package com.creazzy.novakeys.ime

import android.content.ClipData
import android.content.Context
import android.content.ClipDescription
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

/**
 * Bilingual keyboard with a persistent local image strip.
 * The image clipboard works in compatible editors via InputConnection.commitContent;
 * for editors that reject images, the image URI is placed on the system clipboard.
 */
class NovaInputMethodService : InputMethodService() {
    private var language = "en"
    private var numeric = false
    private var shifted = false
    private var drawingMode = false
    private var typographyMode = false
    private var drawingCanvas: DrawingCanvasView? = null
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
        val sensitiveField = isSensitiveInput()
        if (sensitiveField) {
            drawingMode = false
            typographyMode = false
            drawingCanvas = null
        }
        toolbar.addView(label("NovaKeys", 14f, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        toolbar.addView(toolButton(if (language == "en") "EN · عربي" else "عربي · EN") {
            language = if (language == "en") "ar" else "en"; rebuild()
        }, LinearLayout.LayoutParams(0, dp(38), 1.25f))
        if (!sensitiveField) {
            toolbar.addView(toolButton("Aa") { drawingMode = false; typographyMode = !typographyMode; rebuild() }, LinearLayout.LayoutParams(dp(42), dp(38)))
            toolbar.addView(toolButton("🖼") { drawingMode = false; openImageLibrary() }, LinearLayout.LayoutParams(dp(40), dp(38)))
            toolbar.addView(toolButton("T→IMG") { drawingMode = false; openTextImage() }, LinearLayout.LayoutParams(dp(58), dp(38)))
            toolbar.addView(toolButton("✎ Draw") { drawingMode = !drawingMode; rebuild() }, LinearLayout.LayoutParams(dp(58), dp(38)))
        }
        toolbar.addView(toolButton(if (numeric) "ABC" else "123") {
            numeric = !numeric; rebuild()
        }, LinearLayout.LayoutParams(dp(48), dp(38)))
        toolbar.addView(toolButton("⌄") { requestHideSelf(0) }, LinearLayout.LayoutParams(dp(34), dp(38)))
        panel.addView(toolbar)
        if (drawingMode && !sensitiveField) {
            buildDrawingPanel(panel)
            return
        }
        if (typographyMode && !sensitiveField) {
            buildTypographyPanel(panel)
            return
        }
        if (!sensitiveField) addRecentImages(panel)

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

    private fun buildTypographyPanel(panel: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Unicode styles · select text first", 13f, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        header.addView(toolButton("Done") { typographyMode = false; rebuild() }, LinearLayout.LayoutParams(dp(62), dp(38)))
        panel.addView(header)
        val scroller = android.widget.ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(6))
        }
        val styles = listOf(
            UnicodeTextStyles.Style.BOLD, UnicodeTextStyles.Style.ITALIC, UnicodeTextStyles.Style.BOLD_ITALIC,
            UnicodeTextStyles.Style.SCRIPT, UnicodeTextStyles.Style.FRAKTUR, UnicodeTextStyles.Style.DOUBLE_STRUCK,
            UnicodeTextStyles.Style.SANS, UnicodeTextStyles.Style.MONOSPACE, UnicodeTextStyles.Style.FULLWIDTH,
            UnicodeTextStyles.Style.CIRCLED, UnicodeTextStyles.Style.SMALL_CAPS, UnicodeTextStyles.Style.LEET,
            UnicodeTextStyles.Style.GLITCH
        )
        styles.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { style ->
                row.addView(toolButton(style.title) { transformSelection(style) },
                    LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            repeat(3 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(38), 1f)) }
            content.addView(row)
        }
        content.addView(label("Frames and decorations", 12f, true))
        val decorations = listOf(
            "꧁ ꧂" to "frame", "༒ ༒" to "gothic", "✦ ✧" to "stars",
            "♡ ♥" to "heart", "『 』" to "brackets", "➶ ⇢" to "arrows",
            "[ root ]" to "terminal", "ᚺᚨ ᚾᛟ" to "runes"
        )
        decorations.chunked(4).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { (title, key) ->
                row.addView(toolButton(title) { decorateSelection(key) },
                    LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            repeat(4 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(38), 1f)) }
            content.addView(row)
        }
        content.addView(label("Tap a symbol to insert it", 12f, true))
        val symbolGroups = listOf(
            listOf("★", "☆", "✦", "✧", "✩", "✪", "⋆", "⁂", "✺"),
            listOf("♡", "♥", "❤", "❥", "❣", "ღ", "۵", "დ"),
            listOf("꧁", "꧂", "༒", "༄", "༅", "彡", "⟡", "『", "』", "【", "】"),
            listOf("➶", "➷", "⇢", "↳", "→", "┊", "╰", "╯", "─", "•"),
            listOf("☠", "☢", "☣", "⚰", "⚔", "♛", "♚", "⛧", "♔", "♕", "♖", "♗", "♘", "♙")
        )
        symbolGroups.forEach { symbols ->
            val horizontal = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(LinearLayout(this@NovaInputMethodService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    symbols.forEach { symbol ->
                        addView(toolButton(symbol) { commit(symbol) }, LinearLayout.LayoutParams(dp(42), dp(38)).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        })
                    }
                })
            }
            content.addView(horizontal)
        }
        content.addView(label("Unicode styles change characters, not installed fonts. Arabic and other scripts mostly keep their letters; use frames and symbols around them.", 10f, false))
        scroller.addView(content)
        panel.addView(scroller, LinearLayout.LayoutParams(-1, dp(238)))
    }

    private fun transformSelection(style: UnicodeTextStyles.Style) {
        val connection = currentInputConnection
        val selected = connection?.getSelectedText(0)?.toString().orEmpty()
        if (selected.isBlank()) {
            Toast.makeText(this, "Select text in the app first, then choose a style.", Toast.LENGTH_SHORT).show()
            return
        }
        connection.commitText(UnicodeTextStyles.transform(selected, style), 1)
    }

    private fun decorateSelection(decoration: String) {
        val connection = currentInputConnection
        val selected = connection?.getSelectedText(0)?.toString().orEmpty()
        if (selected.isBlank()) {
            Toast.makeText(this, "Select text in the app first, then choose a decoration.", Toast.LENGTH_SHORT).show()
            return
        }
        connection.commitText(UnicodeTextStyles.decorate(selected, decoration), 1)
    }

    private fun buildDrawingPanel(panel: LinearLayout) {
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(label("Draw with finger", 13f, true), LinearLayout.LayoutParams(0, dp(42), 1f))
        controls.addView(toolButton("Undo") { drawingCanvas?.undo() }, LinearLayout.LayoutParams(dp(58), dp(42)))
        controls.addView(toolButton("Clear") { drawingCanvas?.clear() }, LinearLayout.LayoutParams(dp(58), dp(42)))
        controls.addView(toolButton("Cancel") { drawingMode = false; drawingCanvas = null; rebuild() }, LinearLayout.LayoutParams(dp(64), dp(42)))
        panel.addView(controls)
        val canvasView = DrawingCanvasView(this).apply {
            contentDescription = "Drawing canvas. Draw with one finger."
        }
        drawingCanvas = canvasView
        panel.addView(canvasView, LinearLayout.LayoutParams(-1, dp(190)).apply {
            setMargins(dp(3), dp(4), dp(3), dp(4))
        })
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(toolButton("Save & insert image") {
            val drawing = drawingCanvas
            if (drawing == null || !drawing.hasInk()) {
                Toast.makeText(this@NovaInputMethodService, "Draw something first", Toast.LENGTH_SHORT).show()
            } else {
                val bitmap = drawing.exportBitmap()
                val saved = runCatching { ScreenshotClipboardStore.saveBitmap(this@NovaInputMethodService, bitmap) }
                bitmap.recycle()
                saved.onSuccess { image ->
                    drawingMode = false
                    drawingCanvas = null
                    rebuild()
                    commitImage(image)
                }.onFailure {
                    Toast.makeText(this@NovaInputMethodService, "Could not save drawing: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        panel.addView(actions)
        panel.addView(label("Saved as a local PNG. Image insertion depends on the receiving app.", 10f, false))
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

    private fun isSensitiveInput(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        val inputClass = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        return when (inputClass) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    private fun commitImage(image: ClipboardImage) {
        if (isSensitiveInput()) {
            Toast.makeText(this, "Image clipboard is hidden for password fields", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = runCatching { ScreenshotClipboardStore.uriFor(this, image) }.getOrElse {
            Toast.makeText(this, "Image is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        val mime = ScreenshotClipboardStore.mimeType(image)
        val editorInfo = currentInputEditorInfo
        val supportedTypes = editorInfo?.let { EditorInfoCompat.getContentMimeTypes(it) }
        val editorSupportsImage = supportedTypes?.any { type ->
            ClipDescription.compareMimeTypes(mime, type)
        } == true
        val connection = currentInputConnection
        if (editorSupportsImage && connection != null && editorInfo != null) {
            val description = ClipDescription(image.fileName, arrayOf(mime))
            val content = InputContentInfoCompat(uri, description, null)
            val accepted = runCatching {
                InputConnectionCompat.commitContent(
                    connection, editorInfo, content,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, Bundle()
                )
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


/** Lightweight, offline drawing surface used directly inside the IME window. */
private class DrawingCanvasView(context: Context) : View(context) {
    private data class Stroke(val path: Path, val paint: Paint)
    private val strokes = mutableListOf<Stroke>()
    private var activePath: Path? = null
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 54, 85)
        style = Paint.Style.STROKE
        strokeWidth = 7f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val backgroundPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }

    init {
        setBackgroundColor(Color.WHITE)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        strokes.forEach { canvas.drawPath(it.path, it.paint) }
        activePath?.let { canvas.drawPath(it, activePaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePath = Path().apply { moveTo(event.x, event.y) }
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                activePath?.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePath?.let { path ->
                    strokes += Stroke(path, Paint(activePaint))
                }
                activePath = null
                invalidate()
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }

    fun clear() {
        strokes.clear()
        activePath = null
        invalidate()
    }

    fun hasInk(): Boolean = strokes.isNotEmpty() || activePath != null

    fun exportBitmap(): Bitmap {
        val bitmapWidth = width.coerceAtLeast((resources.displayMetrics.widthPixels - 16).coerceAtLeast(320))
        val bitmapHeight = height.coerceAtLeast((190 * resources.displayMetrics.density).toInt())
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), backgroundPaint)
        strokes.forEach { canvas.drawPath(it.path, it.paint) }
        activePath?.let { canvas.drawPath(it, activePaint) }
        return bitmap
    }
}
