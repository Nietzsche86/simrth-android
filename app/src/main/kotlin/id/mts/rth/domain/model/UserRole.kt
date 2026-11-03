package id.mts.rth.domain.model

/**
 * Matriks Peran Pengguna (RBAC) pada sistem SIMRTH:
 * - GURU: Guru Mapel biasa (hanya akses kelas & mapel yang diampu, tidak mengunduh detail EMIS).
 * - GURU_WALI: Wali Kelas (akses kelas ajar + akses penuh detail EMIS siswa kelas binaannya).
 * - ADMIN: Operator TU / Kepala Madrasah (akses master data, konfigurasi KKM, dan mutasi).
 */
enum class UserRole {
    GURU,
    GURU_WALI,
    ADMIN;

    companion object {
        fun fromString(role: String?): UserRole {
            return entries.firstOrNull { it.name.equals(role, ignoreCase = true) } ?: GURU
        }
    }
}
