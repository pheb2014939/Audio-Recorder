package com.example.audiorecorder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update

@Dao
interface AudioRecordDao {



    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>

    //@Query("SELECT * FROM audioRecords WHERE filename LIKE :query")
    @Query("SELECT * FROM audioRecords WHERE filename LIKE :query OR username LIKE :query OR phonenumber LIKE :query")
    fun searchDatabase(query: String): List<AudioRecord>

    @Query("SELECT * FROM audioRecords ORDER BY timestamp DESC")
    suspend fun getAllSortedByTimestampDescending(): List<AudioRecord>

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun  delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)


    @Query("SELECT * FROM audioRecords WHERE filePath = :filePath LIMIT 1")
    fun getRecordByFilePath(filePath: String): AudioRecord?






}