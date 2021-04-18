package com.streetvoice.streetvoice.view.widget

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class NestedScrollableRecyclerView : NestedScrollView {
    private var mTopView: View? = null
    private var mBottomView: View? = null
    private var mTopOffset: Int = 0
    private var mBottomOffset: Int = 0
    private var scrollAmount: Int = 0
    private var scrollState: ScrollingState = ScrollingState.BEFORE_TOP_OFFSET

    private val velocityThreshold: Int = 300
    private val slowDownRatio: Float = 0.5F

    private lateinit var mRecyclerView: RecyclerView

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        val recyclerView = target as RecyclerView

        // prevent scrolling too fast
        val finalAmount: Int = if (abs(dy) > velocityThreshold) {
            (slowDownRatio * dy).toInt()
        } else {
            dy
        }
        scrollAmount += finalAmount

        // process scrolling with states
        when (scrollState) {

            ScrollingState.BEFORE_TOP_OFFSET -> {
                if (!canScrollVertically(-1) && !recyclerView.canScrollVertically(-1)) {
                    scrollAmount = 0
                }

                if (scrollAmount > mTopOffset) {
                    scrollState = ScrollingState.TOP_BOTTOM_INTERNAL
                }

                scrollBy(0, finalAmount)
                consumed[1] = finalAmount
                return
            }

            ScrollingState.TOP_BOTTOM_INTERNAL -> {
                if (!recyclerView.canScrollVertically(1)) {
                    scrollState = ScrollingState.AFTER_BOTTOM
                    scrollAmount = -abs(mBottomOffset)
                } else if (!recyclerView.canScrollVertically(-1)) {
                    scrollState = ScrollingState.BEFORE_TOP_OFFSET
                    scrollAmount = mTopOffset
                }

                super.onNestedPreScroll(target, dx, finalAmount, consumed, type)
            }

            ScrollingState.AFTER_BOTTOM -> {
                if (!canScrollVertically(1) && !recyclerView.canScrollVertically(1)) {
                    scrollAmount = 0
                }

                if (scrollAmount < -abs(mBottomOffset)) {
                    scrollState = ScrollingState.TOP_BOTTOM_INTERNAL
                }

                scrollBy(0, finalAmount)
                consumed[1] = finalAmount
                return
            }
        }
    }

    fun initView(topView: View?, bottomView: View?, recyclerView: RecyclerView) {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL

        mTopView = topView
        mBottomView = bottomView
        mRecyclerView = recyclerView

        mTopView?.let {
            linearLayout.addView(it)
        }

        linearLayout.addView(mRecyclerView)

        mBottomView?.let {
            linearLayout.addView(it)
        }

        this.addView(linearLayout)

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                adjustView()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun adjustView() {
        val displayPoint = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(displayPoint)
        val recyclerViewHeight = mRecyclerView.height

        if (recyclerViewHeight > displayPoint.y) {
            mRecyclerView.layoutParams.height = displayPoint.y
            mRecyclerView.requestLayout()
            mTopOffset = mTopView?.height ?: 0
            mBottomOffset = mBottomView?.height ?: 0
        } else {
            //this will make function onNestedPreScroll not be called
            mRecyclerView.isNestedScrollingEnabled = false
        }
    }

    private enum class ScrollingState {
        BEFORE_TOP_OFFSET,
        TOP_BOTTOM_INTERNAL,
        AFTER_BOTTOM
    }
}