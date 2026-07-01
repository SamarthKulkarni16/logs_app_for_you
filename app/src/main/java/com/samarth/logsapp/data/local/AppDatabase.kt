package com.samarth.logsapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SyncedDay::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun syncedDayDao(): SyncedDayDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logs_app_sync_ledger.db"
                ).build().also { instance = it }
            }
    }
}
