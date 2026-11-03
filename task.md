# Project Implementation Roadmap & Task Tracker (`task.md`)

Gunakan dokumen ini sebagai daftar periksa (*checklist*) eksekusi bertahap untuk **SIMRTH (`id.mts.rth`)**. Agen AI harus mengerjakan fase secara berurutan dari **Fase 1 hingga Fase 7**, melakukan `git push` untuk memicu verifikasi kompilasi/unit test di **GitHub Actions** (pantau via `gh run watch`), dan mengubah status `[ ]` menjadi `[x]` setelah task selesai.

---

## Fase 1: Inisialisasi Proyek, Konfigurasi Build & Cloud CI/CD
**Tujuan:** Membangun kerangka proyek Android `id.mts.rth` yang dapat dikompilasi secara otomatis di GitHub Actions tanpa perlu menjalankan Gradle lokal di Termux.

### 1.1 Setup Gradle & Version Catalog
- [x] Buat `settings.gradle.kts` dengan `rootProject.name = "SIMRTH"` dan `include(":app")`.
- [x] Buat `gradle/libs.versions.toml` dengan definisi Kotlin 2.0.21, AGP 8.7.2, KSP 2.0.21-1.0.27, Compose BOM, Room, Koin, Ktor, Kotlinx Serialization, DataStore, dan Coil.
- [x] Konfigurasi `gradle.properties` dengan optimasi memori CI (`org.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8`, `org.gradle.caching=true`, `org.gradle.parallel=true`, `android.useAndroidX=true`).
- [x] Buat `build.gradle.kts` (root plugins setup). (Modul `app/build.gradle.kts` pada sub-task 1.2).

### 1.2 Keamanan Jaringan & Manifest Dasar
- [x] Buat `app/src/main/res/xml/network_security_config.xml` untuk mengizinkan komunikasi HTTP pada jaringan lokal madrasah (`simrth.local` dan IP Private).
- [x] Buat `app/src/main/res/values/strings.xml` dengan `app_name` bernilai `"SIMRTH"` dan `themes.xml`.
- [x] Konfigurasi `AndroidManifest.xml` (izin `INTERNET`, `ACCESS_NETWORK_STATE`, `android:name=".SimrthApp"`, atribut `networkSecurityConfig`, dan `MainActivity`).
- [x] Buat kerangka dasar `SimrthApp.kt` dan `MainActivity.kt` di dalam paket `app/src/main/kotlin/id/mts/rth/`.

### 1.3 Pipeline GitHub Actions (Cloud Compiler untuk Termux)
- [x] Buat `.github/workflows/android-ci.yml` yang menggunakan `actions/setup-java@v4` (Temurin 17) dan `gradle/actions/setup-gradle@v4` (`gradle-version: 8.10.2`), menjalankan `gradle testDebugUnitTest assembleDebug`, serta mengunggah artefak `simrth-debug-apk` menggunakan `actions/upload-artifact@v4`.
- [x] Lakukan push ke GitHub dan verifikasi build pertama sukses menggunakan `gh run watch`.

---

## Fase 2: Layer Database Lokal (Room SQLite) & Outbox Pattern
**Tujuan:** Mengimplementasikan seluruh 14 entitas data sesuai standar EMIS & K13 beserta mekanisme antrean sinkronisasi anti-duplikat di paket `id.mts.rth.data.local`.

### 2.1 Implementasi Entitas Room (`data/local/entity/`)
- [x] Buat `AcademicEntities.kt` (`TahunAjaranEntity`, `GuruEntity`, `KelasEntity`, `MapelEntity`, `PenugasanGuruEntity`) lengkap dengan `isDeleted` dan `updatedAt`.
- [x] Buat `StudentEntities.kt` (`SiswaCoreEntity`, `SiswaDetailEntity`, `AnggotaRombelEntity`) dengan indeks unik yang sesuai.
- [x] Buat `AttendanceEntities.kt` (`PresensiSesiEntity`, `PresensiDetailEntity`).
- [x] Buat `GradingEntities.kt` (`KkmMapelEntity`, `KomponenNilaiEntity`, `NilaiSiswaEntity`).
- [x] Buat `SyncOutboxEntity.kt` dengan indeks unik komposit pada `(tableNameTarget, recordId)`.

