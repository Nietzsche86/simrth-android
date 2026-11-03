package id.mts.rth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val serverTime: Long
)

@Serializable
data class TahunAjaranDto(
    val id: String,
    val tahun: String,
    val semester: String,
    val isActive: Boolean,
    val tanggalMulai: String,
    val tanggalSelesai: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class GuruDto(
    val id: String,
    val nip: String? = null,
    val npk: String? = null,
    val nuptk: String? = null,
    val nik: String,
    val namaLengkap: String,
    val gelarDepan: String? = null,
    val gelarBelakang: String? = null,
    val jenisKelamin: String,
    val noWa: String,
    val statusKepegawaian: String,
    val role: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class KelasDto(
    val id: String,
    val tingkat: Int,
    val namaRombel: String,
    val waliKelasGuruId: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class MapelDto(
    val id: String,
    val kodeMapel: String,
    val namaMapel: String,
    val kelompok: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class PenugasanGuruDto(
    val id: String,
    val guruId: String,
    val mapelId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val jadwalJson: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class SiswaCoreDto(
    val id: String,
    val nisLokal: String,
    val nisn: String? = null,
    val namaLengkap: String,
    val jenisKelamin: String,
    val fotoFileName: String? = null,
    val statusSiswa: String,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)

@Serializable
data class SiswaDetailDto(
    val id: String,
    val siswaId: String,
    val nik: String,
    val noKk: String,
    val tempatLahir: String,
    val tanggalLahir: String,
    val agama: String = "Islam",
    val anakKe: Int,
    val jumlahSaudara: Int,
    val alamatJalan: String,
    val rt: String,
    val rw: String,
    val desaKelurahan: String,
    val kecamatan: String,
    val kabupatenKota: String,
    val kodePos: String? = null,
    val statusTempatTinggal: String,
    val nikAyah: String? = null,
    val namaAyah: String? = null,
    val tahunLahirAyah: String? = null,
    val statusAyah: String? = null,
    val pendidikanAyah: String? = null,
    val pekerjaanAyah: String? = null,
    val penghasilanAyah: String? = null,
    val nikIbu: String? = null,
    val namaIbu: String? = null,
    val tahunLahirIbu: String? = null,
    val statusIbu: String? = null,
    val pendidikanIbu: String? = null,
    val pekerjaanIbu: String? = null,
    val penghasilanIbu: String? = null,
    val noHpOrangTuaWa: String,
    val namaWali: String? = null,
    val hubunganWali: String? = null,
    val noHpWali: String? = null,
    val noKip: String? = null,
    val noKisPkh: String? = null,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)

@Serializable
data class AnggotaRombelDto(
    val id: String,
    val siswaId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val nomorAbsen: Int,
    val statusKeanggotaan: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class PresensiSesiDto(
    val id: String,
    val penugasanId: String,
    val tanggal: String,
    val jamKe: String,
    val materiPokok: String? = null,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PresensiDetailDto(
    val id: String,
    val sesiId: String,
    val siswaId: String,
    val statusKehadiran: String,
    val catatan: String? = null,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)

@Serializable
data class KkmMapelDto(
    val id: String,
    val mapelId: String,
    val tingkatKelas: Int,
    val tahunAjaranId: String,
    val nilaiKkm: Int,
    val bobotHarian: Int = 2,
    val bobotUlangan: Int = 2,
    val bobotUjianAkhir: Int = 1,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class KomponenNilaiDto(
    val id: String,
    val penugasanId: String,
    val aspek: String,
    val kategori: String,
    val judul: String,
    val tanggalInput: String,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)

@Serializable
data class NilaiSiswaDto(
    val id: String,
    val komponenId: String,
    val siswaId: String,
    val skor: Float,
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long
)

@Serializable
data class SyncPullResponse(
    val serverTime: Long,
    val tahunAjaranList: List<TahunAjaranDto> = emptyList(),
    val guruList: List<GuruDto> = emptyList(),
    val kelasList: List<KelasDto> = emptyList(),
    val mapelList: List<MapelDto> = emptyList(),
    val penugasanList: List<PenugasanGuruDto> = emptyList(),
    val siswaCoreList: List<SiswaCoreDto> = emptyList(),
    val siswaDetailList: List<SiswaDetailDto> = emptyList(),
    val anggotaRombelList: List<AnggotaRombelDto> = emptyList(),
    val presensiSesiList: List<PresensiSesiDto> = emptyList(),
    val presensiDetailList: List<PresensiDetailDto> = emptyList(),
    val kkmList: List<KkmMapelDto> = emptyList(),
    val komponenList: List<KomponenNilaiDto> = emptyList(),
    val nilaiList: List<NilaiSiswaDto> = emptyList()
)
