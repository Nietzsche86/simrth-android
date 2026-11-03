package id.mts.rth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.mts.rth.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY priorityOrder ASC, createdAt ASC")
    fun getPendingOutbox(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox ORDER BY priorityOrder ASC, createdAt ASC LIMIT :limit")
    suspend fun getPendingOutboxBatch(limit: Int): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE tableNameTarget = :targetTable AND recordId = :recordId LIMIT 1")
    suspend fun getOutboxByTargetAndRecord(targetTable: String, recordId: String): SyncOutboxEntity?

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun getOutboxCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(entity: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET actionType = :actionType, payloadJson = :payloadJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOutboxPayload(id: String, actionType: String, payloadJson: String, updatedAt: Long)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteOutboxById(id: String)

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    suspend fun deleteOutboxByIds(ids: List<String>)

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1, lastError = :error, updatedAt = :nowMillis WHERE id = :id")
    suspend fun markOutboxError(id: String, error: String, nowMillis: Long = System.currentTimeMillis())

    @Transaction
    suspend fun recordMutationWithCoalescing(
        targetTable: String,
        recordId: String,
        priority: Int, // 1 = Parent (Sesi/Komponen), 2 = Child (Detail/Nilai/Siswa)
        action: String, // "UPSERT" atau "DELETE"
        jsonPayload: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val existing = getOutboxByTargetAndRecord(targetTable, recordId)
        if (existing != null) {
            updateOutboxPayload(
                id = existing.id,
                actionType = action,
                payloadJson = jsonPayload,
                updatedAt = nowMillis
            )
        } else {
            insertOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    tableNameTarget = targetTable,
                    priorityOrder = priority,
                    recordId = recordId,
                    actionType = action,
                    payloadJson = jsonPayload,
                    createdAt = nowMillis,
                    updatedAt = nowMillis
                )
            )
        }
    }
}
