package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek API ile bilinmeyen paketleri kategorilere atar.
 * - try/catch: hata durumunda CAT_OTHER döner
 * - withTimeout(10_000L): 10 sn üstünde CAT_OTHER
 * - Cache: aynı paketi tekrar sorgulamaz (in-memory + AppPrefs'te kalıcı, K1 Dongu 227)
 */
@Singleton
class CategoryLLMFallback @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // packageName → categoryId cache (uygulama yaşam süresi boyunca)
    // ConcurrentHashMap: classify() ve classifyBatch() paralel çağrılsa bile thread-safe
    // Ilk deger AppPrefs'teki kalici cache'den yuklenir — yeniden baslatmada DeepSeek'e tekrar gidilmez.
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>(AppPrefs.getLlmCategoryCache(context))

    /**
     * Batch sınıflandırma — birden fazla paketi tek API çağrısında sınıflandırır (max 15).
     */
    suspend fun classifyBatch(packageNames: List<String>, apiKey: String): Map<String, String> {
        // Only normalized values are valid cache hits. Old/invalid cache values must
        // be retried instead of silently disappearing from the batch result.
        val uncached = packageNames.filter { normalizeCategoryId(cache[it]) == null }
        val results = mutableMapOf<String, String>()

        // Cache hit'lerini gerçek Room kategori ID'sine normalize et.
        packageNames.forEach { pkg ->
            cache[pkg]?.let { rawCategory ->
                normalizeCategoryId(rawCategory)?.let { results[pkg] = it }
            }
        }

        if (uncached.isEmpty()) return results

        // Batch'lere böl (max 15)
        uncached.chunked(15).forEach { batch ->
            try {
                withTimeout(10_000L) {
                    val batchResult = callDeepSeek(batch, apiKey)
                    val toPersist = mutableMapOf<String, String>()
                    batch.forEach { pkg ->
                        val category = normalizeCategoryId(batchResult[pkg]) ?: Category.CAT_OTHER
                        cache[pkg] = category
                        results[pkg] = category
                        toPersist[pkg] = category
                    }
                    AppPrefs.putLlmCategoryCacheAll(context, toPersist)
                }
            } catch (e: Exception) {
                Timber.w(e, "LLM batch fallback failed for ${batch.size} packages")
                // cache'e CAT_OTHER yazma — timeout/network hatası geçici olabilir,
                // cache'e yazarsak sonraki açılışta da hatalı kategori kalır
                batch.forEach { pkg -> results[pkg] = Category.CAT_OTHER }
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
                return@withContext packageNames.associateWith { Category.CAT_OTHER }
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
                    val category = normalizeCategoryId(parts[1])
                    if (pkg in packageNames && category != null) {
                        result[pkg] = category
                    }
                }
            }
            // Eksik paketlere CAT_OTHER
            packageNames.forEach { pkg ->
                if (!result.containsKey(pkg)) result[pkg] = Category.CAT_OTHER
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse DeepSeek response")
            packageNames.associateWith { Category.CAT_OTHER }
        }
    }

    internal companion object {
        /** Converts the LLM's CAT_* wire values to the IDs used by Room and UI. */
        fun normalizeCategoryId(raw: String?): String? = when (raw?.trim()?.uppercase(Locale.ROOT)) {
            "CAT_GAMES" -> Category.CAT_GAMES
            "CAT_SOCIAL" -> Category.CAT_SOCIAL
            "CAT_COMMUNICATION" -> Category.CAT_COMMUNICATION
            "CAT_FINANCE" -> Category.CAT_FINANCE
            "CAT_HEALTH" -> Category.CAT_HEALTH
            "CAT_SHOPPING" -> Category.CAT_SHOPPING
            "CAT_TRAVEL" -> Category.CAT_TRAVEL
            "CAT_MUSIC" -> Category.CAT_MUSIC
            "CAT_VIDEO" -> Category.CAT_VIDEO
            "CAT_PHOTO", "CAT_PHOTOGRAPHY" -> Category.CAT_PHOTOGRAPHY
            "CAT_PRODUCTIVITY" -> Category.CAT_PRODUCTIVITY
            "CAT_TOOLS", "CAT_UTILITIES" -> Category.CAT_UTILITIES
            "CAT_EDUCATION" -> Category.CAT_EDUCATION
            "CAT_NEWS" -> Category.CAT_NEWS
            "CAT_FOOD" -> Category.CAT_FOOD
            "CAT_SPORTS" -> Category.CAT_SPORTS
            "CAT_MAPS" -> Category.CAT_MAPS
            "CAT_WEATHER" -> Category.CAT_WEATHER
            "CAT_BOOKS" -> Category.CAT_BOOKS
            "CAT_DATING" -> Category.CAT_DATING
            "CAT_BUSINESS" -> Category.CAT_BUSINESS
            "CAT_AUTO" -> Category.CAT_AUTO
            "CAT_LIFESTYLE" -> Category.CAT_LIFESTYLE
            "CAT_ART" -> Category.CAT_ART
            "CAT_BEAUTY" -> Category.CAT_BEAUTY
            "CAT_HOUSE" -> Category.CAT_HOUSE
            "CAT_PARENTING" -> Category.CAT_PARENTING
            "CAT_EVENTS" -> Category.CAT_EVENTS
            "CAT_COMICS" -> Category.CAT_COMICS
            "CAT_PERSONALIZATION" -> Category.CAT_PERSONALIZATION
            "CAT_ENTERTAINMENT" -> Category.CAT_ENTERTAINMENT
            "CAT_OTHER" -> Category.CAT_OTHER
            else -> null
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
