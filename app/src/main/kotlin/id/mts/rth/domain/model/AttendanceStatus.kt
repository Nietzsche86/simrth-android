package id.mts.rth.domain.model

/**
 * Representasi status kehadiran siswa pada sesi presensi KBM:
 * - H = Hadir
 * - S = Sakit
 * - I = Izin
 * - A = Alpha (Tanpa Keterangan)
 */
enum class AttendanceStatus(val code: String, val label: String) {
    HADIR("H", "Hadir"),
    SAKIT("S", "Sakit"),
    IZIN("I", "Izin"),
    ALPHA("A", "Alpha");

    /**
     * Memfasilitasi rotasi satu-sentuhan (Single-Tap Sequential Rotation):
     * H -> S -> I -> A -> H
     */
    fun nextStatus(): AttendanceStatus {
        return when (this) {
            HADIR -> SAKIT
            SAKIT -> IZIN
            IZIN -> ALPHA
            ALPHA -> HADIR
        }
    }

    companion object {
        fun fromCode(code: String): AttendanceStatus {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HADIR
        }
    }
}
