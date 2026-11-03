package id.mts.rth.data.repository

import androidx.room.withTransaction
import id.mts.rth.core.common.Constants
import id.mts.rth.core.common.DateTimeUtils
import id.mts.rth.core.common.ResultState
import id.mts.rth.core.media.StudentPhotoManager
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.data.local.db.AppDatabase
import id.mts.rth.data.local.entity.AnggotaRombelEntity
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.NilaiSiswaEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.PresensiDetailEntity
import id.mts.rth.data.local.entity.PresensiSesiEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import id.mts.rth.data.remote.api.SyncApiService
import id.mts.rth.data.remote.dto.OutboxMutationDto
import id.mts.rth.data.remote.dto.SyncPushRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class SyncReport(
    val pushedMutationsCount: Int,
    val pulledEntitiesCount: Int,
    val downloadedPhotosCount: Int,
    val serverTime: Long
)

/**
 * Orkestrator 3 Tahap Sinkronisasi Manual Antara SQLite Lokal dan Server Wi-Fi Madrasah:
 * - Tahap 0: Health Check & Clock Drift Guard (<= 5 menit).
 * - Tahap 1: Push Mutasi Outbox (Client -> Server) dalam chunk 250 item dengan aturan LWW.
 * - Tahap 2: Pull Delta (Server -> Client) dengan proteksi data lokal yang belum tersinkronkan.
 * - Tahap 3: Background Media Sync (Foto Siswa WebP).
 */
