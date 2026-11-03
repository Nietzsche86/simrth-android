package id.mts.rth.domain.calculator

import kotlin.math.roundToInt

/**
 * Mesin kalkulasi nilai murni Kurikulum 2013 (K13) MTs Roudhotut Thowalib.
 * Mengimplementasikan rumus deterministik untuk NA Pengetahuan (KI-3),
 * NA Keterampilan (KI-4), serta interval predikat prediktif (A, B, C, D) berbasis KKM.
 */
object K13GradeCalculator {

    /**
     * Menghitung nilai rata-rata dari sekumpulan skor valid (non-null).
     * Mengembalikan null jika daftar kosong.
     */
    fun hitungRataRata(scores: List<Float?>): Float? {
        val validScores = scores.filterNotNull()
        if (validScores.isEmpty()) return null
        return validScores.sum() / validScores.size
    }

    /**
     * Menghitung Nilai Akhir Pengetahuan (NA KI-3) dengan normalisasi bobot aktif.
     * Komponen yang masih bernilai null sementara dikecualikan dari pembagi
     * sehingga guru dapat melihat nilai sementara sebelum PAS/PAT selesai diinput.
     */
    fun hitungNilaiAkhirPengetahuan(
        tugasList: List<Float?>,
        uhList: List<Float?>,
        pasScore: Float?,
        bobotHarian: Int = 2,
        bobotUlangan: Int = 2,
        bobotUjianAkhir: Int = 1
    ): Int {
        val avgTugas = hitungRataRata(tugasList)
        val avgUh = hitungRataRata(uhList)

        var totalWeightedScore = 0.0f
        var activeWeightSum = 0

        if (avgTugas != null && bobotHarian > 0) {
            totalWeightedScore += avgTugas * bobotHarian
            activeWeightSum += bobotHarian
        }

        if (avgUh != null && bobotUh > 0) {
            totalWeightedScore += avgUh * bobotUh
            activeWeightSum += bobotUh
        }

        if (pasScore != null && bobotUjianAkhir > 0) {
            totalWeightedScore += pasScore * bobotUjianAkhir
            activeWeightSum += bobotUjianAkhir
        }

        if (activeWeightSum == 0) return 0

        return (totalWeightedScore / activeWeightSum).roundToInt()
    }

    /**
     * Menghitung Nilai Akhir Keterampilan (NA KI-4).
     * Rata-rata aritmatika dari seluruh komponen praktik, proyek, dan portofolio.
     */
    fun hitungNilaiAkhirKeterampilan(keterampilanScores: List<Float?>): Int {
        val avg = hitungRataRata(keterampilanScores) ?: return 0
        return avg.roundToInt()
    }

    /**
     * Menghitung batas interval nilai K13 secara deterministik berdasarkan KKM.
     * Rentang = (100 - KKM) / 3.0
     * Batas B = round(KKM + Rentang)
     * Batas A = round(KKM + 2 * Rentang)
     *
     * @return Triple(batasBawahC = KKM, batasBawahB = Batas B, batasBawahA = Batas A)
     */
    fun hitungBatasInterval(kkm: Int): Triple<Int, Int, Int> {
        val rentang = (100 - kkm) / 3.0
        val batasB = (kkm + rentang).roundToInt()
        val batasA = (kkm + 2 * rentang).roundToInt()
        return Triple(kkm, batasB, batasA)
    }

    /**
     * Menentukan predikat mutu akademik K13 (A, B, C, D) berdasarkan Nilai Akhir dan KKM.
     * - D : Nilai < KKM (Perlu Bimbingan / Belum Tuntas)
     * - C : KKM <= Nilai < Batas B (Cukup)
     * - B : Batas B <= Nilai < Batas A (Baik)
     * - A : Batas A <= Nilai <= 100 (Sangat Baik)
     */
    fun hitungPredikatK13(nilaiAkhir: Int, kkm: Int): Char {
        val (batasC, batasB, batasA) = hitungBatasInterval(kkm)
        return when {
            nilaiAkhir >= batasA -> 'A'
            nilaiAkhir >= batasB -> 'B'
            nilaiAkhir >= batasC -> 'C'
            else -> 'D'
        }
    }

    /**
     * Memeriksa apakah nilai akhir siswa berada di bawah KKM (remedial).
     */
    fun isRemedial(nilaiAkhir: Int, kkm: Int): Boolean {
        return nilaiAkhir < kkm
    }
}
