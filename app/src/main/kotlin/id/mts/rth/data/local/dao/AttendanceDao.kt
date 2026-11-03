package id.mts.rth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.mts.rth.data.local.entity.PresensiDetailEntity
import id.mts.rth.data.local.entity.PresensiSesiEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AttendanceDao {
    @Query("SELECT * FROM presensi_sesi WHERE penugasanId = :penugasanId AND tanggal = :tanggal AND isDeleted = 0 ORDER BY jamKe ASC")
    abstract fun getSessionByPenugasanAndDate(penugasanId: String, tanggal: String): Flow<List<PresensiSesiEntity>>

    @Query("SELECT * FROM presensi_sesi WHERE id = :sesiId AND isDeleted = 0 LIMIT 1")
    abstract suspend fun getSessionById(sesiId: String): PresensiSesiEntity?

    @Query("SELECT * FROM presensi_detail WHERE sesiId = :sesiId AND isDeleted = 0")
    abstract fun getDetailsBySesiId(sesiId: String): Flow<List<PresensiDetailEntity>>

    @Query("SELECT * FROM presensi_detail WHERE sesiId = :sesiId AND isDeleted = 0")
    abstract suspend fun getDetailsBySesiIdList(sesiId: String): List<PresensiDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateSession(session: PresensiSesiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateDetails(details: List<PresensiDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSessions(sessions: List<PresensiSesiEntity>)

    /**
     * Menyimpan sesi presensi beserta detail kehadiran secara atomik ke Room
     * dan mencatatkan mutasi ke SyncOutboxDao dengan skema Coalescing.
     */
    @Transaction
    open suspend fun saveAttendanceSessionAndOutbox(
        sesi: PresensiSesiEntity,
        details: List<PresensiDetailEntity>,
        outboxDao: SyncOutboxDao,
        sesiJson: String,
        detailJsonList: List<Pair<String, String>> // (detailId, jsonPayload)
    ) {
        insertOrUpdateSession(sesi)
        insertOrUpdateDetails(details)

        // 1. Coalesce Session (Priority 1 = Parent)
        outboxDao.recordMutationWithCoalescing(
            targetTable = "presensi_sesi",
            recordId = sesi.id,
            priority = 1,
            action = "UPSERT",
            jsonPayload = sesiJson,
            nowMillis = sesi.updatedAt
        )

        // 2. Coalesce Details (Priority 2 = Child)
        for ((detailId, payload) in detailJsonList) {
            outboxDao.recordMutationWithCoalescing(
                targetTable = "presensi_detail",
                recordId = detailId,
                priority = 2,
                action = "UPSERT",
                jsonPayload = payload,
                nowMillis = sesi.updatedAt
            )
        }
    }
}
