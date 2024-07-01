package com.example.audiorecorder
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.audiorecorder.AppDatabase.Companion.getDatabase
import com.example.audiorecorder.ConnectionClass.username
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.slider.RangeSlider
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import java.text.NumberFormat
import org.florescu.android.rangeseekbar.RangeSeekBar
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

    class AudioPlayerActivity : AppCompatActivity() {
        private lateinit var statusRadioGroup: RadioGroup
        private lateinit var records: ArrayList<AudioRecord>
        private lateinit var rangeSeekBar: RangeSeekBar<Long>
        private lateinit var btnCutAudio: Button
        private lateinit var tvStart: TextView
        private lateinit var tvEnd: TextView
        private lateinit var toolbar: MaterialToolbar
        private lateinit var waveformPlayerView: WaveformPlayerView
        private lateinit var tvFilename: TextView
        private lateinit var tvTrackProgress: TextView
        private lateinit var tvTrackDuration: TextView
        private lateinit var btnBackward: ImageButton
        private lateinit var btnForward: ImageButton
        private lateinit var btnPlay: ImageButton
        private lateinit var speedChip: Chip
        private lateinit var seekBar: SeekBar
        private lateinit var mediaPlayer: MediaPlayer
        private var visualizer: Visualizer? = null
        private lateinit var handler: Handler
        private lateinit var runnable: Runnable
        private var playbackSpeed = 1.0f
        private val jumpValue = 10000 // 10 seconds
        private val delay = 100L // 100 milliseconds
        private val REQUEST_PERMISSION_CODE = 1001
        private var username: String? = null
        private var phonenumber: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_audio_player)
            val filePath = intent.getStringExtra("filepath")
            val fileName = intent.getStringExtra("filename")


            // Check if filePath is null
            if (filePath == null) {
                Toast.makeText(this, "File path is null", Toast.LENGTH_SHORT).show()
                finish() // Finish activity if filePath is null
                return
            }
            records = ArrayList()
            toolbar = findViewById(R.id.toolbar)
            waveformPlayerView = findViewById(R.id.waveformPlayerView)
            tvFilename = findViewById(R.id.tvFilename)
            tvTrackProgress = findViewById(R.id.tvTrackProgress)
            tvTrackDuration = findViewById(R.id.tvTrackDuration)
            rangeSeekBar = findViewById(R.id.rangeSeekBar)
            btnCutAudio = findViewById(R.id.btnCutAudio)
            tvStart = findViewById(R.id.tvStart)
            tvEnd = findViewById(R.id.tvEnd)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
            tvFilename.text = fileName
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            tvTrackDuration.text = dateFormat(mediaPlayer.duration)
            btnBackward = findViewById(R.id.btnBackward)
            btnForward = findViewById(R.id.btnForward)
            btnPlay = findViewById(R.id.btnPlay)
            speedChip = findViewById(R.id.chip)
            seekBar = findViewById(R.id.seekBar)
            btnForward.setOnClickListener {
                mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
                seekBar.progress += jumpValue
            }
            btnBackward.setOnClickListener {
                mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
                seekBar.progress -= jumpValue
            }
            speedChip.setOnClickListener {
                playbackSpeed = if (playbackSpeed != 2f) playbackSpeed + 0.5f else 0.5f
                mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                speedChip.text = "x $playbackSpeed"
            }
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if (p2)
                        mediaPlayer.seekTo(p1)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
            handler = Handler(Looper.getMainLooper())
            runnable = object : Runnable {
                override fun run() {
                    if (mediaPlayer.isPlaying) {
                        tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
                        seekBar.progress = mediaPlayer.currentPosition
                        val maxAmplitude = mediaPlayer.audioSessionId.toFloat()
                        waveformPlayerView.addAmplitude(maxAmplitude)
                        handler.postDelayed(this, 0)
                    }
                }
            }

            // Handle cut audio button click
            btnCutAudio.setOnClickListener {
                val startSeconds = rangeSeekBar.selectedMinValue.toInt() / 1000 // Convert to seconds
                val endSeconds = rangeSeekBar.selectedMaxValue.toInt() / 1000 // Convert to seconds
                cutAudio(startSeconds, endSeconds, filePath)
            }
            btnPlay.setOnClickListener {
                playPausePlayer()
            }
            playPausePlayer()
            seekBar.max = mediaPlayer.duration
            mediaPlayer.setOnCompletionListener {
                btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
                handler.removeCallbacks(runnable)
                visualizer?.enabled = false
            }
            setupVisualizer()
            // Setup range seek bar
            rangeSeekBar.setRangeValues(0L, mediaPlayer.duration.toLong())
            rangeSeekBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
                val minTime = minValue.toInt()
                val maxTime = maxValue.toInt()
                tvStart.text = dateFormat(minTime)
                tvEnd.text = dateFormat(maxTime)
            }
        }

        private fun setupVisualizer() {
            visualizer = Visualizer(mediaPlayer.audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
                        val amplitude = waveform[0].toInt() and 0xFF
                        waveformPlayerView.addAmplitude(amplitude.toFloat() * 14)
                    }
                    override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                        // Not used
                    }
                }, Visualizer.getMaxCaptureRate() / 1, true, false)
                enabled = true
            }
        }

        private fun playPausePlayer() {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_pause_24, theme)
                handler.postDelayed(runnable, delay)
                visualizer?.enabled = true
            } else {
                mediaPlayer.pause()
                btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
                handler.removeCallbacks(runnable)
                visualizer?.enabled = false
            }
        }

        override fun onBackPressed() {
            super.onBackPressed()
            mediaPlayer.stop()
            mediaPlayer.release()
            handler.removeCallbacks(runnable)
            visualizer?.release()
        }

        private fun dateFormat(duration: Int): String {
            val d = duration / 1000
            val s = d % 60
            val m = (d / 60 % 60)
            val h = ((d - m * 60) / 360).toInt()
            val f: NumberFormat = DecimalFormat("00")
            var str = "$m:${f.format(s)}"
            if (h > 0) str = "$h:$str"
            return str
        }
