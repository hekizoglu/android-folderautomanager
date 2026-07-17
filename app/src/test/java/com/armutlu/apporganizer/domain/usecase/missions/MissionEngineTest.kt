package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.utils.TaskScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionEngine — deterministik uretim + checkProgress senaryolari (D257).
 *
 * NOT (Döngü H00 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md P0 2.4 / 2.5):
 * Asagidaki "currentBehavior_p0_..." testleri REFAKTOR ONCESI mevcut (hatali) davranisi
 * kilitler; amac bu testleri kirmizi yapmak degil, mevcut hatayi görünür kilmaktir.
 * Roadmap sonraki dongude (WeekUtils / donem-farkinda checkProgress) bu testleri
 * dogru davranisa cevirecektir.
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
    fun `screen time under 3 hours completes daily mission and over does not`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertTrue(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 120L)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 200L)))
        // Veri yoksa (izin verilmemis) uydurma basari yok
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = null)))
    }

    @Test
    fun `weekly screen less requires valid previous baseline`() {
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_SCREEN_LESS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 500L, previousWeeklyScreenTimeMinutes = 700L),
            )
        )
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 800L, previousWeeklyScreenTimeMinutes = 700L),
            )
        )
        // Baseline 0 (ilk hafta) — sahte basari verilmez
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 100L, previousWeeklyScreenTimeMinutes = 0L),
            )
        )
    }

    @Test
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
    fun `no late night mission true only when hourly data confirms no usage`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertTrue(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = false)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = true)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = null)))
    }

    @Test
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

    // ── P0 2.4: donemsel gorevler erken tamamlaniyor ────────────────────────────
    // MissionEngine.checkProgress() zaman/donem farkindaligi olmayan SAF bir fonksiyondur:
    // sadece o an gecirilen anlik degeri degerlendirir, gunun/haftanin bitip bitmedigini bilmez.
    // Cagiran taraf (MissionsViewModel.computeAndAward, her ekran acilisinda refresh() ile
    // cagrilir) bu anlik sonucu DOGRUDAN "basarili" olarak isaretleyip yildiz veriyor.
    // Su an Clock/now MissionEngine'e enjekte edilemiyor (checkProgress zaman parametresi almiyor,
    // "simdi gunun/haftanin ortasi mi sonu mu" bilgisi yok) - bu yuzden "donem bitmeden odul
    // verilmemeli" davranisini checkProgress seviyesinde dogrudan test edemiyoruz.
    // Asagidaki testler MEVCUT (hatali) davranisi checkProgress girdi/cikti sozlesmesi
    // uzerinden kanitliyor: ust sinir hedefine SABAH ARADA BILE ulasilsa (gun bitmeden)
    // fonksiyon "basarili" doner - cunku gunun geri kalaninda sinirin asilip asilmayacagini
    // bilmiyor.

    @Test
    fun `currentBehavior_p0_earlyRewardGivenForUpperLimitMission`() {
        // Sabah ekran suresi 90 dakika, gunluk hedef "3 saatin (180dk) altinda kal".
        // Gun henuz bitmedi (bu bilgi checkProgress'e hic verilmiyor) ama fonksiyon
        // simdiden "basarili" donuyor - MissionsViewModel bunu aninda yildizla odullendirir.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        val morningInput = MissionEngine.MissionCheckInput(screenTimeMinutesToday = 90L)

        val result = MissionEngine.checkProgress(mission, morningInput)

        // P0: dogru davranis "gun bitmeden basari kesinlesmemeli" olurdu; mevcut kod
        // yalnizca anlik degeri esikle karsilastirdigi icin erken basari doner.
        assertTrue(
            "P0 2.4 kaniti: checkProgress zaman/donem bilgisi olmadan anlik degeri " +
                "dogrudan basari sayiyor (90dk < 180dk esigi, gun henuz bitmemis olsa bile)",
            result
        )
    }

    @Test
    fun `currentBehavior_p0_noLateNightMissionHasNoTimeOfDayAwareness`() {
        // Saat 20:00'de (gece henuz gelmedi, 23:00 saati icin veri yok) "usedAfter23Today"
        // sinyali henuz bilinemez durumda olabilir; ancak eger cagiran taraf yanlislikla
        // "simdiye kadar 23:00'te kullanim yok" der ve false gecirirse, checkProgress
        // saatin kac oldugunu HIC bilmedigi icin gunun geri kalaninda (20:00-23:59 arasi)
        // kullanici gec saatte telefona bakabilecek olsa bile "basarili" doner.
        // MissionEngine, "simdi saat kac / gun bitti mi" bilgisini enjekte edecek bir
        // Clock/now parametresi ALMAZ - bu yuzden "saat 20:00'de gece gorevi
        // odullendirilmemeli" senaryosunu checkProgress duzeyinde dogrudan simule edemiyoruz;
        // yalnizca sozlesmenin zaman-korlugunu (time-blindness) belgeliyoruz.
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        // usedAfter23Today=false her zaman "basarili" doner - gunun hangi saatinde
        // cagrildigindan BAGIMSIZ (checkProgress imzasinda saat/zaman parametresi yok).
        val result = MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = false))

        assertTrue(
            "P0 2.4 kaniti: checkProgress saat bilgisini hic almadigi icin gun bitmeden " +
                "(ornegin saat 20:00'de) de usedAfter23Today=false verilirse basari doner",
            result
        )
    }

    @Test
    fun `currentBehavior_p0_weeklyComparisonRewardsBeforeWeekEnds`() {
        // Bu haftanin ekran suresi simdiye kadar 60dk, gecen hafta TOPLAM 1000dk idi.
        // Hafta henuz bitmemis olsa bile (checkProgress bu bilgiyi bilmiyor) 60 < 1000
        // oldugu icin basari doner - MissionsViewModel bunu hafta ortasinda bile odullendirir.
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_SCREEN_LESS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        val midWeekInput = MissionEngine.MissionCheckInput(
            weeklyScreenTimeMinutes = 60L,
            previousWeeklyScreenTimeMinutes = 1000L,
        )

        val result = MissionEngine.checkProgress(mission, midWeekInput)

        assertTrue(
            "P0 2.4 kaniti: hafta henuz bitmeden (pazartesi, sadece 60dk gecmisken) " +
                "gecen haftanin 1000dk'sina gore karsilastirma zaten basari donuyor",
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
}
