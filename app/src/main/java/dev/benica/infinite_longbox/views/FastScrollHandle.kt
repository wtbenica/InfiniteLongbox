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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.benica.infinite_longbox.R
import kotlin.math.roundToInt

/**
 * A scroll handle for long lists, laid over the list's right edge. The list's built-in fast
 * scroller sizes its handle by how much of the list is visible, so on a list of thousands of items
 * the handle shrinks to nothing and can't be grabbed. This one is always the same size and maps
 * the drag straight to an item position.
 */
class FastScrollHandle(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    interface Listener {
        /** The handle was dragged to [position]; the list is scrolling there. */
        fun onDrag(position: Int)
        fun onDragEnd()
    }

    var listener: Listener? = null

    /** Where the middle of the handle is, in this view's coordinates, for placing a label by it. */
    val thumbCenterY: Float
        get() = thumbTop + thumbHeight / 2f

    var isDragging = false
        private set

    private var recycler: RecyclerView? = null
    private var fraction = 0f

    private val density = resources.displayMetrics.density
    private val thumbWidth = 10 * density
    private val thumbHeight = 56 * density
    private val edgeMargin = 4 * density
    private val touchWidth = 44 * density
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val restColor = ContextCompat.getColor(context, R.color.accent)
    private val dragColor = ContextCompat.getColor(context, R.color.accent_strong)

    private val hide = Runnable { animate().alpha(0f).setDuration(200).start() }

    init {
        alpha = 0f
    }

    private val thumbTop: Float
        get() = fraction * (height - thumbHeight).coerceAtLeast(0f)

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (isDragging) return
            val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
            val total = rv.adapter?.itemCount ?: return
            val first = layoutManager.findFirstVisibleItemPosition()
            val visible = layoutManager.findLastVisibleItemPosition() - first + 1
            if (first < 0 || total <= visible) return

            fraction = (first.toFloat() / (total - visible)).coerceIn(0f, 1f)
            show()
            invalidate()
        }
    }

    fun attach(recyclerView: RecyclerView) {
        recycler?.removeOnScrollListener(scrollListener)
        recycler = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
    }

    fun detach() {
        recycler?.removeOnScrollListener(scrollListener)
        recycler = null
        removeCallbacks(hide)
    }

    private fun show() {
        removeCallbacks(hide)
        animate().cancel()
        alpha = 1f
        if (!isDragging) postDelayed(hide, HIDE_DELAY_MS)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = if (isDragging) dragColor else restColor
        val right = width - edgeMargin
        rect.set(right - thumbWidth, thumbTop, right, thumbTop + thumbHeight)
        canvas.drawRoundRect(rect, thumbWidth / 2, thumbWidth / 2, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only the handle itself is grabbed; everything else falls through to the list
                val onHandle = alpha > 0f && event.x >= width - touchWidth &&
                        event.y in (thumbTop - GRAB_SLOP * density)..(thumbTop + thumbHeight + GRAB_SLOP * density)
                if (!onHandle) return false
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                show()
                scrollTo(event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> if (isDragging) {
                scrollTo(event.y)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isDragging) {
                isDragging = false
                invalidate()
                postDelayed(hide, HIDE_DELAY_MS)
                listener?.onDragEnd()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun scrollTo(y: Float) {
        val rv = recycler ?: return
        val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
        val total = rv.adapter?.itemCount ?: return
        if (total == 0) return

        val travel = (height - thumbHeight).coerceAtLeast(1f)
        fraction = ((y - thumbHeight / 2f) / travel).coerceIn(0f, 1f)
        val position = (fraction * (total - 1)).roundToInt()
        layoutManager.scrollToPositionWithOffset(position, 0)
        invalidate()
        listener?.onDrag(position)
    }

    companion object {
        private const val HIDE_DELAY_MS = 1500L
        private const val GRAB_SLOP = 12
    }
}
