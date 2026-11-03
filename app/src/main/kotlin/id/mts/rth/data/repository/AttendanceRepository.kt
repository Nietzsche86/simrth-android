package id.mts.rth.data.repository

import androidx.room.withTransaction
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.dao.AttendanceDao
import id.mts.rth.data.local.dao.StudentDao
import id.mts.rth.data.local.dao.SyncOutboxDao
import id.mts.rth.data.local.db.AppDatabase
import id.mts.rth.data.local.entity.PresensiDetailEntity
import id.mts.rth.data.local.entity.PresensiSesiEntity
import id.mts.rth.data.remote.dto.PresensiDetailDto
import id.mts.rth.data.remote.dto.PresensiSesiDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class AttendanceStudentItem(
    val siswaId: String,
    val nomorAbsen: Int,
    val nisLokal: String,
    val nisn: String?,
    val namaLengkap: String,
    val jenisKelamin: String,
    val fotoFileName: String?
)

data class StudentAttendanceDetailInput(
    val siswaId: String,
    val statusKehadiran: String,
    val catatan: String? = null
)

/**
 * Repository untuk Fast Attendance Engine:
 * - Query siswa per rombel terurut nomor absen.
 * - Simpan sesi presensi & detail siswa secara atomik ke Room SQLite.
 * - Coalesce rekaman mutasi ke sync_outbox (Priority 1 Parent, Priority 2 Child).
 */
class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val appDatabase: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Mengambil daftar siswa aktif dalam suatu kelas dan tahun ajaran,
     * terurut berdasarkan nomor absen.
     */
    fun getStudentsByKelas(kelasId: String, tahunAjaranId: String): Flow<List<AttendanceStudentItem>> {
        return combine(
            studentDao.getSiswaCoreByKelas(kelasId, tahunAjaranId),
            studentDao.getAnggotaRombelByKelas(kelasId, tahunAjaranId)
        ) { siswaList, anggotaList ->
            val absenMap = anggotaList.associateBy { it.siswaId }
            siswaList.mapIndexed { index, siswa ->
                val nomorAbsen = absenMap[siswa.id]?.nomorAbsen ?: (index + 1)
                AttendanceStudentItem(
                    siswaId = siswa.id,
                    nomorAbsen = nomorAbsen,
                    nisLokal = siswa.nisLokal,
                    nisn = siswa.nisn,
                    namaLengkap = siswa.namaLengkap,
                    jenisKelamin = siswa.jenisKelamin,
                    fotoFileName = siswa.fotoFileName
                )
            }.sortedBy { it.nomorAbsen }
        }
    }

    /**
     * Mengambil sesi presensi pada tanggal tertentu untuk jadwal mengajar (penugasan) tertentu.
     */
    fun getSessionByPenugasanAndDate(penugasanId: String, tanggal: String): Flow<List<PresensiSesiEntity>> {
        return attendanceDao.getSessionByPenugasanAndDate(penugasanId, tanggal)
    }

    /**
     * Mengambil seluruh detail presensi siswa untuk sesi tertentu.
     */
    fun getDetailsBySesiId(sesiId: String): Flow<List<PresensiDetailEntity>> {
        return attendanceDao.getDetailsBySesiId(sesiId)
    }

    /**
     * Mengambil detail presensi siswa dalam bentuk List secara sinkron.
     */
    suspend fun getDetailsBySesiIdList(sesiId: String): List<PresensiDetailEntity> = withContext(Dispatchers.IO) {
        attendanceDao.getDetailsBySesiIdList(sesiId)
    }

    /**
     * Menyimpan atau memperbarui sesi presensi beserta seluruh detail kehadiran siswa
     * secara atomik (@Transaction) ke Room dan mencatatkannya ke sync_outbox.
     */
    suspend fun saveAttendance(
        sesiId: String?,
        penugasanId: String,
        tanggal: String,
        jamKe: String,
        materiPokok: String?,
        details: List<StudentAttendanceDetailInput>
    ): ResultState<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val finalSesiId = if (sesiId.isNullOrBlank()) UUID.randomUUID().toString() else sesiId

            // Periksa apakah sesi sebelumnya sudah ada untuk mempertahankan createdAt
            val existingSesi = attendanceDao.getSessionById(finalSesiId)
            val createdAt = existingSesi?.createdAt ?: now

            val sesiEntity = PresensiSesiEntity(
                id = finalSesiId,
                penugasanId = penugasanId,
                tanggal = tanggal,
                jamKe = jamKe,
                materiPokok = materiPokok,
                isDeleted = false,
                syncStatus = "PENDING",
                createdAt = createdAt,
                updatedAt = now
            )

            val sesiDto = PresensiSesiDto(
                id = sesiEntity.id,
                penugasanId = sesiEntity.penugasanId,
                tanggal = sesiEntity.tanggal,
                jamKe = sesiEntity.jamKe,
                materiPokok = sesiEntity.materiPokok,
                isDeleted = false,
                syncStatus = "PENDING",
                createdAt = sesiEntity.createdAt,
                updatedAt = sesiEntity.updatedAt
            )
            val sesiJson = json.encodeToString(sesiDto)

            // Ambil detail yang sudah ada untuk mempertahankan ID record jika update
            val existingDetails = attendanceDao.getDetailsBySesiIdList(finalSesiId)
            val existingDetailMap = existingDetails.associateBy { it.siswaId }

            val detailEntities = mutableListOf<PresensiDetailEntity>()
            val detailJsonList = mutableListOf<Pair<String, String>>()

            for (input in details) {
                val existing = existingDetailMap[input.siswaId]
                val detailId = existing?.id ?: UUID.randomUUID().toString()

                val entity = PresensiDetailEntity(
                    id = detailId,
                    sesiId = finalSesiId,
                    siswaId = input.siswaId,
                    statusKehadiran = input.statusKehadiran,
                    catatan = input.catatan,
                    isDeleted = false,
                    syncStatus = "PENDING",
                    updatedAt = now
                )
                detailEntities.add(entity)

                val dto = PresensiDetailDto(
                    id = entity.id,
                    sesiId = entity.sesiId,
                    siswaId = entity.siswaId,
                    statusKehadiran = entity.statusKehadiran,
                    catatan = entity.catatan,
                    isDeleted = false,
                    syncStatus = "PENDING",
                    updatedAt = entity.updatedAt
                )
                detailJsonList.add(Pair(detailId, json.encodeToString(dto)))
            }

            // Simpan atomik ke Room & Outbox
            attendanceDao.saveAttendanceSessionAndOutbox(
                sesi = sesiEntity,
                details = detailEntities,
                outboxDao = syncOutboxDao,
                sesiJson = sesiJson,
                detailJsonList = detailJsonList
            )

            ResultState.Success(finalSesiId)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal menyimpan presensi: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }
}
