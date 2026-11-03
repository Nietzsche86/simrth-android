package id.mts.rth.data.repository

import id.mts.rth.data.local.dao.MasterAcademicDao
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk data master akademik (Tahun Ajaran, Jadwal Mengajar, Kelas, Mapel).
 */
class AcademicRepository(
    private val masterAcademicDao: MasterAcademicDao
) {
    fun getActiveTahunAjaran(): Flow<TahunAjaranEntity?> =
        masterAcademicDao.getActiveTahunAjaran()

    fun getAllTahunAjaran(): Flow<List<TahunAjaranEntity>> =
        masterAcademicDao.getAllTahunAjaran()

    fun getAllKelas(): Flow<List<KelasEntity>> =
        masterAcademicDao.getAllKelas()

    fun getAllMapel(): Flow<List<MapelEntity>> =
        masterAcademicDao.getAllMapel()

    fun getPenugasanByGuru(guruId: String, tahunAjaranId: String): Flow<List<PenugasanGuruEntity>> =
        masterAcademicDao.getPenugasanByGuru(guruId, tahunAjaranId)

    fun getPenugasanByTahunAjaran(tahunAjaranId: String): Flow<List<PenugasanGuruEntity>> =
        masterAcademicDao.getPenugasanByTahunAjaran(tahunAjaranId)

    suspend fun getPenugasanById(penugasanId: String): PenugasanGuruEntity? =
        masterAcademicDao.getPenugasanById(penugasanId)

    suspend fun getKelasById(kelasId: String): KelasEntity? =
        masterAcademicDao.getKelasById(kelasId)

    suspend fun getMapelById(mapelId: String): MapelEntity? =
        masterAcademicDao.getMapelById(mapelId)

    suspend fun getGuruById(guruId: String): GuruEntity? =
        masterAcademicDao.getGuruById(guruId)
}
