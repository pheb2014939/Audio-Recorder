package com.pro.audiotrimmer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class SlidingWindowView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var paint = Paint()
    var startX = 0f
    var endX = 0f
    private var isDraggingStart = false
    private var isDraggingEnd = false

    init {
        paint.color = Color.BLUE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.argb(50, 0, 0, 255)
        canvas.drawRect(startX, 0f, endX, height.toFloat(), paint)
        paint.color = Color.BLUE
        canvas.drawRect(startX - 10, 0f, startX + 10, height.toFloat(), paint)
        canvas.drawRect(endX - 10, 0f, endX + 10, height.toFloat(), paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x in startX - 10..startX + 10) {
                    isDraggingStart = true
                } else if (event.x in endX - 10..endX + 10) {
                    isDraggingEnd = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingStart) {
                    startX = event.x
                } else if (isDraggingEnd) {
                    endX = event.x
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isDraggingStart = false
                isDraggingEnd = false
            }
        }
        return true
    }
}



//class SlidingWindowView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private val paint = Paint().apply {
//        color = Color.RED
//        style = Paint.Style.STROKE
//        strokeWidth = 5f
//    }
//
//    private var startX = 0f
//    private var endX = 0f
//    private var isDraggingStart = false
//    private var isDraggingEnd = false
//
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        canvas.drawRect(startX, 0f, endX, height.toFloat(), paint)
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                if (Math.abs(event.x - startX) < 50) {
//                    isDraggingStart = true
//                } else if (Math.abs(event.x - endX) < 50) {
//                    isDraggingEnd = true
//                }
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (isDraggingStart) {
//                    startX = event.x
//                    invalidate()
//                } else if (isDraggingEnd) {
//                    endX = event.x
//                    invalidate()
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//                isDraggingStart = false
//                isDraggingEnd = false
//            }
//        }
//        return true
//    }
//}