package com.lalilu.ui.appbar

import android.widget.OverScroller
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import kotlin.math.abs
import kotlin.math.roundToInt

open class AppbarOffsetHelper(
    protected val appbar: CoverAppbar,
) : AppbarProgressHelper() {
    private val scroller: OverScroller by lazy { OverScroller(appbar.context) }
    private val animator: SpringAnimation by lazy {
        springAnimationOf(
            setter = { updatePosition(it.toInt()) },
            getter = { position.toFloat() },
            finalPosition = 0f
        ).withSpringForceProperties {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }.apply {
            addEndListener { animation, canceled, value, velocity ->
                if (!canceled) {
                    // 任何动画结束后都根据当前位置更新一次进度，并且标记为非用户操作更新
                    actionFromUser = false
                    updateProgressByPosition(minPosition, middlePosition, maxPosition, position)
                }
            }
        }
    }
    private val anchors = listOf(::minPosition::get, ::maxPosition::get, { 0 })

    val minPosition: Int
        get() = appbar.minAnchorHeight
    val maxPosition: Int
        get() = appbar.maxAnchorHeight
    val middlePosition: Int
        get() = appbar.middleAnchorHeight

    /**
     * 实际控制元素位置的变量
     * [(minHeight - middleHeight) ~ 0 ~ (maxHeight - middleHeight)]
     */
    var position = INVALID_POSITION


    /**
     * 根据当前从Appbar获取到的数据判断，当前页面状态是否正常可进行操作
     */
    fun isValueValidNow(): Boolean {
        return !(minPosition >= middlePosition || minPosition >= maxPosition || middlePosition >= maxPosition)
    }

    /**
     * 更新当前位置
     *
     * @return 当前位置是否发生变更
     */
    protected open fun updatePosition(value: Number) {
        if (position == value) return
        position = value.toInt().coerceIn(minPosition, maxPosition)
        updateProgressByPosition(minPosition, middlePosition, maxPosition, position)
        updateOffset(position)
    }

    open fun onViewLayout() {
        // 当预想位置和当前位置不一致时进行修正
        if (appbar.bottom != position) {
            updateOffset(position)
        }
    }

    open fun updateOffset(target: Int) {
//        ViewCompat.offsetTopAndBottom(appbar, target.coerceAtMost(0) - appbar.top)
        appbar.bottom = target
    }

    open fun animateTo(position: Number) {
        animator.animateToFinalPosition(position.toFloat())
    }

    open fun cancelAnimation() {
        if (animator.isRunning) {
            animator.cancel()
        }
    }

    open fun snapIfNeeded(fromUser: Boolean) {
        if (!anchors.any { it() == position }) {
            actionFromUser = fromUser
            snapBy(position, fromUser)
        }
    }

    open fun snapBy(position: Int, fromUser: Boolean) {
        val target = calcSnapToOffset(position, anchors.map { it() })
        actionFromUser = fromUser
        animateTo(target)
    }

    open fun calcSnapToOffset(value: Int, vararg anchors: Int): Int {
        return calcSnapToOffset(value, anchors.asIterable())
    }

    open fun calcSnapToOffset(value: Int, anchors: Iterable<Int>): Int {
        var min = Int.MAX_VALUE
        var target = value
        for (anchor in anchors) {
            val temp = abs(value - anchor)
            if (temp < min) {
                min = temp
                target = anchor
            }
        }
        return target
    }

    open fun scrollBy(dy: Int): Int {
        val nowPosition = position
        actionFromUser = true
        updatePosition(position - dy)
        return nowPosition - position
    }

    open fun fling(velocityY: Float) {
        scroller.fling(
            0, position,            // startX / Y
            0, velocityY.roundToInt(),  // velocityX / Y
            0, 0,                    // minX / maxX
            minPosition, maxPosition              // minY / maxY
        )
        snapBy(scroller.finalY, true)
    }
}