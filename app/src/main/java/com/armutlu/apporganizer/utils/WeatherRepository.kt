package com.armutlu.apporganizer.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object WeatherRepository {

    data class HourlyTemp(
        val hourLabel: String,
        val tempC: Int,
    )

    data class Snapshot(
        val locationLabel: String,
        val currentTempC: Int,
        val conditionLabel: String,
        val conditionEmoji: String,
        val minTempC: Int,
        val maxTempC: Int,
        val hourly: List<HourlyTemp>,
        val fetchedAt: Long,
        val isStale: Boolean,
    ) {
        val fetchedAtLabel: String
            get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fetchedAt))
    }

    sealed interface Result {
        data class Success(val snapshot: Snapshot) : Result
        data object Disabled : Result
        data object MissingSetup : Result
        data class Error(val message: String) : Result
    }

    private const val PREFS_NAME = "weather_cache_prefs"
    private const val KEY_CACHE_JSON = "weather_cache_json"
    private const val KEY_CACHE_TARGET = "weather_cache_target"
    private const val CACHE_TTL_MS = 45L * 60L * 1000L
    internal const val CACHE_TTL_FOR_TEST_MS = CACHE_TTL_MS

    suspend fun getWeather(context: Context): Result = withContext(Dispatchers.IO) {
        if (!AppPrefs.isHomeWeatherEnabled(context)) return@withContext Result.Disabled
        val target = resolveTarget(context) ?: return@withContext Result.MissingSetup
        val cached = loadCached(context, target.cacheKey)
        if (cached != null && !cached.isStale) {
            return@withContext Result.Success(cached)
        }

        val fetched = runCatching { fetchSnapshot(target) }.getOrNull()
        if (fetched != null) {
            saveCache(context, target.cacheKey, fetched)
            return@withContext Result.Success(fetched)
        }
        if (cached != null) return@withContext Result.Success(cached.copy(isStale = true))
        Result.Error("Weather fetch failed")
    }

    private data class WeatherTarget(
        val latitude: Double,
        val longitude: Double,
        val label: String,
        val cacheKey: String,
    )

    private suspend fun resolveTarget(context: Context): WeatherTarget? {
        if (AppPrefs.isHomeWeatherUseLocation(context)) {
            lastKnownLocation(context)?.let { location ->
                val label = reverseGeocodeLabel(context, location)
                    ?: AppPrefs.getHomeWeatherManualCity(context)
                        .takeIf { it.isNotBlank() }
                    ?: "Yakindaki hava"
                return WeatherTarget(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    label = label,
                    cacheKey = "loc:${"%.2f".format(Locale.US, location.latitude)},${"%.2f".format(Locale.US, location.longitude)}",
                )
            }
        }

        val manualCity = AppPrefs.getHomeWeatherManualCity(context).trim()
        if (manualCity.isBlank()) return null
        val geocoded = geocodeCity(manualCity) ?: return null
        return WeatherTarget(
            latitude = geocoded.first,
            longitude = geocoded.second,
            label = manualCity,
            cacheKey = "city:${manualCity.lowercase(Locale.ROOT)}",
        )
    }

    private fun lastKnownLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = runCatching { locationManager.getProviders(true) }.getOrDefault(emptyList())
        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private suspend fun reverseGeocodeLabel(context: Context, location: Location): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        continuation.resume(addresses.firstOrNull()?.locality ?: addresses.firstOrNull()?.subAdminArea)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea }
            }
        }.getOrNull()
    }

    private suspend fun geocodeCity(city: String): Pair<Double, Double>? {
        val encoded = URLEncoder.encode(city, Charsets.UTF_8.name())
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
        val root = JSONObject(httpGet(url))
        val results = root.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.getJSONObject(0)
        return first.getDouble("latitude") to first.getDouble("longitude")
    }

    private fun fetchSnapshot(target: WeatherTarget): Snapshot {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=${target.latitude}")
            append("&longitude=${target.longitude}")
            append("&current=temperature_2m,weather_code")
            append("&hourly=temperature_2m")
            append("&daily=temperature_2m_max,temperature_2m_min,weather_code")
            append("&timezone=auto")
            append("&forecast_days=2")
        }
        val root = JSONObject(httpGet(url))
        val current = root.getJSONObject("current")
        val hourly = root.getJSONObject("hourly")
        val daily = root.getJSONObject("daily")

        val currentTemp = current.optDouble("temperature_2m").toInt()
        val currentCode = current.optInt("weather_code")
        val currentTime = current.optString("time")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val startIndex = findHourlyStartIndex(times, currentTime)
        val hourlyTemps = buildHourlyList(times, temps, startIndex)

        return Snapshot(
            locationLabel = target.label,
            currentTempC = currentTemp,
            conditionLabel = weatherLabel(currentCode),
            conditionEmoji = weatherEmoji(currentCode),
            minTempC = daily.getJSONArray("temperature_2m_min").optDouble(0).toInt(),
            maxTempC = daily.getJSONArray("temperature_2m_max").optDouble(0).toInt(),
            hourly = hourlyTemps,
            fetchedAt = System.currentTimeMillis(),
            isStale = false,
        )
    }

    private fun buildHourlyList(times: JSONArray, temps: JSONArray, startIndex: Int): List<HourlyTemp> {
        return buildHourlyListFromArrays(
            times = List(times.length()) { index -> times.optString(index) },
            temps = List(temps.length()) { index -> temps.optDouble(index).toInt() },
            startIndex = startIndex,
        )
    }

    private fun findHourlyStartIndex(times: JSONArray, currentTime: String): Int {
        for (index in 0 until times.length()) {
            if (times.optString(index) == currentTime) return index
        }
        return 0
    }

    private fun saveCache(context: Context, cacheKey: String, snapshot: Snapshot) {
        val json = encodeSnapshot(snapshot)
        prefs(context).edit()
            .putString(KEY_CACHE_TARGET, cacheKey)
            .putString(KEY_CACHE_JSON, json)
            .apply()
    }

    private fun loadCached(context: Context, cacheKey: String): Snapshot? {
        val prefs = prefs(context)
        if (prefs.getString(KEY_CACHE_TARGET, null) != cacheKey) return null
        val raw = prefs.getString(KEY_CACHE_JSON, null) ?: return null
        return decodeSnapshot(raw, System.currentTimeMillis())
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "Acik"
        1, 2, 3 -> "Parcali bulutlu"
        45, 48 -> "Sis"
        51, 53, 55, 56, 57 -> "Cisenti"
        61, 63, 65, 80, 81, 82 -> "Yagmur"
        66, 67 -> "Donan yagmur"
        71, 73, 75, 77, 85, 86 -> "Kar"
        95, 96, 99 -> "Firtina"
        else -> "Hava"
    }

    private fun weatherEmoji(code: Int): String = when (code) {
        0 -> "☀"
        1, 2 -> "⛅"
        3 -> "☁"
        45, 48 -> "🌫"
        51, 53, 55, 56, 57 -> "🌦"
        61, 63, 65, 80, 81, 82 -> "🌧"
        66, 67 -> "🌧"
        71, 73, 75, 77, 85, 86 -> "❄"
        95, 96, 99 -> "⛈"
        else -> "🌤"
    }
    internal fun buildHourlyListFromArrays(
        times: List<String>,
        temps: List<Int>,
        startIndex: Int,
    ): List<HourlyTemp> {
        if (times.isEmpty() || temps.isEmpty()) return emptyList()
        val safeStart = startIndex.coerceAtLeast(0).coerceAtMost(minOf(times.lastIndex, temps.lastIndex))
        val endExclusive = minOf(times.size, temps.size, safeStart + 6)
        return (safeStart until endExclusive).map { index ->
            HourlyTemp(
                hourLabel = times[index].substringAfter("T").take(2),
                tempC = temps[index],
            )
        }
    }

    internal fun encodeSnapshot(snapshot: Snapshot): String {
        val hourly = JSONArray().apply {
            snapshot.hourly.forEach { item ->
                put(
                    JSONObject().apply {
                        put("hour", item.hourLabel)
                        put("temp", item.tempC)
                    }
                )
            }
        }
        return JSONObject().apply {
            put("locationLabel", snapshot.locationLabel)
            put("currentTempC", snapshot.currentTempC)
            put("conditionLabel", snapshot.conditionLabel)
            put("conditionEmoji", snapshot.conditionEmoji)
            put("minTempC", snapshot.minTempC)
            put("maxTempC", snapshot.maxTempC)
            put("fetchedAt", snapshot.fetchedAt)
            put("hourly", hourly)
        }.toString()
    }

    internal fun decodeSnapshot(raw: String, nowMillis: Long): Snapshot? = runCatching {
        val root = JSONObject(raw)
        val fetchedAt = root.optLong("fetchedAt", 0L)
        val hourly = root.optJSONArray("hourly") ?: JSONArray()
        Snapshot(
            locationLabel = root.optString("locationLabel"),
            currentTempC = root.optInt("currentTempC"),
            conditionLabel = root.optString("conditionLabel"),
            conditionEmoji = root.optString("conditionEmoji"),
            minTempC = root.optInt("minTempC"),
            maxTempC = root.optInt("maxTempC"),
            hourly = (0 until hourly.length()).map { index ->
                val item = hourly.getJSONObject(index)
                HourlyTemp(
                    hourLabel = item.optString("hour"),
                    tempC = item.optInt("temp"),
                )
            },
            fetchedAt = fetchedAt,
            isStale = isCacheStale(fetchedAt = fetchedAt, nowMillis = nowMillis),
        )
    }.getOrNull()

    internal fun isCacheStale(fetchedAt: Long, nowMillis: Long): Boolean =
        nowMillis - fetchedAt > CACHE_TTL_MS
}