private fun cutAudio(startSeconds: Int, endSeconds: Int, filePath: String) {
    if (filePath.isEmpty()) {
        Toast.makeText(this, "File path is null or empty", Toast.LENGTH_SHORT).show()
        return
    }
    val inputFile = File(filePath)
    val originalFileName = inputFile.name
    val outputFileName = "cut_$originalFileName"
    val path = File(Environment.getExternalStorageDirectory().absolutePath + "/Download")
    if (!path.exists()) {
        path.mkdirs()
    }
    val outputFile = File(path, outputFileName)
    val startTime = formatTimeFFmpeg(startSeconds)
    val duration = formatTimeFFmpeg(endSeconds - startSeconds)
    val command = arrayOf(
        "-y",
        "-i", inputFile.absolutePath,
        "-ss", startTime,
        "-t", duration,
        "-acodec", "mp3",
        "-vn",
        outputFile.absolutePath
    )
    FFmpeg.executeAsync(command) { executionId, returnCode ->
        runOnUiThread {
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                val outputPath = outputFile.absolutePath
                showSaveDialog(outputFileName, outputPath, startSeconds, endSeconds - startSeconds, filePath)
            } else {
                Toast.makeText(this@AudioPlayerActivity, "Failed to cut audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


private fun showSaveDialog(defaultFileName: String, filePath: String, startMillis: Int, durationMillis: Int, originalFilePath: String) {
    val dialogView = layoutInflater.inflate(R.layout.rename_layout1, null)
    val filenameInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val phonenumberInput = dialogView.findViewById<TextInputEditText>(R.id.phonenumberInput)
    val addressInput = dialogView.findViewById<TextInputEditText>(R.id.addressInput)
    val areaCodeInput = dialogView.findViewById<TextInputEditText>(R.id.areaCodeInput)

    val context = applicationContext ?: return

    // Truy xuất bản ghi gốc từ cơ sở dữ liệu
    Thread {
        val audioRecordDao = getDatabase(context).audioRecordDao()
        val originalRecord = audioRecordDao.getRecordByFilePath(originalFilePath)

        runOnUiThread {
            // Thiết lập giá trị mặc định cho các trường nhập liệu
            filenameInput.setText(defaultFileName)
            if (originalRecord != null) {
                usernameInput.setText(originalRecord.username)
                phonenumberInput.setText(originalRecord.phonenumber)
                addressInput.setText(originalRecord.currentAddress)
                areaCodeInput.setText(originalRecord.areaCode)
            }

            mediaPlayer.pause()

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val filename = filenameInput.text.toString()
                    val username = usernameInput.text.toString()
                    val phonenumber = phonenumberInput.text.toString()
                    val currentAddress = addressInput.text.toString()
                    val areaCode = areaCodeInput.text.toString()
                    saveAudioRecordToDatabase(filename, filePath, startMillis, durationMillis, username, phonenumber, currentAddress, areaCode,  originalFilePath)
                    startActivity(Intent(this, MainActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }.start()
}
        private fun saveAudioRecordToDatabase(fileName: String, filePath: String, startMillis: Int, durationMillis: Int, username: String, phonenumber: String,currentAddress: String,areaCode: String, originalFilePath: String) {
            val durationSeconds = durationMillis * 1000 // Convert milliseconds to seconds
            val context = applicationContext ?: return

            Thread {
                val audioRecordDao = getDatabase(context).audioRecordDao()
                val originalRecord = audioRecordDao.getRecordByFilePath(originalFilePath)

                val audioRecord = AudioRecord(
                    filename = fileName,
                    filePath = filePath,
                    timestamp = System.currentTimeMillis(),
                    duration = dateFormat(durationSeconds),
                    currentAddress = currentAddress,
                    username = username,
                    phonenumber = phonenumber,
                    gps = originalRecord?.gps ?: "",
                    areaCode = areaCode
                )

                audioRecordDao.insert(audioRecord)
            }.start()
        }

        private fun deleteOldAudioRecordAndFile(filePath: String) {
            val context = applicationContext ?: return
            Thread {
                val audioRecordDao = getDatabase(context).audioRecordDao()
                val oldRecord = audioRecordDao.getRecordByFilePath(filePath)
                if (oldRecord != null) {
                    audioRecordDao.delete(oldRecord)
                    val oldFile = File(filePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
            }.start()
        }
        private fun formatTimeFFmpeg(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
        private fun playAudio(file: File) {
            val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "audio/x-wav")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }


//new 2
//package com.example.audiorecorder
//
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.media.MediaPlayer
//import android.media.PlaybackParams
//import android.media.audiofx.Visualizer
//import android.os.Bundle
//import android.os.Environment
//import android.os.Handler
//import android.os.Looper
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.SeekBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import androidx.core.content.res.ResourcesCompat
//import com.arthenica.mobileffmpeg.BuildConfig
//import com.arthenica.mobileffmpeg.Config
//import com.arthenica.mobileffmpeg.FFmpeg
//import com.example.audiorecorder.AppDatabase.Companion.getDatabase
//import com.google.android.material.appbar.MaterialToolbar
//import com.google.android.material.chip.Chip
//import com.google.android.material.slider.RangeSlider
//import org.florescu.android.rangeseekbar.RangeSeekBar
//import java.io.File
//import java.io.IOException
//import java.nio.ByteBuffer
//import java.text.DecimalFormat
//import java.text.NumberFormat
//
//class AudioPlayerActivity : AppCompatActivity() {
//    private lateinit var rangeSeekBar: RangeSeekBar<Long>
//    private lateinit var btnCutAudio: Button
//    private lateinit var tvStart: TextView
//    private lateinit var tvEnd: TextView
//    private lateinit var toolbar: MaterialToolbar
//    private lateinit var waveformPlayerView: WaveformView
//    private lateinit var tvFilename: TextView
//    private lateinit var tvTrackProgress: TextView
//    private lateinit var tvTrackDuration: TextView
//    private lateinit var btnBackward: ImageButton
//    private lateinit var btnForward: ImageButton
//    private lateinit var btnPlay: ImageButton
//    private lateinit var speedChip: Chip
//    private lateinit var seekBar: SeekBar
//    private lateinit var mediaPlayer: MediaPlayer
//    private var visualizer: Visualizer? = null
//    private lateinit var handler: Handler
//    private lateinit var runnable: Runnable
//    private var playbackSpeed = 1.0f
//    private val jumpValue = 10000 // 10 seconds
//    private val delay = 100L // 100 milliseconds
//    private val REQUEST_PERMISSION_CODE = 1001
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_audio_player)
//
//        val filePath = intent.getStringExtra("filepath")
//        val fileName = intent.getStringExtra("filename")
//
//        // Check if filePath is null
//        if (filePath == null) {
//            Toast.makeText(this, "File path is null", Toast.LENGTH_SHORT).show()
//            finish() // Finish activity if filePath is null
//            return
//        }
//
//        toolbar = findViewById(R.id.toolbar)
//        waveformPlayerView = findViewById(R.id.waveformPlayerView)
//        tvFilename = findViewById(R.id.tvFilename)
//        tvTrackProgress = findViewById(R.id.tvTrackProgress)
//        tvTrackDuration = findViewById(R.id.tvTrackDuration)
//        rangeSeekBar = findViewById(R.id.rangeSeekBar)
//        btnCutAudio = findViewById(R.id.btnCutAudio)
//        tvStart = findViewById(R.id.tvStart)
//        tvEnd = findViewById(R.id.tvEnd)
//
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//        toolbar.setNavigationOnClickListener {
//            onBackPressed()
//        }
//
//        tvFilename.text = fileName
//        mediaPlayer = MediaPlayer().apply {
//            setDataSource(filePath)
//            prepare()
//        }
//        tvTrackDuration.text = dateFormat(mediaPlayer.duration)
//
//        btnBackward = findViewById(R.id.btnBackward)
//        btnForward = findViewById(R.id.btnForward)
//        btnPlay = findViewById(R.id.btnPlay)
//        speedChip = findViewById(R.id.chip)
//        seekBar = findViewById(R.id.seekBar)
//
//        btnForward.setOnClickListener {
//            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
//            seekBar.progress += jumpValue
//        }
//
//        btnBackward.setOnClickListener {
//            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
//            seekBar.progress -= jumpValue
//        }
//
//        speedChip.setOnClickListener {
//            playbackSpeed = if (playbackSpeed != 2f) playbackSpeed + 0.5f else 0.5f
//            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
//            speedChip.text = "x $playbackSpeed"
//        }
//
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
//                if (p2)
//                    mediaPlayer.seekTo(p1)
//            }
//            override fun onStartTrackingTouch(p0: SeekBar?) {}
//            override fun onStopTrackingTouch(p0: SeekBar?) {}
//        })
//
//        handler = Handler(Looper.getMainLooper())
//        runnable = object : Runnable {
//            override fun run() {
//                if (mediaPlayer.isPlaying) {
//                    tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
//                    seekBar.progress = mediaPlayer.currentPosition
//                    val maxAmplitude = mediaPlayer.audioSessionId.toFloat()
//                    waveformPlayerView.addAmplitude(maxAmplitude)
//                    handler.postDelayed(this, 100)
//                }
//            }
//        }
//
//        btnCutAudio.setOnClickListener {
//            val startSeconds = rangeSeekBar.selectedMinValue.toInt() / 1000 // Convert to seconds
//            val endSeconds = rangeSeekBar.selectedMaxValue.toInt() / 1000 // Convert to seconds
//            cutAudio(startSeconds, endSeconds, filePath)
//        }
//
//        btnPlay.setOnClickListener {
//            playPausePlayer()
//        }
//
//        playPausePlayer()
//        seekBar.max = mediaPlayer.duration
//
//        mediaPlayer.setOnCompletionListener {
//            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
//            handler.removeCallbacks(runnable)
//            visualizer?.enabled = false
//        }
//
//        setupVisualizer()
//
//        // Setup range seek bar
//        rangeSeekBar.setRangeValues(0L, mediaPlayer.duration.toLong())
//        rangeSeekBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
//            val minTime = minValue.toInt()
//            val maxTime = maxValue.toInt()
//            tvStart.text = dateFormat(minTime)
//            tvEnd.text = dateFormat(maxTime)
//        }
//    }
//
//    private fun setupVisualizer() {
//        visualizer = Visualizer(mediaPlayer.audioSessionId).apply {
//            captureSize = Visualizer.getCaptureSizeRange()[1]
//            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
//                override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
//                    val amplitude = waveform[0].toInt() and 0xFF
//                    waveformPlayerView.addAmplitude(amplitude.toFloat() * 14)
//                }
//
//                override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
//                    // Not used in this case
//                }
//            }, Visualizer.getMaxCaptureRate() / 2, true, false)
//            enabled = true
//        }
//    }
//
//    private fun playPausePlayer() {
//        if (!mediaPlayer.isPlaying) {
//            mediaPlayer.start()
//            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_pause_24, theme)
//            handler.postDelayed(runnable, delay)
//            visualizer?.enabled = true
//        } else {
//            mediaPlayer.pause()
//            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
//            handler.removeCallbacks(runnable)
//            visualizer?.enabled = false
//        }
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        mediaPlayer.stop()
//        mediaPlayer.release()
//        handler.removeCallbacks(runnable)
//        visualizer?.release()
//    }
//
//    private fun dateFormat(duration: Int): String {
//        val d = duration / 1000
//        val s = d % 60
//        val m = (d / 60 % 60)
//        val h = ((d - m * 60) / 360).toInt()
//        val f: NumberFormat = DecimalFormat("00")
//        var str = "$m:${f.format(s)}"
//        if (h > 0) str = "$h:$str"
//        return str
//    }
//
//    private fun cutAudio(startSeconds: Int, endSeconds: Int, filePath: String) {
//        if (filePath.isEmpty()) {
//            Toast.makeText(this, "File path is null or empty", Toast.LENGTH_SHORT).show()
//            return
//        }
//        val inputFile = File(filePath)
//        val originalFileName = inputFile.name
//        val outputFileName = "cut_$originalFileName"
//        val path = File(Environment.getExternalStorageDirectory().absolutePath + "/Download")
//        if (!path.exists()) {
//            path.mkdirs()
//        }
//        val outputFile = File(path, outputFileName)
//        val startTime = formatTimeFFmpeg(startSeconds)
//        val duration = formatTimeFFmpeg(endSeconds - startSeconds)
//        val command = arrayOf(
//            "-y",
//            "-i", inputFile.absolutePath,
//            "-ss", startTime,
//            "-t", duration,
//            "-acodec", "mp3",
//            "-vn",
//            outputFile.absolutePath
//        )
//        FFmpeg.executeAsync(command) { executionId, returnCode ->
//            runOnUiThread {
//                if (returnCode == Config.RETURN_CODE_SUCCESS) {
//                    val outputPath = outputFile.absolutePath
//                    //Toast.makeText(this@AudioPlayerActivity,"Successfully! $outputPath",Toast.LENGTH_LONG).show()
//                    saveAudioRecordToDatabase(outputFileName, outputPath, startSeconds, endSeconds - startSeconds)
//                    mediaPlayer.pause()
//                    playAudio(outputFile)
//
//                } else {
//                    Toast.makeText(this@AudioPlayerActivity,"Failed to cut audio",Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//        startActivity(Intent(this, MainActivity::class.java))
//    }
//
//    private fun formatTimeFFmpeg(seconds: Int): String {
//        val hours = seconds / 3600
//        val minutes = (seconds % 3600) / 60
//        val secs = seconds % 60
//        return String.format("%02d:%02d:%02d", hours, minutes, secs)
//    }
//    private fun playAudio(file: File) {
//        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
//        val intent = Intent(Intent.ACTION_VIEW)
//        intent.setDataAndType(uri, "audio/x-wav")
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        startActivity(intent)
//    }
//
//    private fun saveAudioRecordToDatabase(fileName: String, filePath: String, startMillis: Int, durationMillis: Int) {
//        val durationSeconds = durationMillis * 1000 // Convert milliseconds to seconds
//        val audioRecord = AudioRecord(
//            filename = fileName,
//            filePath = filePath,
//            timestamp = System.currentTimeMillis(),
//            duration = dateFormat(durationSeconds), // Format the duration in seconds
//            ampsPath = "", // Add appropriate ampsPath if available
//            currentAddress = null, // Add current address if available
//            username = null, // Add username if available
//            phonenumber = null // Add phone number if available
//        )
//        val context = applicationContext ?: return
//        Thread {
//            getDatabase(context).audioRecordDao().insert(audioRecord)
//        }.start()
//    }
//}



////new1
//package com.example.audiorecorder
//
//import android.content.Intent
//import android.media.MediaPlayer
//import android.media.audiofx.Visualizer
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.widget.*
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.res.ResourcesCompat
//import com.arthenica.mobileffmpeg.Config
//import com.arthenica.mobileffmpeg.FFmpeg
//import com.google.android.material.appbar.MaterialToolbar
//import com.google.android.material.chip.Chip
//import org.florescu.android.rangeseekbar.RangeSeekBar
//import java.io.File
//import java.text.DecimalFormat
//import java.text.NumberFormat
//
//class AudioPlayerActivity : AppCompatActivity() {
//    private lateinit var rangeSeekBar: RangeSeekBar<Long>
//    private lateinit var btnCutAudio: Button
//    private lateinit var tvStart: TextView
//    private lateinit var tvEnd: TextView
//    private lateinit var toolbar: MaterialToolbar
//    private lateinit var waveformPlayerView: WaveformPlayerView
//    private lateinit var tvFilename: TextView
//    private lateinit var tvTrackProgress: TextView
//    private lateinit var tvTrackDuration: TextView
//    private lateinit var btnBackward: ImageButton
//    private lateinit var btnForward: ImageButton
//    private lateinit var btnPlay: ImageButton
//    private lateinit var speedChip: Chip
//    private lateinit var seekBar: SeekBar
//    private lateinit var mediaPlayer: MediaPlayer
//    private var visualizer: Visualizer? = null
//    private lateinit var handler: Handler
//    private lateinit var runnable: Runnable
//    private var playbackSpeed = 1.0f
//    private val jumpValue = 10000 // 10 seconds
//    private val delay = 100L // 100 milliseconds
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_audio_player)
//
//        val filePath = intent.getStringExtra("filepath")
//        val fileName = intent.getStringExtra("filename")
//
//        // Check if filePath is null
//        if (filePath == null) {
//            Toast.makeText(this, "File path is null", Toast.LENGTH_SHORT).show()
//            finish() // Finish activity if filePath is null
//            return
//        }
//
//        toolbar = findViewById(R.id.toolbar)
//        waveformPlayerView = findViewById(R.id.waveformPlayerView)
//        tvFilename = findViewById(R.id.tvFilename)
//        tvTrackProgress = findViewById(R.id.tvTrackProgress)
//        tvTrackDuration = findViewById(R.id.tvTrackDuration)
//        rangeSeekBar = findViewById(R.id.rangeSeekBar)
//        btnCutAudio = findViewById(R.id.btnCutAudio)
//        tvStart = findViewById(R.id.tvStart)
//        tvEnd = findViewById(R.id.tvEnd)
//
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//        toolbar.setNavigationOnClickListener {
//            onBackPressed()
//        }
//        tvFilename.text = fileName
//
//        mediaPlayer = MediaPlayer().apply {
//            setDataSource(filePath)
//            prepare()
//        }
//
//        tvTrackDuration.text = dateFormat(mediaPlayer.duration)
//
//        btnBackward = findViewById(R.id.btnBackward)
//        btnForward = findViewById(R.id.btnForward)
//        btnPlay = findViewById(R.id.btnPlay)
//        speedChip = findViewById(R.id.chip)
//        seekBar = findViewById(R.id.seekBar)
//
//        btnForward.setOnClickListener {
//            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
//            seekBar.progress += jumpValue
//        }
//        btnBackward.setOnClickListener {
//            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
//            seekBar.progress -= jumpValue
//        }
//
//        speedChip.setOnClickListener {
//            playbackSpeed = if (playbackSpeed != 2f) playbackSpeed + 0.5f else 0.5f
//            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
//            speedChip.text = "x $playbackSpeed"
//        }
//
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                if (fromUser) {
//                    mediaPlayer.seekTo(progress)
//                }
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//
//        })
//
//        handler = Handler(Looper.getMainLooper())
//        runnable = object : Runnable {
//            override fun run() {
//                if (mediaPlayer.isPlaying) {
//                    updateProgressViews()
//                    updateWaveformView()
//                    handler.postDelayed(this, delay)
//                }
//            }
//        }
//
//        btnPlay.setOnClickListener {
//            playPausePlayer()
//        }
//
//        playPausePlayer()
//        seekBar.max = mediaPlayer.duration
//
//        mediaPlayer.setOnCompletionListener {
//            btnPlay.background =
//                ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
//            handler.removeCallbacks(runnable)
//            visualizer?.enabled = false
//        }
//
//        setupVisualizer()
//
//        // Setup range seek bar
//        rangeSeekBar.setRangeValues(0L, mediaPlayer.duration.toLong())
//        rangeSeekBar.setOnRangeSeekBarChangeListener(object : RangeSeekBar.OnRangeSeekBarChangeListener<Long> {
//            override fun onRangeSeekBarValuesChanged(bar: RangeSeekBar<*>, minValue: Long, maxValue: Long) {
//                val minTime = minValue.toInt()
//                val maxTime = maxValue.toInt()
//                tvStart.text = dateFormat(minTime)
//                tvEnd.text = dateFormat(maxTime)
//            }
//
//        })
//
//        // Handle cut audio button click
//        btnCutAudio.setOnClickListener {
//            val startMillis = rangeSeekBar.selectedMinValue.toInt()
//            val endMillis = rangeSeekBar.selectedMaxValue.toInt()
//            cutAudio(startMillis, endMillis, filePath)
//        }
//    }
//
//    private fun updateProgressViews() {
//        tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
//        seekBar.progress = mediaPlayer.currentPosition
//    }
//
//    private fun updateWaveformView() {
//        waveformPlayerView.updateVisualizer(getWaveformBytes())
//    }
//
////    private fun getWaveformBytes(): ByteArray {
////        val amplitude = mediaPlayer.audioSessionId.toFloat() // Example, you need to adjust this logic
////        return byteArrayOf(amplitude.toInt().toByte())
////    }
////private lateinit var visualizer: Visualizer
//
//    private fun getWaveformBytes(): ByteArray {
//        // Ensure visualizer is initialized and enabled
////        if (!visualizer?.enabled) {
////            return byteArrayOf() // Return empty byte array if visualizer is not properly setup or not enabled
////        }
//
//        // Initialize variables to capture waveform data
//        val captureSize = Visualizer.getCaptureSizeRange().last() // Using .last instead of [1] for clarity
//        val waveform = ByteArray(captureSize)
//
//        // Capture waveform data (safe call operator used)
//        visualizer?.getWaveForm(waveform)
//
//        // Process waveform data (example: reduce the size of waveform array if necessary)
//        val amplitudes = ByteArray(waveform.size / 2)
//        for (i in amplitudes.indices) {
//            amplitudes[i] = waveform[i * 2] // Example: take every second element for simplicity
//        }
//
//        return amplitudes
//    }
//    private fun setupVisualizer() {
//        visualizer = Visualizer(mediaPlayer.audioSessionId).apply {
//            captureSize = Visualizer.getCaptureSizeRange()[1]
//            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
//                override fun onWaveFormDataCapture(
//                    visualizer: Visualizer,
//                    waveform: ByteArray,
//                    samplingRate: Int
//                ) {
//                    val amplitude = waveform[0].toInt() and 0xFF
//                    waveformPlayerView.addAmplitude(amplitude.toFloat() * 14)
//                }
//
//                override fun onFftDataCapture(
//                    visualizer: Visualizer,
//                    fft: ByteArray,
//                    samplingRate: Int
//                ) {
//                    // Not used in this case
//                }
//            }, Visualizer.getMaxCaptureRate() / 2, true, false)
//            enabled = true
//        }
//    }
//
//    private fun playPausePlayer() {
//        if (!mediaPlayer.isPlaying) {
//            mediaPlayer.start()
//            btnPlay.background =
//                ResourcesCompat.getDrawable(resources, R.drawable.round_pause_24, theme)
//            handler.postDelayed(runnable, delay)
//            visualizer?.enabled = true
//        } else {
//            mediaPlayer.pause()
//            btnPlay.background =
//                ResourcesCompat.getDrawable(resources, R.drawable.round_play_circle_24, theme)
//            handler.removeCallbacks(runnable)
//            visualizer?.enabled = false
//        }
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        mediaPlayer.stop()
//        mediaPlayer.release()
//        handler.removeCallbacks(runnable)
//        visualizer?.release()
//    }
//
//    private fun dateFormat(duration: Int): String {
//        val d = duration / 1000
//        val s = d % 60
//        val m = (d / 60) % 60
//        val h = d / 360
//        val f: NumberFormat = DecimalFormat("00")
//        var str = "${f.format(m)}:${f.format(s)}"
//        if (h > 0) str = "${f.format(h)}:$str"
//        return str
//    }
//
//    private fun cutAudio(startSeconds: Int, endSeconds: Int, filePath: String) {
//        if (filePath.isEmpty()) {
//            Toast.makeText(this, "File path is null or empty", Toast.LENGTH_SHORT).show()
//            return
//        }
//        val inputFile = File(filePath)
//        val originalFileName = inputFile.name
//        val outputFileName = "cut_$originalFileName"
//        val path = File(getExternalFilesDir(null), "CutAudios")
//        if (!path.exists()) {
//            path.mkdirs()
//        }
//        val outputFile = File(path, outputFileName)
//        val startTime = startSeconds / 1000
//        val duration = (endSeconds - startSeconds) / 1000
//        val command = arrayOf(
//            "-y",
//            "-i", filePath,
//            "-ss", startTime.toString(),
//            "-t", duration.toString(),
//            "-acodec", "copy",
//            outputFile.absolutePath
//        )
//
//        FFmpeg.executeAsync(command) { _, returnCode ->
//            runOnUiThread {
//                if (returnCode == Config.RETURN_CODE_SUCCESS) {
//                    val outputPath = outputFile.absolutePath
//                    Toast.makeText(
//                        this@AudioPlayerActivity,
//                        "Audio cut successfully! Output file saved at: $outputPath",
//                        Toast.LENGTH_LONG
//                    ).show()
//                } else {
//                    Toast.makeText(
//                        this@AudioPlayerActivity,
//                        "Failed to cut audio",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }
//}
