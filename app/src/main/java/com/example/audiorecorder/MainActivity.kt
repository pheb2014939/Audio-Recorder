package com.example.audiorecorder
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
const val REQUEST_CODE = 200
class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentAddress: String? = null
    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false
    private lateinit var timer: Timer
    private var duration = ""
    private lateinit var tvUserLocation: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var vibrator: Vibrator
    private lateinit var btnDelete: ImageButton
    private lateinit var btnDone: ImageButton
    private lateinit var btnList: ImageButton
    private lateinit var btnRecord: ImageButton
    private lateinit var tvTimer: TextView
    private lateinit var waveformView: WaveformView
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBG: View
    private lateinit var filenameInput: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnOk: MaterialButton
    private lateinit var db: AppDatabase
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var usernameInput: TextInputEditText
    private lateinit var phonenumberInput: TextInputEditText
    private lateinit var areaCodeInput: TextInputEditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
         btnDelete = findViewById(R.id.btnDelete)
        btnDone = findViewById(R.id.btnDone)
        btnList = findViewById(R.id.btnList)
        btnRecord = findViewById(R.id.btnRecord)
        tvTimer = findViewById(R.id.tvTimer)
        waveformView = findViewById(R.id.waveformView)
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBG = findViewById(R.id.bottomSheetBG)
        filenameInput = findViewById(R.id.filenameInput)
        btnCancel = findViewById(R.id.btnCancel)
        btnOk = findViewById(R.id.btnOk)
        areaCodeInput = findViewById(R.id.areaCodeInput)
        tvUserLocation = findViewById(R.id.tvUserLocation)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        usernameInput = findViewById(R.id.usernameInput)
        phonenumberInput = findViewById(R.id.phonenumberInput)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }
        db = AppDatabase.getDatabase(this)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }


        btnDone.setOnClickListener {
            stopRecorder()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            filenameInput.setText(filename)
        }

        btnCancel.setOnClickListener {
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        btnOk.setOnClickListener {
            dismiss()
            save()
            clearBottomSheetData()

        }

        bottomSheetBG.setOnClickListener {
//            File("$dirPath$filename.mp3").delete()
//            dismiss()
        }
        btnDelete.setOnClickListener {
            stopRecorder()
            File("$dirPath$filename.mp3").delete()
            Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
        }
        btnDelete.isClickable = false

    }
    private fun getLastLocation(callback: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        try {
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                val address = addresses[0].getAddressLine(0)
                                tvUserLocation.text = "Address: $address"

                                val latitude = location.latitude
                                tvLatitude.text = "Latitude: $latitude"

                                val longitude = location.longitude
                                tvLongitude.text = "Longitude: $longitude"

                                callback(address)
                            } else {
                                callback("Unknown_Location")
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            callback("Unknown_Location")
                        }
                    } else {
                        callback("Unknown_Location")
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                callback("Unknown_Location")
            }
        } else {
            askPermission()
            callback("Unknown_Location")
        }
    }
    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }
        // Fetch location and start recording
        getLastLocation { address ->
            currentAddress = address
            // Start recording
            recorder = MediaRecorder()
            dirPath = "${externalCacheDir?.absolutePath}/"
            val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
            val date = simpleDateFormat.format(Date())
            filename = "${address.replace("[,\\s]+".toRegex(), "_")}_$date"
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile("$dirPath$filename.mp3")
                try {
                    prepare()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                start()
            }
            btnRecord.setImageResource(R.drawable.ic_pause)
            isRecording = true
            isPaused = false
            timer.start()
            btnDelete.isClickable = true
            btnDelete.setImageResource(R.drawable.round_clear_24)
            btnList.visibility = View.GONE
            btnDone.visibility = View.VISIBLE
        }
    }
    private fun pauseRecorder() {
        recorder.pause()
        isPaused = true
        btnRecord.setImageResource(R.drawable.ic_record)
        timer.pause()
    }
    private fun resumeRecorder() {
        recorder.resume()
        isPaused = false
        btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
    }
    private fun stopRecorder() {
        timer.stop()
        recorder.apply {
            stop()
            release()
        }
        isPaused = false
        isRecording = false
        btnList.visibility = View.VISIBLE
        btnDone.visibility = View.GONE
        btnDelete.isClickable = false
        btnDelete.setImageResource(R.drawable.round_clear_24)
        btnRecord.setImageResource(R.drawable.ic_record)
        tvTimer.text = "00:00.00"
        amplitudes = waveformView.clear()
    }



    private fun save() {
        val newFileName = filenameInput.text.toString()
        val username = usernameInput.text.toString()
        val phonenumber = phonenumberInput.text.toString()
        if (newFileName != filename) {
            val newFile = File("$dirPath$newFileName.mp3")
            File("$dirPath$filename.mp3").renameTo(newFile)
        }
        val filePath = "$dirPath$newFileName.mp3"
        val timestamp = Date().time
        val gps = "${tvLatitude.text},${tvLongitude.text}"
        val areaCode = areaCodeInput.text.toString()
        val status = 0
        val record = AudioRecord(newFileName, filePath, timestamp, duration, currentAddress, username, phonenumber, gps, areaCode , status)
        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }
    }
    private fun dismiss() {
        bottomSheetBG.visibility = View.GONE
        hideKeyboard(filenameInput)
        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    override fun onTimerTick(duration: String) {
        tvTimer.text = duration
        this.duration = duration.dropLast(3)
        waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
    }
    private fun clearBottomSheetData() {
        filenameInput.text?.clear()
        usernameInput.text?.clear()
        phonenumberInput.text?.clear()
        areaCodeInput.text?.clear()

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            }
        }
    }
}
