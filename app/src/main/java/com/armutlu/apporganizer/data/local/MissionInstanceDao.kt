package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionInstanceDao {

    @Query(
        """
        SELECT * FROM mission_instances
        WHERE periodType = :periodType AND periodStartEpoch = :periodStartEpoch
        """
    )
    suspend fun getInstancesForPeriod(periodType: String, periodStartEpoch: Long): List<MissionInstanceEntity>

    @Query(
        """
        SELECT * FROM mission_instances
        WHERE status = 'assigned'
        ORDER BY periodStartEpoch DESC
        """
    )
    fun observeActiveInstances(): Flow<List<MissionInstanceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(instances: List<MissionInstanceEntity>): List<Long>

    @Query("UPDATE mission_instances SET status = :status WHERE instanceId = :instanceId")
    suspend fun updateStatus(instanceId: String, status: String)

    @Query(
        """
        UPDATE mission_instances SET status = :status, settledAt = :settledAt
        WHERE instanceId = :instanceId
        """
    )
    suspend fun settleInstance(instanceId: String, status: String, settledAt: Long)

    @Query(
        """
        SELECT * FROM mission_instances
        WHERE status = 'assigned' AND periodEndAt < :beforeEpochMillis
        """
    )
    suspend fun getUnsettledBefore(beforeEpochMillis: Long): List<MissionInstanceEntity>

    /**
     * Döngü U03 — Sağlık raporu "Settlement bekleyen" sayacı için: dönemi bitmiş ama hâlâ
     * "assigned" olan (henüz [SettleMissionInstancesUseCase.settleOverdue] tarafından
     * sonuçlandırılmamış) instance sayısı. [getUnsettledBefore] ile aynı koşulu kullanır,
     * sadece COUNT döner — rapor tam listeye ihtiyaç duymaz.
     */
    @Query(
        """
        SELECT COUNT(*) FROM mission_instances
        WHERE status = 'assigned' AND periodEndAt < :beforeEpochMillis
        """
    )
    suspend fun countUnsettledBefore(beforeEpochMillis: Long): Int

    /**
     * Döngü G5 — sabah özeti ("Dün 2/3 tamamladın"): dünün GÜNLÜK instance'larından settled
     * (completed/failed/data_unavailable — status != 'assigned') olanların toplam sayısı.
     * Henüz settle edilmemiş ('assigned' kalan) instance'lar sayılmaz — gün henüz kapanmamış
     * demektir, sabah özeti üretilmemeli (bkz. MissionPulseTickerFactory.morningSummaryCandidate).
     */
    @Query(
        """
        SELECT COUNT(*) FROM mission_instances
        WHERE periodType = 'daily' AND periodStartEpoch = :epochDay AND status != 'assigned'
        """
    )
    suspend fun countSettledForDay(epochDay: Long): Int

    /** Aynı gün için, settled olanlar arasında COMPLETED olanların sayısı. */
    @Query(
        """
        SELECT COUNT(*) FROM mission_instances
        WHERE periodType = 'daily' AND periodStartEpoch = :epochDay AND status = 'completed'
        """
    )
    suspend fun countCompletedForDay(epochDay: Long): Int

    @Query("DELETE FROM mission_instances")
    suspend fun clearAll()
}
