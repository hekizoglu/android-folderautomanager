package com.armutlu.apporganizer.domain.usecase.classify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek API ile bilinmeyen paketleri kategorilere atar.
 * - try/catch: hata durumunda CAT_OTHER döner
 * - withTimeout(10_000L): 10 sn üstünde CAT_OTHER
 * - Cache: aynı paketi tekrar sorgulamaz
 */
@Singleton
class CategoryLLMFallback @Inject constructor() {

    // packageName → categoryId cache (uygulama yaşam süresi boyunca)
    private val cache = mutableMapOf<String, String>()

    /**
     * Tek paket için kategori döndürür.
     * Her zaman güvenli: exception fırlatmaz, timeout aşılırsa CAT_OTHER.
     */
    suspend fun classify(packageName: String, apiKey: String): String {
        cache[packageName]?.let { return it }

        return try {
            withTimeout(10_000L) {
                val result = callDeepSeek(listOf(packageName), apiKey)
                val category = result[packageName] ?: AppClassifier.CAT_OTHER
                cache[packageName] = category
                category
            }
        } catch (e: Exception) {
            Timber.w(e, "LLM fallback failed for $packageName, defaulting to CAT_OTHER")
            AppClassifier.CAT_OTHER
        }
    }

    /**
     * Batch sınıflandırma — birden fazla paketi tek API çağrısında sınıflandırır (max 15).
     */
    suspend fun classifyBatch(packageNames: List<String>, apiKey: String): Map<String, String> {
        val uncached = packageNames.filter { !cache.containsKey(it) }
        val results = mutableMapOf<String, String>()

        // Cache hit'leri ekle
        packageNames.forEach { pkg ->
            cache[pkg]?.let { results[pkg] = it }
        }

        if (uncached.isEmpty()) return results

        // Batch'lere böl (max 15)
        uncached.chunked(15).forEach { batch ->
            try {
                withTimeout(10_000L) {
                    val batchResult = callDeepSeek(batch, apiKey)
                    batch.forEach { pkg ->
                        val category = batchResult[pkg] ?: AppClassifier.CAT_OTHER
                        cache[pkg] = category
                        results[pkg] = category
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "LLM batch fallback failed for ${batch.size} packages")
                batch.forEach { pkg ->
                    cache[pkg] = AppClassifier.CAT_OTHER
                    results[pkg] = AppClassifier.CAT_OTHER
                }
            }
        }

        return results
    }

    private suspend fun callDeepSeek(packageNames: List<String>, apiKey: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            val prompt = buildPrompt(packageNames)
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.0)
                put("max_tokens", 512)
            }.toString()

            val url = URL("https://api.deepseek.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody) }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Timber.w("DeepSeek API returned $responseCode")
                return@withContext packageNames.associateWith { AppClassifier.CAT_OTHER }
            }

            val response = conn.inputStream.bufferedReader().readText()
            parseResponse(response, packageNames)
        }

    private fun parseResponse(response: String, packageNames: List<String>): Map<String, String> {
        return try {
            val json = JSONObject(response)
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            // Beklenen format: "com.example.app=CAT_GAMES\ncom.foo.bar=CAT_SOCIAL"
            val result = mutableMapOf<String, String>()
            content.lines().forEach { line ->
                val parts = line.trim().split("=")
                if (parts.size == 2) {
                    val pkg = parts[0].trim()
                    val cat = parts[1].trim()
                    if (pkg in packageNames && cat.startsWith("CAT_")) {
                        result[pkg] = cat
                    }
                }
            }
            // Eksik paketlere CAT_OTHER
            packageNames.forEach { pkg ->
                if (!result.containsKey(pkg)) result[pkg] = AppClassifier.CAT_OTHER
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse DeepSeek response")
            packageNames.associateWith { AppClassifier.CAT_OTHER }
        }
    }

    private fun buildPrompt(packageNames: List<String>): String {
        val list = packageNames.joinToString("\n")
        return """
Classify the following Android package names into ONE of these categories:
CAT_GAMES, CAT_SOCIAL, CAT_COMMUNICATION, CAT_FINANCE, CAT_HEALTH, CAT_SHOPPING,
CAT_TRAVEL, CAT_MUSIC, CAT_VIDEO, CAT_PHOTO, CAT_PRODUCTIVITY, CAT_TOOLS,
CAT_EDUCATION, CAT_NEWS, CAT_FOOD, CAT_SPORTS, CAT_MAPS, CAT_WEATHER,
CAT_BOOKS, CAT_DATING, CAT_BUSINESS, CAT_AUTO, CAT_LIFESTYLE, CAT_ART,
CAT_SECURITY, CAT_PARENTING, CAT_ACCESSIBILITY, CAT_DEVELOPER, CAT_RELIGION,
CAT_GOVERNMENT, CAT_ENTERTAINMENT, CAT_OTHER

Reply with ONLY lines in format: packageName=CAT_XXX
No explanations, no markdown.

Packages:
$list
        """.trimIndent()
    }
}
