package id.mts.rth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.data.local.entity.NilaiSiswaEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GradingDao {
    // KKM & Bobot Mapel
    @Query("SELECT * FROM kkm_mapel WHERE mapelId = :mapelId AND tingkatKelas = :tingkatKelas AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 LIMIT 1")
    abstract fun getKkm(mapelId: String, tingkatKelas: Int, tahunAjaranId: String): Flow<KkmMapelEntity?>

    @Query("SELECT * FROM kkm_mapel WHERE mapelId = :mapelId AND tingkatKelas = :tingkatKelas AND tahunAjaranId = :tahunAjaranId AND isDeleted = 0 LIMIT 1")
    abstract suspend fun getKkmSync(mapelId: String, tingkatKelas: Int, tahunAjaranId: String): KkmMapelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertKkm(entities: List<KkmMapelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSingleKkm(entity: KkmMapelEntity)

    // Komponen Penilaian (Kolom Dinamis)
    @Query("SELECT * FROM komponen_nilai WHERE penugasanId = :penugasanId AND aspek = :aspek AND isDeleted = 0 ORDER BY tanggalInput ASC")
    abstract fun getKomponenByPenugasan(penugasanId: String, aspek: String): Flow<List<KomponenNilaiEntity>>

    @Query("SELECT * FROM komponen_nilai WHERE penugasanId = :penugasanId AND isDeleted = 0 ORDER BY aspek ASC, tanggalInput ASC")
    abstract fun getAllKomponenByPenugasan(penugasanId: String): Flow<List<KomponenNilaiEntity>>

    @Query("SELECT * FROM komponen_nilai WHERE id = :komponenId AND isDeleted = 0 LIMIT 1")
    abstract suspend fun getKomponenById(komponenId: String): KomponenNilaiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateKomponen(entity: KomponenNilaiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertKomponenList(entities: List<KomponenNilaiEntity>)

    // Skor Nilai Siswa
    @Query("SELECT * FROM nilai_siswa WHERE komponenId = :komponenId AND isDeleted = 0")
    abstract fun getNilaiByKomponen(komponenId: String): Flow<List<NilaiSiswaEntity>>

    @Query("SELECT * FROM nilai_siswa WHERE komponenId = :komponenId AND isDeleted = 0")
    abstract suspend fun getNilaiByKomponenList(komponenId: String): List<NilaiSiswaEntity>

    @Query("""
        SELECT ns.* FROM nilai_siswa ns
        INNER JOIN komponen_nilai kn ON ns.komponenId = kn.id
        WHERE kn.penugasanId = :penugasanId AND ns.isDeleted = 0 AND kn.isDeleted = 0
    """)
    abstract fun getNilaiSiswaByPenugasan(penugasanId: String): Flow<List<NilaiSiswaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateNilaiBatch(entities: List<NilaiSiswaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertNilaiList(entities: List<NilaiSiswaEntity>)

    /**
     * Menyimpan komponen nilai beserta batch skor nilai siswa secara atomik ke Room
     * dan mencatatkan mutasi ke SyncOutboxDao dengan skema Coalescing.
     */
    @Transaction
    open suspend fun saveBatchGradesAndOutbox(
        komponen: KomponenNilaiEntity?,
        nilaiList: List<NilaiSiswaEntity>,
        outboxDao: SyncOutboxDao,
        komponenJson: String?,
        nilaiJsonList: List<Pair<String, String>> // (nilaiId, jsonPayload)
    ) {
        if (komponen != null) {
            insertOrUpdateKomponen(komponen)
            if (komponenJson != null) {
                outboxDao.recordMutationWithCoalescing(
                    targetTable = "komponen_nilai",
                    recordId = komponen.id,
                    priority = 1, // Parent
                    action = "UPSERT",
                    jsonPayload = komponenJson,
                    nowMillis = komponen.updatedAt
                )
            }
        }

        if (nilaiList.isNotEmpty()) {
            insertOrUpdateNilaiBatch(nilaiList)
            val now = System.currentTimeMillis()
            for ((nilaiId, payload) in nilaiJsonList) {
                outboxDao.recordMutationWithCoalescing(
                    targetTable = "nilai_siswa",
                    recordId = nilaiId,
                    priority = 2, // Child
                    action = "UPSERT",
                    jsonPayload = payload,
                    nowMillis = now
                )
            }
        }
    }
}
