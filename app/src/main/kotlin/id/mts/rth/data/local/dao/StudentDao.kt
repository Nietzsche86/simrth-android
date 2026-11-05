package id.mts.rth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.mts.rth.data.local.entity.AnggotaRombelEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    // Siswa Core Queries (Accessible by all subject teachers teaching the class)
    @Query("""
        SELECT sc.* FROM siswa_core sc
        INNER JOIN anggota_rombel ar ON sc.id = ar.siswaId
        WHERE ar.kelasId = :kelasId 
          AND ar.tahunAjaranId = :tahunAjaranId 
          AND sc.isDeleted = 0 
          AND ar.isDeleted = 0
        ORDER BY ar.nomorAbsen ASC
    """)
    fun getSiswaCoreByKelas(kelasId: String, tahunAjaranId: String): Flow<List<SiswaCoreEntity>>

    @Query("SELECT * FROM siswa_core WHERE id = :siswaId AND isDeleted = 0 LIMIT 1")
    suspend fun getSiswaCoreById(siswaId: String): SiswaCoreEntity?

    @Query("SELECT * FROM siswa_core WHERE isDeleted = 0 ORDER BY namaLengkap ASC")
    fun getAllSiswaCore(): Flow<List<SiswaCoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiswaCore(entities: List<SiswaCoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingleSiswaCore(entity: SiswaCoreEntity)

    // Siswa Detail Queries (Strict RBAC: ONLY downloaded/queried by Wali Kelas & Admin)
    @Query("SELECT * FROM siswa_detail WHERE siswaId = :siswaId AND isDeleted = 0 LIMIT 1")
    suspend fun getSiswaDetailBySiswaId(siswaId: String): SiswaDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiswaDetail(entities: List<SiswaDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingleSiswaDetail(entity: SiswaDetailEntity)

    // Anggota Rombel Queries
    @Query("SELECT * FROM anggota_rombel WHERE kelasId = :kelasId AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 ORDER BY nomorAbsen ASC")
    fun getAnggotaRombelByKelas(kelasId: String, tahunAjaranId: String): Flow<List<AnggotaRombelEntity>>

    @Query("SELECT * FROM anggota_rombel WHERE kelasId = :kelasId AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 ORDER BY nomorAbsen ASC")
    suspend fun getAnggotaRombelByKelasSync(kelasId: String, tahunAjaranId: String): List<AnggotaRombelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnggotaRombel(entities: List<AnggotaRombelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingleAnggotaRombel(entity: AnggotaRombelEntity)

    @Query("SELECT * FROM anggota_rombel WHERE siswaId = :siswaId AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 LIMIT 1")
    suspend fun getAnggotaRombelBySiswaAndTahun(siswaId: String, tahunAjaranId: String): AnggotaRombelEntity?

    @Query("SELECT COUNT(*) > 0 FROM siswa_core WHERE nisLokal = :nisLokal AND id != :excludeSiswaId AND isDeleted = 0")
    suspend fun isNisLokalTaken(nisLokal: String, excludeSiswaId: String): Boolean
}
