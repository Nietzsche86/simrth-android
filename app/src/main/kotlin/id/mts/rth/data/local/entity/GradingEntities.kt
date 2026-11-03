package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 11. Konfigurasi KKM & Bobot Mapel (Diatur oleh Admin)
@Entity(
    tableName = "kkm_mapel",
    indices = [Index(value = ["mapelId", "tingkatKelas", "tahunAjaranId"], unique = true)]
)
data class KkmMapelEntity(
    @PrimaryKey val id: String,
    val mapelId: String,
    val tingkatKelas: Int, // 7, 8, 9
    val tahunAjaranId: String,
    val nilaiKkm: Int = 75, // Default: 75
    val bobotHarian: Int = 2, // Rata-rata Tugas/PR
    val bobotUlangan: Int = 2, // Rata-rata PH/UH
    val bobotUjianAkhir: Int = 1, // PAS (Ganjil) atau PAT (Genap)
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 12. Komponen Penilaian (Header Kolom Dinamis)
@Entity(
    tableName = "komponen_nilai",
    indices = [Index(value = ["penugasanId", "aspek", "kategori"])]
)
data class KomponenNilaiEntity(
    @PrimaryKey val id: String, // UUID
    val penugasanId: String,
    val aspek: String, // "KI3" (Pengetahuan) | "KI4" (Keterampilan)
    val kategori: String, // "TUGAS", "UH", "UJIAN_AKHIR" (PAS/PAT), "PRAKTIK", "PROYEK", "PORTOFOLIO"
    val judul: String, // Contoh: "Tugas 1", "PH Bab 1", "Praktik Muhadatsah"
    val tanggalInput: String, // YYYY-MM-DD
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 13. Skor Nilai Siswa
@Entity(
    tableName = "nilai_siswa",
    indices = [
        Index(value = ["komponenId", "siswaId"], unique = true),
        Index(value = ["siswaId"])
    ]
)
data class NilaiSiswaEntity(
    @PrimaryKey val id: String, // UUID
    val komponenId: String,
    val siswaId: String,
    val skor: Float, // Rentang valid: 0.0f .. 100.0f
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)
