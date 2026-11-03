package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 14. Tabel Antrean Sinkronisasi dengan Coalescing (Outbox Pattern)
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["tableNameTarget", "recordId"], unique = true)]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String, // UUID
    val tableNameTarget: String,
    val priorityOrder: Int, // 1: Sesi/Komponen (Parent), 2: Detail/Nilai/Siswa (Child)
    val recordId: String,
    val actionType: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
