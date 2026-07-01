package com.samarth.logsapp.sync

import com.samarth.logsapp.data.local.LogFileStore
import com.samarth.logsapp.data.local.SyncedDay
import com.samarth.logsapp.data.local.SyncedDayDao
import com.samarth.logsapp.data.remote.LogsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pushes any not-yet-synced day's .md file to the Supabase `daily_logs`
 * table, upserting on (user_id, log_date) so re-syncing the same day never
 * creates duplicate rows.
 *
 * The .md file is always written first (instant, no network dependency);
 * this class is only ever called afterward, opportunistically, to catch
 * the local file up to the cloud. If it fails (offline, transient error),
 * the day simply stays marked unsynced and gets retried on the next call —
 * there is no data loss, since the .md file itself is unaffected.
 */
class SyncManager(
    private val fileStore: LogFileStore,
    private val syncedDayDao: SyncedDayDao,
    private val logsRepository: LogsRepository
) {
    private val syncMutex = Mutex()

    /**
     * Marks today's day as unsynced in the ledger, then immediately
     * attempts to push it. Safe to call even if offline — the attempt will
     * simply fail and retry later via [syncPendingDays].
     *
     * Returns true only if *this* day specifically ended up synced — used
     * by the write screen's status indicator. A false return does not mean
     * data loss: the .md file is untouched and will retry later.
     */
    suspend fun registerAndSync(dateKey: String, userId: String): Boolean {
        syncedDayDao.upsert(SyncedDay(dateKey = dateKey, synced = false))
        syncPendingDays(userId)
        return syncedDayDao.getByDateKey(dateKey)?.synced == true
    }

    /** Retries every not-yet-synced day. Call on app start and on regaining connectivity. */
    suspend fun syncPendingDays(userId: String) = syncMutex.withLock {
        val unsynced = syncedDayDao.getUnsynced()
        for (entry in unsynced) {
            val body = fileStore.readBody(entry.dateKey) ?: continue
            if (body.isBlank()) continue // never sync empty drafts

            runCatching {
                val remoteRow = logsRepository.upsertLog(
                    userId = userId,
                    logDate = entry.dateKey,
                    body = body
                )
                val remoteId = remoteRow.id
                if (remoteId != null) {
                    syncedDayDao.markSynced(entry.dateKey, remoteId)
                }
            }.onFailure {
                syncedDayDao.markAttempted(entry.dateKey, System.currentTimeMillis())
                // Swallow and continue — this day stays unsynced and will
                // be retried on the next pass. One failure shouldn't block
                // syncing the rest of the queue.
            }
        }
    }
}
