# Project Architecture & Directory Tree (`architecture.md`)

Dokumen ini adalah referensi struktur direktori tunggal (*Single Source of Truth*) untuk proyek **SIMRTH (Sistem Informasi Madrasah Roudhotut Thowalib)**. Agen AI wajib menempatkan setiap file baru sesuai dengan pohon direktori dan tanggung jawab modul di bawah ini.

---

## 1. Diagram Alur Data (Offline-First Unidirectional Data Flow)

```text
[ Jetpack Compose UI ]
       |        ^
 (User Action)  | (StateFlow<UiState>)
       v        |
[ Presentation ViewModel ] -- (In-Memory Buffer untuk Grid Nilai)
       |        ^
 (Suspend Call) | (Flow<DomainModel> dari Room)
       v        |
[ Data Repository ]
       +--> 1. @Transaction Tulis ke [ Room Entity DAO ]
       +--> 2. @Transaction Coalesce ke [ SyncOutboxDao ]
                                              |
                     (Saat Tombol "Sinkronkan Sekarang" Ditekan)
                                              v
                                    [ SyncManager / Engine ]
                                     +--> Push Outbox (Ktor POST)
                                     +--> Pull Delta (Ktor GET)
                                     +--> Media Sync (.webp via Coil/FileIO)
```

---

## 2. Struktur Pohon Direktori Lengkap (Folder Tree)

- **Nama Aplikasi:** `SIMRTH` (Sistem Informasi Madrasah Roudhotut Thowalib)
- **Application ID / Namespace:** `id.mts.rth`

