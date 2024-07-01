package com.example.audiorecorder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AudioRecord::class], version = 4)
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
//package com.example.audiorecorder
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase
//
//@Database(entities = [AudioRecord::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun audioRecordDao(): AudioRecordDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "audioRecords_new"
//                )
//                    .addMigrations(MIGRATION_1_2)
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//
//        val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE audioRecords ADD COLUMN username TEXT")
//                database.execSQL("ALTER TABLE audioRecords ADD COLUMN phonenumber TEXT")
//            }
//        }
////
////        val MIGRATION_2_3 = object : Migration(2, 3) {
////            override fun migrate(database: SupportSQLiteDatabase) {
////                database.execSQL("ALTER TABLE audioRecords ADD COLUMN gps TEXT")
////            }
////        }
////
////        val MIGRATION_3_4 = object : Migration(3, 4) {
////            override fun migrate(database: SupportSQLiteDatabase) {
////                database.execSQL("ALTER TABLE audioRecords ADD COLUMN areaCode TEXT")
////            }
////        }
////
////        val MIGRATION_4_5 = object : Migration(4, 5) {
////            override fun migrate(database: SupportSQLiteDatabase) {
////                database.execSQL("""
////                    CREATE TABLE IF NOT EXISTS audioRecords_new (
////                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
////                        filename TEXT NOT NULL,
////                        filePath TEXT NOT NULL,
////                        timestamp INTEGER NOT NULL,
////                        duration TEXT NOT NULL,
////                        currentAddress TEXT,
////                        username TEXT,
////                        phonenumber TEXT,
////                        gps TEXT,
////                        areaCode TEXT
////                    )
////                """)
////                database.execSQL("""
////                    INSERT INTO audioRecords_new (id, filename, filePath, timestamp, duration, currentAddress, username, phonenumber, gps, areaCode)
////                    SELECT id, filename, filePath, timestamp, duration, currentAddress, username, phonenumber, gps, areaCode
////                    FROM audioRecords
////                """)
////                database.execSQL("DROP TABLE audioRecords")
////                database.execSQL("ALTER TABLE audioRecords_new RENAME TO audioRecords")
////            }
////        }
//    }
//}
//package com.example.audiorecorder
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase

//@Database(entities = [AudioRecord::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun audioRecordDao(): AudioRecordDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "audioRecords_new"
//                )
//                    .fallbackToDestructiveMigration()
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
//@Database(entities = [AudioRecord::class], version = 2)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun audioRecordDao(): AudioRecordDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "audioRecords_new"
//                )
//                    .addMigrations(MIGRATION_1_2)
//                    .fallbackToDestructiveMigration() // This will recreate the database if migration is not provided
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//
//        val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE audioRecords ADD COLUMN status TEXT")
//            }
//        }
//    }
//}