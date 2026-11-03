package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 1. Tahun Ajaran & Semester
@Entity(tableName = "tahun_ajaran")
data class TahunAjaranEntity(
    @PrimaryKey val id: String, // UUID
    val tahun: String, // "2026/2027"
    val semester: String, // "GANJIL" | "GENAP"
    val isActive: Boolean,
    val tanggalMulai: String, // YYYY-MM-DD
    val tanggalSelesai: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 2. Master Guru & GTK
@Entity(tableName = "master_guru")
data class GuruEntity(
    @PrimaryKey val id: String,
    val nip: String?,
    val npk: String?,
    val nuptk: String?,
    val nik: String,
    val namaLengkap: String,
    val gelarDepan: String?,
    val gelarBelakang: String?,
    val jenisKelamin: String, // "L" | "P"
    val noWa: String,
    val statusKepegawaian: String, // "PNS", "PPPK", "GTY", "GTT"
    val role: String, // "GURU", "GURU_WALI", "ADMIN"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 3. Master Rombel / Kelas
@Entity(tableName = "master_kelas")
data class KelasEntity(
    @PrimaryKey val id: String,
    val tingkat: Int, // 7, 8, 9
    val namaRombel: String, // "7A", "7B", "8A", "8B", "9A", "9B"
    val waliKelasGuruId: String?, // FK ke master_guru.id
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 4. Master Mata Pelajaran
@Entity(tableName = "master_mapel")
data class MapelEntity(
    @PrimaryKey val id: String,
    val kodeMapel: String, // "TIK-07", "BAR-07"
    val namaMapel: String,
    val kelompok: String, // "A", "B", "MULOK"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 5. Penugasan & Jadwal Mengajar Guru
@Entity(
    tableName = "penugasan_guru",
    indices = [
        Index(value = ["guruId", "mapelId", "kelasId", "tahunAjaranId"], unique = true),
        Index(value = ["kelasId"]),
        Index(value = ["mapelId"])
    ]
)
data class PenugasanGuruEntity(
    @PrimaryKey val id: String,
    val guruId: String,
    val mapelId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val jadwalJson: String, // Contoh: [{"hari":1,"jamKe":"1-2","mulai":"07:30"}]
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
