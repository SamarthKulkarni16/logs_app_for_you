package com.samarth.logsapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the `daily_logs` table (Phase 1 backend addition needed before
 * this app can sync):
 *
 *   id uuid primary key default gen_random_uuid()
 *   user_id uuid not null references auth.users(id)
 *   log_date date not null
 *   body text not null
 *   updated_at timestamptz not null default now()
 *   unique (user_id, log_date)
 *
 * The unique constraint on (user_id, log_date) is what makes upsert-by-day
 * work: writing today's log twice updates the same row instead of creating
 * duplicates, matching the local file store's one-file-per-day model.
 */
@Serializable
data class LogRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("log_date") val logDate: String,
    val body: String,
    @SerialName("updated_at") val updatedAt: String? = null
)
