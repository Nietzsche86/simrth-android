# Product Requirements Document (PRD)
## SIMRTH — Sistem Informasi Madrasah Roudhotut Thowalib (Offline-First K13)

- **Nama Aplikasi:** SIMRTH (Sistem Informasi Madrasah Roudhotut Thowalib)
- **Application ID / Package:** `id.mts.rth`
- **Versi Dokumen:** 2.1.0 (SIMRTH Production-Ready Revision)
- **Target Platform:** Android (Min SDK 26 / Android 8.0, Target SDK 35 / Android 15)
- **Arsitektur:** Offline-First (Room SQLite + Coalesced Outbox Pattern)
- **Metode Pengembangan & Kompilasi:** Termux CLI + 100% Cloud Build via GitHub Actions & GitHub CLI (`gh`)
- **Topologi Server:** Server Fisik Lokal Madrasah (LAN / Wi-Fi mDNS `simrth.local` / Static Private IP)

---

## 1. Ringkasan Eksekutif & Tujuan Sistem

**SIMRTH (Sistem Informasi Madrasah Roudhotut Thowalib)** adalah aplikasi sistem informasi manajemen madrasah dan pencatatan akademik berbasis Android dengan pendekatan **100% Offline-First**. Aplikasi beroperasi penuh tanpa internet publik di ruang kelas MTs Roudhotut Thowalib dan melakukan sinkronisasi delta secara manual saat perangkat guru terhubung ke jaringan Wi-Fi lokal madrasah.

### Sasaran Utama
1. **Zero-Latency In-Class Operation:** Guru menyelesaikan presensi dan input nilai di bawah $10\text{ detik}$ tanpa bergantung pada sinyal seluler atau Wi-Fi di kelas.
2. **Kepatuhan Format EMIS Kemenag & Kurikulum 2013 (K13):** Struktur biodata mematuhi standar EMIS Kemenag, sedangkan penilaian memisahkan Pengetahuan (KI-3) dan Keterampilan (KI-4) dengan interval predikat K13 dinamis berbasis KKM.
3. **Data Privacy by Hard Physical Segmentation:** Guru mapel biasa tidak hanya dibatasi secara UI, tetapi perangkatnya **tidak pernah mengunduh** baris tabel `siswa_detail` (NIK, KK, data orang tua) dari server. Hanya Admin/Operator TU dan Wali Kelas (khusus kelas binaannya) yang menerima payload tersebut saat sinkronisasi.
4. **Termux & Cloud CI Friendly:** Bebas dependensi berat (tanpa XML layout, tanpa Dagger/Hilt KAPT), menggunakan Kotlin DSL murni dan KSP agar cepat dikompilasi di GitHub Actions dan mudah dimodifikasi dari lingkungan Termux HP.

---

## 2. Arsitektur Teknis & Topologi Jaringan

### 2.1 Spesifikasi Stack Teknologi
* **Bahasa Pemrograman:** Kotlin 2.0+ (Coroutines + StateFlow/SharedFlow)
* **User Interface:** Jetpack Compose (Material Design 3, Single-Activity Architecture, Compose Navigation)
* **Database Lokal:** AndroidX Room SQLite (`simrth_k13.db`, KSP-based, `JournalMode.WRITE_AHEAD_LOGGING`)
* **Dependency Injection:** Koin (Kotlin DSL murni, zero code-generation overhead)
* **Networking & Serialisasi:** Ktor Client (`OkHttp` Engine) + `kotlinx.serialization` (JSON)
* **Penyimpanan Preferensi & Keamanan:** Jetpack `DataStore<Preferences>` + `PBKDF2WithHmacSHA256` untuk PIN
* **Pemrosesan Gambar Lokal:** Coil 3 (Compose) + Kompresi WebP (`WEBP_LOSSY` pada API 30+, fallback ke `WEBP` pada API 26--29)
* **Backend Rekomendasi:** PocketBase (Go single binary) atau FastAPI/LiteStar (Python + SQLite WAL) pada server lokal madrasah (STB Armbian / Mini PC).

