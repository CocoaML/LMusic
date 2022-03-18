package com.lalilu.lmusic.ui.seekbar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.IntDef
import androidx.core.view.GestureDetectorCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.blankj.utilcode.util.SizeUtils
import com.lalilu.lmusic.utils.StatusBarUtil
import com.lalilu.lmusic.utils.TextUtils
import kotlin.math.abs
import kotlin.math.roundToInt

const val CLICK_PART_UNSPECIFIED = 0
const val CLICK_PART_LEFT = 1
const val CLICK_PART_MIDDLE = 2
const val CLICK_PART_RIGHT = 3

@IntDef(
    CLICK_PART_UNSPECIFIED,
    CLICK_PART_LEFT,
    CLICK_PART_MIDDLE,
    CLICK_PART_RIGHT
)
@Retention(AnnotationRetention.SOURCE)
annotation class ClickPart

interface OnSeekBarScrollListener {
    fun onScroll(scrollValue: Float)
}

interface OnSeekBarCancelListener {
    fun onCancel()
}

interface OnSeekBarSeekToListener {
    fun onSeekTo(value: Float)
}

interface OnSeekBarClickListener {
    fun onClick(
        @ClickPart clickPart: Int = CLICK_PART_UNSPECIFIED,
        action: Int
    )

    fun onLongClick(
        @ClickPart clickPart: Int = CLICK_PART_UNSPECIFIED,
        action: Int
    )

    fun onDoubleClick(
        @ClickPart clickPart: Int = CLICK_PART_UNSPECIFIED,
        action: Int
    )
}

abstract class OnSeekBarScrollToThresholdListener(
    var threshold: Float
) : OnSeekBarScrollListener {
    private val helper = ThresholdHelper { it >= threshold }

    abstract fun onScrollToThreshold()
    open fun onScrollRecover() {}

    override fun onScroll(scrollValue: Float) {
        helper.check(
            scrollValue,
            this::onScrollToThreshold,
            this::onScrollRecover
        )
    }
}

class NewSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : NewProgressBar(context, attrs) {
    var cancelThreshold = 100f
        set(value) {
            field = value
            cancelScrollListener.threshold = value
        }

    val scrollListeners = ArrayList<OnSeekBarScrollListener>()
    val clickListeners = ArrayList<OnSeekBarClickListener>()
    val cancelListeners = ArrayList<OnSeekBarCancelListener>()
    val seekToListeners = ArrayList<OnSeekBarSeekToListener>()
    var valueToText: ((Float) -> String)? = null

    private var canceled = true
    private var touching = false
    private var previousLeft = -1
    private var previousRight = -1
    private var nextLeft = -1
    private var nextRight = -1

    var startValue: Float = nowValue
    var dataValue: Float = nowValue
    var sensitivity = 1.3f

    private val cancelScrollListener =
        object : OnSeekBarScrollToThresholdListener(cancelThreshold) {
            override fun onScrollToThreshold() {
                animateValueTo(dataValue)
                cancelListeners.forEach { it.onCancel() }
                canceled = true
            }

            override fun onScrollRecover() {
                canceled = false
            }
        }


    private val mProgressAnimation: SpringAnimation by lazy {
        SpringAnimation(this, ProgressFloatProperty(), nowValue).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val mPaddingAnimation: SpringAnimation by lazy {
        SpringAnimation(this, PaddingFloatProperty(), padding).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val mOutSideAlphaAnimation: SpringAnimation by lazy {
        SpringAnimation(this, OutSideAlphaFloatProperty(), outSideAlpha.toFloat()).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    override fun valueToText(value: Float): String {
        return valueToText?.invoke(value) ?: TextUtils.durationToString(value)
    }

    override fun isDarkModeNow(): Boolean {
        return StatusBarUtil.isDarkMode(context)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        previousLeft = left
        previousRight = left + width * 2 / 5
        nextLeft = left + width * 3 / 5
        nextRight = left + width
    }

    /**
     * 判断触摸事件所点击的部分位置
     */
    fun checkClickPart(e: MotionEvent): Int {
        return when (e.rawX.toInt()) {
            in previousLeft..previousRight -> CLICK_PART_LEFT
            in previousRight..nextLeft -> CLICK_PART_MIDDLE
            in nextLeft..nextRight -> CLICK_PART_RIGHT
            else -> CLICK_PART_UNSPECIFIED
        }
    }

    private val gestureDetector = GestureDetectorCompat(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                touching = true
                canceled = false
                startValue = nowValue
                animateScaleTo(SizeUtils.dp2px(3f).toFloat())
                animateOutSideAlphaTo(255f)
                return super.onDown(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                clickListeners.forEach {
                    it.onClick(checkClickPart(e), e.action)
                }
                performClick()
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                clickListeners.forEach {
                    it.onDoubleClick(checkClickPart(e), e.action)
                }
                return super.onDoubleTap(e)
            }

            override fun onLongPress(e: MotionEvent) {
                clickListeners.forEach {
                    it.onLongClick(checkClickPart(e), e.action)
                }
                animateValueTo(startValue)
                canceled = true
            }

            override fun onScroll(
                downEvent: MotionEvent,
                moveEvent: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                updateValueByDelta(-distanceX)
                scrollListeners.forEach {
                    it.onScroll((top - moveEvent.rawY).coerceAtLeast(0f))
                }
                parent.requestDisallowInterceptTouchEvent(true)
                return super.onScroll(downEvent, moveEvent, distanceX, distanceY)
            }
        })

    fun updateValueByDelta(delta: Float) {
        if (touching && !canceled) {
            mProgressAnimation.cancel()
            nowValue = (nowValue + delta / width * maxValue * sensitivity)
                .coerceIn(minValue, maxValue)
        }
    }

    fun updateValue(value: Float) {
        if (value !in minValue..maxValue) return

        if (!touching || canceled) {
            animateValueTo(value)
        }
        dataValue = value
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!canceled && abs(nowValue - startValue) > 500) {
                    seekToListeners.forEach { it.onSeekTo(nowValue) }
                }
                animateScaleTo(0f)
                animateOutSideAlphaTo(0f)
                touching = false
                canceled = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    fun animateOutSideAlphaTo(value: Float) {
        mOutSideAlphaAnimation.cancel()
        mOutSideAlphaAnimation.animateToFinalPosition(value)
    }

    fun animateScaleTo(value: Float) {
        mPaddingAnimation.cancel()
        mPaddingAnimation.animateToFinalPosition(value)
    }

    fun animateValueTo(value: Float) {
        mProgressAnimation.cancel()
        mProgressAnimation.animateToFinalPosition(value)
    }

    class ProgressFloatProperty :
        FloatPropertyCompat<NewProgressBar>("progress") {
        override fun getValue(obj: NewProgressBar): Float = obj.nowValue
        override fun setValue(obj: NewProgressBar, value: Float) {
            obj.nowValue = value
        }
    }

    class PaddingFloatProperty :
        FloatPropertyCompat<NewProgressBar>("padding") {
        override fun getValue(obj: NewProgressBar): Float = obj.padding
        override fun setValue(obj: NewProgressBar, value: Float) {
            obj.padding = value
            obj.outSideAlpha = (value * 50f).toInt()
        }
    }

    class OutSideAlphaFloatProperty :
        FloatPropertyCompat<NewProgressBar>("outside_alpha") {
        override fun getValue(obj: NewProgressBar): Float = obj.outSideAlpha.toFloat()
        override fun setValue(obj: NewProgressBar, value: Float) {
            obj.outSideAlpha = value.roundToInt()
        }
    }

    init {
        scrollListeners.add(cancelScrollListener)
    }
}