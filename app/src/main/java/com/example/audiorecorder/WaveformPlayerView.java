package com.example.audiorecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class WaveformPlayerView extends View {
    private byte[] mBytes;      // Waveform data in bytes
    private float[] mPoints;    // Points array for drawing lines
    private Paint mForePaint;   // Paint object for drawing lines
    private Rect mRect;         // Rect object for defining the view's bounds

    public WaveformPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mBytes = null;
        mPoints = null;
        mRect = new Rect();

        mForePaint = new Paint();
        mForePaint.setStrokeWidth(10f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.GREEN);
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate(); // Request a redraw
    }

    public void addAmplitude(float amplitude) {
        // Update waveform based on the amplitude passed in
        if (mBytes == null || mBytes.length < 1) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        // Calculate points for drawing lines
        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (float)(mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2 + ((mBytes[i] + 128) * (mRect.height() / 2) / 128f);
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (float)(mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2 + ((mBytes[i + 1] + 128) * (mRect.height() / 2) / 128f);
        }

        invalidate(); // Request a redraw to display the updated waveform
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null || mPoints == null) {
            return;
        }

        canvas.drawLines(mPoints, mForePaint); // Draw the waveform
    }
}