### 2.2 Topologi Jaringan & Penanganan Khusus Android
1. **Local Wi-Fi Endpoint & Service Discovery:**
   * Server berjalan pada IP statis LAN (contoh: `http://192.168.1.50:8090`) atau hostname mDNS (`http://simrth.local:8090`).
   * Admin/Guru dapat mengubah IP Server pada halaman Pengaturan Koneksi jika terjadi perubahan router madrasah.
2. **Scoped Cleartext Traffic & Interceptor Protection:**
   * Konfigurasi di `res/xml/network_security_config.xml` mengizinkan cleartext HTTP khusus untuk komunikasi LAN lokal, dikombinasikan dengan validator di level Ktor Client Interceptor (`LanSecurityInterceptor`) yang menolak koneksi HTTP non-enkripsi jika IP tujuan bukan IP Private RFC 1918 (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`) atau domain `.local`.
3. **Manajemen Media Foto Siswa (Anti-CursorWindow Error):**
   * Foto **dilarang keras** disimpan sebagai BLOB/Base64 di Room SQLite maupun disisipkan di dalam payload JSON utama.
   * Setiap foto dikompresi di klien menjadi format WebP ukuran maksimal $300 \times 300\text{ px}$ (kualitas $80\%$, ukuran $\le 25\text{ KB}$) dan disimpan di `context.filesDir/students/{fotoHash}.webp`.
   * Database hanya menyimpan nama file (`fotoFileName`) untuk pengecekan *cache-miss* saat sinkronisasi media terpisah.

---

## 3. Matriks Hak Akses & Peran Pengguna (RBAC)

Peran pengguna ditetapkan oleh server saat autentikasi awal dan dikunci dalam token sesi lokal.

| Modul / Tindakan | Guru Mapel Biasa | Wali Kelas (Kelas Binaan) | Admin / Operator TU |
| :--- | :---: | :---: | :---: |
| **Presensi Siswa** | Jam Pelajaran Sendiri | Jam Sendiri & Rekap Harian Kelas | Seluruh Rombel |
| **Input Nilai (KI-3 & KI-4)** | Mapel yang Diampu | Mapel Sendiri & Pantau Leger Kelas | Seluruh Mapel |
| **Lihat Biodata (`siswa_core`)** | Ya (Kelas yang Diajar) | Ya (Kelas yang Diajar) | Seluruh Madrasah |
| **Lihat & Edit EMIS (`siswa_detail`)**| Tidak Diunduh ke HP | Baca & Usul Edit (Kelas Binaan) | Akses Penuh (CRUD) |
| **Kontak WhatsApp Ortu** | Tidak Tersedia | Tombol Direct WA (`wa.me`) | Tombol Direct WA (`wa.me`) |
| **Atur KKM & Bobot Mapel** | Hanya Baca | Hanya Baca | Kelola Nilai KKM |
| **Kenaikan / Kelulusan / Mutasi** | Tidak Ada | Hanya Lihat Histori | Eksekusi Batch Mutasi |
| **Cakupan Sinkronisasi Pull** | Partisi Kelas yang Diajar | Partisi Kelas Ajar + Detail Wali | Full Madrasah Database |

---

## 4. Skema Database Room SQLite (Data Entities)

> **Aturan Skema:** Seluruh tabel master dan transaksi wajib berada di paket `id.mts.rth.data.local.entity`, menyertakan `updatedAt: Long` (Epoch Millis UTC), dan mendukung penghapusan logis (`isDeleted: Boolean = false`) agar penghapusan data dapat tersinkronisasi antar perangkat.

### 4.1 Master Akademik, Penugasan & Jadwal

```kotlin
package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 1. Tahun Ajaran & Semester
@Entity(tableName = "tahun_ajaran")
data class TahunAjaranEntity(
    @PrimaryKey val id: String, // UUID
    val tahun: String, // "2026/2027"
    val semester: String, // "GANJIL" | "GENAP"
    val isActive: Boolean,
    val tanggalMulai: String, // YYYY-MM-DD
    val tanggalSelesai: String,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 2. Master Guru & GTK
@Entity(tableName = "master_guru")
data class GuruEntity(
    @PrimaryKey val id: String,
    val nip: String?,
    val npk: String?,
    val nuptk: String?,
    val nik: String,
    val namaLengkap: String,
    val gelarDepan: String?,
    val gelarBelakang: String?,
    val jenisKelamin: String, // "L" | "P"
    val noWa: String,
    val statusKepegawaian: String, // "PNS", "PPPK", "GTY", "GTT"
    val role: String, // "GURU", "GURU_WALI", "ADMIN"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 3. Master Rombel / Kelas
@Entity(tableName = "master_kelas")
data class KelasEntity(
    @PrimaryKey val id: String,
    val tingkat: Int, // 7, 8, 9
    val namaRombel: String, // "7A", "7B", "8A", "8B", "9A", "9B"
    val waliKelasGuruId: String?, // FK ke master_guru.id
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 4. Master Mata Pelajaran
@Entity(tableName = "master_mapel")
data class MapelEntity(
    @PrimaryKey val id: String,
    val kodeMapel: String, // "TIK-07", "BAR-07"
    val namaMapel: String,
    val kelompok: String, // "A", "B", "MULOK"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 5. Penugasan & Jadwal Mengajar Guru
@Entity(
    tableName = "penugasan_guru",
    indices = [
        Index(value = ["guruId", "mapelId", "kelasId", "tahunAjaranId"], unique = true),
        Index(value = ["kelasId"]),
        Index(value = ["mapelId"])
    ]
)
data class PenugasanGuruEntity(
    @PrimaryKey val id: String,
    val guruId: String,
    val mapelId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val jadwalJson: String, // Contoh: [{"hari":1,"jamKe":"1-2","mulai":"07:30"}]
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
```

### 4.2 Data Siswa Sesuai Standar EMIS (Separated Core & Detail)

```kotlin
package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 6. Data Siswa Core (Diunduh oleh semua Guru yang mengajar kelas terkait)
@Entity(
    tableName = "siswa_core",
    indices = [Index(value = ["nisLokal"], unique = true), Index(value = ["nisn"])]
)
data class SiswaCoreEntity(
    @PrimaryKey val id: String, // UUID
    val nisLokal: String,
    val nisn: String?,
    val namaLengkap: String,
    val jenisKelamin: String, // "L" | "P"
    val fotoFileName: String?, // Nama file relatif: "siswa_uuid_hash.webp"
    val statusSiswa: String, // "AKTIF", "LULUS", "MUTASI_KELUAR", "DO"
    val isDeleted: Boolean = false,
    val syncStatus: String, // "SYNCED", "PENDING_UPDATE"
    val updatedAt: Long
)

// 7. Data Siswa Detail EMIS (HANYA diunduh oleh Admin & Wali Kelas Binaan)
@Entity(
    tableName = "siswa_detail",
    indices = [Index(value = ["siswaId"], unique = true), Index(value = ["nik"])]
)
data class SiswaDetailEntity(
    @PrimaryKey val id: String,
    val siswaId: String, // FK ke siswa_core.id
    val nik: String,
    val noKk: String,
    val tempatLahir: String,
    val tanggalLahir: String, // YYYY-MM-DD
    val agama: String = "Islam",
    val anakKe: Int,
    val jumlahSaudara: Int,
    
    // Alamat Domisili
    val alamatJalan: String,
    val rt: String,
    val rw: String,
    val desaKelurahan: String,
    val kecamatan: String,
    val kabupatenKota: String,
    val kodePos: String?,
    val statusTempatTinggal: String, // "Bersama Orang Tua", "Wali", "Pondok Pesantren"
    
    // Data Ayah Kandung
    val nikAyah: String?,
    val namaAyah: String?,
    val tahunLahirAyah: String?,
    val statusAyah: String?, // "Masih Hidup", "Meninggal", "Tidak Diketahui"
    val pendidikanAyah: String?,
    val pekerjaanAyah: String?,
    val penghasilanAyah: String?,
    
    // Data Ibu Kandung
    val nikIbu: String?,
    val namaIbu: String?,
    val tahunLahirIbu: String?,
    val statusIbu: String?,
    val pendidikanIbu: String?,
    val pekerjaanIbu: String?,
    val penghasilanIbu: String?,
    
    // Kontak & Wali
    val noHpOrangTuaWa: String, // Format standar: "62812..."
    val namaWali: String?,
    val hubunganWali: String?,
    val noHpWali: String?,
    
    // Bantuan Pemerintah (Standar EMIS)
    val noKip: String?,
    val noKisPkh: String?,
    
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 8. Anggota Rombel (Lifecycle / Histori Kelas per Tahun Ajaran)
@Entity(
    tableName = "anggota_rombel",
    indices = [
        Index(value = ["siswaId", "tahunAjaranId"], unique = true),
        Index(value = ["kelasId", "tahunAjaranId"])
    ]
)
data class AnggotaRombelEntity(
    @PrimaryKey val id: String,
    val siswaId: String,
    val kelasId: String,
    val tahunAjaranId: String,
    val nomorAbsen: Int,
    val statusKeanggotaan: String, // "AKTIF", "PINDAH_ROMBEL", "KELUAR"
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
```

### 4.3 Presensi, Penilaian Dinamis K13 & Outbox

```kotlin
package id.mts.rth.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 9. Sesi Presensi & Jurnal Mengajar
@Entity(
    tableName = "presensi_sesi",
    indices = [Index(value = ["penugasanId", "tanggal", "jamKe"], unique = true)]
)
data class PresensiSesiEntity(
    @PrimaryKey val id: String, // UUID
    val penugasanId: String,
    val tanggal: String, // YYYY-MM-DD
    val jamKe: String, // Contoh: "1-2" atau "5"
    val materiPokok: String?, // Jurnal singkat KBM hari itu
    val isDeleted: Boolean = false,
    val syncStatus: String, // "SYNCED", "PENDING"
    val createdAt: Long,
    val updatedAt: Long
)

// 10. Detail Presensi Siswa
@Entity(
    tableName = "presensi_detail",
    indices = [
        Index(value = ["sesiId", "siswaId"], unique = true),
        Index(value = ["siswaId"])
    ]
)
data class PresensiDetailEntity(
    @PrimaryKey val id: String,
    val sesiId: String,
    val siswaId: String,
    val statusKehadiran: String, // "H" (Hadir), "S" (Sakit), "I" (Izin), "A" (Alpha)
    val catatan: String?,
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 11. Konfigurasi KKM & Bobot Mapel (Diatur oleh Admin)
@Entity(
    tableName = "kkm_mapel",
    indices = [Index(value = ["mapelId", "tingkatKelas", "tahunAjaranId"], unique = true)]
)
data class KkmMapelEntity(
    @PrimaryKey val id: String,
    val mapelId: String,
    val tingkatKelas: Int, // 7, 8, 9
    val tahunAjaranId: String,
    val nilaiKkm: Int, // Default: 75
    val bobotHarian: Int = 2, // Rata-rata Tugas/PR
    val bobotUlangan: Int = 2, // Rata-rata PH/UH
    val bobotUjianAkhir: Int = 1, // PAS (Ganjil) atau PAT (Genap)
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

// 12. Komponen Penilaian (Header Kolom Dinamis)
@Entity(
    tableName = "komponen_nilai",
    indices = [Index(value = ["penugasanId", "aspek", "kategori"])]
)
data class KomponenNilaiEntity(
    @PrimaryKey val id: String, // UUID
    val penugasanId: String,
    val aspek: String, // "KI3" (Pengetahuan) | "KI4" (Keterampilan)
    val kategori: String, // "TUGAS", "UH", "UJIAN_AKHIR" (PAS/PAT), "PRAKTIK", "PROYEK", "PORTOFOLIO"
    val judul: String, // Contoh: "Tugas 1", "PH Bab 1", "Praktik Muhadatsah"
    val tanggalInput: String, // YYYY-MM-DD
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 13. Skor Nilai Siswa
@Entity(
    tableName = "nilai_siswa",
    indices = [
        Index(value = ["komponenId", "siswaId"], unique = true),
        Index(value = ["siswaId"])
    ]
)
data class NilaiSiswaEntity(
    @PrimaryKey val id: String, // UUID
    val komponenId: String,
    val siswaId: String,
    val skor: Float, // Rentang valid: 0.0f .. 100.0f
    val isDeleted: Boolean = false,
    val syncStatus: String,
    val updatedAt: Long
)

// 14. Tabel Antrean Sinkronisasi dengan Coalescing (Outbox Pattern)
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["tableNameTarget", "recordId"], unique = true)]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String, // UUID
    val tableNameTarget: String,
    val priorityOrder: Int, // 1: Sesi/Komponen (Parent), 2: Detail/Nilai/Siswa (Child)
    val recordId: String,
    val actionType: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

---

## 5. Logika Bisnis & Spesifikasi Fungsional

### 5.1 Autentikasi, Keamanan PIN & Recovery Perangkat
1. **Initial Setup & Full State Hydration (Wajib di Wi-Fi Madrasah):**
   * Guru memasukkan IP Server lokal (default `http://simrth.local:8090` atau IP statis), Username/NIP, dan Password, lalu membuat 6-digit PIN lokal.
   * Server memvalidasi kredensial dan mengirimkan *Full Partition Snapshot* (seluruh master data + riwayat presensi & nilai yang pernah dibuat guru tersebut di semester aktif). Ini menjamin jika guru ganti HP di tengah semester, data lama langsung pulih $100\%$.
   * Aplikasi menyimpan Access Token dan menghasilkan hash PIN menggunakan `PBKDF2WithHmacSHA256` ($10.000$ iterasi + 16-byte cryptographic random salt) di `DataStore`.
2. **Daily Offline Login & Brute-Force Protection:**
   * Di ruang kelas, guru cukup membuka kunci aplikasi dengan 6-digit PIN (validasi murni offline $< 50\text{ ms}$).
   * Jika salah memasukkan PIN **5 kali berturut-turut**, sesi lokal dihapus dan guru wajib terhubung ke Wi-Fi madrasah untuk login ulang menggunakan password akun.

### 5.2 Modul Presensi Cepat (Fast Attendance Engine)
1. Guru memilih jadwal aktif hari ini $\rightarrow$ Menampilkan daftar siswa di rombel terurut berdasarkan `nomorAbsen`.
2. **Default State:** Seluruh siswa otomatis berstatus `Hadir (H)`. Guru hanya perlu mengetuk siswa yang **tidak hadir**.
3. **Interaksi Multi-Tap Sekuensial:**
   $$\text{Hadir (Hijau)} \longrightarrow \text{Sakit (Kuning)} \longrightarrow \text{Izin (Biru)} \longrightarrow \text{Alpha (Merah)} \longrightarrow \text{Hadir (Hijau)}$$
4. **Simpan Atomik:** Menekan tombol **"Simpan Presensi"** mengeksekusi `@Transaction` Room untuk menyimpan `presensi_sesi` (beserta catatan singkat materi hari itu), seluruh baris `presensi_detail`, dan melakukan *upsert* ke `sync_outbox`.

### 5.3 Modul Penilaian Fleksibel & Kalkulasi K13 (KI-3 & KI-4)

#### A. Kalkulasi Nilai Akhir Pengetahuan (KI-3)
1. Hitung rata-rata Tugas ($\overline{T}$) dan Ulangan Harian ($\overline{UH}$) dari komponen yang tidak berstatus `isDeleted`. Kolom nilai yang masih kosong (`null`) pada siswa tertentu diberi indikator khusus di UI dan secara default dikecualikan sementara dari pembagi hingga nilai susulan diinput.
2. Hitung Nilai Akhir Pengetahuan ($\text{NA}_{\text{KI3}}$) berdasarkan bobot di `kkm_mapel`:
   $$\text{NA}_{\text{KI3}} = \frac{(B_{\text{harian}} \times \overline{T}) + (B_{\text{ulangan}} \times \overline{UH}) + (B_{\text{ujian}} \times \text{Skor}_{\text{PAS/PAT}})}{B_{\text{aktif}}}$$
   *(Di mana $B_{\text{aktif}}$ adalah jumlah bobot dari komponen yang sudah memiliki minimal 1 kolom nilai).*

#### B. Kalkulasi Nilai Akhir Keterampilan (KI-4)
Rata-rata dari seluruh komponen kategori keterampilan (`PRAKTIK`, `PROYEK`, `PORTOFOLIO`) yang telah diinput pada semester tersebut, dibulatkan ke bilangan bulat terdekat (`roundToInt()`).

#### C. Rumus Deterministik Interval Predikat K13
Untuk mencegah inkonsistensi pembulatan pada berbagai nilai KKM, aplikasi menggunakan rumus batas bawah integer standar rapor K13:
$$\text{Rentang} = \frac{100 - \text{KKM}}{3.0}$$
$$\text{Batas B} = \text{round}(\text{KKM} + \text{Rentang}), \quad \text{Batas A} = \text{round}(\text{KKM} + 2 \times \text{Rentang})$$

Contoh untuk $\text{KKM} = 75$ ($\text{Rentang} = 8.33$):
* **D (Perlu Bimbingan):** $< 75$
* **C (Cukup):** $75 \le \text{Nilai} \le 82$
* **B (Baik):** $83 \le \text{Nilai} \le 91$
* **A (Sangat Baik):** $92 \le \text{Nilai} \le 100$

### 5.4 Fitur Ekspor Darurat Offline (Emergency CSV Export)
Jika server lokal madrasah mengalami kendala perangkat keras saat masa pengumpulan nilai rapor:
* Guru dapat menekan tombol **"Ekspor Rekap Nilai (.CSV)"** di halaman Penilaian.
* Aplikasi membuat file CSV berformat standar leger rapor K13 MTs Roudhotut Thowalib melalui `ActivityResultContracts.CreateDocument("text/csv")` yang dapat langsung dibagikan ke WhatsApp Operator TU atau disimpan di memori perangkat.

---

## 6. Protokol Sinkronisasi Manual & Resolusi Konflik

### 6.1 Aturan Outbox Coalescing (Anti-Duplikasi Antrean)
Setiap kali terjadi perubahan lokal pada record dengan `recordId` dan `tableNameTarget`:
* DAO memeriksa apakah sudah ada antrean di `sync_outbox` untuk pasangan `(tableNameTarget, recordId)`.
* Jika sudah ada, **jangan buat baris baru**; cukup perbarui kolom `actionType`, `payloadJson`, dan `updatedAt` pada baris antrean tersebut.

### 6.2 Alur Tombol "Sinkronkan Sekarang"
1. **Health Check & Validasi Waktu (Clock Drift Protection):**
   * Request `GET /api/v1/health` (timeout $3.000\text{ ms}$). Server mengembalikan `{"status": "ok", "server_time": 1730551200000}`.
   * Jika selisih $|t_{\text{device}} - t_{\text{server}}| > 300.000\text{ ms}$ ($5\text{ menit}$), sinkronisasi ditolak dengan dialog peringatan agar guru mengaktifkan pengaturan waktu otomatis.
2. **Tahap 1: Push Mutasi Lokal (Client $\rightarrow$ Server):**
   * Ambil data dari `sync_outbox` diurutkan berdasarkan `priorityOrder ASC, createdAt ASC` dalam *chunk* maksimal 250 item per request.
   * Kirim ke `POST /api/v1/sync/push`.
   * Server memproses dalam satu database transaction menggunakan aturan **Last-Write-Wins (LWW)** berbasis `updatedAt` dan mengembalikan daftar `accepted_mutation_ids`.
   * Aplikasi menghapus `accepted_mutation_ids` dari `sync_outbox` dan mengubah `syncStatus` entitas terkait menjadi `"SYNCED"`.
3. **Tahap 2: Pull Pembaruan Delta (Server $\rightarrow$ Client):**
   * Request `GET /api/v1/sync/pull?since={last_server_sync_timestamp}`.
   * Server mengirimkan delta data sesuai RBAC guru tersebut.
   * Room melakukan `@Transaction` *Upsert* hanya untuk record yang tidak sedang berada di `sync_outbox` lokal (mencegah perubahan lokal yang belum terkirim tertimpa data lama server).
   * Simpan `server_timestamp` baru ke `DataStore`.
4. **Tahap 3: Background Media Sync (Foto Siswa):**
   * Bandingkan daftar `fotoFileName` di `siswa_core` dengan file yang ada di `context.filesDir/students/`.
   * Unduh file `.webp` yang belum ada secara asinkron di background thread tanpa memblokir UI.

---

## 7. Desain Sistem UI / UX & Performa (Jetpack Compose)

### 7.1 Hirarki Navigasi Antarmuka Guru
* **Top App Bar:**
  * Judul: **SIMRTH** + Subjudul Rombel/Semester Aktif.
  * Indikator Status Sync: Hijau (`outboxCount == 0`) atau Oranye (`outboxCount > 0` dengan angka badge jumlah antrean).
  * Tombol Pintas Sinkronisasi Manual.
* **Bottom Navigation Bar (4 Tab):**
  1. `Beranda`: Jadwal mengajar hari ini (klik langsung buka presensi), ringkasan status sinkronisasi, info tahun ajaran aktif.
  2. `Presensi`: Pilih Rombel & Jam Ke $\rightarrow$ Daftar siswa dengan indikator warna multi-tap $\rightarrow$ Input singkat materi jurnal $\rightarrow$ Tombol Simpan.
  3. `Penilaian`: Pilih Rombel & Mapel $\rightarrow$ Segmented Button (`KI-3 Pengetahuan` | `KI-4 Keterampilan` | `Rekap Akhir`) $\rightarrow$ Daftar komponen nilai & form input vertikal cepat.
  4. `Siswa`: Direktori siswa. Mode Wali Kelas memunculkan *Badge* **"Kelas Binaan"**, tab detail EMIS lengkap, serta tombol pintas WhatsApp Orang Tua.

### 7.2 Optimasi Performa Input Nilai pada Perangkat Low-End
* **In-Memory State Buffer:** Pada layar input nilai satu rombel ($30\text{--}40\text{ siswa}$), `TextField` **dilarang** menulis langsung ke database Room pada setiap event `onValueChange`.
* Gunakan `mutableStateMapOf<String, String>()` di dalam `ViewModel` untuk menampung input ketikan secara instan ($0\text{ ms}$ *lag*).
* Validasi rentang angka ($0.0 \dots 100.0$) dilakukan secara real-time di memori. Penulisan ke Room + `sync_outbox` dieksekusi sekaligus dalam satu batch `@Transaction` saat guru menekan tombol **"Simpan Nilai"** atau berpindah kolom.

### 7.3 Palet Warna Material 3 (Emerald Madrasah Theme)
* **Primary:** Emerald Teal (`#0F766E`) | **OnPrimary:** Putih (`#FFFFFF`)
* **PrimaryContainer:** Mint Lembut (`#CCFBF1`) | **OnPrimaryContainer:** Teal Gelap (`#134E4A`)
* **Surface / Background:** Slate Putih Bersih (`#F8FAFC`)
* **Semantic Badges (Presensi & KKM):**
  * Hadir (`H`) / Lulus KKM: Hijau (`#16A34A`, bg `#DCFCE7`)
  * Sakit (`S`): Amber (`#D97706`, bg `#FEF3C7`)
  * Izin (`I`): Biru (`#0284C7`, bg `#E0F2FE`)
  * Alpha (`A`) / Di Bawah KKM (Remedial): Merah Rose (`#E11D48`, bg `#FFE4E6`)