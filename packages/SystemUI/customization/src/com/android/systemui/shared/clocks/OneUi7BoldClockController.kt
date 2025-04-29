package com.android.systemui.shared.clocks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import com.android.systemui.R
import com.android.systemui.monet.SysuiColorExtractor

class OneUi7BoldClockController(private val context: Context) : ClockController {

    private val smallClockView: TwoToneClockView = LayoutInflater.from(context)
        .inflate(R.layout.clock_oneui7_bold_small, null) as TwoToneClockView

    private val largeClockView: TwoToneClockView = LayoutInflater.from(context)
        .inflate(R.layout.clock_oneui7_bold_large, null) as TwoToneClockView

    private val colorExtractor = SysuiColorExtractor.getInstance(context)
    private var isAod = false

    override fun getName(): String = "oneui7_bold"
    override fun getTitle(): String = "One UI 7 Bold"
    override fun getThumbnail(): Int = R.drawable.ic_clock_oneui7_bold
    override fun getSmallClock(): View = smallClockView
    override fun getLargeClock(): View = largeClockView
    override fun getAnimations(): List<AnimatableClockView> = emptyList()

    fun onAodStateChanged(aod: Boolean) {
        isAod = aod
        updateColors()
        animateClock()
    }

    private fun updateColors() {
        val (top, bottom) = if (isAod) {
            Pair(0xFFFFFFFF.toInt(), 0xFFB3B3B3.toInt())
        } else {
            val colors = colorExtractor.colors
            Pair(
                colors.accent1!!.get(500),
                colors.accent1!!.get(700)
            )
        }
        smallClockView.setGradientColors(top, bottom)
        largeClockView.setGradientColors(top, bottom)
    }

    private fun animateClock() {
        val scale = if (isAod) 1.0f else 1.05f
        val alpha = if (isAod) 0.85f else 1.0f

        listOf(smallClockView, largeClockView).forEach {
            it.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(550)
                .setInterpolator(OvershootInterpolator(1.0f))
                .start()
        }
    }
}