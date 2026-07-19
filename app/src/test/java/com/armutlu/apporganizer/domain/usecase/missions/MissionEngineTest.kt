package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.utils.TaskScoreManager
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionEngine — deterministik uretim + checkProgress/evaluate senaryolari (D257, M00).
 *
 * NOT (Döngü M00 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md P0 2.4 fix):
 * H00'daki "currentBehavior_p0_..." testleri REFAKTOR ONCESI hatali davranisi (donem
 * bitmeden erken odul) kilitliyordu. Bu dongude MissionEngine.evaluate() zaman/donem
 * farkindaligi kazandi; asagidaki testler artik DOGRU davranisi dogrular.
 */
class MissionEngineTest {
    @Test
    fun behaviorChangeRewardsAreGreaterThanViewingReward() {
        assertTrue(TaskScoreManager.EventType.ClassificationApproved.delta > TaskScoreManager.EventType.NotificationReportViewed.delta)
        assertTrue(TaskScoreManager.EventType.FolderSuggestionAccepted.delta > TaskScoreManager.EventType.NotificationReportViewed.delta)
    }

    @Test
    fun `same epochDay generates identical daily missions`() {
        val day = 20_650L
        val selection = MissionEngine.MissionSelectionInput(
            checkInput = MissionEngine.MissionCheckInput(
                screenTimeMinutesToday = 120L,
                usedAfter23Today = false,
                unlockCountToday = 10,
            )
        )
        val first = MissionEngine.generateDaily(day, selection)
        val second = MissionEngine.generateDaily(day, selection)

        assertEquals(MissionEngine.DAILY_MISSION_COUNT, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first.all { it.type == MissionEngine.MissionType.DAILY })
        assertTrue(first.all { it.starReward == MissionEngine.DAILY_STAR })
        // Ayni id iki kez secilmez
        assertEquals(first.size, first.map { it.id }.distinct().size)
    }

