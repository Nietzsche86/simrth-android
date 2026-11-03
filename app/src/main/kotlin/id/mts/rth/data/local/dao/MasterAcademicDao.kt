package id.mts.rth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterAcademicDao {
    // Tahun Ajaran
    @Query("SELECT * FROM tahun_ajaran WHERE isActive = 1 AND isDeleted = 0 LIMIT 1")
    fun getActiveTahunAjaran(): Flow<TahunAjaranEntity?>

    @Query("SELECT * FROM tahun_ajaran WHERE isDeleted = 0 ORDER BY tahun DESC, semester ASC")
    fun getAllTahunAjaran(): Flow<List<TahunAjaranEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTahunAjaran(entities: List<TahunAjaranEntity>)

    // Master Guru
    @Query("SELECT * FROM master_guru WHERE id = :guruId AND isDeleted = 0 LIMIT 1")
    suspend fun getGuruById(guruId: String): GuruEntity?

    @Query("SELECT * FROM master_guru WHERE isDeleted = 0 ORDER BY namaLengkap ASC")
    fun getAllGuru(): Flow<List<GuruEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGuru(entities: List<GuruEntity>)

    // Master Kelas
    @Query("SELECT * FROM master_kelas WHERE isDeleted = 0 ORDER BY tingkat ASC, namaRombel ASC")
    fun getAllKelas(): Flow<List<KelasEntity>>

    @Query("SELECT * FROM master_kelas WHERE id = :kelasId AND isDeleted = 0 LIMIT 1")
    suspend fun getKelasById(kelasId: String): KelasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKelas(entities: List<KelasEntity>)

    // Master Mapel
    @Query("SELECT * FROM master_mapel WHERE isDeleted = 0 ORDER BY kelompok ASC, namaMapel ASC")
    fun getAllMapel(): Flow<List<MapelEntity>>

    @Query("SELECT * FROM master_mapel WHERE id = :mapelId AND isDeleted = 0 LIMIT 1")
    suspend fun getMapelById(mapelId: String): MapelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapel(entities: List<MapelEntity>)

    // Penugasan & Jadwal Guru
    @Query("SELECT * FROM penugasan_guru WHERE guruId = :guruId AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0")
    fun getPenugasanByGuru(guruId: String, tahunAjaranId: String): Flow<List<PenugasanGuruEntity>>

    @Query("SELECT * FROM penugasan_guru WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getPenugasanById(id: String): PenugasanGuruEntity?

    @Query("SELECT * FROM penugasan_guru WHERE kelasId = :kelasId AND mapelId = :mapelId AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 LIMIT 1")
    suspend fun getPenugasanByKelasAndMapel(kelasId: String, mapelId: String, tahunAjaranId: String): PenugasanGuruEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPenugasanGuru(entities: List<PenugasanGuruEntity>)
}
