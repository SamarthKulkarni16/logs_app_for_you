package com.samarth.logsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync ledger row — one per calendar day (yyyy-MM-dd). Holds no journal
 * content; the .md file in LogFileStore is the single source of truth for
 * what was written. This table only tracks "has today's file made it to
 * Supabase yet," so a background worker knows what still needs pushing.
 * Re-editing an already-synced day simply flips it back to unsynced.
 */
@Entity(tableName = "synced_days")
data class SyncedDay(
    @PrimaryKey val dateKey: String,
    val synced: Boolean,
    val remoteId: String? = null,
    val lastAttemptMillis: Long = 0L
)