### 2.2 Implementasi DAO & Coalescing Transaction (`data/local/dao/`)
- [x] Buat `SyncOutboxDao.kt` lengkap dengan fungsi `@Transaction recordMutationWithCoalescing(...)` dan query antrean berurut berdasarkan `priorityOrder ASC, createdAt ASC`.
- [x] Buat `MasterAcademicDao.kt` untuk pembacaan jadwal aktif, kelas, mapel, dan tahun ajaran menggunakan `Flow`.
- [x] Buat `StudentDao.kt` dengan pemisahan query `getSiswaCoreByKelas(...)` (untuk semua guru) dan `getSiswaDetailById(...)` (khusus Wali Kelas/Admin).
- [x] Buat `AttendanceDao.kt` dengan fungsi `@Transaction saveAttendanceSessionAndOutbox(...)`.
- [x] Buat `GradingDao.kt` dengan fungsi `@Transaction saveBatchGradesAndOutbox(...)` serta pengelolaan `KomponenNilaiEntity`.

### 2.3 Inisialisasi Database & Koin Module (`data/local/db/` & `di/`)
- [x] Buat `AppDatabase.kt` (`simrth_k13.db`) dengan konfigurasi `JournalMode.WRITE_AHEAD_LOGGING` dan `DbMigrations.kt`.
- [x] Buat `DatabaseModule.kt` untuk menyediakan instance `AppDatabase` dan seluruh DAO via Koin.
- [x] Buat unit test `OutboxCoalescingLogicTest.kt` dan verifikasi kelulusan KSP Room di GitHub Actions.

---

## Fase 3: Logika Bisnis Murni (Domain Layer) & Keamanan Lokal
**Tujuan:** Membangun mesin kalkulasi nilai K13 dan sistem keamanan PIN offline yang teruji dengan Unit Test $100\%$ di CI.

### 3.1 Mesin Kalkulator K13 (`domain/calculator/`)
- [x] Buat model domain `AttendanceStatus.kt`, `UserRole.kt`, dan `StudentGradeSummary.kt` di paket `id.mts.rth.domain.model`.
- [x] Buat `K13GradeCalculator.kt` yang mengimplementasikan:
  - Kalkulasi rata-rata Tugas ($\overline{T}$) dan Ulangan Harian ($\overline{UH}$) dengan penanganan nilai kosong (`null`).
  - Kalkulasi Nilai Akhir Pengetahuan ($\text{NA}_{\text{KI3}}$) dengan normalisasi bobot aktif.
  - Kalkulasi Nilai Akhir Keterampilan ($\text{NA}_{\text{KI4}}$).
  - Fungsi deterministik `hitungPredikatK13(nilaiAkhirBulat, kkm)` untuk menghasilkan predikat A, B, C, atau D.
- [x] Buat unit test `K13GradeCalculatorTest.kt` di `app/src/test/kotlin/id/mts/rth/domain/calculator/` yang menguji berbagai skenario nilai KKM ($70, 75, 78, 80$) dan komponen ujian yang belum terisi.

### 3.2 Keamanan PIN Offline & Sesi (`core/security/`)
- [x] Buat `PinCryptoManager.kt` menggunakan `PBKDF2WithHmacSHA256` ($10.000$ iterasi, 16-byte random salt).
- [x] Buat `SessionPreferences.kt` berbasis Jetpack `DataStore<Preferences>` untuk menyimpan IP Server, JWT Token, Role aktif, Hash PIN, Salt, `lastSyncTimestamp`, dan penghitung gagal PIN (maksimal $5\times$).
- [x] Buat unit test `PinCryptoManagerTest.kt` untuk memverifikasi akurasi hash dan pencocokan PIN di GitHub Actions.

---

