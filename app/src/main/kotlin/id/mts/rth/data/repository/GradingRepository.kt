package id.mts.rth.data.repository

import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.dao.GradingDao
import id.mts.rth.data.local.dao.SyncOutboxDao
import id.mts.rth.data.local.db.AppDatabase
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.data.local.entity.NilaiSiswaEntity
import id.mts.rth.data.remote.dto.KomponenNilaiDto
import id.mts.rth.data.remote.dto.NilaiSiswaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository untuk Modul Penilaian K13:
 * - Mengambil dan mengelola kolom komponen penilaian dinamis (KI-3 & KI-4).
 * - Mengambil dan menyimpan skor nilai siswa secara batch dalam transaksi atomik Room.
 * - Mengelola data konfigurasi KKM dan pembobotan nilai per mata pelajaran.
 * - Mencatat mutasi ke sync_outbox dengan aturan Coalescing (Komponen = Priority 1, Nilai = Priority 2).
 */
class GradingRepository(
    private val gradingDao: GradingDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val appDatabase: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getKomponenByPenugasan(penugasanId: String, aspek: String): Flow<List<KomponenNilaiEntity>> {
        return gradingDao.getKomponenByPenugasan(penugasanId, aspek)
    }

    fun getAllKomponenByPenugasan(penugasanId: String): Flow<List<KomponenNilaiEntity>> {
        return gradingDao.getAllKomponenByPenugasan(penugasanId)
    }

    suspend fun getKomponenById(komponenId: String): KomponenNilaiEntity? = withContext(Dispatchers.IO) {
        gradingDao.getKomponenById(komponenId)
    }

    fun getNilaiByKomponen(komponenId: String): Flow<List<NilaiSiswaEntity>> {
        return gradingDao.getNilaiByKomponen(komponenId)
    }

    suspend fun getNilaiByKomponenList(komponenId: String): List<NilaiSiswaEntity> = withContext(Dispatchers.IO) {
        gradingDao.getNilaiByKomponenList(komponenId)
    }

    fun getNilaiSiswaByPenugasan(penugasanId: String): Flow<List<NilaiSiswaEntity>> {
        return gradingDao.getNilaiSiswaByPenugasan(penugasanId)
    }

    fun getKkm(mapelId: String, tingkatKelas: Int, tahunAjaranId: String): Flow<KkmMapelEntity?> {
        return gradingDao.getKkm(mapelId, tingkatKelas, tahunAjaranId)
    }

    suspend fun getKkmSync(mapelId: String, tingkatKelas: Int, tahunAjaranId: String): KkmMapelEntity? = withContext(Dispatchers.IO) {
        gradingDao.getKkmSync(mapelId, tingkatKelas, tahunAjaranId)
    }

    /**
     * Membuat kolom komponen nilai baru (misal: "Tugas 1", "UH Bab 2", "Praktik Sholat")
     * dan mencatatkan mutasi ke outbox (Priority 1 = Parent).
     */
    suspend fun createKomponen(
        penugasanId: String,
        aspek: String, // "KI3" atau "KI4"
        kategori: String, // "TUGAS", "UH", "UJIAN_AKHIR", "PRAKTIK", "PROYEK", "PORTOFOLIO"
        judul: String,
        tanggalInput: String
    ): ResultState<KomponenNilaiEntity> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()

            val komponen = KomponenNilaiEntity(
                id = id,
                penugasanId = penugasanId,
                aspek = aspek,
                kategori = kategori,
                judul = judul,
                tanggalInput = tanggalInput,
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = now
            )

            val dto = KomponenNilaiDto(
                id = komponen.id,
                penugasanId = komponen.penugasanId,
                aspek = komponen.aspek,
                kategori = komponen.kategori,
                judul = komponen.judul,
                tanggalInput = komponen.tanggalInput,
                isDeleted = false,
                syncStatus = "PENDING",
                updatedAt = komponen.updatedAt
            )
            val komponenJson = json.encodeToString(dto)

            gradingDao.saveBatchGradesAndOutbox(
                komponen = komponen,
                nilaiList = emptyList(),
                outboxDao = syncOutboxDao,
                komponenJson = komponenJson,
                nilaiJsonList = emptyList()
            )

            ResultState.Success(komponen)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal membuat kolom nilai: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    /**
     * Menyimpan batch skor nilai siswa untuk komponen tertentu ke Room SQLite
     * dan mencatatkan mutasi ke outbox (Priority 2 = Child).
     */
    suspend fun saveBatchGrades(
        komponenId: String,
        scoresMap: Map<String, Float> // Map<siswaId, skor>
    ): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val existingNilaiList = gradingDao.getNilaiByKomponenList(komponenId)
            val existingMap = existingNilaiList.associateBy { it.siswaId }

            val entities = mutableListOf<NilaiSiswaEntity>()
            val jsonList = mutableListOf<Pair<String, String>>()

            for ((siswaId, skor) in scoresMap) {
                val clampedScore = skor.coerceIn(0.0f, 100.0f)
                val existing = existingMap[siswaId]
                val recordId = existing?.id ?: UUID.randomUUID().toString()

                val entity = NilaiSiswaEntity(
                    id = recordId,
                    komponenId = komponenId,
                    siswaId = siswaId,
                    skor = clampedScore,
                    isDeleted = false,
                    syncStatus = "PENDING",
                    updatedAt = now
                )
                entities.add(entity)

                val dto = NilaiSiswaDto(
                    id = entity.id,
                    komponenId = entity.komponenId,
                    siswaId = entity.siswaId,
                    skor = entity.skor,
                    isDeleted = false,
                    syncStatus = "PENDING",
                    updatedAt = entity.updatedAt
                )
                jsonList.add(Pair(recordId, json.encodeToString(dto)))
            }

            gradingDao.saveBatchGradesAndOutbox(
                komponen = null,
                nilaiList = entities,
                outboxDao = syncOutboxDao,
                komponenJson = null,
                nilaiJsonList = jsonList
            )

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal menyimpan nilai siswa: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }
}
