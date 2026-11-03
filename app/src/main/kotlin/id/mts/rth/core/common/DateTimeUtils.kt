package id.mts.rth.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DateTimeUtils {
    private const val ISO_DATE_PATTERN = "yyyy-MM-dd"

    /**
     * Mengembalikan tanggal hari ini dalam format standar ISO "YYYY-MM-DD".
     */
    fun getTodayIsoDate(): String {
        val sdf = SimpleDateFormat(ISO_DATE_PATTERN, Locale.US)
        return sdf.format(Date())
    }

    /**
     * Mengonversi epoch millis ke format string tanggal "YYYY-MM-DD".
     */
    fun formatEpochToIsoDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat(ISO_DATE_PATTERN, Locale.US)
        return sdf.format(Date(epochMillis))
    }

    /**
     * Memeriksa apakah selisih waktu perangkat dengan server masih dalam batas toleransi (<= 5 menit).
     */
    fun isClockDriftAcceptable(deviceTime: Long, serverTime: Long): Boolean {
        return abs(deviceTime - serverTime) <= Constants.MAX_CLOCK_DRIFT_MS
    }

    /**
     * Mengembalikan selisih waktu dalam satuan menit (untuk pesan peringatan user).
     */
    fun getClockDriftMinutes(deviceTime: Long, serverTime: Long): Long {
        return abs(deviceTime - serverTime) / (1000 * 60)
    }
}
