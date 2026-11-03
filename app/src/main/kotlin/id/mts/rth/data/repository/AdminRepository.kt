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
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.remote.dto.AnggotaRombelDto
import id.mts.rth.data.remote.dto.KkmMapelDto
import id.mts.rth.data.remote.dto.SiswaCoreDto
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
}
