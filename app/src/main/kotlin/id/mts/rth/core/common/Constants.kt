package id.mts.rth.core.common

object Constants {
    // Toleransi Clock Drift maksimal (5 menit)
    const val MAX_CLOCK_DRIFT_MS = 300_000L

    // Ukuran batch maksimal pengiriman mutasi outbox ke server
    const val SYNC_BATCH_SIZE = 250

    // Manajemen foto siswa WebP
    const val STUDENT_PHOTO_DIR = "students"
    const val STUDENT_PHOTO_MAX_WIDTH = 300
    const val STUDENT_PHOTO_MAX_HEIGHT = 300
    const val STUDENT_PHOTO_QUALITY = 80
    const val STUDENT_PHOTO_MAX_BYTES = 25 * 1024 // 25 KB
}
