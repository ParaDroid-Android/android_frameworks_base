import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.palette.graphics.Palette
import kotlin.math.coerceIn

class WaveformSeekBar @JvmOverloads constructor( context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0 ) : View(context, attrs, defStyleAttr) {

private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.LTGRAY
    strokeWidth = dpToPx(2f)
    strokeCap = Paint.Cap.ROUND
}

private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = resolveAccentColor()
    strokeWidth = dpToPx(2.5f)
    strokeCap = Paint.Cap.ROUND
    setShadowLayer(8f, 0f, 0f, resolveAccentColor())
}

private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = resolveAccentColor()
    style = Paint.Style.FILL
}

private var waveform = FloatArray(100) { i -> ((Math.sin(i * 0.3) * 0.5 + 0.5).toFloat()) }
private var progress: Float = 0f
private var seekListener: ((Float) -> Unit)? = null

private var visualizer: Visualizer? = null
private var waveOffset = 0f
private var animator: ValueAnimator? = null

init {
    setLayerType(LAYER_TYPE_SOFTWARE, null)
    setupRipple()
}

private fun setupRipple() {
    val highlight = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorControlHighlight, highlight, true)
    val rippleColor = ColorStateList.valueOf(highlight.data)

    val mask = ShapeDrawable(RectShape()).apply {
        paint.color = Color.WHITE
    }

    background = RippleDrawable(rippleColor, null, mask)
    isClickable = true
    isFocusable = true
}

fun animateIn() {
    alpha = 0f
    visibility = VISIBLE
    animate().alpha(1f).setDuration(300).start()
}

fun animateOut() {
    animate().alpha(0f).setDuration(300).withEndAction {
        visibility = GONE
    }.start()
}

fun setProgress(progress: Float) {
    this.progress = progress.coerceIn(0f, 1f)
    invalidate()
}

fun setOnSeekListener(listener: (Float) -> Unit) {
    seekListener = listener
}

fun attachToSession(sessionId: Int) {
    visualizer = Visualizer(sessionId).apply {
        captureSize = Visualizer.getCaptureSizeRange()[1]
        setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {
                data?.let {
                    for (i in waveform.indices) {
                        waveform[i] = (it[i % it.size].toInt() and 0xFF) / 255f
                    }
                    invalidate()
                }
            }
            override fun onFftDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {}
        }, Visualizer.getMaxCaptureRate() / 2, true, false)
        enabled = true
    }
}

fun releaseVisualizer() {
    visualizer?.release()
    visualizer = null
}

fun setWaveColorFromBitmap(bitmap: Bitmap) {
    Palette.from(bitmap).generate { palette ->
        val color = palette?.getVibrantColor(Color.WHITE) ?: Color.WHITE
        playedPaint.color = color
        playedPaint.setShadowLayer(8f, 0f, 0f, color)
        thumbPaint.color = color
        invalidate()
    }
}

private fun startAnimating() {
    animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            waveOffset = it.animatedFraction
            invalidate()
        }
        start()
    }
}

private fun stopAnimating() {
    animator?.cancel()
}

override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    startAnimating()
}

override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopAnimating()
    releaseVisualizer()
}

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val width = width.toFloat()
    val height = height.toFloat()
    val centerY = height / 2f
    val itemWidth = width / waveform.size

    for (i in waveform.indices) {
        val x = ((i + waveOffset * waveform.size) % waveform.size) * itemWidth
        val barHeight = waveform[i] * height * 0.5f
        val paint = if (x / width <= progress) playedPaint else wavePaint
        canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint)
    }

    // Draw scrub thumb
    val thumbX = progress * width
    val thumbY = centerY
    canvas.drawCircle(thumbX, thumbY, dpToPx(6f), thumbPaint)
}

override fun onTouchEvent(event: MotionEvent): Boolean {
    val newProgress = (event.x / width).coerceIn(0f, 1f)
    when (event.action) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
            setProgress(newProgress)
            return true
        }
        MotionEvent.ACTION_UP -> {
            seekListener?.invoke(newProgress)
            return true
        }
    }
    return super.onTouchEvent(event)
}

private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

private fun resolveAccentColor(): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
    return typedValue.data
}

}

