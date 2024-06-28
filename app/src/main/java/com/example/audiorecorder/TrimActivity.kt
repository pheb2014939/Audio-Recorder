package com.example.audiorecorder

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.audiorecorder.R
import com.pro.audiotrimmer.SlidingWindowView
import com.pro.audiotrimmer.WaveformSeekBar
class TrimActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var waveformView: WaveformSeekBar
    private lateinit var slidingWindowView: SlidingWindowView

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var delay = 1000L
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim)

        waveformView = findViewById(R.id.waveFormView)
        slidingWindowView = findViewById(R.id.slidingWindowView)
        playButton = findViewById(R.id.playButton)
        saveButton = findViewById(R.id.saveButton)

        val filePath = intent.getStringExtra("filepath")

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            val progress = mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration
            waveformView.updateProgress(progress)
            handler.postDelayed(runnable, delay)
        }

        playButton.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }

        mediaPlayer.setOnCompletionListener {
            stopAudio()
        }
    }

    private fun playAudio() {
        mediaPlayer.start()
        playButton.setImageResource(R.drawable.ic_pause) // Update with your pause icon resource
        handler.postDelayed(runnable, delay)
        isPlaying = true
    }

    private fun pauseAudio() {
        mediaPlayer.pause()
        playButton.setImageResource(R.drawable.ic_round_play) // Update with your play icon resource
        handler.removeCallbacks(runnable)
        isPlaying = false
    }

    private fun stopAudio() {
        mediaPlayer.pause()
        mediaPlayer.seekTo(0)
        playButton.setImageResource(R.drawable.ic_round_play) // Update with your play icon resource
        handler.removeCallbacks(runnable)
        waveformView.updateProgress(0f)
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }
}