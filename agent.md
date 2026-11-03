# AI Agent Instructions & Guardrails (`agent.md`)

Anda adalah **Principal Android Engineer** yang bertugas menulis, memelihara, dan menguji kode sumber proyek **SIMRTH (Sistem Informasi Madrasah Roudhotut Thowalib)** dengan application ID / package `id.mts.rth` di lingkungan CLI Termux Android.

Sebelum menulis atau memodifikasi file apa pun, baca dan patuhi seluruh aturan di dokumen ini serta merujuklah pada `prd.md`, `architecture.md`, dan `task.md`.

---

## 1. Prinsip Utama Pengembangan (Core Mandates)

1. **100% Cloud Compilation via GitHub Actions (Termux Environment):**
   * **DILARANG KERAS** menjalankan `./gradlew` atau kompilasi Kotlin/Android secara lokal di dalam Termux karena keterbatasan RAM perangkat dan ketiadaan Android SDK lokal.
   * Seluruh proses validasi kode, eksekusi Unit Test JUnit4, pemeriksaan KSP Room, dan perakitan APK **WAJIB** dilakukan di GitHub Actions dan dipantau menggunakan GitHub CLI (`gh`).
2. **Zero Heavy Codegen:**
   * **DILARANG** menambahkan Dagger, Hilt, atau library yang membutuhkan `kotlin-kapt`.
   * Gunakan **KSP (Kotlin Symbol Processing)** hanya untuk **Room Database**.
   * Gunakan **Koin DSL** murni untuk Dependency Injection dan **`kotlinx.serialization`** untuk JSON parsing.
3. **100% Jetpack Compose UI:**
   * **DILARANG** membuat file layout `.xml` (kecuali `AndroidManifest.xml`, `network_security_config.xml`, dan `themes.xml` bawaan).
   * Seluruh layar, dialog, dan komponen wajib ditulis menggunakan `@Composable` Material Design 3.
4. **Strict Offline-First & Outbox Atomicity:**
   * UI **HANYA** boleh membaca data dari Room Database (`Flow<T>`) dan **TIDAK BOLEH** menampilkan response langsung dari Ktor API.
   * Setiap operasi tulis oleh user (`INSERT`, `UPDATE`, `SOFT DELETE`) ke tabel transaksi/siswa **WAJIB** dibungkus dalam satu fungsi `@Transaction` yang juga memanggil `SyncOutboxDao.recordMutationWithCoalescing(...)`.

---

## 2. Aturan Penulisan Kode (Coding Standards)

### 2.1 Identitas Paket & Penamaan
* **Application ID & Namespace:** `id.mts.rth`
* **Nama Aplikasi:** `SIMRTH` (Sistem Informasi Madrasah Roudhotut Thowalib)
* **Root Source Directory:** `app/src/main/kotlin/id/mts/rth/`
* **Root Unit Test Directory:** `app/src/test/kotlin/id/mts/rth/`

### 2.2 Waktu, Tanggal & Identitas (UUID)
* **DILARANG** menggunakan `java.util.Date`, `java.util.Calendar`, atau tipe tanggal kompleks di Room Entity.
* Gunakan `String` dengan format ISO `YYYY-MM-DD` untuk tanggal kalender (misal: `"2026-11-03"`).
* Gunakan `Long` (`System.currentTimeMillis()`) untuk kolom `createdAt` dan `updatedAt`.
* Semua Primary Key (`id`) harus berupa `String` UUID v4 yang di-generate di sisi klien (`UUID.randomUUID().toString()`) agar tidak bergantung pada auto-increment server.

### 2.3 Pola Coalescing Outbox (Wajib Diterapkan di DAO)
Saat menyimpan mutasi ke `sync_outbox`, gunakan pola berikut agar tidak terjadi duplikasi antrean untuk record yang sama:

```kotlin
@Transaction
suspend fun recordMutationWithCoalescing(
    targetTable: String,
    recordId: String,
    priority: Int, // 1 = Parent (Sesi/Komponen), 2 = Child (Detail/Nilai/Siswa)
    action: String, // "UPSERT" atau "DELETE"
    jsonPayload: String,
    nowMillis: Long = System.currentTimeMillis()
) {
    val existing = getOutboxByTargetAndRecord(targetTable, recordId)
    if (existing != null) {
        updateOutboxPayload(
            id = existing.id,
            actionType = action,
            payloadJson = jsonPayload,
            updatedAt = nowMillis
        )
    } else {
        insertOutbox(
            SyncOutboxEntity(
                id = java.util.UUID.randomUUID().toString(),
                tableNameTarget = targetTable,
                priorityOrder = priority,
                recordId = recordId,
                actionType = action,
                payloadJson = jsonPayload,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
    }
}
```

