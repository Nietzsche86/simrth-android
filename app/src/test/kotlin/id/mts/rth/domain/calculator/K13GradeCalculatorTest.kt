package id.mts.rth.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test untuk memverifikasi kalkulasi nilai rapor K13 MTs Roudhotut Thowalib.
 * Menguji kepatuhan terhadap formula PRD 5.3:
 * 1. Batas interval predikat deterministik untuk berbagai nilai KKM.
 * 2. Normalisasi bobot aktif saat komponen belum terisi lengkap.
 * 3. Kalkulasi nilai akhir keterampilan dan status remedial.
 */
class K13GradeCalculatorTest {

    @Test
    fun `test interval predikat KKM 75 sesuai standar PRD`() {
        val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(kkm = 75)
        assertEquals(75, batasC)
        assertEquals(83, batasB)
        assertEquals(92, batasA)

        // Predikat D (Perlu Bimbingan): < 75
        assertEquals('D', K13GradeCalculator.hitungPredikatK13(0, 75))
        assertEquals('D', K13GradeCalculator.hitungPredikatK13(74, 75))
        assertTrue(K13GradeCalculator.isRemedial(74, 75))

        // Predikat C (Cukup): 75 .. 82
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(75, 75))
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(80, 75))
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(82, 75))
        assertFalse(K13GradeCalculator.isRemedial(75, 75))

        // Predikat B (Baik): 83 .. 91
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(83, 75))
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(88, 75))
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(91, 75))

        // Predikat A (Sangat Baik): 92 .. 100
        assertEquals('A', K13GradeCalculator.hitungPredikatK13(92, 75))
        assertEquals('A', K13GradeCalculator.hitungPredikatK13(95, 75))
        assertEquals('A', K13GradeCalculator.hitungPredikatK13(100, 75))
    }

    @Test
    fun `test interval predikat KKM 70`() {
        val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(kkm = 70)
        assertEquals(70, batasC)
        assertEquals(80, batasB)
        assertEquals(90, batasA)

        assertEquals('D', K13GradeCalculator.hitungPredikatK13(69, 70))
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(70, 70))
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(79, 70))
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(80, 70))
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(89, 70))
        assertEquals('A', K13GradeCalculator.hitungPredikatK13(90, 70))
    }

    @Test
    fun `test interval predikat KKM 80`() {
        val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(kkm = 80)
        assertEquals(80, batasC)
        assertEquals(87, batasB)
        assertEquals(93, batasA)

        assertEquals('D', K13GradeCalculator.hitungPredikatK13(79, 80))
        assertEquals('C', K13GradeCalculator.hitungPredikatK13(80, 80))
        assertEquals('B', K13GradeCalculator.hitungPredikatK13(87, 80))
        assertEquals('A', K13GradeCalculator.hitungPredikatK13(93, 80))
    }

    @Test
    fun `test hitung rata rata dengan elemen null`() {
        assertNull(K13GradeCalculator.hitungRataRata(emptyList()))
        assertNull(K13GradeCalculator.hitungRataRata(listOf(null, null)))

        val result = K13GradeCalculator.hitungRataRata(listOf(80.0f, null, 90.0f))
        assertEquals(85.0f, result!!, 0.001f)
    }

    @Test
    fun `test kalkulasi NA KI-3 dengan bobot dinamis aktif`() {
        // Kasus 1: Lengkap (Tugas, UH, PAS) dengan bobot 2, 2, 1
        // Rata Tugas = 80, Rata UH = 85, PAS = 90
        // NA = (80*2 + 85*2 + 90*1) / 5 = (160 + 170 + 90) / 5 = 420 / 5 = 84
        val naLengkap = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
            tugasList = listOf(80f, 80f),
            uhList = listOf(85f),
            pasScore = 90f,
            bobotHarian = 2,
            bobotUlangan = 2,
            bobotUjianAkhir = 1
        )
        assertEquals(84, naLengkap)

        // Kasus 2: PAS belum diisi (bobot pembagi hanya tugas & UH = 4)
        // NA = (80*2 + 85*2) / 4 = 330 / 4 = 82.5 -> roundToInt() = 83
        val naTanpaPas = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
            tugasList = listOf(80f),
            uhList = listOf(85f),
            pasScore = null,
            bobotHarian = 2,
            bobotUlangan = 2,
            bobotUjianAkhir = 1
        )
        assertEquals(83, naTanpaPas)

        // Kasus 3: Baru Tugas 1 yang diinput (pembagi hanya bobot harian = 2)
        // NA = (75*2) / 2 = 75
        val naAwalSemester = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
            tugasList = listOf(75f),
            uhList = emptyList(),
            pasScore = null,
            bobotHarian = 2,
            bobotUlangan = 2,
            bobotUjianAkhir = 1
        )
        assertEquals(75, naAwalSemester)

        // Kasus 4: Belum ada nilai sama sekali
        val naKosong = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
            tugasList = emptyList(),
            uhList = emptyList(),
            pasScore = null
        )
        assertEquals(0, naKosong)
    }

    @Test
    fun `test kalkulasi NA KI-4 keterampilan`() {
        val emptyScore = K13GradeCalculator.hitungNilaiAkhirKeterampilan(emptyList())
        assertEquals(0, emptyScore)

        // Praktik = 85, Proyek = 90, Portofolio = 88 -> Rata = 263 / 3 = 87.666 -> 88
        val scores = listOf(85f, 90f, 88f)
        val naKeterampilan = K13GradeCalculator.hitungNilaiAkhirKeterampilan(scores)
        assertEquals(88, naKeterampilan)
    }
}
