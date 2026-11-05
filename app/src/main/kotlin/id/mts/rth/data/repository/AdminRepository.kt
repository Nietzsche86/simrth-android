package id.mts.rth.data.repository

import androidx.room.withTransaction
import id.mts.rth.core.common.ResultState
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.data.local.dao.GradingDao
import id.mts.rth.data.local.dao.MasterAcademicDao
import id.mts.rth.data.local.dao.StudentDao
import id.mts.rth.data.local.dao.SyncOutboxDao
import id.mts.rth.data.local.db.AppDatabase
import id.mts.rth.data.local.entity.AnggotaRombelEntity
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import id.mts.rth.data.remote.dto.AnggotaRombelDto
import id.mts.rth.data.remote.dto.KkmMapelDto
import id.mts.rth.data.remote.dto.SiswaCoreDto
import id.mts.rth.data.remote.dto.SiswaDetailDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository khusus Administrator madrasah:
 * - Pengaturan KKM dan Bobot penilaian mata pelajaran per tingkat kelas.
 * - Eksekusi mutasi siklus kesiswaan massal: Kenaikan Kelas & Kelulusan Kelas 9.
 * - Seluruh mutasi dicatatkan ke sync_outbox secara atomik (@Transaction).
 */
class AdminRepository(
    private val gradingDao: GradingDao,
    private val studentDao: StudentDao,
    private val masterAcademicDao: MasterAcademicDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val sessionPreferences: SessionPreferences,
    private val appDatabase: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getAllKkm(tahunAjaranId: String): Flow<List<KkmMapelEntity>> {
        return gradingDao.getAllKkmByTahunAjaran(tahunAjaranId)
    }

    suspend fun getKkm(mapelId: String, tingkatKelas: Int, tahunAjaranId: String): KkmMapelEntity? = withContext(Dispatchers.IO) {
        gradingDao.getKkmSync(mapelId, tingkatKelas, tahunAjaranId)
    }

    /**
     * Memperbarui atau menyimpan konfigurasi KKM dan Bobot penilaian mata pelajaran.
     */
    suspend fun saveKkmConfig(
        mapelId: String,
        tingkatKelas: Int,
        tahunAjaranId: String,
        nilaiKkm: Int,
        bobotHarian: Int,
        bobotUlangan: Int,
        bobotUjianAkhir: Int
    ): ResultState<KkmMapelEntity> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            if (!session.activeRole.equals("ADMIN", ignoreCase = true)) {
                return@withContext ResultState.Error("Akses Ditolak: Hanya Administrator yang berwenang mengubah KKM & Bobot.")
            }

            val now = System.currentTimeMillis()
            val existing = gradingDao.getKkmSync(mapelId, tingkatKelas, tahunAjaranId)
            val recordId = existing?.id ?: UUID.randomUUID().toString()

            val kkmEntity = KkmMapelEntity(
                id = recordId,
                mapelId = mapelId,
                tingkatKelas = tingkatKelas,
                tahunAjaranId = tahunAjaranId,
                nilaiKkm = nilaiKkm.coerceIn(0, 100),
                bobotHarian = bobotHarian.coerceAtLeast(1),
                bobotUlangan = bobotUlangan.coerceAtLeast(1),
                bobotUjianAkhir = bobotUjianAkhir.coerceAtLeast(1),
                isDeleted = false,
                updatedAt = now
            )

            val dto = KkmMapelDto(
                id = kkmEntity.id,
                mapelId = kkmEntity.mapelId,
                tingkatKelas = kkmEntity.tingkatKelas,
                tahunAjaranId = kkmEntity.tahunAjaranId,
                nilaiKkm = kkmEntity.nilaiKkm,
                bobotHarian = kkmEntity.bobotHarian,
                bobotUlangan = kkmEntity.bobotUlangan,
                bobotUjianAkhir = kkmEntity.bobotUjianAkhir,
                isDeleted = false,
                updatedAt = kkmEntity.updatedAt
            )
            val kkmJson = json.encodeToString(dto)

            appDatabase.withTransaction {
                gradingDao.upsertSingleKkm(kkmEntity)
                syncOutboxDao.recordMutationWithCoalescing(
                    targetTable = "kkm_mapel",
                    recordId = kkmEntity.id,
                    priority = 1,
                    action = "UPSERT",
                    jsonPayload = kkmJson,
                    nowMillis = now
                )
            }

            ResultState.Success(kkmEntity)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal menyimpan konfigurasi KKM: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    /**
     * Mengeksekusi mutasi batch kenaikan kelas (misal: 7A -> 8A, 8A -> 9A).
     */
    suspend fun executeClassPromotion(
        sourceKelasId: String,
        targetKelasId: String,
        currentTahunAjaranId: String,
        targetTahunAjaranId: String,
        selectedSiswaIds: List<String>
    ): ResultState<Int> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            if (!session.activeRole.equals("ADMIN", ignoreCase = true)) {
                return@withContext ResultState.Error("Akses Ditolak: Hanya Administrator yang berwenang mengeksekusi mutasi kelas.")
            }

            if (selectedSiswaIds.isEmpty()) {
                return@withContext ResultState.Error("Tidak ada siswa yang dipilih untuk kenaikan kelas.")
            }

            val now = System.currentTimeMillis()

            appDatabase.withTransaction {
                // 1. Ambil keanggotaan rombel lama
                val existingAnggota = studentDao.getAnggotaRombelByKelasSync(sourceKelasId, currentTahunAjaranId)
                val existingMap = existingAnggota.associateBy { it.siswaId }

                val updatedOldList = mutableListOf<AnggotaRombelEntity>()
                val newTargetList = mutableListOf<AnggotaRombelEntity>()

                for ((index, siswaId) in selectedSiswaIds.withIndex()) {
                    // Update keanggotaan lama
                    val oldRecord = existingMap[siswaId]
                    if (oldRecord != null) {
                        val updatedOld = oldRecord.copy(
                            statusKeanggotaan = "PINDAH_ROMBEL",
                            updatedAt = now
                        )
                        updatedOldList.add(updatedOld)

                        val oldDto = AnggotaRombelDto(
                            id = updatedOld.id,
                            siswaId = updatedOld.siswaId,
                            kelasId = updatedOld.kelasId,
                            tahunAjaranId = updatedOld.tahunAjaranId,
                            nomorAbsen = updatedOld.nomorAbsen,
                            statusKeanggotaan = updatedOld.statusKeanggotaan,
                            isDeleted = updatedOld.isDeleted,
                            updatedAt = updatedOld.updatedAt
                        )
                        syncOutboxDao.recordMutationWithCoalescing(
                            targetTable = "anggota_rombel",
                            recordId = updatedOld.id,
                            priority = 2,
                            action = "UPSERT",
                            jsonPayload = json.encodeToString(oldDto),
                            nowMillis = now
                        )
                    }

                    // Buat keanggotaan rombel baru
                    val newId = UUID.randomUUID().toString()
                    val newAnggota = AnggotaRombelEntity(
                        id = newId,
                        siswaId = siswaId,
                        kelasId = targetKelasId,
                        tahunAjaranId = targetTahunAjaranId,
                        nomorAbsen = index + 1,
                        statusKeanggotaan = "AKTIF",
                        isDeleted = false,
                        updatedAt = now
                    )
                    newTargetList.add(newAnggota)

                    val newDto = AnggotaRombelDto(
                        id = newAnggota.id,
                        siswaId = newAnggota.siswaId,
                        kelasId = newAnggota.kelasId,
                        tahunAjaranId = newAnggota.tahunAjaranId,
                        nomorAbsen = newAnggota.nomorAbsen,
                        statusKeanggotaan = newAnggota.statusKeanggotaan,
                        isDeleted = newAnggota.isDeleted,
                        updatedAt = newAnggota.updatedAt
                    )
                    syncOutboxDao.recordMutationWithCoalescing(
                        targetTable = "anggota_rombel",
                        recordId = newAnggota.id,
                        priority = 2,
                        action = "UPSERT",
                        jsonPayload = json.encodeToString(newDto),
                        nowMillis = now
                    )
                }

                if (updatedOldList.isNotEmpty()) {
                    studentDao.upsertAnggotaRombel(updatedOldList)
                }
                if (newTargetList.isNotEmpty()) {
                    studentDao.upsertAnggotaRombel(newTargetList)
                }
            }

            ResultState.Success(selectedSiswaIds.size)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal mengeksekusi kenaikan kelas: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    /**
     * Mengeksekusi mutasi batch kelulusan rombel kelas 9 (statusSiswa = "LULUS", statusKeanggotaan = "KELUAR").
     */
    suspend fun executeGraduation(
        kelasId: String,
        tahunAjaranId: String,
        selectedSiswaIds: List<String>
    ): ResultState<Int> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            if (!session.activeRole.equals("ADMIN", ignoreCase = true)) {
                return@withContext ResultState.Error("Akses Ditolak: Hanya Administrator yang berwenang mengeksekusi kelulusan.")
            }

            if (selectedSiswaIds.isEmpty()) {
                return@withContext ResultState.Error("Tidak ada siswa yang dipilih untuk kelulusan.")
            }

            val now = System.currentTimeMillis()

            appDatabase.withTransaction {
                // 1. Update keanggotaan rombel menjadi "KELUAR"
                val existingAnggota = studentDao.getAnggotaRombelByKelasSync(kelasId, tahunAjaranId)
                val existingAnggotaMap = existingAnggota.associateBy { it.siswaId }
                val updatedAnggotaList = mutableListOf<AnggotaRombelEntity>()

                // 2. Update status siswa core menjadi "LULUS"
                val updatedCoreList = mutableListOf<SiswaCoreEntity>()

                for (siswaId in selectedSiswaIds) {
                    val anggota = existingAnggotaMap[siswaId]
                    if (anggota != null) {
                        val updated = anggota.copy(
                            statusKeanggotaan = "KELUAR",
                            updatedAt = now
                        )
                        updatedAnggotaList.add(updated)

                        val dto = AnggotaRombelDto(
                            id = updated.id,
                            siswaId = updated.siswaId,
                            kelasId = updated.kelasId,
                            tahunAjaranId = updated.tahunAjaranId,
                            nomorAbsen = updated.nomorAbsen,
                            statusKeanggotaan = updated.statusKeanggotaan,
                            isDeleted = updated.isDeleted,
                            updatedAt = updated.updatedAt
                        )
                        syncOutboxDao.recordMutationWithCoalescing(
                            targetTable = "anggota_rombel",
                            recordId = updated.id,
                            priority = 2,
                            action = "UPSERT",
                            jsonPayload = json.encodeToString(dto),
                            nowMillis = now
                        )
                    }

                    val core = studentDao.getSiswaCoreById(siswaId)
                    if (core != null) {
                        val updatedCore = core.copy(
                            statusSiswa = "LULUS",
                            syncStatus = "PENDING",
                            updatedAt = now
                        )
                        updatedCoreList.add(updatedCore)

                        val coreDto = SiswaCoreDto(
                            id = updatedCore.id,
                            nisLokal = updatedCore.nisLokal,
                            nisn = updatedCore.nisn,
                            namaLengkap = updatedCore.namaLengkap,
                            jenisKelamin = updatedCore.jenisKelamin,
                            fotoFileName = updatedCore.fotoFileName,
                            statusSiswa = updatedCore.statusSiswa,
                            isDeleted = updatedCore.isDeleted,
                            syncStatus = "PENDING",
                            updatedAt = updatedCore.updatedAt
                        )
                        syncOutboxDao.recordMutationWithCoalescing(
                            targetTable = "siswa_core",
                            recordId = updatedCore.id,
                            priority = 2,
                            action = "UPSERT",
                            jsonPayload = json.encodeToString(coreDto),
                            nowMillis = now
                        )
                    }
                }

                if (updatedAnggotaList.isNotEmpty()) {
                    studentDao.upsertAnggotaRombel(updatedAnggotaList)
                }
                if (updatedCoreList.isNotEmpty()) {
                    studentDao.upsertSiswaCore(updatedCoreList)
                }
            }

            ResultState.Success(selectedSiswaIds.size)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal mengeksekusi kelulusan siswa: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    /**
     * Menambah atau mengedit data siswa secara atomik,
     * serta mencatat mutasi ke sync_outbox untuk disinkronkan ke server PocketBase.
     */
    suspend fun saveStudent(
        id: String?,
        nisLokal: String,
        nisn: String?,
        namaLengkap: String,
        jenisKelamin: String,
        kelasId: String,
        nomorAbsen: Int,
        tahunAjaranId: String,
        nik: String = "",
        noKk: String = "",
        tempatLahir: String = "Kudus",
        tanggalLahir: String = "2012-01-01",
        alamatJalan: String = "",
        desaKelurahan: String = "",
        kecamatan: String = "",
        kabupatenKota: String = "Kudus",
        namaAyah: String? = null,
        namaIbu: String? = null,
        noHpOrangTuaWa: String = ""
    ): ResultState<SiswaCoreEntity> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            if (!session.activeRole.equals("ADMIN", ignoreCase = true)) {
                return@withContext ResultState.Error("Akses Ditolak: Hanya Administrator yang berwenang menambah atau mengedit siswa.")
            }

            val studentId = id?.ifBlank { null } ?: UUID.randomUUID().toString()

            // Validasi NIS Lokal unik
            if (studentDao.isNisLokalTaken(nisLokal.trim(), studentId)) {
                return@withContext ResultState.Error("NIS Lokal '$nisLokal' sudah digunakan oleh siswa lain.")
            }

            val now = System.currentTimeMillis()

            val coreEntity = SiswaCoreEntity(
                id = studentId,
                nisLokal = nisLokal.trim(),
                nisn = nisn?.trim()?.ifBlank { null },
                namaLengkap = namaLengkap.trim(),
                jenisKelamin = jenisKelamin.trim(),
                fotoFileName = null,
                statusSiswa = "AKTIF",
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = now
            )

            val coreDto = SiswaCoreDto(
                id = coreEntity.id,
                nisLokal = coreEntity.nisLokal,
                nisn = coreEntity.nisn,
                namaLengkap = coreEntity.namaLengkap,
                jenisKelamin = coreEntity.jenisKelamin,
                fotoFileName = coreEntity.fotoFileName,
                statusSiswa = coreEntity.statusSiswa,
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = now
            )

            // Siswa detail
            val detailId = "detail_$studentId"
            val detailEntity = SiswaDetailEntity(
                id = detailId,
                siswaId = studentId,
                nik = nik.trim().ifBlank { "330101" + nisLokal.trim() },
                noKk = noKk.trim().ifBlank { "3301019999990001" },
                tempatLahir = tempatLahir.trim().ifBlank { "Kudus" },
                tanggalLahir = tanggalLahir.trim().ifBlank { "2012-01-01" },
                agama = "Islam",
                anakKe = 1,
                jumlahSaudara = 0,
                alamatJalan = alamatJalan.trim().ifBlank { "Jl. Madrasah" },
                rt = "001",
                rw = "001",
                desaKelurahan = desaKelurahan.trim().ifBlank { "Bae" },
                kecamatan = kecamatan.trim().ifBlank { "Bae" },
                kabupatenKota = kabupatenKota.trim().ifBlank { "Kudus" },
                kodePos = "59381",
                statusTempatTinggal = "Bersama Orang Tua",
                nikAyah = null,
                namaAyah = namaAyah?.trim()?.ifBlank { null },
                tahunLahirAyah = null,
                statusAyah = "Masih Hidup",
                pendidikanAyah = null,
                pekerjaanAyah = null,
                penghasilanAyah = null,
                nikIbu = null,
                namaIbu = namaIbu?.trim()?.ifBlank { null },
                tahunLahirIbu = null,
                statusIbu = "Masih Hidup",
                pendidikanIbu = null,
                pekerjaanIbu = null,
                penghasilanIbu = null,
                noHpOrangTuaWa = noHpOrangTuaWa.trim(),
                namaWali = null,
                hubunganWali = null,
                noHpWali = null,
                noKip = null,
                noKisPkh = null,
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = now
            )

            val detailDto = SiswaDetailDto(
                id = detailEntity.id,
                siswaId = detailEntity.siswaId,
                nik = detailEntity.nik,
                noKk = detailEntity.noKk,
                tempatLahir = detailEntity.tempatLahir,
                tanggalLahir = detailEntity.tanggalLahir,
                agama = detailEntity.agama,
                anakKe = detailEntity.anakKe,
                jumlahSaudara = detailEntity.jumlahSaudara,
                alamatJalan = detailEntity.alamatJalan,
                rt = detailEntity.rt,
                rw = detailEntity.rw,
                desaKelurahan = detailEntity.desaKelurahan,
                kecamatan = detailEntity.kecamatan,
                kabupatenKota = detailEntity.kabupatenKota,
                kodePos = detailEntity.kodePos,
                statusTempatTinggal = detailEntity.statusTempatTinggal,
                namaAyah = detailEntity.namaAyah,
                statusAyah = detailEntity.statusAyah,
                namaIbu = detailEntity.namaIbu,
                statusIbu = detailEntity.statusIbu,
                noHpOrangTuaWa = detailEntity.noHpOrangTuaWa,
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = now
            )

            // Anggota rombel
            val existingRombel = studentDao.getAnggotaRombelBySiswaAndTahun(studentId, tahunAjaranId)
            val rombelId = existingRombel?.id ?: UUID.randomUUID().toString()
            val rombelEntity = AnggotaRombelEntity(
                id = rombelId,
                siswaId = studentId,
                kelasId = kelasId,
                tahunAjaranId = tahunAjaranId,
                nomorAbsen = nomorAbsen,
                statusKeanggotaan = "AKTIF",
                isDeleted = false,
                updatedAt = now
            )
            val rombelDto = AnggotaRombelDto(
                id = rombelEntity.id,
                siswaId = rombelEntity.siswaId,
                kelasId = rombelEntity.kelasId,
                tahunAjaranId = rombelEntity.tahunAjaranId,
                nomorAbsen = rombelEntity.nomorAbsen,
                statusKeanggotaan = rombelEntity.statusKeanggotaan,
                isDeleted = false,
                updatedAt = now
            )

            appDatabase.withTransaction {
                studentDao.upsertSingleSiswaCore(coreEntity)
                studentDao.upsertSingleSiswaDetail(detailEntity)
                studentDao.upsertSingleAnggotaRombel(rombelEntity)

                syncOutboxDao.recordMutationWithCoalescing(
                    targetTable = "siswa_core",
                    recordId = coreEntity.id,
                    priority = 2,
                    action = "UPSERT",
                    jsonPayload = json.encodeToString(coreDto),
                    nowMillis = now
                )
                syncOutboxDao.recordMutationWithCoalescing(
                    targetTable = "siswa_detail",
                    recordId = detailEntity.id,
                    priority = 2,
                    action = "UPSERT",
                    jsonPayload = json.encodeToString(detailDto),
                    nowMillis = now
                )
                syncOutboxDao.recordMutationWithCoalescing(
                    targetTable = "anggota_rombel",
                    recordId = rombelEntity.id,
                    priority = 2,
                    action = "UPSERT",
                    jsonPayload = json.encodeToString(rombelDto),
                    nowMillis = now
                )
            }

            ResultState.Success(coreEntity)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal menyimpan data siswa: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    fun getAllGuru(): Flow<List<GuruEntity>> = masterAcademicDao.getAllGuru()
    fun getAllKelas(): Flow<List<KelasEntity>> = masterAcademicDao.getAllKelas()
    fun getAllMapel(): Flow<List<MapelEntity>> = masterAcademicDao.getAllMapel()
    fun getAllTahunAjaran(): Flow<List<TahunAjaranEntity>> = masterAcademicDao.getAllTahunAjaran()
    fun getActiveTahunAjaran(): Flow<TahunAjaranEntity?> = masterAcademicDao.getActiveTahunAjaran()
    fun getPenugasanByTahunAjaran(tahunAjaranId: String): Flow<List<PenugasanGuruEntity>> =
        masterAcademicDao.getPenugasanByTahunAjaran(tahunAjaranId)
    suspend fun getStudentDetail(siswaId: String): SiswaDetailEntity? =
        studentDao.getSiswaDetailBySiswaId(siswaId)
}
