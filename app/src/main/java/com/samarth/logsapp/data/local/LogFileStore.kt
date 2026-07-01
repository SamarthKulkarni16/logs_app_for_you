package com.samarth.logsapp.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A single day's log as read off disk. [dateKey] is yyyy-MM-dd — the filename stem and the sync/DB primary key. */
data class LogFile(
    val dateKey: String,
    val dateLabel: String,
    val body: String,
    val lastModifiedMillis: Long
)

/**
 * Reads and writes daily logs as .md files in app-private internal storage
 * (context.filesDir/logs/), one file per calendar day: yyyy-MM-dd.md.
 *
 * Unlike a freeform notes app (many timestamped entries), a log is keyed by
 * *day* — opening the app always resolves to today's file, and typing
 * overwrites that same file all day long. Every previous day's file is
 * immutable once the day has passed (nothing in the UI writes to a past
 * date), which is what makes the month-grid history meaningful: a filled
 * cell means "this day has a file," full stop.
 */
class LogFileStore(context: Context) {

    private val logsDir: File = File(context.filesDir, "logs").apply {
        if (!exists()) mkdirs()
    }

    private val dateKeyFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
    private val dateLabelFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    /** yyyy-MM-dd for today, in the device's local timezone. This is the single source of truth for "which day is 'today' right now." */
    fun todayKey(): String = LocalDate.now().format(dateKeyFormatter)

    fun dateLabelFor(dateKey: String): String {
        val date = LocalDate.parse(dateKey, dateKeyFormatter)
        val cal = java.util.Calendar.getInstance()
        cal.set(date.year, date.monthValue - 1, date.dayOfMonth, 0, 0, 0)
        return dateLabelFormat.format(cal.time)
    }

    /** Overwrites (or creates) today's file with the latest full body. Called on every autosave — the file always holds the complete current text, not an append log. */
    suspend fun write(dateKey: String, body: String) = withContext(Dispatchers.IO) {
        File(logsDir, "$dateKey.md").writeText(body)
    }

    suspend fun readBody(dateKey: String): String? = withContext(Dispatchers.IO) {
        val file = File(logsDir, "$dateKey.md")
        if (!file.exists()) return@withContext null
        file.readText()
    }

    /** Set of every dateKey that has a non-empty local file — used to decide which month-grid cells are tappable. */
    suspend fun listWrittenDateKeys(): Set<String> = withContext(Dispatchers.IO) {
        logsDir.listFiles { f -> f.isFile && f.name.endsWith(".md") }
            ?.filter { it.length() > 0L }
            ?.map { it.name.removeSuffix(".md") }
            ?.toSet()
            ?: emptySet()
    }

    /** All written logs, for feeding a month's worth of text into the AI summary. */
    suspend fun readAllForMonth(yearMonth: String): List<LogFile> = withContext(Dispatchers.IO) {
        logsDir.listFiles { f -> f.isFile && f.name.startsWith(yearMonth) && f.name.endsWith(".md") }
            ?.filter { it.length() > 0L }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                val dateKey = file.name.removeSuffix(".md")
                runCatching {
                    LogFile(dateKey, dateLabelFor(dateKey), file.readText(), file.lastModified())
                }.getOrNull()
            }
            ?: emptyList()
    }

    /** Word count for a given day's file — powers the month-grid dot sizing. Returns 0 if nothing written. */
    suspend fun wordCount(dateKey: String): Int = withContext(Dispatchers.IO) {
        val file = File(logsDir, "$dateKey.md")
        if (!file.exists()) return@withContext 0
        file.readText().trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}
