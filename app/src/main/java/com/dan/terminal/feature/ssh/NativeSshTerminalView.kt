package com.dan.terminal.feature.ssh

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import kotlin.math.ceil
import kotlin.math.max

/**
 * First native SSH terminal renderer.
 *
 * The Termux TerminalEmulator parses ANSI/VT output and maintains the cursor,
 * alternate screen and a bounded scrollback. This initial canvas renderer
 * draws the emulator's visible character grid (monochrome). Colors, text
 * selection, Sixel and complex grapheme sizing need dedicated follow-up tests.
 *
 * Unlike TerminalView, this View does not need a local subprocess and sends
 * its keyboard/IME input straight to the SSH channel.
 */
internal class NativeSshTerminalView(
    context: Context,
    private val ssh: NativeSshController
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 237, 243)
        typeface = Typeface.MONOSPACE
        textSize = 14f * resources.displayMetrics.scaledDensity
    }
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(111, 216, 177)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density.coerceAtLeast(1f)
    }

    private val fontWidth: Float
        get() = paint.measureText("M").coerceAtLeast(1f)
    private val lineHeight: Int
        get() = ceil(paint.fontSpacing.toDouble()).toInt().coerceAtLeast(1)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        contentDescription = "Remote BlackArch SSH terminal"
        setBackgroundColor(Color.rgb(12, 17, 23))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ssh.redraw = { postInvalidateOnAnimation() }
        requestFocus()
    }

    override fun onDetachedFromWindow() {
        ssh.redraw = null
        super.onDetachedFromWindow()
    }

    fun setFontSp(size: Int) {
        val newSize = size.coerceIn(10, 28) * resources.displayMetrics.scaledDensity
        if (paint.textSize != newSize) {
            paint.textSize = newSize
            updateTerminalSize(width, height)
            invalidate()
        }
    }

    private fun updateTerminalSize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val columns = max(4, (w / fontWidth).toInt()).coerceAtMost(240)
        val rows = max(4, h / lineHeight).coerceAtMost(120)
        ssh.resize(columns, rows, fontWidth.toInt().coerceAtLeast(1), lineHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTerminalSize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val emulator = ssh.emulator
        val screen = emulator.screen
        val fm = paint.fontMetrics
        val baselineOffset = -fm.ascent

        for (row in 0 until emulator.mRows) {
            val line = screen.getSelectedText(0, row, emulator.mColumns - 1, row)
            if (line.isNotEmpty()) {
                canvas.drawText(line, 0f, row * lineHeight + baselineOffset, paint)
            }
        }

        val row = emulator.cursorRow
        val column = emulator.cursorCol
        if (row in 0 until emulator.mRows && column in 0 until emulator.mColumns) {
            val x = column * fontWidth
            val y = row * lineHeight.toFloat()
            canvas.drawRect(x, y, x + fontWidth, y + lineHeight, cursorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            requestFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            performClick()
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_ACTION_NONE

        return object : BaseInputConnection(this, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (text != null) ssh.sendText(text.toString())
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                repeat(beforeLength.coerceIn(0, 80)) { ssh.sendBytes(byteArrayOf(0x7f)) }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean =
                if (event.action == KeyEvent.ACTION_DOWN) {
                    handleKeyDown(event.keyCode, event)
                } else true

            override fun performEditorAction(actionCode: Int): Boolean {
                ssh.sendBytes(byteArrayOf(13))
                return true
            }
        }
    }

    private fun handleKeyDown(code: Int, event: KeyEvent): Boolean {
        when (code) {
            KeyEvent.KEYCODE_ENTER -> ssh.sendBytes(byteArrayOf(13))
            KeyEvent.KEYCODE_DEL -> ssh.sendBytes(byteArrayOf(0x7f))
            KeyEvent.KEYCODE_TAB -> ssh.sendBytes(byteArrayOf(9))
            KeyEvent.KEYCODE_ESCAPE -> ssh.sendBytes(byteArrayOf(27))
            KeyEvent.KEYCODE_DPAD_UP -> ssh.sendText("\u001b[A")
            KeyEvent.KEYCODE_DPAD_DOWN -> ssh.sendText("\u001b[B")
            KeyEvent.KEYCODE_DPAD_RIGHT -> ssh.sendText("\u001b[C")
            KeyEvent.KEYCODE_DPAD_LEFT -> ssh.sendText("\u001b[D")
            else -> {
                val unicode = event.unicodeChar
                if (unicode != 0 && !Character.isISOControl(unicode)) {
                    ssh.sendText(String(Character.toChars(unicode)))
                } else {
                    return false
                }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        handleKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
}
