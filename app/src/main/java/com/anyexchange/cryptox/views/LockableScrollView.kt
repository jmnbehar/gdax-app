package com.anyexchange.cryptox.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import android.view.MotionEvent
import android.animation.ObjectAnimator


/**
 * Created by anyexchange on 1/15/2018.
 */

class LockableScrollView: ScrollView {
    constructor(ctx: Context) : super(ctx)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var scrollLocked = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // only continue to handle the touch event if scrolling enabled
                if (scrollLocked) {
                    false
                } else {
                    super.onTouchEvent(ev)
                }
            }
            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Don't do anything with intercepted touch events if not scrollable
        return if (scrollLocked) {
            false
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    fun scrollToTop(duration: Long) {
        val animator: ObjectAnimator = ObjectAnimator.ofInt(this, "scrollY", 0)
        animator.duration = duration
        animator.start()
    }
}