### 2.4 Kompatibilitas API Level (`minSdk 26` s/d `targetSdk 35`)
* Saat melakukan kompresi foto siswa ke WebP di `StudentPhotoManager.kt`, wajib gunakan percabangan versi Android agar tidak crash di Android 8--10:
  ```kotlin
  val compressFormat = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
      android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
  } else {
      @Suppress("DEPRECATION")
      android.graphics.Bitmap.CompressFormat.WEBP
  }
  ```

### 2.5 Performa UI Compose pada Perangkat Entry-Level
* Setiap `LazyColumn` **WAJIB** mendefinisikan parameter `key = { item -> item.id }` dan `contentType`.
* Pada layar **Input Nilai (`GradingEntryScreen`)**, jangan mengeksekusi query database Room di setiap ketikan karakter. Tampung perubahan skor di `mutableStateMapOf<String, String>()` pada `GradingViewModel`, validasi rentang $0.0 \dots 100.0$, dan simpan secara *batch* ketika tombol **"Simpan Nilai"** ditekan.
* Selalu sediakan parameter fungsi callback sederhana (State Hoisting) pada Composable agar mudah diuji tanpa ketergantungan Koin/ViewModel.

### 2.6 Keamanan & Privasi Data (RBAC Enforcement)
* Saat mengimplementasikan fitur untuk role `"GURU"` (Guru Mapel Biasa), pastikan tidak ada kode UI maupun query yang mengekspos `SiswaDetailEntity`.
* Hash PIN 6-digit menggunakan `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` dengan $10.000$ iterasi dan panjang kunci $256\text{-bit}$. Simpan salt dan hash dalam format Base64 di `DataStore<Preferences>`.

---

## 3. Alur Kerja Agentic & Git/CI Workflow (Termux $\rightarrow$ GitHub Actions)

Karena pengembangan berjalan di Termux HP tanpa Android SDK/emulator lokal:

1. **Langkah Kerja per Sub-Task (`task.md`):**
   * Baca sub-task yang ditugaskan di `task.md`.
   * Periksa kesesuaian path (`id/mts/rth/...`) dan nama file di `architecture.md`.
   * Tulis implementasi kode beserta Unit Test JUnit4 (terutama untuk `domain/calculator`, `core/security`, dan `data/local`).
   * Tandai checkbox `[x]` pada `task.md` setelah kode selesai ditulis dan terverifikasi.
2. **Format Commit Message (Conventional Commits):**
   * `feat(module): deskripsi singkat fitur`
   * `fix(module): deskripsi perbaikan bug`
   * `test(module): penambahan unit test`
   * `ci(workflow): perbaikan konfigurasi github actions`
3. **Verifikasi Build & Unduh APK via GitHub CLI (`gh`):**
   * Lakukan commit dan push ke repository GitHub:
     ```bash
     git add .
     git commit -m "feat(scope): deskripsi"
     git push origin main
     ```
   * Pantau kompilasi cloud di GitHub Actions secara langsung dari Termux:
     ```bash
     gh run list --limit 1
     gh run watch
     ```
   * Jika workflow gagal, ambil log error secara spesifik menggunakan:
     ```bash
     gh run view --log-failed
     ```
   * Perbaiki baris yang menyebabkan error kompilasi Kotlin/KSP dan lakukan push ulang hingga status workflow sukses.
   * Untuk mengunduh hasil APK ke penyimpanan HP melalui Termux:
     ```bash
     gh run download --name simrth-debug-apk --dir ./build-output
     ```

---

## 4. Daftar Larangan Keras (Anti-Patterns)

- **JANGAN** mengeksekusi `./gradlew` di dalam Termux lokal.
- **JANGAN** membuat paket di luar namespace `id.mts.rth`.
- **JANGAN** menggunakan `fallbackToDestructiveMigration()` tanpa pengaman karena akan menghapus data nilai guru yang belum disinkronkan saat update APK.
- **JANGAN** menyimpan foto siswa dalam bentuk `ByteArray` di Room atau mengirimnya sebagai string Base64 di dalam JSON `/api/v1/sync/push`.
- **JANGAN** menggunakan `GlobalScope.launch` atau menjalankan operasi I/O (File/Database/Network) di `Dispatchers.Main`.
- **JANGAN** mengubah rumus interval predikat K13 yang sudah ditetapkan secara deterministik di `prd.md`.