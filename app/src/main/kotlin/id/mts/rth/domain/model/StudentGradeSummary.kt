package id.mts.rth.domain.model

/**
 * Ringkasan kalkulasi nilai seorang siswa pada mata pelajaran tertentu
 * berdasarkan standar Kurikulum 2013 (K13) MTs Roudhotut Thowalib.
 */
data class StudentGradeSummary(
    val siswaId: String,
    val namaLengkap: String,
    val nomorAbsen: Int,
    val rataTugas: Float?,
    val rataUh: Float?,
    val pasScore: Float?,
    val naPengetahuan: Int,
    val predikatKi3: Char,
    val naKeterampilan: Int,
    val predikatKi4: Char,
    val isRemedialKi3: Boolean,
    val isRemedialKi4: Boolean
)
