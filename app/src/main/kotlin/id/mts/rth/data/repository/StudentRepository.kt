package id.mts.rth.data.repository

import id.mts.rth.core.common.ResultState
import id.mts.rth.core.media.StudentPhotoManager
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.data.local.dao.MasterAcademicDao
import id.mts.rth.data.local.dao.StudentDao
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

data class StudentDirectoryItem(
    val siswaId: String,
    val nomorAbsen: Int,
    val nisLokal: String,
    val nisn: String?,
    val namaLengkap: String,
    val jenisKelamin: String,
    val fotoFile: File?,
    val statusSiswa: String
)

/**
 * Repository untuk direktori siswa dan segmentasi keamanan RBAC data EMIS:
 * - Query siswa per rombel terurut nomor absen.
 * - Proteksi akses data detail EMIS hanya untuk Admin & Wali Kelas dari rombel terkait.
 * - Resolusi file foto lokal WebP dari StudentPhotoManager.
 */
class StudentRepository(
    private val studentDao: StudentDao,
    private val masterAcademicDao: MasterAcademicDao,
    private val studentPhotoManager: StudentPhotoManager,
    private val sessionPreferences: SessionPreferences
) {
    /**
     * Mengambil daftar siswa aktif dalam rombel tertentu dengan nomor absen dan file foto lokal.
     */
    fun getStudentsByKelas(kelasId: String, tahunAjaranId: String): Flow<List<StudentDirectoryItem>> {
        return combine(
            studentDao.getSiswaCoreByKelas(kelasId, tahunAjaranId),
            studentDao.getAnggotaRombelByKelas(kelasId, tahunAjaranId)
        ) { siswaList, anggotaList ->
            val absenMap = anggotaList.associateBy { it.siswaId }

            siswaList.mapIndexed { index, siswa ->
                val nomorAbsen = absenMap[siswa.id]?.nomorAbsen ?: (index + 1)
                val photoFile = if (studentPhotoManager.hasPhoto(siswa.fotoFileName)) {
                    studentPhotoManager.getPhotoFile(siswa.fotoFileName!!)
                } else null

                StudentDirectoryItem(
                    siswaId = siswa.id,
                    nomorAbsen = nomorAbsen,
                    nisLokal = siswa.nisLokal,
                    nisn = siswa.nisn,
                    namaLengkap = siswa.namaLengkap,
                    jenisKelamin = siswa.jenisKelamin,
                    fotoFile = photoFile,
                    statusSiswa = siswa.statusSiswa
                )
            }.sortedBy { it.nomorAbsen }
        }
    }

    suspend fun getStudentCoreById(siswaId: String): SiswaCoreEntity? = withContext(Dispatchers.IO) {
        studentDao.getSiswaCoreById(siswaId)
    }

    /**
     * Mengambil data lengkap EMIS siswa dengan validasi hak akses RBAC ketat.
     * HANYA Administrator atau Wali Kelas dari kelas siswa bersangkutan yang berhak mengakses.
     */
    suspend fun getStudentDetailWithRbac(siswaId: String, kelasId: String): ResultState<SiswaDetailEntity?> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            val userRole = session.activeRole
            val userId = session.userId

            val kelas = masterAcademicDao.getKelasById(kelasId)
            val isWaliKelas = !userId.isNullOrBlank() && kelas?.waliKelasGuruId == userId
            val isAdmin = userRole.equals("ADMIN", ignoreCase = true)

            if (!isAdmin && !isWaliKelas) {
                return@withContext ResultState.Error(
                    "Akses Terbatas: Data EMIS hanya dapat diakses oleh Wali Kelas yang membina rombel ini dan Administrator madrasah."
                )
            }

            val detail = studentDao.getSiswaDetailBySiswaId(siswaId)
            ResultState.Success(detail)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal memuat data EMIS: ${e.localizedMessage ?: "Terjadi kesalahan internal."}",
                throwable = e
            )
        }
    }

    fun getPhotoFile(fotoFileName: String?): File? {
        if (fotoFileName.isNullOrBlank()) return null
        return if (studentPhotoManager.hasPhoto(fotoFileName)) {
            studentPhotoManager.getPhotoFile(fotoFileName)
        } else null
    }
}
