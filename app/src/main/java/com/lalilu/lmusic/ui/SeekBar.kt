package com.lalilu.lmusic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.lalilu.R
import com.lalilu.common.TextUtils
import java.util.*
import kotlin.concurrent.schedule

fun PlaybackStateCompat.getPositionByNow(): Long {
    return this.position + (this.playbackSpeed * (SystemClock.elapsedRealtime() - this.lastPositionUpdateTime)).toLong()
}

class SeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var positionTimer: Timer? = null

    fun updateNowPosition(playbackStateCompat: PlaybackStateCompat) {
        var currentDuration = playbackStateCompat.getPositionByNow()
        positionTimer?.cancel()
        if (playbackStateCompat.state == PlaybackStateCompat.STATE_PLAYING) {
            positionTimer = Timer()
            positionTimer?.schedule(0, 100) {
                updateDuration(currentDuration)
                currentDuration += 100
                if (currentDuration >= sumDuration) this.cancel()
            }
        } else {
            updateDuration(currentDuration)
        }
    }

    private var downDuration: Long = -1L

    private var sumDuration: Long = 0L
    private var nowDuration: Long = 0L

    private var textPadding: Long = 40L
    private var textHeight: Float = 45f

    private var paint: Paint
    private var textPaint: TextPaint
    private var textPaintWhite: TextPaint
    private var backgroundPaint: Paint
    private var touching: Boolean = false

    private var radius = 30f
    private var scaleAnimatorTo = 1.1f
    private var scaleAnimatorDuration = 200L

    private var rawX: Float = -1f
    private var rawY: Float = -1f
    private var deltaX: Float = 0f
    private var deltaY: Float = 0f
    private var progress: Double = 0.0
    private var maxProgress: Float = 1f
    private var minProgress: Float = 0f
    private var sensitivity: Float = 0.0015f

    lateinit var onActionUp: (selectDuration: Long) -> Unit

    fun setSumDuration(duration: Long) {
        sumDuration = duration
        invalidate()
    }

    private fun updateDuration(duration: Long) {
        if (!touching) {
            nowDuration = duration
            progress = nowDuration / sumDuration.toDouble()
            invalidate()
        }
    }

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.SeekBar, defStyleAttr, 0
        )
        a.recycle()

        paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.DKGRAY
        }
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.argb(50, 100, 100, 100)
        }
        textPaint = TextPaint().also {
            it.textSize = textHeight
            it.color = Color.BLACK
            it.isSubpixelText = true
        }
        textPaintWhite = TextPaint().also {
            it.textSize = textHeight
            it.color = Color.WHITE
            it.isSubpixelText = true
        }
    }

    fun setThumbColor(color: Int) {
        paint.color = color
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                rawX = event.rawX
                rawY = event.rawY
                downDuration = nowDuration
                this.animate()
                    .scaleX(scaleAnimatorTo)
                    .scaleY(scaleAnimatorTo)
                    .setDuration(scaleAnimatorDuration)
                    .start()
            }
            MotionEvent.ACTION_UP -> {
                positionTimer?.cancel()
                rawX = -1f
                rawY = -1f
                if (downDuration != nowDuration) {
                    onActionUp(nowDuration)
                } else {
                    performClick()
                }
                this.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(scaleAnimatorDuration)
                    .start()
                touching = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (touching) {
                    deltaX = (event.rawX - rawX)
                    deltaY = (event.rawY - rawY)
                    rawX = event.rawX
                    progress = clamp(
                        progress + deltaX * sensitivity,
                        maxProgress, minProgress
                    ).toDouble()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                touching = false
            }
        }
        invalidate()
        return true
    }

    private var progressWidth: Float = 0F
    private var sumDurationText = ""
    private var sumDurationTextWidth: Float = 0F
    private var nowDurationText = ""
    private var nowDurationTextWidth: Float = 0F
    private var nowDurationTextOffset: Float = 0F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        progressWidth = (progress * width).toFloat()
        nowDuration = (progress * sumDuration).toLong()

        sumDurationText = TextUtils.durationToString(sumDuration)
        sumDurationTextWidth = textPaint.measureText(sumDurationText)
        nowDurationText = TextUtils.durationToString(nowDuration)
        nowDurationTextWidth = textPaintWhite.measureText(nowDurationText)

        val textCenterHeight = (height + textPaint.textSize) / 2f - 5
        val offsetTemp = nowDurationTextWidth + textPadding * 2
        nowDurationTextOffset = if (offsetTemp < progressWidth) progressWidth else offsetTemp

        paint.alpha = clamp(progressWidth / radius / 2 * 255, 255, 0).toInt()

        // draw background
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            radius,
            radius,
            backgroundPaint
        )

        // draw sumDuration
        canvas.drawText(
            sumDurationText,
            width - sumDurationTextWidth - textPadding,
            textCenterHeight,
            textPaint
        )

        // draw thumb
        canvas.drawRoundRect(
            0f, 0f, progressWidth, height.toFloat(), radius, radius, paint
        )

        // draw nowDuration
        canvas.drawText(
            nowDurationText, nowDurationTextOffset - nowDurationTextWidth - textPadding,
            textCenterHeight,
            textPaintWhite
        )
    }

    private fun clamp(num: Number, max: Number, min: Number): Number {
        if (num.toDouble() < min.toDouble()) return min.toDouble()
        if (num.toDouble() > max.toDouble()) return max.toDouble()
        return num.toDouble()
    }
}