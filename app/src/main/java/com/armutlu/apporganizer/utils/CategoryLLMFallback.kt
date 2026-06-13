package com.armutlu.apporganizer.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CategoryLLMFallback {

    private const val DEEPSEEK_ENDPOINT = "https://api.deepseek.com/chat/completions"
    private const val BATCH_SIZE = 15

    private val VALID_CATEGORIES = setOf(
        "social", "productivity", "games", "shopping", "news", "health",
        "finance", "education", "utilities", "travel", "entertainment",
        "food", "photography", "other"
    )

    /**
     * Verilen uygulamalari DeepSeek API ile toplu kategorize eder.
     * @param apps packageName -> appName eslesme haritasi
     * @param apiKey DeepSeek API anahtari
     * @param onProgress (islenmisSayisi, toplamSayi) geri donus
     * @return packageName -> categoryId haritasi (basarisiz olanlar donmez)
     */
    suspend fun categorize(
        apps: Map<String, String>,
        apiKey: String,
        onProgress: (classified: Int, total: Int) -> Unit = { _, _ -> }
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyMap()
        val result = mutableMapOf<String, String>()
        val entries = apps.entries.toList()
        var processed = 0
        entries.chunked(BATCH_SIZE).forEach { batch ->
            runCatching {
                val batchResult = categorizeBatch(batch.associate { it.key to it.value }, apiKey)
                result.putAll(batchResult)
            }
            processed += batch.size
            onProgress(processed, entries.size)
        }
        result
    }

    private fun categorizeBatch(batch: Map<String, String>, apiKey: String): Map<String, String> {
        val appListText = batch.entries.joinToString("\n") { (pkg, name) -> "- $name ($pkg)" }
        val categoryList = VALID_CATEGORIES.joinToString(", ")
        val prompt = """
            Asagidaki Android uygulamalarini kategorize et. Her uygulama icin JSON formatinda cevap ver.

            Gecerli kategoriler: $categoryList

            Uygulamalar:
            $appListText

            Cevabi SADECE JSON array olarak ver, baska hicbir sey yazma:
            [{"pkg":"paket.adi","cat":"kategori"},...]
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 600)
            put("temperature", 0.1)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }.toString()

        val url = URL(DEEPSEEK_ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(requestBody) }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyMap()

            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val content = JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val jsonArray = JSONArray(content)
            return (0 until jsonArray.length()).associate { i ->
                val obj = jsonArray.getJSONObject(i)
                val pkg = obj.getString("pkg")
                val cat = obj.optString("cat", "other").lowercase()
                pkg to (if (cat in VALID_CATEGORIES) cat else "other")
            }
        } finally {
            connection.disconnect()
        }
    }
}
