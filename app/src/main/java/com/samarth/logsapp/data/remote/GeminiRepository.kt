package com.samarth.logsapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Calls the Gemini API directly to turn a month's worth of daily logs into
 * a short reflective summary — the one feature that's actually specific to
 * *logs* rather than freeform notes. This is deliberately a plain
 * HttpURLConnection call rather than a new SDK dependency, to keep the
 * build lean.
 *
 * GEMINI_API_KEY comes from BuildConfig, same pattern as the Supabase keys
 * — injected at build time via CI secrets, never hardcoded.
 */
class GeminiRepository(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns a 3-5 sentence reflection over the given logs, or null if the
     * call fails (offline, quota, bad key, etc.) — callers should treat a
     * null result as "summary unavailable right now," never as an error
     * that blocks the rest of the screen.
     */
    suspend fun summarizeMonth(dateLabelsAndBodies: List<Pair<String, String>>): String? =
        withContext(Dispatchers.IO) {
            if (dateLabelsAndBodies.isEmpty() || apiKey.isBlank()) return@withContext null

            runCatching {
                val logsText = dateLabelsAndBodies.joinToString("\n\n") { (date, body) ->
                    "$date:\n$body"
                }

                val prompt = """
                    Here are someone's daily journal entries for one month. Write a
                    short, warm, specific 3-5 sentence reflection noticing real
                    patterns, recurring themes, or shifts in mood across the entries.
                    Do not invent details that aren't there. No headers, no bullet
                    points, just plain reflective prose, second person ("you").

                    $logsText
                """.trimIndent()

                val requestBody = buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") {
                                addJsonObject { put("text", prompt) }
                            }
                        }
                    }
                }.toString()

                val url = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 20_000
                }

                OutputStreamWriter(connection.outputStream).use { it.write(requestBody) }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) return@withContext null

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root: JsonObject = json.parseToJsonElement(responseText).jsonObject
                val candidates: JsonArray = root["candidates"]?.jsonArray ?: return@withContext null
                val firstCandidate = candidates.firstOrNull()?.jsonObject ?: return@withContext null
                val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
                parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
            }.getOrNull()
        }
}