    @Test
    fun `weekly missions carry double star reward`() {
        val weekly = MissionEngine.generateWeekly(
            epochWeek = 2_950L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    weeklyScreenTimeMinutes = 500L,
                    previousWeeklyScreenTimeMinutes = 700L,
                )
            )
        )

        assertEquals(2, weekly.size)
        assertTrue(weekly.all { it.type == MissionEngine.MissionType.WEEKLY })
        assertTrue(weekly.all { it.starReward == MissionEngine.WEEKLY_STAR })
    }

    @Test
    fun `daily selection skips missions whose required signal is unavailable`() {
        val missions = MissionEngine.generateDaily(
            epochDay = 20_651L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    screenTimeMinutesToday = null,
                    usedAfter23Today = null,
                    unlockCountToday = null,
                )
            )
        )

        assertFalse(missions.any { it.id == MissionEngine.DAILY_SCREEN_UNDER_3H })
        assertFalse(missions.any { it.id == MissionEngine.DAILY_NO_LATE_NIGHT })
        assertFalse(missions.any { it.id == MissionEngine.DAILY_UNLOCK_UNDER_30 })
        assertTrue(missions.any { it.id == MissionEngine.DAILY_CLASSIFICATION_CLEANUP })
        assertTrue(missions.any { it.id == MissionEngine.DAILY_VIEW_NOTIF_REPORT })
    }

    @Test
    fun `daily selection honors cooldown when enough alternatives exist`() {
        val missions = MissionEngine.generateDaily(
            epochDay = 20_652L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    screenTimeMinutesToday = 90L,
                    usedAfter23Today = false,
                    unlockCountToday = 8,
                ),
                recentlyCompletedMissionIds = setOf(MissionEngine.DAILY_SCREEN_UNDER_3H)
            )
        )

        assertEquals(MissionEngine.DAILY_MISSION_COUNT, missions.size)
        assertFalse(missions.any { it.id == MissionEngine.DAILY_SCREEN_UNDER_3H })
    }

    @Test
    fun `screen time under 3 hours completes daily mission only after day ends, over fails immediately`() {
        // M00: ust sinir gorevi donem bitmeden COMPLETED olamaz -> gun surerken IN_PROGRESS,
        // gun bittiyse (dayEnded=true) ve hedefin altindaysa COMPLETED. Hedef asilirsa
        // (>=180) donem bitmesini beklemeden FAILED.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertEquals(
            MissionStatus.IN_PROGRESS,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 120L), dayEnded = false).status
        )
        assertEquals(
            MissionStatus.COMPLETED,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 120L), dayEnded = true).status
        )
        assertEquals(
            MissionStatus.FAILED,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 200L), dayEnded = false).status
        )
        // Veri yoksa (izin verilmemis) uydurma basari yok
        assertEquals(
            MissionStatus.DATA_UNAVAILABLE,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = null), dayEnded = false).status
        )
    }

    @Test
    fun `weekly screen less requires valid previous baseline and week end for completion`() {
        // M00: haftalik karsilastirma hafta bitmeden COMPLETED olamaz -> IN_PROGRESS.
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_SCREEN_LESS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        val improvingInput = MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 500L, previousWeeklyScreenTimeMinutes = 700L)
        assertEquals(MissionStatus.IN_PROGRESS, MissionEngine.evaluate(mission, improvingInput, weekEnded = false).status)
        assertEquals(MissionStatus.COMPLETED, MissionEngine.evaluate(mission, improvingInput, weekEnded = true).status)

        val worseInput = MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 800L, previousWeeklyScreenTimeMinutes = 700L)
        assertEquals(MissionStatus.FAILED, MissionEngine.evaluate(mission, worseInput, weekEnded = true).status)

        // Baseline 0 (ilk hafta) — sahte basari verilmez
        val noBaselineInput = MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 100L, previousWeeklyScreenTimeMinutes = 0L)
        assertEquals(MissionStatus.DATA_UNAVAILABLE, MissionEngine.evaluate(mission, noBaselineInput, weekEnded = true).status)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `classification cleanup mission needs a real classification action`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_CLASSIFICATION_CLEANUP, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput()))
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(
                    taskEvents = MissionEngine.TaskEventInput(classificationActionsToday = 1)
                ),
            )
        )
    }

    @Test
    fun `no late night mission resolves only after 23h00 with hourly data confirming no usage`() {
        // M00: gece gorevi zaman farkindalidir - 23:00 sonrasi degerlendirilebilir.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val afterNight = LocalTime.of(23, 30)
        assertEquals(
            MissionStatus.SAFE,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(usedAfter23Today = false), now = afterNight).status
        )
        assertEquals(
            MissionStatus.FAILED,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(usedAfter23Today = true), now = afterNight).status
        )
        assertEquals(
            MissionStatus.DATA_UNAVAILABLE,
            MissionEngine.evaluate(mission, MissionEngine.MissionCheckInput(usedAfter23Today = null), now = afterNight).status
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun `weekly positive actions needs three real positive events`() {
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_POSITIVE_ACTIONS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(taskEvents = MissionEngine.TaskEventInput(positiveEventsThisWeek = 2)),
            )
        )
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(taskEvents = MissionEngine.TaskEventInput(positiveEventsThisWeek = 3)),
            )
        )
    }

    // ── M00: donemsel gorevlerde erken odul kaldirildi (P0 2.4 fix) ─────────────
    // MissionEngine.evaluate() artik zaman/donem farkindalidir: ust sinir ve haftalik
    // karsilastirma gorevleri donem bitmeden (dayEnded/weekEnded=false) COMPLETED
    // DONEMEZ, sadece IN_PROGRESS/AT_RISK/SAFE gibi ara durumlar doner. MissionsViewModel
    // sadece status == COMPLETED oldugunda yildiz yazar; bu yuzden asagidaki testler
    // "donem surerken odul yok" davranisini dogrudan evaluate() uzerinden kanitlar.

    @Test
    fun `morning screen time under target stays in progress without reward`() {
        // Sabah ekran suresi 90 dakika, gunluk hedef "3 saatin (180dk) altinda kal".
        // Gun henuz bitmedi (dayEnded=false) - dogru davranis IN_PROGRESS, COMPLETED DEGIL.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val morningInput = MissionEngine.MissionCheckInput(screenTimeMinutesToday = 90L)

        val result = MissionEngine.evaluate(mission, morningInput, dayEnded = false)

        assertEquals(MissionStatus.IN_PROGRESS, result.status)
        assertEquals(90L, result.currentValue)
        assertEquals(180L, result.targetValue)
    }

    @Test
    fun `screen time at target upper limit fails even before day ends`() {
        // 180/180 dakika -> ust sinire ULASILDI (asildi degil ama esit) - FAILED, donem
        // bitmesini beklemeye gerek yok cunku kural zaten bozuldu.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val input = MissionEngine.MissionCheckInput(screenTimeMinutesToday = 180L)

        val result = MissionEngine.evaluate(mission, input, dayEnded = false)

        assertEquals(MissionStatus.FAILED, result.status)
        assertEquals("UPPER_LIMIT_EXCEEDED", result.failureReasonCode)
    }

    @Test
    fun `night mission at 20h00 is not started yet`() {
        // Saat 20:00'de gece gorevi henuz baslamamis olmali (23:00 esigi gecilmedi).
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val result = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(usedAfter23Today = false),
            now = LocalTime.of(20, 0),
        )

        assertEquals(MissionStatus.NOT_STARTED, result.status)
    }

    @Test
    fun `night mission at 23h30 with no usage is safe but not yet rewarded`() {
        // Saat 23:30, 23:00'ten sonra kullanim yok -> SAFE (gun sonuna kadar odul yok,
        // COMPLETED sadece dayEnded=true oldugunda gelir).
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val result = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(usedAfter23Today = false),
            now = LocalTime.of(23, 30),
            dayEnded = false,
        )

        assertEquals(MissionStatus.SAFE, result.status)

        val settled = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(usedAfter23Today = false),
            now = LocalTime.of(23, 59),
            dayEnded = true,
        )
        assertEquals(MissionStatus.COMPLETED, settled.status)
    }

    @Test
    fun `night mission with late usage fails regardless of day end`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val result = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(usedAfter23Today = true),
            now = LocalTime.of(23, 30),
            dayEnded = false,
        )

        assertEquals(MissionStatus.FAILED, result.status)
        assertEquals("LATE_NIGHT_USAGE_DETECTED", result.failureReasonCode)
    }

    @Test
    fun `weekly comparison mid week stays in progress without reward`() {
        // Bu haftanin ekran suresi simdiye kadar 60dk, gecen hafta TOPLAM 1000dk idi.
        // Hafta henuz bitmedi (weekEnded=false) -> dogru davranis IN_PROGRESS.
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_SCREEN_LESS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        val midWeekInput = MissionEngine.MissionCheckInput(
            weeklyScreenTimeMinutes = 60L,
            previousWeeklyScreenTimeMinutes = 1000L,
        )

        val result = MissionEngine.evaluate(mission, midWeekInput, weekEnded = false)

        assertEquals(MissionStatus.IN_PROGRESS, result.status)

        val settled = MissionEngine.evaluate(mission, midWeekInput, weekEnded = true)
        assertEquals(MissionStatus.COMPLETED, settled.status)
    }

    @Test
    fun `checkProgress bridge only returns true when evaluate resolves to COMPLETED`() {
        // Kopru sozlesmesi: donemsel gorev donem surerken artik true DONMEMELI (P0 2.4 fix).
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val morningInput = MissionEngine.MissionCheckInput(screenTimeMinutesToday = 90L)

        @Suppress("DEPRECATION")
        val result = MissionEngine.checkProgress(mission, morningInput)

        assertFalse(
            "M00 fix: checkProgress kopru fonksiyonu artik donem bitmeden true DONMEMELI",
            result
        )
    }

    // ── P0 2.5: haftalik sinir epochDay / 7 kullaniyor (ISO/takvim haftasi degil) ──

    @Test
    fun `currentBehavior_p0_epochWeekBoundaryDoesNotAlignWithMonday`() {
        // epochDay=4 -> 1970-01-05 Pazartesi (epochDay/7 = 0). epochDay=3 -> 1970-01-04 Pazar,
        // yani AYNI takvim haftasinin son gunu degil, bir ONCEKI hafta (epochDay/7 = 0 de).
        // epochDay/7 sinirlari GERCEK Pazartesi-Pazar sinirlariyla ORTUSMEYEBILIR cunku
        // epochDay=0 (1970-01-01) bir Persembe'dir - epochDay/7 bloklari Persembe'den baslar,
        // Pazartesi'den degil.
        val thursdayEpochDay = 0L // 1970-01-01 Persembe - epochDay/7 bolgesinin baslangici
        val nextWednesdayEpochDay = 6L // 1970-01-07 Carsamba - hala ayni epochDay/7=0 bloğunda

        val thursdayWeek = thursdayEpochDay / 7
        val nextWednesdayWeek = nextWednesdayEpochDay / 7

        // P0 kaniti: Persembe (1970-01-01) ile ayni "hafta" olarak sayilan bir sonraki Carsamba
        // (1970-01-07) GERCEKTE takvimde IKI FARKLI ISO haftasindadir (Persembe -> hafta 1,
        // ertesi hafta Carsamba -> hafta 2), ama epochDay/7 HER IKISINI DE "0" bloguna koyar.
        assertEquals(
            "epochDay/7 bloklama Persembe baslangicli, Pazartesi degil - " +
                "bu yuzden 1970-01-01 (Per) ve 1970-01-07 (Car) ayni epochWeek'e dusuyor",
            thursdayWeek, nextWednesdayWeek
        )

        // Ayrica dogrulama: Pazartesi (epochDay=4, 1970-01-05) farkli bir epochWeek blogunda
        // degil, HALA ayni blokta (4/7=0) - yani "hafta" Pazartesi'de baslamiyor, Persembe'de basliyor.
        val mondayEpochDay = 4L
        assertEquals(0L, mondayEpochDay / 7)
        assertEquals(0L, thursdayEpochDay / 7)
    }

    @Test
    fun `currentBehavior_p0_weeklyMissionGeneratedWithThursdayStartingEpochWeek`() {
        // generateWeekly epochWeek parametresini dogrudan epochDay/7 olarak alir (MissionsViewModel
        // satir 76: "val epochWeek = epochDay / 7"). Bu test, Persembe gunku bir epochDay'den
        // turetilen epochWeek ile ayni "blok" icindeki bir sonraki Carsamba gununden turetilen
        // epochWeek'in AYNI gorev setini urettigini (cunku ikisi de epochWeek=0) gosterir -
        // kullanicinin bekledigi Pazartesi-Pazar takvim haftasindan FARKLI bir sinirlama.
        val thursdayEpochWeek = 0L / 7   // 1970-01-01 Persembe
        val wednesdayEpochWeek = 6L / 7  // 1970-01-07 Carsamba (ayni epochDay/7 blogu)

        val selection = MissionEngine.MissionSelectionInput(
            checkInput = MissionEngine.MissionCheckInput(
                weeklyScreenTimeMinutes = 300L,
                previousWeeklyScreenTimeMinutes = 400L,
            )
        )
        val thursdayMissions = MissionEngine.generateWeekly(thursdayEpochWeek, selection)
        val wednesdayMissions = MissionEngine.generateWeekly(wednesdayEpochWeek, selection)

        assertEquals(thursdayEpochWeek, wednesdayEpochWeek)
        assertEquals(thursdayMissions.map { it.id }, wednesdayMissions.map { it.id })
    }

    // ── Dongu G1: kisisel hedef enjeksiyonu ─────────────────────────────────────────────
    @Test
    fun `personal screen target overrides default 180 minute limit`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        // Kisisel hedef 160dk: 150dk kullanim hedefin altinda ama >=%80 -> AT_RISK (M00 kurali)
        val underPersonal = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(screenTimeMinutesToday = 150L, personalScreenTargetMinutes = 160L),
            dayEnded = false,
        )
        assertEquals(MissionStatus.AT_RISK, underPersonal.status)
        assertEquals(160L, underPersonal.targetValue)

        // Ayni 150dk sabit varsayilan (180dk) ile degerlendirilseydi de IN_PROGRESS olurdu ama
        // burada asil nokta: kisisel hedef asilinca (170 >= 160) donem bitmeden FAILED olmasi -
        // sabit 180dk varsayilaninda bu deger hala basarili sayilirdi.
        val overPersonal = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(screenTimeMinutesToday = 170L, personalScreenTargetMinutes = 160L),
            dayEnded = false,
        )
        assertEquals(MissionStatus.FAILED, overPersonal.status)
    }

    @Test
    fun `null personal target falls back to fixed default`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val evaluation = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(screenTimeMinutesToday = 170L, personalScreenTargetMinutes = null),
            dayEnded = false,
        )
        assertEquals(MissionEngine.DEFAULT_SCREEN_TARGET_MINUTES, evaluation.targetValue)
        // 170/180 = %94 kullanim >= %80 -> AT_RISK (M00 kurali); asil dogrulama targetValue fallback'i
        assertEquals(MissionStatus.AT_RISK, evaluation.status)
    }

    @Test
    fun `personal unlock target overrides default 30 count limit`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_UNLOCK_UNDER_30, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val evaluation = MissionEngine.evaluate(
            mission,
            MissionEngine.MissionCheckInput(unlockCountToday = 20, personalUnlockTarget = 18L),
            dayEnded = false,
        )
        assertEquals(MissionStatus.FAILED, evaluation.status)
        assertEquals(18L, evaluation.targetValue)
    }
}