```text
simrth-android/
|-- .github/
|   `-- workflows/
|       `-- android-ci.yml                  # Pipeline GitHub Actions (Setup Gradle + Unit Test + Build APK)
|-- app/
|   |-- build.gradle.kts                    # Konfigurasi namespace "id.mts.rth", KSP Room, Compose, Koin, Ktor
|   |-- proguard-rules.pro                  # Aturan R8 untuk kotlinx.serialization, Ktor, dan Room
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml         # Registrasi SimrthApp, MainActivity, permissions
|       |   |-- res/
|       |   |   |-- drawable/               # Ikon vektor aplikasi & logo MTs Roudhotut Thowalib
|       |   |   |-- values/
|       |   |   |   |-- strings.xml         # app_name = "SIMRTH"
|       |   |   |   `-- themes.xml
|       |   |   `-- xml/
|       |   |       `-- network_security_config.xml  # Konfigurasi izin HTTP LAN Lokal (simrth.local / RFC1918)
|       |   `-- kotlin/id/mts/rth/
|       |       |-- SimrthApp.kt            # Application class (Inisialisasi Koin & Coil ImageLoader)
|       |       |-- MainActivity.kt         # Single Activity host untuk Compose Navigation
|       |       |
|       |       |-- core/                   # Utilitas lintas modul & Keamanan
|       |       |   |-- common/
|       |       |   |   |-- Constants.kt
|       |       |   |   |-- DateTimeUtils.kt        # Helper tanggal YYYY-MM-DD & validasi Clock Drift
|       |       |   |   `-- ResultState.kt          # Sealed class Success, Error, Loading
|       |       |   |-- media/
|       |       |   |   |-- StudentPhotoManager.kt  # Kompresi WebP 300x300 (API 26+ safe) & filesDir/students/
|       |       |   |   `-- CsvExporter.kt          # Generator leger nilai K13 (.csv) via SAF
|       |       |   `-- security/
|       |       |       |-- PinCryptoManager.kt     # PBKDF2WithHmacSHA256 hashing & verifikasi PIN offline
|       |       |       `-- SessionPreferences.kt   # Jetpack DataStore (Token, Role, LastSync, FailedPinCount)
|       |       |
|       |       |-- di/                     # Dependency Injection (Koin Modules)
|       |       |   |-- DatabaseModule.kt   # Singleton AppDatabase & penyedia seluruh DAO
|       |       |   |-- NetworkModule.kt    # Ktor HttpClient + LanSecurityInterceptor
|       |       |   |-- RepositoryModule.kt # Binding Repository & SyncEngine
|       |       |   `-- ViewModelModule.kt  # Deklarasi viewModelOf(...)
|       |       |
|       |       |-- data/                   # Layer Data (Local Room + Remote Ktor + Repository)
|       |       |   |-- local/
|       |       |   |   |-- db/
|       |       |   |   |   |-- AppDatabase.kt      # RoomDatabase class (simrth_k13.db, WAL Mode aktif)
|       |       |   |   |   `-- DbMigrations.kt     # Skrip migrasi skema antar versi
|       |       |   |   |-- entity/
|       |       |   |   |   |-- AcademicEntities.kt # TahunAjaran, Guru, Kelas, Mapel, PenugasanGuru
|       |       |   |   |   |-- StudentEntities.kt  # SiswaCore, SiswaDetail, AnggotaRombel
|       |       |   |   |   |-- AttendanceEntities.kt # PresensiSesi, PresensiDetail
|       |       |   |   |   |-- GradingEntities.kt  # KkmMapel, KomponenNilai, NilaiSiswa
|       |       |   |   |   `-- SyncOutboxEntity.kt # Tabel antrean mutasi keluar
|       |       |   |   `-- dao/
|       |       |   |       |-- MasterAcademicDao.kt
|       |       |   |       |-- StudentDao.kt       # Query terpisah Core vs Detail (RBAC Safe)
|       |       |   |       |-- AttendanceDao.kt    # @Transaction simpan sesi + detail + outbox
|       |       |   |       |-- GradingDao.kt       # @Transaction batch simpan nilai + outbox
|       |       |   |       `-- SyncOutboxDao.kt    # Coalescing upsert & antrean prioritas
|       |       |   |
|       |       |   |-- remote/
|       |       |   |   |-- api/
|       |       |   |   |   |-- AuthApiService.kt   # Login awal & Full State Hydration
|       |       |   |   |   `-- SyncApiService.kt   # HealthCheck, Push Outbox, Pull Delta, Download Foto
|       |       |   |   |-- dto/
|       |       |   |   |   |-- AuthDto.kt
|       |       |   |   |   |-- SyncPushDto.kt
|       |       |   |   |   `-- SyncPullDto.kt
|       |       |   |   `-- interceptor/
|       |       |   |       `-- LanSecurityInterceptor.kt # Blokir trafik HTTP jika bukan IP LAN madrasah
|       |       |   |
|       |       |   `-- repository/
|       |       |       |-- AuthRepository.kt
|       |       |       |-- AcademicRepository.kt
|       |       |       |-- AttendanceRepository.kt
|       |       |       |-- GradingRepository.kt
|       |       |       |-- StudentRepository.kt
|       |       |       `-- SyncEngineRepository.kt # Orkestrator 3 tahap sinkronisasi manual
|       |       |
|       |       |-- domain/                 # Logika Bisnis Murni (Tanpa dependensi Android UI)
|       |       |   |-- model/
|       |       |   |   |-- AttendanceStatus.kt     # Enum H, S, I, A beserta urutan rotasi tap
|       |       |   |   |-- UserRole.kt             # Enum GURU, GURU_WALI, ADMIN
|       |       |   |   `-- StudentGradeSummary.kt  # Model rekap NA KI-3, NA KI-4, Predikat A/B/C/D
|       |       |   `-- calculator/
|       |       |       `-- K13GradeCalculator.kt   # Rumus NA Pengetahuan, Keterampilan & Interval KKM
|       |       |
|       |       `-- ui/                     # Layer Presentasi (Jetpack Compose)
|       |           |-- theme/
|       |           |   |-- Color.kt                # Palet Emerald Madrasah & warna status presensi
|       |           |   |-- Type.kt
|       |           |   `-- Theme.kt
|       |           |-- components/                 # Reusable UI Components
|       |           |   |-- SyncTopAppBar.kt        # AppBar dengan badge jumlah outbox & tombol Sync
|       |           |   |-- AttendanceRowItem.kt    # Baris siswa multi-tap cepat
|       |           |   |-- KkmPredicateBadge.kt    # Chip warna predikat A/B/C/D & remedial
|       |           |   `-- PinNumPad.kt            # Keypad 6-digit PIN untuk login harian di kelas
|       |           |-- navigation/
|       |           |   |-- ScreenRoutes.kt
|       |           |   `-- AppNavHost.kt
|       |           `-- screens/
|       |               |-- auth/
|       |               |   |-- InitialSetupScreen.kt # Input IP Server, Login & Buat PIN (Wi-Fi Madrasah)
|       |               |   |-- PinLockScreen.kt      # Layar kunci harian offline di kelas
|       |               |   `-- AuthViewModel.kt
|       |               |-- home/
|       |               |   |-- HomeScreen.kt         # Jadwal aktif hari ini & kartu ringkasan outbox
|       |               |   `-- HomeViewModel.kt
|       |               |-- attendance/
|       |               |   |-- AttendanceScreen.kt   # Fast Attendance Engine
|       |               |   `-- AttendanceViewModel.kt
|       |               |-- grading/
|       |               |   |-- GradingOverviewScreen.kt # Tab KI-3, KI-4, Rekap Akhir & Tambah Kolom
|       |               |   |-- GradingEntryScreen.kt    # Grid input vertikal cepat berbuffer memori
|       |               |   `-- GradingViewModel.kt
|       |               |-- students/
|       |               |   |-- StudentListScreen.kt     # Direktori siswa per kelas
|       |               |   |-- StudentEmisDetailScreen.kt # Detail EMIS & tombol WA (Khusus Wali/Admin)
|       |               |   `-- StudentViewModel.kt
|       |               `-- admin/
|       |                   |-- KkmConfigScreen.kt       # Pengaturan KKM & Bobot (Khusus Admin)
|       |                   `-- LifecycleMutationScreen.kt # Kenaikan kelas & kelulusan massal
|       |
|       `-- test/kotlin/id/mts/rth/         # Unit Tests (Berjalan cepat di GitHub Actions tanpa emulator)
|           |-- domain/calculator/
|           |   `-- K13GradeCalculatorTest.kt       # Pengujian batas interval KKM & bobot dinamis
|           |-- core/security/
|           |   `-- PinCryptoManagerTest.kt         # Pengujian hash PBKDF2 & lockout counter
|           `-- data/local/
|               `-- OutboxCoalescingLogicTest.kt    # Pengujian penggabungan mutasi berulang
|
|-- gradle/
|   `-- libs.versions.toml                  # Version Catalog terpusat untuk semua library
|-- build.gradle.kts                        # Root build file
|-- settings.gradle.kts                     # rootProject.name = "SIMRTH"
|-- gradle.properties                       # Tuning JVM Heap & caching untuk GitHub Actions
|-- prd.md                                  # Dokumen Spesifikasi Produk SIMRTH
|-- agent.md                                # Instruksi Operasional Agen AI
|-- architecture.md                         # Referensi Pohon Direktori & Arsitektur (File ini)
`-- task.md                                 # Daftar Fase & Task Implementasi Berurutan
```

---

## 3. Konfigurasi `gradle/libs.versions.toml` & Cloud CI Acuan

Agar tidak terjadi konflik versi saat agen AI menginisialisasi proyek dari Termux dan mengompilasinya di GitHub Actions, gunakan acuan versi stabil berikut:

* **Gradle Runner (via `gradle/actions/setup-gradle@v4`):** `8.10.2`
* **JDK Runner (via `actions/setup-java@v4`):** `Temurin 17`
* **Kotlin:** `2.0.21`
* **AGP (Android Gradle Plugin):** `8.7.2`
* **KSP:** `2.0.21-1.0.27`
* **Compose BOM:** `2024.10.01`
* **Room:** `2.6.1`
* **Koin BOM:** `4.0.0`
* **Ktor:** `3.0.1`
* **Kotlinx Serialization JSON:** `1.7.3`
* **DataStore Preferences:** `1.1.1`
* **Coil Compose:** `3.0.2`