## Fase 4: Mesin Sinkronisasi Jaringan Lokal & Manajemen Media
**Tujuan:** Menghubungkan aplikasi SIMRTH dengan server lokal Wi-Fi MTs Roudhotut Thowalib secara aman dan efisien.

### 4.1 Ktor Client & Keamanan Subnet (`data/remote/`)
- [x] Buat `LanSecurityInterceptor.kt` yang memastikan request HTTP cleartext hanya diizinkan menuju IP Private (RFC 1918) atau domain `.local` (`simrth.local`).
- [x] Buat DTO serialisasi di `data/remote/dto/` (`AuthDto.kt`, `SyncPushDto.kt`, `SyncPullDto.kt`).
- [x] Buat `AuthApiService.kt` (endpoint login + initial seed snapshot) dan `SyncApiService.kt` (endpoint `/health`, `/sync/push`, `/sync/pull`, dan unduh foto).
- [x] Buat `NetworkModule.kt` di paket `id.mts.rth.di`.

### 4.2 Orkestrator Sinkronisasi 3 Tahap (`data/repository/SyncEngineRepository.kt`)
- [x] Implementasikan pengecekan `/api/v1/health` (timeout $3.000\text{ ms}$) sekaligus validasi *Clock Drift* ($|\Delta t| \le 5\text{ menit}$).
- [x] Implementasikan **Tahap 1 (Push Outbox):** pengiriman batch maksimal 250 mutasi dan penghapusan record outbox yang sukses diterima server.
- [x] Implementasikan **Tahap 2 (Pull Delta):** pengambilan pembaruan server berdasarkan `lastSyncTimestamp` dan `@Transaction` upsert ke Room (tanpa menimpa record yang masih ada di `sync_outbox` lokal).
- [x] Implementasikan **Tahap 3 (Media Sync) `StudentPhotoManager.kt`:** kompresi foto WebP ($300 \times 300\text{ px}$, $\le 25\text{ KB}$, kompatibel API 26--35) di `context.filesDir/students/` dan pengunduhan foto siswa secara background.

---

## Fase 5: Antarmuka Pengguna (Jetpack Compose) - Autentikasi, Beranda & Presensi Cepat
**Tujuan:** Membangun pengalaman pengguna di ruang kelas MTs Roudhotut Thowalib yang responsif, jelas, dan instan.

### 5.1 Tema Material 3 & Komponen Bersama (`ui/theme/` & `ui/components/`)
- [x] Buat `Color.kt`, `Type.kt`, dan `Theme.kt` dengan palet **Emerald Madrasah** (`#0F766E`) dan warna semantik presensi/KKM.
- [x] Buat `SyncTopAppBar.kt` yang menampilkan identitas **SIMRTH**, status antrean outbox secara reaktif, beserta tombol **"Sinkronkan Sekarang"**.
- [x] Buat `PinNumPad.kt` untuk layar masuk cepat di ruang kelas.

### 5.2 Alur Autentikasi & Beranda (`ui/screens/auth/` & `ui/screens/home/`)
- [x] Buat `InitialSetupScreen.kt` (input IP Server lokal, NIP/Username, Password, pembuatan 6-digit PIN baru, dan indikator progres *Initial Seed*).
- [x] Buat `PinLockScreen.kt` untuk verifikasi PIN offline harian beserta proteksi *lockout* $5\times$ salah.
- [x] Buat `HomeScreen.kt` & `HomeViewModel.kt` yang menampilkan daftar jam mengajar aktif hari ini, ringkasan outbox, dan pintasan langsung ke presensi kelas.

### 5.3 Fast Attendance Engine (`ui/screens/attendance/`)
- [x] Buat `AttendanceRowItem.kt` yang mendukung *single-tap sequential rotation* ($\text{H} \rightarrow \text{S} \rightarrow \text{I} \rightarrow \text{A} \rightarrow \text{H}$) dengan perubahan warna instan.
- [x] Buat `AttendanceScreen.kt` & `AttendanceViewModel.kt` dengan default seluruh siswa `Hadir (H)`, kolom input singkat materi jurnal mengajar, dan tombol **"Simpan Presensi"** (*Optimistic UI*).

