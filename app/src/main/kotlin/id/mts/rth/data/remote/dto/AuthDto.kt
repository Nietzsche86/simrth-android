package id.mts.rth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val nipOrUsername: String,
    val password: String
)

@Serializable
data class UserProfileDto(
    val id: String,
    val nip: String? = null,
    val namaLengkap: String,
    val role: String,
    val noWa: String? = null
)

@Serializable
data class InitialSnapshotDto(
    val tahunAjaranList: List<TahunAjaranDto> = emptyList(),
    val guruList: List<GuruDto> = emptyList(),
    val kelasList: List<KelasDto> = emptyList(),
    val mapelList: List<MapelDto> = emptyList(),
    val penugasanList: List<PenugasanGuruDto> = emptyList(),
    val siswaCoreList: List<SiswaCoreDto> = emptyList(),
    val siswaDetailList: List<SiswaDetailDto> = emptyList(),
    val anggotaRombelList: List<AnggotaRombelDto> = emptyList(),
    val kkmList: List<KkmMapelDto> = emptyList(),
    val komponenList: List<KomponenNilaiDto> = emptyList(),
    val nilaiList: List<NilaiSiswaDto> = emptyList(),
    val presensiSesiList: List<PresensiSesiDto> = emptyList(),
    val presensiDetailList: List<PresensiDetailDto> = emptyList(),
    val serverTime: Long
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserProfileDto,
    val initialSnapshot: InitialSnapshotDto? = null
)
