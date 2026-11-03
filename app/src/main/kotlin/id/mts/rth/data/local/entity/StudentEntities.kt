package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 6. Data Siswa Core (Diunduh oleh semua Guru yang mengajar kelas terkait)
@Entity(
    tableName = "siswa_core",
    indices = [Index(value = ["nisLokal"], unique = true), Index(value = ["nisn"])]
)
data class SiswaCoreEntity(
    @PrimaryKey val id: String, // UUID
    val nisLokal: String,
    val nisn: String?,
    val namaLengkap: String,
    val jenisKelamin: String, // "L" | "P"
    val fotoFileName: String?, // Nama file relatif: "siswa_uuid_hash.webp"
    val statusSiswa: String, // "AKTIF", "LULUS", "MUTASI_KELUAR", "DO"
    val isDeleted: Boolean = false,
    val syncStatus: String, // "SYNCED", "PENDING_UPDATE"
    val updatedAt: Long
)

// 7. Data Siswa Detail EMIS (HANYA diunduh oleh Admin & Wali Kelas Binaan)
@Entity(
    tableName = "siswa_detail",
    indices = [Index(value = ["siswaId"], unique = true), Index(value = ["nik"])]
)
data class SiswaDetailEntity(
    @PrimaryKey val id: String,
    val siswaId: String, // FK ke siswa_core.id
    val nik: String,
    val noKk: String,
    val tempatLahir: String,
    val tanggalLahir: String, // YYYY-MM-DD
    val agama: String = "Islam",
    val anakKe: Int,
    val jumlahSaudara: Int,

    // Alamat Domisili
    val alamatJalan: String,
    val rt: String,
    val rw: String,
    val desaKelurahan: String,
    val kecamatan: String,
    val kabupatenKota: String,
    val kodePos: String?,
    val statusTempatTinggal: String, // "Bersama Orang Tua", "Wali", "Pondok Pesantren"

    // Data Ayah Kandung
    val nikAyah: String?,
    val namaAyah: String?,
    val tahunLahirAyah: String?,
    val statusAyah: String?, // "Masih Hidup", "Meninggal", "Tidak Diketahui"
    val pendidikanAyah: String?,
    val pekerjaanAyah: String?,
    val penghasilanAyah: String?,

    // Data Ibu Kandung
    val nikIbu: String?,
    val namaIbu: String?,
    val tahunLahirIbu: String?,
    val statusIbu: String?,
    val pendidikanIbu: String?,
    val pekerjaanIbu: String?,
    val penghasilanIbu: String?,

    // Kontak & Wali
    val noHpOrangTuaWa: String, // Format standar: "62812..."
    val namaWali: String?,
    val hubunganWali: String?,
    val noHpWali: String?,

    // Bantuan Pemerintah (Standar EMIS)
    val noKip: String?,
    val noKisPkh: String?,

    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 8. Anggota Rombel (Lifecycle / Histori Kelas per Tahun Ajaran)
@Entity(
    tableName = "anggota_rombel",
    indices = [
        Index(value = ["siswaId", "tahunAjaranId"], unique = true),
        Index(value = ["kelasId", "tahunAjaranId"])
    ]
)
data class AnggotaRombelEntity(
    @PrimaryKey val id: String,
    val siswaId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val nomorAbsen: Int,
    val statusKeanggotaan: String, // "AKTIF", "PINDAH_ROMBEL", "KELUAR"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