---

## Fase 6: Modul Penilaian Dinamis K13, Direktori EMIS & Ekspor CSV
**Tujuan:** Menyelesaikan fitur input nilai berkecepatan tinggi, akses data EMIS berbasis peran (RBAC), dan fitur penyelamatan data darurat.

### 6.1 Modul Penilaian K13 & In-Memory Grid (`ui/screens/grading/`)
- [ ] Buat `GradingOverviewScreen.kt` dengan tab pemisah `KI-3 Pengetahuan`, `KI-4 Keterampilan`, dan `Rekap Nilai Akhir`, dilengkapi dialog **"+ Tambah Kolom Nilai"** (Tugas, UH, PAS/PAT, Praktik, Proyek, Portofolio).
- [ ] Buat `GradingEntryScreen.kt` & `GradingViewModel.kt` menggunakan buffer `mutableStateMapOf<String, String>()` untuk input vertikal cepat (`KeyboardType.Decimal`, `ImeAction.Next`) tanpa *recomposition lag*.
- [ ] Tampilkan kalkulasi otomatis Nilai Akhir, warna indikator ketuntasan KKM, dan badge Predikat (`A`/`B`/`C`/`D`) di tab Rekap Nilai Akhir.
- [ ] Buat `CsvExporter.kt` dan integrasikan tombol **"Ekspor Rekap Nilai (.CSV)"** menggunakan Storage Access Framework.

### 6.2 Direktori Siswa & Segmentasi RBAC EMIS (`ui/screens/students/`)
- [ ] Buat `StudentListScreen.kt` untuk melihat daftar siswa per kelas (`7A`, `7B`, `8A`, `8B`, `9A`, `9B`) beserta foto profil `.webp` via Coil.
- [ ] Buat `StudentEmisDetailScreen.kt` yang **hanya dapat diakses oleh Wali Kelas (untuk kelas binaannya) dan Admin**, menampilkan data lengkap EMIS (NIK, KK, Alamat, Orang Tua, KIP) dan tombol pintas WhatsApp Orang Tua (`Intent.ACTION_VIEW` ke `https://wa.me/62...`).

### 6.3 Fitur Khusus Admin (`ui/screens/admin/`)
- [ ] Buat `KkmConfigScreen.kt` bagi Admin untuk mengubah nilai KKM dan bobot penilaian per mata pelajaran/tingkat kelas.
- [ ] Buat `LifecycleMutationScreen.kt` bagi Admin untuk mengeksekusi kenaikan kelas dan kelulusan rombel.

---

## Fase 7: Integrasi Navigasi Penuh, Pengujian Akhir & Rilis Cloud APK
**Tujuan:** Menyatukan seluruh layar dalam grafik navigasi tunggal dan menghasilkan APK siap pakai dari GitHub Actions.

### 7.1 Navigasi & Dependency Injection Penuh
- [ ] Lengkapi `AppNavHost.kt` dan `ScreenRoutes.kt` dengan penjagaan rute berdasarkan status sesi login/PIN dan Role pengguna.
- [ ] Daftarkan seluruh Repository dan ViewModel di `RepositoryModule.kt` dan `ViewModelModule.kt` serta inisialisasi Koin di `SimrthApp.kt`.

### 7.2 Verifikasi Kualitas & Optimasi Rilis di GitHub Actions
- [ ] Konfigurasi `proguard-rules.pro` agar model serialisasi JSON (`id.mts.rth.data.remote.dto.**`) dan entitas Room (`id.mts.rth.data.local.entity.**`) tidak terhapus oleh R8 saat build release.
- [ ] Push seluruh perubahan ke GitHub, pantau via `gh run watch` hingga seluruh unit test dan `assembleDebug` lulus $100\%$.
- [ ] Unduh APK hasil kompilasi ke perangkat menggunakan `gh run download --name simrth-debug-apk` dan lakukan verifikasi instalasi.