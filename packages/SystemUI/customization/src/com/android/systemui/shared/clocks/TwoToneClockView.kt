package com.android.systemui.shared.clocks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils

class TwoToneClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var topColor: Int = Color.WHITE
    private var bottomColor: Int = Color.LTGRAY
    private var animator: ValueAnimator? = null

    fun setGradientColors(newTop: Int, newBottom: Int) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener {
                val f = it.animatedFraction
                topColor = ColorUtils.blendARGB(topColor, newTop, f)
                bottomColor = ColorUtils.blendARGB(bottomColor, newBottom, f)
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            topColor,
            bottomColor,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        super.onDraw(canvas)
        paint.shader = null
    }
}