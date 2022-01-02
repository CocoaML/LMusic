package com.lalilu.lmusic.ui.appbar

import androidx.dynamicanimation.animation.DynamicAnimation
import com.google.android.material.appbar.AppBarLayout

/**
 * 监听AppBar的滚动和存储绘制部分的关键数据的全局单例
 */
object AppBarStatusHelper : OnPercentChangeListener(), DynamicAnimation.OnAnimationEndListener {
    private lateinit var appbar: AppBarLayout
    private lateinit var listenPercent: (percent: Float) -> Unit
    private var lastStatus = AppBarStatus.STATUS_NORMAL

    var normalHeight = -1
    var maxExpandHeight = -1
    var maxDragHeight = 200
    var fullyExpend = false
    var mBottom = -1

    /**
     * 初始化
     */
    fun initial(appbar: AppBarLayout, listenPercent: (percent: Float) -> Unit): AppBarStatusHelper {
        if (normalHeight <= 0) normalHeight = appbar.height
        if (mBottom <= 0 || !fullyExpend) mBottom = normalHeight

        this.appbar = appbar
        this.appbar.removeOnOffsetChangedListener(this)
        this.appbar.addOnOffsetChangedListener(this)
        this.listenPercent = listenPercent

        return this
    }

    /**
     * 根据下一个位置获取状态
     */
    fun getNextStatusByNextPosition(nextPosition: Int): AppBarStatus {
        lastStatus = when {
            (nextPosition > appbar.height) -> AppBarStatus.STATUS_EXPENDED
            (appbar.y + appbar.totalScrollRange <= 0 || verticalOffset < 0) -> AppBarStatus.STATUS_NORMAL
            else -> lastStatus
        }
        return lastStatus
    }

    override fun onPercentChanged(percent: Float) {
        listenPercent.invoke(percent)
    }

    override fun onAnimationEnd(
        animation: DynamicAnimation<*>?,
        canceled: Boolean,
        value: Float,
        velocity: Float
    ) {

    }
}