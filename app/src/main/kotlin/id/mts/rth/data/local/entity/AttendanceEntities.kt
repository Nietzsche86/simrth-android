package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 9. Sesi Presensi & Jurnal Mengajar
@Entity(
    tableName = "presensi_sesi",
    indices = [Index(value = ["penugasanId", "tanggal", "jamKe"], unique = true)]
)
data class PresensiSesiEntity(
    @PrimaryKey val id: String, // UUID
    val penugasanId: String,
    val tanggal: String, // YYYY-MM-DD
    val jamKe: String, // Contoh: "1-2" atau "5"
    val materiPokok: String?, // Jurnal singkat KBM hari itu
    val isDeleted: Boolean = false,
    val syncStatus: String, // "SYNCED", "PENDING"
    val createdAt: Long,
    val updatedAt: Long
)

// 10. Detail Presensi Siswa
@Entity(
    tableName = "presensi_detail",
    indices = [
        Index(value = ["sesiId", "siswaId"], unique = true),
        Index(value = ["siswaId"])
    ]
)
data class PresensiDetailEntity(
    @PrimaryKey val id: String,
    val sesiId: String,
    val siswaId: String,
    val statusKehadiran: String, // "H" (Hadir), "S" (Sakit), "I" (Izin), "A" (Alpha)
    val catatan: String?,
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)
