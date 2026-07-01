package com.samarth.logsapp.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Talks to the `daily_logs` table. Unlike the freeform notes table (always
 * insert), this always upserts on (user_id, log_date) — writing today's log
 * a hundred times over the course of a day should still leave exactly one
 * row for today, with the body simply replaced each time.
 */
class LogsRepository(private val supabase: SupabaseClient) {

    suspend fun upsertLog(userId: String, logDate: String, body: String): LogRow {
        return supabase.from("daily_logs")
            .upsert(LogRow(userId = userId, logDate = logDate, body = body)) {
                onConflict = "user_id,log_date"
                select()
            }
            .decodeSingle<LogRow>()
    }
}
