package com.armutlu.apporganizer.domain.usecase.wrapped

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WrappedAiCoach @Inject constructor() {

    suspend fun summarize(report: WrappedEngine.WrappedReport, apiKey: String): String? {
        if (apiKey.isBlank()) return null
        return runCatching {
            withTimeout(10_000L) {
                callDeepSeek(report, apiKey)
            }
        }.onFailure {
            Timber.w(it, "Wrapped AI coach failed")
        }.getOrNull()
    }

    private suspend fun callDeepSeek(report: WrappedEngine.WrappedReport, apiKey: String): String? =
        withContext(Dispatchers.IO) {
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", buildPrompt(report))
                    })
                })
                put("temperature", 0.4)
                put("max_tokens", 120)
            }.toString()

            val conn = (URL("https://api.deepseek.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody) }

            if (conn.responseCode != 200) {
                Timber.w("Wrapped AI coach DeepSeek returned ${conn.responseCode}")
                return@withContext null
            }

            val content = JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            content
                .lines()
                .joinToString(" ") { it.trim() }
                .replace(Regex("\\s+"), " ")
                .take(260)
                .takeIf { it.length >= 24 }
        }

    private fun buildPrompt(report: WrappedEngine.WrappedReport): String {
        val earnedBadges = report.badges.count { it.earned }
        val categoryMix = report.personality.categoryPercentages
            .filterValues { it > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString(", ") { "${it.key}:${it.value}%" }
        val growth = report.weeklyComparison?.topGrowingCategories
            ?.take(3)
            ?.joinToString(", ") { "${it.categoryId}:${it.deltaPercent}%" }
            .orEmpty()
        val reasons = report.score.reasons
            .take(3)
            .joinToString("; ") { "${it.label} (${it.delta})" }

        return """
You are a concise digital wellbeing coach. Write exactly two short Turkish sentences.
Use only this aggregate weekly report. Do not mention app names, package names, or private data.
Be supportive, practical, and non-medical.

Aggregate report:
score=${report.score.score}/100
scoreReasons=$reasons
personality=${report.personality.type.label}
dominantPercent=${report.personality.dominantPercentage}
categoryMix=$categoryMix
earnedBadges=$earnedBadges/${report.badges.size}
topGrowingCategories=$growth
        """.trimIndent()
    }
}
