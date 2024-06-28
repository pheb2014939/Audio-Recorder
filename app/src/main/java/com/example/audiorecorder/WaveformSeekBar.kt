package com.pro.audiotrimmer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
class WaveformSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 5f
    }
    private val progressPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
    }
    private var progress = 0f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2

        // Draw waveform (this is a placeholder, replace with actual waveform data)
        var x = 0f
        while (x < width) {
            val waveHeight = (Math.random() * height).toFloat()
            canvas.drawLine(x, centerY - waveHeight / 2, x, centerY + waveHeight / 2, wavePaint)
            x += 7f // wave width + wave gap
        }

        // Draw progress
        val progressWidth = width * progress
        x = 0f
        while (x < progressWidth) {
            val waveHeight = (Math.random() * height).toFloat()
            canvas.drawLine(x, centerY - waveHeight / 2, x, centerY + waveHeight / 2, progressPaint)
            x += 7f // wave width + wave gap
        }
    }

    fun updateProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }
}
//class WaveformSeekBar @JvmOverloads constructor(
//
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private val wavePaint = Paint().apply {
//        color = Color.LTGRAY
//        strokeWidth = 5f
//    }
//    private val progressPaint = Paint().apply {
//        color = Color.BLUE
//        strokeWidth = 5f
//    }
//    private var progress = 0f
//
//    init {
//        // Initialize custom attributes if any
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        val width = width.toFloat()
//        val height = height.toFloat()
//        val centerY = height / 2
//
//        // Draw waveform (this is a placeholder, replace with actual waveform data)
//        var x = 0f
//        while (x < width) {
//            val waveHeight = (Math.random() * height).toFloat()
//            canvas.drawLine(x, centerY - waveHeight / 2, x, centerY + waveHeight / 2, wavePaint)
//            x += 7f // wave width + wave gap
//        }
//
//        // Draw progress
//        val progressWidth = width * progress
//        x = 0f
//        while (x < progressWidth) {
//            val waveHeight = (Math.random() * height).toFloat()
//            canvas.drawLine(x, centerY - waveHeight / 2, x, centerY + waveHeight / 2, progressPaint)
//            x += 7f // wave width + wave gap
//        }
//    }
//
//    fun updateProgress(progress: Float) {
//        this.progress = progress
//        invalidate()
//    }
//}
