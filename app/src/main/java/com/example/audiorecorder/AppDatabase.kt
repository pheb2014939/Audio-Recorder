package com.example.audiorecorder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AudioRecord::class], version = 2)
abstract class AppDatabase : RoomDatabase() {



    abstract fun audioRecordDao(): AudioRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audioRecords4"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Add both migrations here
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE audioRecords ADD COLUMN username TEXT")
                database.execSQL("ALTER TABLE audioRecords ADD COLUMN phonenumber TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Additional migration steps, if any
            }
        }
        private val MIGRATION_3_4 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE audioRecords ADD COLUMN gps TEXT")
            }
        }
    }

}
