package com.samarth.logsapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncedDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SyncedDay)

    @Query("SELECT * FROM synced_days WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDateKey(dateKey: String): SyncedDay?

    @Query("SELECT * FROM synced_days WHERE synced = 0")
    suspend fun getUnsynced(): List<SyncedDay>

    @Query("UPDATE synced_days SET synced = 1, remoteId = :remoteId WHERE dateKey = :dateKey")
    suspend fun markSynced(dateKey: String, remoteId: String)

    @Query("UPDATE synced_days SET lastAttemptMillis = :attemptMillis WHERE dateKey = :dateKey")
    suspend fun markAttempted(dateKey: String, attemptMillis: Long)
}
