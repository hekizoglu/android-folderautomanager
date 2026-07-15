package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.data.local.MissionHistoryDao
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.usecase.missions.MissionEngine
import com.armutlu.apporganizer.utils.MissionPrefs
import com.armutlu.apporganizer.utils.TaskScoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val missionHistoryDao: MissionHistoryDao,
    private val taskScoreEventDao: TaskScoreEventDao,
) {

    suspend fun syncLegacyPrefsIfNeeded() {
        if (MissionPrefs.isV2Migrated(context)) return

        val now = System.currentTimeMillis()
        val epochDay = LocalDate.now().toEpochDay()
        val epochWeek = epochDay / 7

        MissionPrefs.getCompletedDailyIds(context, epochDay).forEach { missionId ->
            missionHistoryDao.insert(
                MissionHistoryEntry(
                    missionId = missionId,
                    periodType = MissionHistoryEntry.PERIOD_DAILY,
                    periodStartEpoch = epochDay,
                    completedAt = now,
                    starReward = MissionEngine.starRewardForMission(missionId),
                    source = "legacy_pref",
                )
            )
        }
        MissionPrefs.getCompletedWeeklyIds(context, epochWeek).forEach { missionId ->
            missionHistoryDao.insert(
                MissionHistoryEntry(
                    missionId = missionId,
                    periodType = MissionHistoryEntry.PERIOD_WEEKLY,
                    periodStartEpoch = epochWeek,
                    completedAt = now,
                    starReward = MissionEngine.starRewardForMission(missionId),
                    source = "legacy_pref",
                )
            )
        }

        val legacyStars = MissionPrefs.getTotalStars(context)
        if (legacyStars > 0) {
            missionHistoryDao.insert(
                MissionHistoryEntry(
                    missionId = "legacy_star_import",
                    periodType = MissionHistoryEntry.PERIOD_LEGACY,
                    periodStartEpoch = 0L,
                    completedAt = now,
                    starReward = legacyStars,
                    source = "legacy_total",
                )
            )
        }

        val legacyScore = TaskScoreManager.getLegacySnapshot(context)
        if (legacyScore.totalScore > 0) {
            taskScoreEventDao.insert(
                com.armutlu.apporganizer.domain.models.TaskScoreEventEntry(
                    eventKey = "legacy_score_import",
                    label = if (legacyScore.lastEventLabel.isNotBlank()) legacyScore.lastEventLabel else "Legacy task score import",
                    delta = legacyScore.totalScore,
                    createdAt = legacyScore.lastEventAt.takeIf { it > 0L } ?: now,
                )
            )
        }

        MissionPrefs.markV2Migrated(context)
    }

    suspend fun getCompletedDailyIds(epochDay: Long): Set<String> =
        missionHistoryDao.getCompletedMissionIds(MissionHistoryEntry.PERIOD_DAILY, epochDay).toSet()

    suspend fun getCompletedWeeklyIds(epochWeek: Long): Set<String> =
        missionHistoryDao.getCompletedMissionIds(MissionHistoryEntry.PERIOD_WEEKLY, epochWeek).toSet()

    suspend fun getRecentlyCompletedDailyIds(
        currentEpochDay: Long,
        cooldownDays: Long,
    ): Set<String> {
        if (cooldownDays <= 0) return emptySet()
        val fromEpochDay = (currentEpochDay - cooldownDays).coerceAtLeast(0L)
        val toEpochDay = (currentEpochDay - 1).coerceAtLeast(fromEpochDay - 1)
        if (toEpochDay < fromEpochDay) return emptySet()
        return missionHistoryDao.getCompletedMissionIdsBetween(
            periodType = MissionHistoryEntry.PERIOD_DAILY,
            fromPeriodStartEpoch = fromEpochDay,
            toPeriodStartEpoch = toEpochDay,
        ).toSet()
    }

    suspend fun getRecentlyCompletedWeeklyIds(
        currentEpochWeek: Long,
        cooldownWeeks: Long,
    ): Set<String> {
        if (cooldownWeeks <= 0) return emptySet()
        val fromEpochWeek = (currentEpochWeek - cooldownWeeks).coerceAtLeast(0L)
        val toEpochWeek = (currentEpochWeek - 1).coerceAtLeast(fromEpochWeek - 1)
        if (toEpochWeek < fromEpochWeek) return emptySet()
        return missionHistoryDao.getCompletedMissionIdsBetween(
            periodType = MissionHistoryEntry.PERIOD_WEEKLY,
            fromPeriodStartEpoch = fromEpochWeek,
            toPeriodStartEpoch = toEpochWeek,
        ).toSet()
    }

    suspend fun markDailyCompleted(epochDay: Long, missionId: String, completedAt: Long = System.currentTimeMillis()) {
        missionHistoryDao.insert(
            MissionHistoryEntry(
                missionId = missionId,
                periodType = MissionHistoryEntry.PERIOD_DAILY,
                periodStartEpoch = epochDay,
                completedAt = completedAt,
                starReward = MissionEngine.starRewardForMission(missionId),
            )
        )
    }

    suspend fun markWeeklyCompleted(epochWeek: Long, missionId: String, completedAt: Long = System.currentTimeMillis()) {
        missionHistoryDao.insert(
            MissionHistoryEntry(
                missionId = missionId,
                periodType = MissionHistoryEntry.PERIOD_WEEKLY,
                periodStartEpoch = epochWeek,
                completedAt = completedAt,
                starReward = MissionEngine.starRewardForMission(missionId),
            )
        )
    }

    suspend fun getTotalStars(): Int = missionHistoryDao.getTotalStars()

    suspend fun buildTaskEventInput(epochDay: Long, epochWeek: Long): MissionEngine.TaskEventInput {
        val zone = ZoneId.systemDefault()
        val dayStart = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val weekStartDay = epochWeek * 7
        val weekStart = LocalDate.ofEpochDay(weekStartDay).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = LocalDate.ofEpochDay(weekStartDay + 7).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        return MissionEngine.TaskEventInput(
            positiveEventsToday = taskScoreEventDao.countEventsBetween(dayStart, dayEnd, positiveOnly = true),
            positiveEventsThisWeek = taskScoreEventDao.countEventsBetween(weekStart, weekEnd, positiveOnly = true),
            classificationActionsToday = taskScoreEventDao.countEventsBetweenByKeys(
                dayStart,
                dayEnd,
                listOf(
                    TaskScoreManager.EventType.ClassificationApproved.eventKey,
                    TaskScoreManager.EventType.ClassificationCorrected.eventKey,
                )
            ),
            notificationReportViewedToday = taskScoreEventDao.countEventsBetweenByKeys(
                dayStart,
                dayEnd,
                listOf(TaskScoreManager.EventType.NotificationReportViewed.eventKey),
            ) > 0,
        )
    }
}
