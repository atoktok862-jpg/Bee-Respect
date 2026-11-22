package com.beerespect.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class LoadingOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onRetry: (() -> Unit)? = null
    var statusText: String = "Готовим улей и 3D-поляну..."
        set(value) { field = value; invalidate() }
    var showRetry: Boolean = false
        set(value) { field = value; invalidate() }

    private val bgTop = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgBottom = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(10f, 0f, 4f, Color.parseColor("#88440000"))
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF7D6"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val barBack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#55FFFFFF") }
    private val barFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFD24A") }
    private val honeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#44FFD24A") }
    private val beePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFC629") }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 34f }
    private var retryRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val sky = LinearGradient(0f, 0f, 0f, h, intArrayOf(Color.parseColor("#FFF3B14A"), Color.parseColor("#FFFFD978"), Color.parseColor("#FF8BD35A"), Color.parseColor("#FF4FAF3A")), floatArrayOf(0f, 0.45f, 0.75f, 1f), Shader.TileMode.CLAMP)
        bgTop.shader = sky
        canvas.drawRect(0f, 0f, w, h, bgTop)

        val cell = min(w, h) * 0.08f
        honeyPaint.strokeWidth = maxOf(2f, cell * 0.05f)
        for (y in -1..(h / (cell * 0.86f)).toInt() + 1) {
            for (x in -1..(w / (cell * 1.5f)).toInt() + 1) {
                val cx = x * cell * 1.5f + if (y % 2 == 0) 0f else cell * 0.75f
                val cy = y * cell * 0.86f
                drawHex(canvas, cx, cy, cell * 0.48f)
            }
        }

        titlePaint.textSize = min(w, h) * 0.085f
        canvas.drawText("КТО ПЧЁЛОК УВАЖАЕТ", w / 2f, h * 0.34f, titlePaint)
        subPaint.textSize = min(w, h) * 0.035f
        canvas.drawText(statusText, w / 2f, h * 0.46f, subPaint)

        val barW = w * 0.52f; val barH = min(w, h) * 0.045f
        val barL = (w - barW) / 2f; val barT = h * 0.56f
        canvas.drawRoundRect(barL, barT, barL + barW, barT + barH, barH / 2f, barH / 2f, barBack)
        val t = (System.currentTimeMillis() % 1800L) / 1800f
        val fillW = barW * (0.15f + 0.75f * ((sin(t * Math.PI * 2).toFloat() + 1f) / 2f))
        canvas.drawRoundRect(barL, barT, barL + fillW, barT + barH, barH / 2f, barH / 2f, barFill)

        val beeX = barL + fillW
        val beeY = barT - barH * 0.9f
        beePaint.color = Color.parseColor("#FFFFC629")
        canvas.drawCircle(beeX, beeY, barH * 0.55f, beePaint)
        beePaint.color = Color.parseColor("#FF3A2200")
        canvas.drawRect(beeX - barH * 0.35f, beeY - barH * 0.15f, beeX + barH * 0.35f, beeY + barH * 0.15f, beePaint)

        if (showRetry) {
            val bw = w * 0.34f; val bh = min(w, h) * 0.085f
            retryRect.set((w - bw) / 2f, h * 0.68f, (w + bw) / 2f, h * 0.68f + bh)
            buttonPaint.style = Paint.Style.FILL; buttonPaint.color = Color.parseColor("#FFFFC629")
            canvas.drawRoundRect(retryRect, 22f, 22f, buttonPaint)
            buttonPaint.style = Paint.Style.STROKE; buttonPaint.strokeWidth = 5f; buttonPaint.color = Color.parseColor("#FF8A4A00")
            canvas.drawRoundRect(retryRect, 22f, 22f, buttonPaint)
            canvas.drawText("Попробовать снова", retryRect.centerX(), retryRect.centerY() + 12f, buttonText)
        }
        if (visibility == VISIBLE) postInvalidateOnAnimation()
    }

    private fun drawHex(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path()
        for (i in 0 until 6) {
            val a = Math.toRadians((60 * i - 30).toDouble())
            val x = cx + cos(a).toFloat() * r
            val y = cy + sin(a).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, honeyPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (showRetry && event.actionMasked == MotionEvent.ACTION_UP && retryRect.contains(event.x, event.y)) {
            onRetry?.invoke(); return true
        }
        return showRetry
    }
}
