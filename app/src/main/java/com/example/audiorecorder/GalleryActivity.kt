package com.example.audiorecorder

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var searchInput: TextInputEditText
    private lateinit var ic_backIV: ImageView
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase
    private lateinit var rv: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var btnDelete: ImageButton
    private lateinit var btnRename: ImageButton
    private lateinit var tvDelete: TextView
    private lateinit var tvRename: TextView
    private lateinit var ListNN: TextView
    private var allChecked = false
    private lateinit var connectionClass: ConnectionClass

    //
    private val PICK_AUDIO_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        rv = findViewById(R.id.rv)
        searchInput = findViewById(R.id.searchInput)
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        editBar = findViewById(R.id.editBar)
        ic_backIV = findViewById(R.id.ic_backIV)
        btnClose = findViewById(R.id.btnClose)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        btnDelete = findViewById(R.id.btnDelete)
        btnRename = findViewById(R.id.btnRename)
        tvDelete = findViewById(R.id.tvDelete)
        tvRename = findViewById(R.id.tvRename)
        ListNN = findViewById(R.id.ListNN)
        records = ArrayList()
        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords4"
        ).build()
        mAdapter = Adapter(records, this)
        rv.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }
        fetchAll()
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        btnClose.setOnClickListener {
            leaveEditMode()
        }
        ic_backIV.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }
        ListNN.setOnClickListener {
            startActivity(Intent(this, GalleryActivity1::class.java))
        }


        btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.forEach { it.isChecked = allChecked }
            mAdapter.notifyDataSetChanged()
            if (allChecked) {
                disableRename()
                enableDelete()
            } else {
                disableDelete()
                disableRename()
            }
        }
        btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Xoá bản ghi?")
            val nbRecords = records.count { it.isChecked }
            builder.setMessage("Bạn có chắc chắn muốn xoá $nbRecords bản ghi?")

            builder.setPositiveButton("Xoá") { _, _ ->
                val toDelete = records.filter { it.isChecked }.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)
                    runOnUiThread {
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }
            }
            builder.setNegativeButton("Huỷ") { _, _ -> }
            val dialog = builder.create()
            dialog.show()
        }

        btnRename.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = this.layoutInflater.inflate(R.layout.rename_layout, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val record = records.find { it.isChecked }
            if (record != null) {
                val textInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
                textInput.setText(record.filename)

                val textInput1 = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
                textInput1.setText(record.username)

                val textInput2 = dialogView.findViewById<TextInputEditText>(R.id.phonenumberInput)
                textInput2.setText(record.phonenumber)

                val textInput3 = dialogView.findViewById<TextInputEditText>(R.id.addressInput)
                textInput3.setText(record.currentAddress)

                val statusRadioGroup: RadioGroup = dialogView.findViewById(R.id.statusRadioGroup)
                when (record.status) {
                    0 -> statusRadioGroup.check(R.id.radioButton) // Bình thường
                    1 -> statusRadioGroup.check(R.id.radioButton1) // Nghi ngờ
                    // Các trường hợp khác có thể xét thêm tại đây
                }

                dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
                    val input = textInput.text.toString()
                    val input1 = textInput1.text.toString()
                    val input2 = textInput2.text.toString()
                    val input3 = textInput3.text.toString()
                    val status = when (statusRadioGroup.checkedRadioButtonId) {
                        R.id.radioButton -> 0
                        R.id.radioButton1 -> 1
                        else -> -1 // Handle case where no RadioButton is checked
                    }

                    var valid = true

                    if (input.isEmpty()) {
                        Toast.makeText(this, "A name is required", Toast.LENGTH_LONG).show()
                        valid = false
                    }
                    if (input1.isEmpty()) {
                        Toast.makeText(this, "A username is required", Toast.LENGTH_LONG).show()
                        valid = false
                    }
                    if (input2.isEmpty()) {
                        Toast.makeText(this, "A phone number is required", Toast.LENGTH_LONG).show()
                        valid = false
                    }
                    if (input3.isEmpty()) {
                        Toast.makeText(this, "A address is required", Toast.LENGTH_LONG).show()
                        valid = false
                    }


                    if (valid) {
                        record.filename = input
                        record.username = input1
                        record.phonenumber = input2
                        record.currentAddress = input3
                        record.status = status


                        GlobalScope.launch {
                            db.audioRecordDao().update(record)
                            runOnUiThread {
                                mAdapter.notifyItemChanged(records.indexOf(record))
                                dialog.dismiss()
                                leaveEditMode()
                            }
                        }
                                startActivity(Intent(this, GalleryActivity1::class.java))

                    }
                }
                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
            }
        }

    }
    private fun openAudioSelector() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_AUDIO_REQUEST)
    }
    private fun leaveEditMode() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        editBar.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        records.forEach {
            it.isChecked = false
        }
        mAdapter.setEditMode(false)
    }

    private fun disableRename() {
        btnDelete.isClickable = false
        btnDelete.backgroundTintList =
            ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme)
        tvRename.setTextColor(
            ResourcesCompat.getColorStateList(
                resources,
                R.color.grayDarkDisabled,
                theme
            )
        )
    }

    private fun disableDelete() {
        btnDelete.isClickable = false
        btnDelete.backgroundTintList =
            ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme)
        tvDelete.setTextColor(
            ResourcesCompat.getColorStateList(
                resources,
                R.color.grayDarkDisabled,
                theme
            )
        )
    }

    private fun enableRename() {
        btnDelete.isClickable = true
        btnDelete.backgroundTintList =
            ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun enableDelete() {
        btnDelete.isClickable = true
        btnDelete.backgroundTintList =
            ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun searchDatabase(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val queryResult = db.audioRecordDao().searchDatabase("%$query%")
            withContext(Dispatchers.Main) {
                records.clear()
                records.addAll(queryResult)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            val queryResult = db.audioRecordDao().getAllSortedByTimestampDescending()
            withContext(Dispatchers.Main) {
                records.clear()
//                records.addAll(queryResult)
                records.addAll(queryResult.filter { it.status == 0 })
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onItemClickListener(position: Int) {
        val audioRecord = records[position]
        if (mAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)
            val nbSelected = records.count { it.isChecked }
            when (nbSelected) {
                0 -> {
                    disableRename()
                    disableDelete()
                }
                1 -> {
                    enableDelete()
                    enableRename()
                }
                else -> {
                    disableRename()
                    enableDelete()
                }
            }
        } else {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra("filepath", audioRecord.filePath)
            intent.putExtra("filename", audioRecord.filename)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        if (mAdapter.isEditMode() && editBar.visibility == View.GONE) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            editBar.visibility = View.VISIBLE
            ic_backIV.visibility = View.GONE
            enableDelete()
            enableRename()
        }
    }
}