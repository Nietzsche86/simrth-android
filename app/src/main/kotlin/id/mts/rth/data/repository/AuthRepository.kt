package id.mts.rth.data.repository

import androidx.room.withTransaction
import id.mts.rth.core.common.ResultState
import id.mts.rth.core.security.PinCryptoManager
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.core.security.UserSession
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
import id.mts.rth.data.remote.api.AuthApiService
import id.mts.rth.data.remote.dto.InitialSnapshotDto
import id.mts.rth.data.remote.dto.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository untuk menangani autentikasi awal di Wi-Fi madrasah,
 * penyemaian data snapshot awal (initial seed), dan verifikasi PIN offline harian di kelas.
 */
class AuthRepository(
    private val authApiService: AuthApiService,
    private val sessionPreferences: SessionPreferences,
    private val appDatabase: AppDatabase
) {

    val sessionFlow: Flow<UserSession> = sessionPreferences.sessionFlow

    /**
     * Melakukan login perdana ke server lokal, menghasilkan salt & hash PIN offline,
     * menyimpan sesi login, dan menyemai initial snapshot data ke Room SQLite.
     */
    suspend fun loginAndInitialSeed(
        serverUrl: String,
        nipOrUsername: String,
        password: String,
        pin6Digit: String
    ): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Request Login ke Server Lokal
            val response = authApiService.login(
                baseUrl = serverUrl,
                request = LoginRequest(nipOrUsername = nipOrUsername, password = password)
            )

            // 2. Hash PIN 6 Digit dengan PBKDF2 & Salt 16-Byte
            val salt = PinCryptoManager.generateSalt()
            val saltBase64 = PinCryptoManager.saltToBase64(salt)
            val pinHashBase64 = PinCryptoManager.hashPin(pin6Digit, salt)

            // 3. Simpan Sesi dan Kredensial PIN di DataStore
            sessionPreferences.saveLoginSession(
                token = response.token,
                userId = response.user.id,
                userName = response.user.namaLengkap,
                role = response.user.role,
                serverUrl = serverUrl
            )
            sessionPreferences.savePin(pinHash = pinHashBase64, pinSalt = saltBase64)

            // 4. Semai Snapshot Data Awal (Jika Ada dari Server)
            val snapshot = response.initialSnapshot
            if (snapshot != null) {
                seedInitialSnapshot(snapshot)
                sessionPreferences.updateLastSyncTimestamp(snapshot.serverTime)
            }

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                message = "Gagal login ke server madrasah: ${e.localizedMessage ?: "Koneksi terputus."}",
                throwable = e
            )
        }
    }

    /**
     * Memverifikasi PIN offline harian di ruang kelas tanpa koneksi jaringan (< 50 ms).
     */
    suspend fun verifyOfflinePin(inputPin: String): Boolean = withContext(Dispatchers.Default) {
        val session = sessionPreferences.sessionFlow.first()
        val pinHash = session.pinHash
        val pinSalt = session.pinSalt

        if (pinHash.isNullOrBlank() || pinSalt.isNullOrBlank()) {
            return@withContext false
        }

        val isValid = PinCryptoManager.verifyPin(
            pin = inputPin,
            saltBase64 = pinSalt,
            expectedHashBase64 = pinHash
        )

        if (isValid) {
            sessionPreferences.resetFailedPinAttempts()
        } else {
            sessionPreferences.recordFailedPinAttempt()
        }

        isValid
    }

    /**
     * Menyimpan seluruh snapshot data dari server ke Room dalam satu transaksi atomik.
     */
    private suspend fun seedInitialSnapshot(snapshot: InitialSnapshotDto) {
        appDatabase.withTransaction {
            if (snapshot.tahunAjaranList.isNotEmpty()) {
                appDatabase.masterAcademicDao().upsertTahunAjaran(
                    snapshot.tahunAjaranList.map {
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
            }

            if (snapshot.guruList.isNotEmpty()) {
                appDatabase.masterAcademicDao().upsertGuru(
                    snapshot.guruList.map {
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
            }

            if (snapshot.kelasList.isNotEmpty()) {
                appDatabase.masterAcademicDao().upsertKelas(
                    snapshot.kelasList.map {
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
            }

            if (snapshot.mapelList.isNotEmpty()) {
                appDatabase.masterAcademicDao().upsertMapel(
                    snapshot.mapelList.map {
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
            }

            if (snapshot.penugasanList.isNotEmpty()) {
                appDatabase.masterAcademicDao().upsertPenugasanGuru(
                    snapshot.penugasanList.map {
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
            }

            if (snapshot.siswaCoreList.isNotEmpty()) {
                appDatabase.studentDao().upsertSiswaCore(
                    snapshot.siswaCoreList.map {
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
                )
            }

            if (snapshot.siswaDetailList.isNotEmpty()) {
                appDatabase.studentDao().upsertSiswaDetail(
                    snapshot.siswaDetailList.map {
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
                )
            }

            if (snapshot.anggotaRombelList.isNotEmpty()) {
                appDatabase.studentDao().upsertAnggotaRombel(
                    snapshot.anggotaRombelList.map {
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
            }

            if (snapshot.kkmList.isNotEmpty()) {
                appDatabase.gradingDao().upsertKkm(
                    snapshot.kkmList.map {
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
            }

            if (snapshot.komponenList.isNotEmpty()) {
                appDatabase.gradingDao().upsertKomponenList(
                    snapshot.komponenList.map {
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
                )
            }

            if (snapshot.nilaiList.isNotEmpty()) {
                appDatabase.gradingDao().upsertNilaiList(
                    snapshot.nilaiList.map {
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
                )
            }

            if (snapshot.presensiSesiList.isNotEmpty()) {
                appDatabase.attendanceDao().upsertSessions(
                    snapshot.presensiSesiList.map {
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
                )
            }

            if (snapshot.presensiDetailList.isNotEmpty()) {
                appDatabase.attendanceDao().insertOrUpdateDetails(
                    snapshot.presensiDetailList.map {
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
                )
            }
        }
    }

    /**
     * Memperbarui PIN offline 6-digit pengguna.
     */
    suspend fun updatePin(oldPin: String, newPin: String): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionPreferences.sessionFlow.first()
            val salt = session.pinSalt
            val hash = session.pinHash
            if (salt.isNullOrBlank() || hash.isNullOrBlank()) {
                return@withContext ResultState.Error("Data PIN belum dikonfigurasi.")
            }
            if (!PinCryptoManager.verifyPin(oldPin, salt, hash)) {
                return@withContext ResultState.Error("PIN lama salah.")
            }
            if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
                return@withContext ResultState.Error("PIN baru harus berupa 6 digit angka.")
            }
            val newSalt = PinCryptoManager.generateSalt()
            val newHash = PinCryptoManager.hashPin(newPin, newSalt)
            val newSaltBase64 = PinCryptoManager.saltToBase64(newSalt)
            sessionPreferences.savePin(newHash, newSaltBase64)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal memperbarui PIN: ${e.localizedMessage}")
        }
    }

    /**
     * Menghapus sesi login pengguna.
     */
    suspend fun logout(): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionPreferences.clearSession()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal keluar: ${e.localizedMessage}")
        }
    }
}
