package com.armutlu.apporganizer.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.domain.usecase.notification.NotificationClassifierUseCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smart Notification Engine CPU mikrobenchmark'ı.
 *
 * Ölçülen yol:
 * - metin/paket normalizasyonu
 * - kategori eşleştirme ve öncelik çatışmaları
 * - hassaslık, önem puanı ve bastırma kararı
 * - SmartNotification sonuç nesnesi üretimi
 *
 * Android Notification extras ayrıştırması ve Room I/O bu sınıfa dahil değildir. Aktif snapshot
 * benchmark'ları, listener'ın aynı callback içinde sınıflandırdığı 10/50/100 kayıtlık CPU yükünü
 * temsil eder.
 *
 * Gerçek cihazda çalıştırma:
 *
 * .\gradlew :benchmark:connectedAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.benchmark.SmartNotificationEngineBenchmark
 *
 * Sonuç alınmadan "1 ms altında" veya benzeri performans iddiası tamamlandı kabul edilmez.
 */
@RunWith(AndroidJUnit4::class)
class SmartNotificationEngineBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val classifier = NotificationClassifierUseCase()
    private val fixtures = buildFixtures(100)

    @Test
    fun classifySingleNotification() = benchmarkRule.measureRepeated {
        val fixture = fixtures[0]
        scoreSink = classifier.classify(
            key = fixture.key,
            packageName = fixture.packageName,
            title = fixture.title,
            text = fixture.text,
            timestamp = fixture.timestamp,
            systemPriority = fixture.systemPriority,
        ).importanceScore
    }

    @Test
    fun classifyActiveSnapshot10() = measureBatch(10)

    @Test
    fun classifyActiveSnapshot50() = measureBatch(50)

    @Test
    fun classifyActiveSnapshot100() = measureBatch(100)

    private fun measureBatch(size: Int) = benchmarkRule.measureRepeated {
        var checksum = 0
        for (index in 0 until size) {
            val fixture = fixtures[index]
            val result = classifier.classify(
                key = fixture.key,
                packageName = fixture.packageName,
                title = fixture.title,
                text = fixture.text,
                timestamp = fixture.timestamp,
                systemPriority = fixture.systemPriority,
            )
            checksum += result.importanceScore
            if (result.shouldSuppress) checksum += 1
        }
        scoreSink = checksum
    }

    private data class Fixture(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val systemPriority: Int,
    )

    private companion object {
        @Volatile
        var scoreSink: Int = 0

        fun buildFixtures(size: Int): List<Fixture> {
            val templates = listOf(
                Triple("com.whatsapp", "Ayşe", "Toplantı tamamlandı, yarın görüşürüz"),
                Triple("com.akbank.android.apps.akbank_direkt", "Güvenlik", "Giriş kodunuz 123456"),
                Triple("com.trendyol", "Kargonuz yolda", "Siparişiniz dağıtıma çıktı"),
                Triple("com.shop.app", "Kampanya", "Yüzde 50 indirim kuponu seni bekliyor"),
                Triple("com.google.android.calendar", "Hatırlatıcı", "Randevunuz 10 dakika sonra"),
                Triple("com.instagram.android", "Yeni etkileşim", "Gönderini beğendi"),
                Triple("com.android.systemui", "Sistem güncellemesi", "Güncelleme indirilmeye hazır"),
                Triple("com.example.unknown", "Bilgi", "Yeni bir bildiriminiz var"),
                Triple("com.bank.mobile", "Ödeme", "Kartınızdan 1.250 TL ödeme yapıldı"),
                Triple("com.delivery.app", "Teslim edildi", "Paketiniz teslim edildi"),
            )
            return List(size) { index ->
                val template = templates[index % templates.size]
                Fixture(
                    key = "benchmark-$index",
                    packageName = template.first,
                    title = template.second,
                    text = template.third,
                    timestamp = 1_800_000_000_000L + index,
                    systemPriority = (index % 5) - 2,
                )
            }
        }
    }
}
