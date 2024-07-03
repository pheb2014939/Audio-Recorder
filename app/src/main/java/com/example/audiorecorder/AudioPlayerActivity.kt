package com.example.audiorecorder
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.audiorecorder.AppDatabase.Companion.getDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.florescu.android.rangeseekbar.RangeSeekBar
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat

class AudioPlayerActivity : AppCompatActivity() {
        private lateinit var connectionClass: ConnectionClass
        private var conn: Connection? = null
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
            connectionClass = ConnectionClass()
            connect()
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
    private fun connect() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                conn = connectionClass.CNN()
                val str = if (conn == null) {
                    "Error in connection with MySQL server"
                } else {
                    "Connected with MySQL server"
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AudioPlayerActivity, str, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

//    private fun uploadAudioToMySQL(
//        filename: String,
//        filePath: String,
//        startMillis: Int,
//        durationMillis: Int,
//        username: String,
//        phonenumber: String,
//        currentAddress: String,
//        areaCode: String,
//        status: Int,
//        outputPath: String
//    ) {
//        val audioBytes = File(outputPath).readBytes()
//        val query = "INSERT INTO Audio (audio_Name, mime_Type) VALUES (?, ?)"
//
//        try {
//            conn?.use { connection ->
//                connection.prepareStatement(query).use { statement ->
//                    statement.setString(1, filename)
//                    statement.setString(2, "audio/mp3") // Đổi kiểu MIME tùy thuộc vào loại file âm thanh bạn sử dụng
//                    // Thêm các tham số còn lại vào câu lệnh INSERT nếu cần
//                    // statement.setInt(3, startMillis)
//                    // statement.setInt(4, durationMillis)
//                    // statement.setString(5, username)
//                    // statement.setString(6, phonenumber)
//                    // statement.setString(7, currentAddress)
//                    // statement.setString(8, areaCode)
//                    // statement.setInt(9, status)
//
//                    val rowsAffected = statement.executeUpdate()
//
//                    if (rowsAffected == 1) {
//                        Log.d("UploadToMySQL", "Uploaded successfully to MySQL")
//                        runOnUiThread {
//                            Toast.makeText(this, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        throw SQLException("Creating Audio failed, no rows affected.")
//                    }
//                }
//            }
//        } catch (e: SQLException) {
//            Log.e("UploadToMySQL", "SQLException: ${e.message}")
//            runOnUiThread {
//                Toast.makeText(this, "Failed to upload to MySQL: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Log.e("UploadToMySQL", "Exception: ${e.message}")
//            runOnUiThread {
//                Toast.makeText(this, "Failed to upload to MySQL: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        } finally {
//            try {
//                conn?.close()
//            } catch (e: SQLException) {
//                Log.e("UploadToMySQL", "Failed to close connection: ${e.message}")
//            }
//        }
//    }

//    fun uploadAudioToMySQL(): List<String> {
//        val query = "SELECT * FROM Audio"
////        Audio (audio_Name, mime_Type) VALUES (?, ?)
//        val resultList = mutableListOf<String>()
//
//        // Kết nối đến cơ sở dữ liệu
//        val connection = ConnectionClass().CNN()
//
//        connection?.use { conn ->
//            try {
//                conn.createStatement().use { statement ->
//                    val resultSet = statement.executeQuery(query)
//
//                    // Duyệt qua kết quả và lưu vào danh sách
//                    while (resultSet.next()) {
//                        val id = resultSet.getInt("audio_Name")
//                        val name = resultSet.getString("mime_Type")
//                        val resultString = "ID: $id, Name: $name"
//                        resultList.add(resultString)
//                    }
//                }
//            } catch (e: SQLException) {
//                println("SQLException: ${e.message}")
//            }
//        }
//        return resultList
//    }
    private fun uploadAudioToMySQL(filename: String, filePath: String, startMillis: Int, durationMillis: Int, username: String, phonenumber: String, currentAddress: String, areaCode: String, status: Int, outputPath: String) {
    val audioBytes = File(outputPath).readBytes()
    // Thực hiện kết nối và tải lên cơ sở dữ liệu MySQL
    val conn = connectionClass.CNN()
    val query = "INSERT INTO Audio (audio_Name, mime_Type, time_stamp, audio_Size, audio_Address, audio_LeakStatus) VALUES (?, ?, ?, ?, ?, ?)"
    try {
        conn?.prepareStatement(query)?.use { statement ->
            statement.setString(1, filename)
            statement.setString(2, "audio/mp3") // Đổi kiểu MIME tùy thuộc vào loại file âm thanh bạn sử dụng
            statement.setTimestamp(3, Timestamp(System.currentTimeMillis())) // Sửa lại thành setTimestamp và thêm dấu đóng ngoặc cho System.currentTimeMillis()
            statement.setInt(4, audioBytes.size)
            statement.setString(5, filePath)
            statement.setInt(6, status)

            statement.executeUpdate()
            Log.d("UploadToMySQL", "Uploaded successfully to MySQL")
            runOnUiThread {
                Toast.makeText(this, "Uploaded successfully!!!!!!!", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: SQLException) {
        Log.e("UploadToMySQL", "Failed to upload to MySQL: ${e.message}")
        runOnUiThread {
            Toast.makeText(this, "Failed to upload to MySQL", Toast.LENGTH_SHORT).show()
        }
    } finally {
        conn?.close()
    }
}


    private fun showSaveDialog(defaultFileName: String, filePath: String, startMillis: Int, durationMillis: Int, originalFilePath: String) {
    val dialogView = layoutInflater.inflate(R.layout.rename_layout1, null)
    val filenameInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val phonenumberInput = dialogView.findViewById<TextInputEditText>(R.id.phonenumberInput)
    val addressInput = dialogView.findViewById<TextInputEditText>(R.id.addressInput)
    val areaCodeInput = dialogView.findViewById<TextInputEditText>(R.id.areaCodeInput)

    // Initialize statusRadioGroup from dialogView
    val statusRadioGroup: RadioGroup = dialogView.findViewById(R.id.statusRadioGroup)

    val context = applicationContext ?: return

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
                // Thiết lập giá trị mặc định cho statusRadioGroup
                when (originalRecord.status) {
                    0 -> statusRadioGroup.check(R.id.radioButton) // Bình thường
                    1 -> statusRadioGroup.check(R.id.radioButton1) // Nghi ngờ
                    // Các trường hợp khác có thể xét thêm tại đây
                }
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

//                     Check which RadioButton is checked
                    val status = when (statusRadioGroup.checkedRadioButtonId) {
                        R.id.radioButton -> 0
                        R.id.radioButton1 -> 1
                        else -> -1 // Handle case where no RadioButton is checked
                }
                    saveAudioRecordToDatabase(filename, filePath, startMillis, durationMillis, username, phonenumber, currentAddress, areaCode, status, originalFilePath)
                    // Tải lên file cắt được lên MySQL
//                    uploadAudioToMySQL(filename, filePath, startMillis, durationMillis, username, phonenumber, currentAddress, areaCode, status, originalFilePath)
//                    uploadAudioToMySQL()
                }

                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }.start()
}


    private fun saveAudioRecordToDatabase(fileName: String, filePath: String, startMillis: Int, durationMillis: Int, username: String, phonenumber: String,currentAddress: String,areaCode: String, status: Int, originalFilePath: String) {
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
                    areaCode = areaCode,
                    status = status
                )

                audioRecordDao.insert(audioRecord)
                if (status == 1) {
                    uploadAudioToMySQL(fileName, filePath, startMillis, durationMillis, username, phonenumber, currentAddress, areaCode, status, originalFilePath)
                }
            }.start()
        startActivity(Intent(this, MainActivity::class.java))

    }
//    private fun uploadAudioToMySQL(filename: String, filePath: String, startMillis: Int, durationMillis: Int, username: String, phonenumber: String, currentAddress: String, areaCode: String, status: Int, outputPath: String) {
//        val audioBytes = File(outputPath).readBytes()
//        // Thực hiện kết nối và tải lên cơ sở dữ liệu MySQL
//        val connection = ConnectionClass().CNN()
//        val query = "INSERT INTO Audio (audio_Name, mime_Type, time_stamp, audio_Size, audio_Address, audio_LeakStatus) " +
//                "VALUES (?, ?, ?, ?, ?, ?)"
//        try {
//            connection?.prepareStatement(query)?.use { statement ->
//                statement.setString(1, filename)
//                statement.setString(2, "audio/mp3") // Đổi kiểu MIME tùy thuộc vào loại file âm thanh bạn sử dụng
//                statement.setLong(3, System.currentTimeMillis())
//                statement.setInt(4, audioBytes.size)
//                statement.setString(5, filePath)
//                statement.setInt(6, status)
//                statement.executeUpdate()
//                Log.d("UploadToMySQL", "Uploaded successfully to MySQL")
//                Toast.makeText(this, "Uploaded successfully!!!!!!!", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: SQLException) {
//            Log.e("UploadToMySQL", "Failed to upload to MySQL: ${e.message}")
//            Toast.makeText(this, "Failed to upload to MySQL", Toast.LENGTH_SHORT).show()
//        } finally {
//            connection?.close()
//        }
//    }
//private fun uploadAudioToMySQL(filename: String, filePath: String, startMillis: Int, durationMillis: Int, username: String, phonenumber: String, currentAddress: String, areaCode: String, status: Int, outputPath: String) {
//    val audioBytes = File(outputPath).readBytes()
//    // Thực hiện kết nối và tải lên cơ sở dữ liệu MySQL
//    val conn = connectionClass.CNN()
//    val query = "INSERT INTO Audio (audio_Name, mime_Type, time_stamp, audio_Size, audio_Address, audio_LeakStatus) VALUES (?, ?, ?, ?, ?, ?)"
//    try {
//        conn?.prepareStatement(query)?.use { statement ->
//            statement.setString(1, filename)
//            statement.setString(2, "audio/mp3") // Đổi kiểu MIME tùy thuộc vào loại file âm thanh bạn sử dụng
//            statement.setLong(3, System.currentTimeMillis())
//            statement.setInt(4, audioBytes.size)
//            statement.setString(5, filePath)
//            statement.setInt(6, status)
//            statement.executeUpdate()
//            Log.d("UploadToMySQL", "Uploaded successfully to MySQL")
//            runOnUiThread {
//                Toast.makeText(this, "Uploaded successfully!!!!!!!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    } catch (e: SQLException) {
//        Log.e("UploadToMySQL", "Failed to upload to MySQL: ${e.message}")
//        runOnUiThread {
//            Toast.makeText(this, "Failed to upload to MySQL", Toast.LENGTH_SHORT).show()
//        }
//    } finally {
//        conn?.close()
//    }
//}