class SyncEngineRepository(
    private val appDatabase: AppDatabase,
    private val syncApiService: SyncApiService,
    private val sessionPreferences: SessionPreferences,
    private val studentPhotoManager: StudentPhotoManager
) {

    suspend fun performManualSync(): ResultState<SyncReport> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            val baseUrl = session.serverUrl
            val token = session.authToken

            if (token.isNullOrBlank()) {
                return@withContext ResultState.Error("Perangkat belum terautentikasi. Silakan login terlebih dahulu.")
            }

            // =================================================================
            // TAHAP 0: HEALTH CHECK & CLOCK DRIFT PROTECTION
            // =================================================================
            val health = syncApiService.checkHealth(baseUrl)
            val deviceTime = System.currentTimeMillis()

            if (!DateTimeUtils.isClockDriftAcceptable(deviceTime, health.serverTime)) {
                val driftMinutes = DateTimeUtils.getClockDriftMinutes(deviceTime, health.serverTime)
                return@withContext ResultState.Error(
                    "Waktu perangkat berselisih $driftMinutes menit dari server madrasah. " +
                        "Sinkronisasi ditolak. Silakan aktifkan pengaturan waktu otomatis di HP Anda."
                )
            }

            // =================================================================
            // TAHAP 1: PUSH MUTASI LOKAL (CLIENT -> SERVER)
            // =================================================================
            val outboxDao = appDatabase.syncOutboxDao()
            var totalPushed = 0

            while (true) {
                val pendingBatch = outboxDao.getPendingOutboxBatch(Constants.SYNC_BATCH_SIZE)
                if (pendingBatch.isEmpty()) break

                val mutationsDto = pendingBatch.map {
                    OutboxMutationDto(
                        id = it.id,
                        tableNameTarget = it.tableNameTarget,
                        priorityOrder = it.priorityOrder,
                        recordId = it.recordId,
                        actionType = it.actionType,
                        payloadJson = it.payloadJson,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }

                val pushResponse = syncApiService.pushMutations(
                    baseUrl = baseUrl,
                    token = token,
                    request = SyncPushRequest(mutations = mutationsDto)
                )

                if (pushResponse.acceptedMutationIds.isNotEmpty()) {
                    outboxDao.deleteOutboxByIds(pushResponse.acceptedMutationIds)
                    totalPushed += pushResponse.acceptedMutationIds.size
                }

                if (pendingBatch.size < Constants.SYNC_BATCH_SIZE) {
                    break
                }
            }

            // =================================================================
            // TAHAP 2: PULL DELTA (SERVER -> CLIENT)
            // =================================================================
            val pullResponse = syncApiService.pullDelta(
                baseUrl = baseUrl,
                token = token,
                sinceTimestamp = session.lastSyncTimestamp
            )

            // Dapatkan set ID record lokal yang masih tertahan di antrean outbox
            // agar data lokal yang baru diubah tidak tertimpa payload lama dari server
            val pendingRecordIds = outboxDao.getPendingOutboxBatch(1000)
                .map { it.recordId }
                .toSet()

            var totalPulled = 0

            appDatabase.withTransaction {
                // 1. Master Akademik
                if (pullResponse.tahunAjaranList.isNotEmpty()) {
                    appDatabase.masterAcademicDao().upsertTahunAjaran(
                        pullResponse.tahunAjaranList.map {
                            TahunAjaranEntity(
                                id = it.id,
                                tahun = it.tahun,
                                semester = it.semester,
                                isActive = it.isActive,
                                tanggalMulai = it.tanggalMulai,
                                tanggalSelesai = it.tanggalSelesai,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.tahunAjaranList.size
                }

                if (pullResponse.guruList.isNotEmpty()) {
                    appDatabase.masterAcademicDao().upsertGuru(
                        pullResponse.guruList.map {
                            GuruEntity(
                                id = it.id,
                                nip = it.nip,
                                npk = it.npk,
                                nuptk = it.nuptk,
                                nik = it.nik,
                                namaLengkap = it.namaLengkap,
                                gelarDepan = it.gelarDepan,
                                gelarBelakang = it.gelarBelakang,
                                jenisKelamin = it.jenisKelamin,
                                noWa = it.noWa,
                                statusKepegawaian = it.statusKepegawaian,
                                role = it.role,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.guruList.size
                }

                if (pullResponse.kelasList.isNotEmpty()) {
                    appDatabase.masterAcademicDao().upsertKelas(
                        pullResponse.kelasList.map {
                            KelasEntity(
                                id = it.id,
                                tingkat = it.tingkat,
                                namaRombel = it.namaRombel,
                                waliKelasGuruId = it.waliKelasGuruId,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.kelasList.size
                }

                if (pullResponse.mapelList.isNotEmpty()) {
                    appDatabase.masterAcademicDao().upsertMapel(
                        pullResponse.mapelList.map {
                            MapelEntity(
                                id = it.id,
                                kodeMapel = it.kodeMapel,
                                namaMapel = it.namaMapel,
                                kelompok = it.kelompok,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.mapelList.size
                }

                if (pullResponse.penugasanList.isNotEmpty()) {
                    appDatabase.masterAcademicDao().upsertPenugasanGuru(
                        pullResponse.penugasanList.map {
                            PenugasanGuruEntity(
                                id = it.id,
                                guruId = it.guruId,
                                mapelId = it.mapelId,
                                kelasId = it.kelasId,
                                tahunAjaranId = it.tahunAjaranId,
                                jadwalJson = it.jadwalJson,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.penugasanList.size
                }

                // 2. Data Siswa Core & Detail
                val filteredSiswaCore = pullResponse.siswaCoreList
                    .filter { it.id !in pendingRecordIds }
                    .map {
                        SiswaCoreEntity(
                            id = it.id,
                            nisLokal = it.nisLokal,
                            nisn = it.nisn,
                            namaLengkap = it.namaLengkap,
                            jenisKelamin = it.jenisKelamin,
                            fotoFileName = it.fotoFileName,
                            statusSiswa = it.statusSiswa,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredSiswaCore.isNotEmpty()) {
                    appDatabase.studentDao().upsertSiswaCore(filteredSiswaCore)
                    totalPulled += filteredSiswaCore.size
                }

                val filteredSiswaDetail = pullResponse.siswaDetailList
                    .filter { it.id !in pendingRecordIds && it.siswaId !in pendingRecordIds }
                    .map {
                        SiswaDetailEntity(
                            id = it.id,
                            siswaId = it.siswaId,
                            nik = it.nik,
                            noKk = it.noKk,
                            tempatLahir = it.tempatLahir,
                            tanggalLahir = it.tanggalLahir,
                            agama = it.agama,
                            anakKe = it.anakKe,
                            jumlahSaudara = it.jumlahSaudara,
                            alamatJalan = it.alamatJalan,
                            rt = it.rt,
                            rw = it.rw,
                            desaKelurahan = it.desaKelurahan,
                            kecamatan = it.kecamatan,
                            kabupatenKota = it.kabupatenKota,
                            kodePos = it.kodePos,
                            statusTempatTinggal = it.statusTempatTinggal,
                            nikAyah = it.nikAyah,
                            namaAyah = it.namaAyah,
                            tahunLahirAyah = it.tahunLahirAyah,
                            statusAyah = it.statusAyah,
                            pendidikanAyah = it.pendidikanAyah,
                            pekerjaanAyah = it.pekerjaanAyah,
                            penghasilanAyah = it.penghasilanAyah,
                            nikIbu = it.nikIbu,
                            namaIbu = it.namaIbu,
                            tahunLahirIbu = it.tahunLahirIbu,
                            statusIbu = it.statusIbu,
                            pendidikanIbu = it.pendidikanIbu,
                            pekerjaanIbu = it.pekerjaanIbu,
                            penghasilanIbu = it.penghasilanIbu,
                            noHpOrangTuaWa = it.noHpOrangTuaWa,
                            namaWali = it.namaWali,
                            hubunganWali = it.hubunganWali,
                            noHpWali = it.noHpWali,
                            noKip = it.noKip,
                            noKisPkh = it.noKisPkh,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredSiswaDetail.isNotEmpty()) {
                    appDatabase.studentDao().upsertSiswaDetail(filteredSiswaDetail)
                    totalPulled += filteredSiswaDetail.size
                }

                if (pullResponse.anggotaRombelList.isNotEmpty()) {
                    appDatabase.studentDao().upsertAnggotaRombel(
                        pullResponse.anggotaRombelList.map {
                            AnggotaRombelEntity(
                                id = it.id,
                                siswaId = it.siswaId,
                                kelasId = it.kelasId,
                                tahunAjaranId = it.tahunAjaranId,
                                nomorAbsen = it.nomorAbsen,
                                statusKeanggotaan = it.statusKeanggotaan,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.anggotaRombelList.size
                }

                // 3. Presensi
                val filteredSesi = pullResponse.presensiSesiList
                    .filter { it.id !in pendingRecordIds }
                    .map {
                        PresensiSesiEntity(
                            id = it.id,
                            penugasanId = it.penugasanId,
                            tanggal = it.tanggal,
                            jamKe = it.jamKe,
                            materiPokok = it.materiPokok,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredSesi.isNotEmpty()) {
                    appDatabase.attendanceDao().upsertSessions(filteredSesi)
                    totalPulled += filteredSesi.size
                }

                val filteredDetailPresensi = pullResponse.presensiDetailList
                    .filter { it.id !in pendingRecordIds }
                    .map {
                        PresensiDetailEntity(
                            id = it.id,
                            sesiId = it.sesiId,
                            siswaId = it.siswaId,
                            statusKehadiran = it.statusKehadiran,
                            catatan = it.catatan,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredDetailPresensi.isNotEmpty()) {
                    appDatabase.attendanceDao().insertOrUpdateDetails(filteredDetailPresensi)
                    totalPulled += filteredDetailPresensi.size
                }

                // 4. Penilaian & KKM
                if (pullResponse.kkmList.isNotEmpty()) {
                    appDatabase.gradingDao().upsertKkm(
                        pullResponse.kkmList.map {
                            KkmMapelEntity(
                                id = it.id,
                                mapelId = it.mapelId,
                                tingkatKelas = it.tingkatKelas,
                                tahunAjaranId = it.tahunAjaranId,
                                nilaiKkm = it.nilaiKkm,
                                bobotHarian = it.bobotHarian,
                                bobotUlangan = it.bobotUlangan,
                                bobotUjianAkhir = it.bobotUjianAkhir,
                                isDeleted = it.isDeleted,
                                updatedAt = it.updatedAt
                            )
                        }
                    )
                    totalPulled += pullResponse.kkmList.size
                }

                val filteredKomponen = pullResponse.komponenList
                    .filter { it.id !in pendingRecordIds }
                    .map {
                        KomponenNilaiEntity(
                            id = it.id,
                            penugasanId = it.penugasanId,
                            aspek = it.aspek,
                            kategori = it.kategori,
                            judul = it.judul,
                            tanggalInput = it.tanggalInput,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredKomponen.isNotEmpty()) {
                    appDatabase.gradingDao().upsertKomponenList(filteredKomponen)
                    totalPulled += filteredKomponen.size
                }

                val filteredNilai = pullResponse.nilaiList
                    .filter { it.id !in pendingRecordIds }
                    .map {
                        NilaiSiswaEntity(
                            id = it.id,
                            komponenId = it.komponenId,
                            siswaId = it.siswaId,
                            skor = it.skor,
                            isDeleted = it.isDeleted,
                            syncStatus = "SYNCED",
                            updatedAt = it.updatedAt
                        )
                    }
                if (filteredNilai.isNotEmpty()) {
                    appDatabase.gradingDao().upsertNilaiList(filteredNilai)
                    totalPulled += filteredNilai.size
                }
            }

            // Simpan timestamp sinkronisasi terbaru
            sessionPreferences.updateLastSyncTimestamp(pullResponse.serverTime)

            // =================================================================
            // TAHAP 3: MEDIA SYNC (UNDUH FOTO SISWA WEBP)
            // =================================================================
            var downloadedPhotos = 0
            val allSiswa = appDatabase.studentDao().getAllSiswaCore().first()

            for (siswa in allSiswa) {
                val fileName = siswa.fotoFileName
                if (!fileName.isNullOrBlank() && !studentPhotoManager.hasPhoto(fileName)) {
                    val photoBytes = syncApiService.downloadStudentPhoto(
                        baseUrl = baseUrl,
                        token = token,
                        fileName = fileName
                    )
                    if (photoBytes != null && photoBytes.isNotEmpty()) {
                        studentPhotoManager.savePhotoBytes(fileName, photoBytes)
                        downloadedPhotos++
                    }
                }
            }

            ResultState.Success(
                SyncReport(
                    pushedMutationsCount = totalPushed,
                    pulledEntitiesCount = totalPulled,
                    downloadedPhotosCount = downloadedPhotos,
                    serverTime = pullResponse.serverTime
                )
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal melakukan sinkronisasi: ${e.localizedMessage ?: "Terjadi kesalahan jaringan."}",
                throwable = e
            )
        }
    }
}
