package com.example.audiorecorder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
@Entity(tableName = "audioRecords")
data class AudioRecord(
    var filename: String,
    var filePath: String,
    var timestamp: Long,
    var duration: String,
    var currentAddress: String?,
    var username: String?,
    var phonenumber: String?,
    var gps: String?,
    var areaCode: String?,
    var status: Int,
    ) {
    @PrimaryKey(autoGenerate = true)
    var id =0
    @Ignore
    var isChecked = false
}