    //        private fun deleteOldAudioRecordAndFile(filePath: String) {
//            val context = applicationContext ?: return
//            Thread {
//                val audioRecordDao = getDatabase(context).audioRecordDao()
//                val oldRecord = audioRecordDao.getRecordByFilePath(filePath)
//                if (oldRecord != null) {
//                    audioRecordDao.delete(oldRecord)
//                    val oldFile = File(filePath)
//                    if (oldFile.exists()) {
//                        oldFile.delete()
//                    }
//                }
//            }.start()
//        }
        private fun formatTimeFFmpeg(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
//        private fun playAudio(file: File) {
//            val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.setDataAndType(uri, "audio/x-wav")
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivity(intent)
//        }



//        private fun checkAndRequestPermissions() {
//            val permissions = arrayOf(
//                android.Manifest.permission.READ_EXTERNAL_STORAGE,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//            val permissionList = ArrayList<String>()
//            for (permission in permissions) {
//                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                    permissionList.add(permission)
//                }
//            }
//            if (permissionList.isNotEmpty()) {
//                ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), REQUEST_PERMISSION_CODE)
//            }
//        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_PERMISSION_CODE) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }



//    private fun connect() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                conn = connectionClass.CNN()
//                val str = if (conn == null) {
//                    "Error in connection with MySQL server"
//                } else {
//                    "Connected with MySQL serrrrrrrver"
//                }
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@AudioPlayerActivity, str, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                throw RuntimeException(e)
//            }
//        }
//    }

    }
