/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.benica.infinite_longbox.LetterIndex
import dev.benica.infinite_longbox.R

/**
 * A column of letters laid over the list's right edge, for lists sorted by name: touch or drag
 * along it to jump to the names starting with that letter. It shows while the list scrolls, with
 * the letter the list is at picked out, and replaces the scroll handle where it is in use.
 */
class LetterStrip(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    interface Listener {
        /** The strip was touched at [letter]; the list should show [position]. */
        fun onLetter(letter: Char, position: Int)

        fun onHide()
    }

    var listener: Listener? = null

    /** Where each letter starts; the strip shows only when there is one. */
    var index: LetterIndex? = null
        set(value) {
            field = value
            if (value == null) {
                removeCallbacks(hide)
                alpha = 0f
            }
            invalidate()
        }

    /** The letter the list is at now. */
    var current: Char? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isDragging = false
        private set

    private val letters = LetterIndex.LETTERS
    private val density = resources.displayMetrics.density
    private val stripWidth = 30 * density
    private val touchWidth = 44 * density
    private val edgeMargin = 2 * density
    private val maxLetterHeight = 34 * density
    private val rect = RectF()
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface_raised)
        alpha = 0xE6
    }
    private val currentBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12 * density
        isFakeBoldText = true
    }
    private val textColor = ContextCompat.getColor(context, R.color.accent_strong)
    private val currentTextColor = ContextCompat.getColor(context, R.color.on_accent)

    private val hide = Runnable {
        animate().alpha(0f).setDuration(200).start()
        listener?.onHide()
    }

    init {
        alpha = 0f
    }

    // The strip stays inside its padding, which keeps it above the bottom sheet like the list
    private val usableHeight: Float
        get() = (height - paddingTop - paddingBottom).toFloat()

    private val letterHeight: Float
        get() = (usableHeight / letters.size).coerceAtMost(maxLetterHeight)

    private val top: Float
        get() = paddingTop + (usableHeight - letterHeight * letters.size) / 2f

    /** Where the middle of [letter] is, in this view's coordinates, for placing a label by it. */
    fun letterCenterY(letter: Char): Float =
        top + (letters.indexOf(letter.uppercaseChar()).coerceAtLeast(0) + 0.5f) * letterHeight

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (index != null && !isDragging) show()
        }
    }

    fun attach(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(scrollListener)
    }

    private fun show() {
        removeCallbacks(hide)
        animate().cancel()
        alpha = 1f
        if (!isDragging) postDelayed(hide, HIDE_DELAY_MS)
    }

    override fun onDraw(canvas: Canvas) {
        val index = index ?: return
        val right = width - edgeMargin
        val lh = letterHeight
        rect.set(right - stripWidth, top, right, top + lh * letters.size)
        canvas.drawRoundRect(rect, stripWidth / 2, stripWidth / 2, background)

        val centerX = right - stripWidth / 2
        letters.forEachIndexed { i, letter ->
            val centerY = top + (i + 0.5f) * lh
            val isCurrent = letter == current
            if (isCurrent) canvas.drawCircle(centerX, centerY, stripWidth / 2 - 2 * density, currentBackground)
            text.color = if (isCurrent) currentTextColor else textColor
            // Letters with no names are faint
            text.alpha = if (isCurrent || index.has(letter)) 0xFF else 0x55
            canvas.drawText(
                letter.toString(),
                centerX,
                centerY - (text.ascent() + text.descent()) / 2,
                text
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only the strip itself is grabbed; everything else falls through to the list
                if (index == null || alpha == 0f || event.x < width - touchWidth) return false
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                show()
                jumpTo(event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> if (isDragging) {
                jumpTo(event.y)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isDragging) {
                isDragging = false
                postDelayed(hide, HIDE_DELAY_MS)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Scrolls to the letter at [y], or to the nearest letter below it that has names. */
    private fun jumpTo(y: Float) {
        val index = index ?: return
        val touched = ((y - top) / letterHeight).toInt().coerceIn(0, letters.size - 1)
        val chosen = (touched until letters.size).firstOrNull { index.has(letters[it]) }
            ?: (touched downTo 0).firstOrNull { index.has(letters[it]) }
            ?: return
        val letter = letters[chosen]
        current = letter
        listener?.onLetter(letter, index.positionOf(letter) ?: return)
    }

    companion object {
        private const val HIDE_DELAY_MS = 1500L
    }
}
