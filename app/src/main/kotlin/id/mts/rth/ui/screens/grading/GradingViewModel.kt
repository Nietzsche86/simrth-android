package id.mts.rth.ui.screens.grading

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.core.export.CsvExporter
import id.mts.rth.core.export.CsvHeaderMetadata
import id.mts.rth.core.export.CsvStudentRowData
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AttendanceRepository
import id.mts.rth.data.repository.AttendanceStudentItem
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.GradingRepository
import id.mts.rth.domain.calculator.K13GradeCalculator
import id.mts.rth.domain.model.StudentGradeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GradingOverviewState(
    val penugasanId: String = "",
    val kelasId: String = "",
    val namaKelas: String = "Memuat...",
    val mapelId: String = "",
    val namaMapel: String = "Memuat...",
    val guruName: String = "",
    val tahunAjaran: String = "",
    val semester: String = "",
    val tingkatKelas: Int = 7,
    val kkm: KkmMapelEntity = KkmMapelEntity(
        id = "default",
        mapelId = "",
        tingkatKelas = 7,
        tahunAjaranId = "",
        nilaiKkm = 75,
        bobotHarian = 2,
        bobotUlangan = 2,
        bobotUjianAkhir = 1,
        updatedAt = 0L
    ),
    val komponenKi3: List<KomponenNilaiEntity> = emptyList(),
    val komponenKi4: List<KomponenNilaiEntity> = emptyList(),
    val students: List<AttendanceStudentItem> = emptyList(),
    val summaries: List<StudentGradeSummary> = emptyList(),
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = true
)

class GradingViewModel(
    private val gradingRepository: GradingRepository,
    private val academicRepository: AcademicRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GradingOverviewState())
    val state: StateFlow<GradingOverviewState> = _state.asStateFlow()

    // In-Memory Buffer untuk input vertikal cepat tanpa recomposition lag
    // Key format: "${siswaId}_${komponenId}" -> Nilai string ("85", "90.5", "")
    val gradeInputBuffer = mutableStateMapOf<String, String>()

    fun initialize(
        penugasanIdParam: String? = null,
        kelasIdParam: String? = null,
        mapelIdParam: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, feedbackMessage = null) }

            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id ?: ""
            val tahunAjaranStr = activeTahun?.tahun ?: "2024/2025"
            val semesterStr = activeTahun?.semester ?: "Ganjil"

            val session = authRepository.sessionFlow.first()
            val currentGuruName = session.userName ?: "Guru SIMRTH"

            // 1. Resolve Penugasan Guru
            var penugasan: PenugasanGuruEntity? = null
            if (!penugasanIdParam.isNullOrBlank()) {
                penugasan = academicRepository.getPenugasanById(penugasanIdParam)
            }

            if (penugasan == null && !kelasIdParam.isNullOrBlank() && !mapelIdParam.isNullOrBlank()) {
                val guruPenugasanList = academicRepository.getPenugasanByGuru(session.userId.orEmpty(), tahunAjaranId).first()
                penugasan = guruPenugasanList.firstOrNull { it.kelasId == kelasIdParam && it.mapelId == mapelIdParam }
                if (penugasan == null) {
                    val allPenugasan = academicRepository.getPenugasanByTahunAjaran(tahunAjaranId).first()
                    penugasan = allPenugasan.firstOrNull { it.kelasId == kelasIdParam && it.mapelId == mapelIdParam }
                }
            }

            val penugasanId = penugasan?.id.orEmpty()
            val kelasId = penugasan?.kelasId ?: kelasIdParam.orEmpty()
            val mapelId = penugasan?.mapelId ?: mapelIdParam.orEmpty()

            val kelas = if (kelasId.isNotBlank()) academicRepository.getKelasById(kelasId) else null
            val mapel = if (mapelId.isNotBlank()) academicRepository.getMapelById(mapelId) else null
            val tingkatKelas = kelas?.tingkat ?: 7

            // 2. Resolve KKM
            val kkmEntity = if (mapelId.isNotBlank()) {
                gradingRepository.getKkmSync(mapelId, tingkatKelas, tahunAjaranId)
            } else null
            val finalKkm = kkmEntity ?: KkmMapelEntity(
                id = "kkm_${mapelId}_${tingkatKelas}",
                mapelId = mapelId,
                tingkatKelas = tingkatKelas,
                tahunAjaranId = tahunAjaranId,
                nilaiKkm = 75,
                bobotHarian = 2,
                bobotUlangan = 2,
                bobotUjianAkhir = 1,
                updatedAt = System.currentTimeMillis()
            )

            // 3. Ambil data siswa rombel
            val studentItems = if (kelasId.isNotBlank()) {
                attendanceRepository.getStudentsByKelas(kelasId, tahunAjaranId).first()
            } else emptyList()

            // 4. Observasi Komponen dan Skor Nilai secara Reaktif
            combine(
                gradingRepository.getKomponenByPenugasan(penugasanId, "KI3"),
                gradingRepository.getKomponenByPenugasan(penugasanId, "KI4"),
                gradingRepository.getNilaiSiswaByPenugasan(penugasanId)
            ) { ki3List, ki4List, nilaiList ->
                // Isi buffer in-memory jika belum diedit lokal
                for (nilai in nilaiList) {
                    val key = "${nilai.siswaId}_${nilai.komponenId}"
                    if (!gradeInputBuffer.containsKey(key)) {
                        gradeInputBuffer[key] = if (nilai.skor % 1.0f == 0.0f) {
                            nilai.skor.toInt().toString()
                        } else {
                            nilai.skor.toString()
                        }
                    }
                }

                // Kalkulasi Rekapitulasi K13 per Siswa
                val computedSummaries = studentItems.map { student ->
                    val sId = student.siswaId

                    val tugasScores = ki3List.filter { it.kategori.equals("TUGAS", ignoreCase = true) }
                        .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
                    val uhScores = ki3List.filter { it.kategori.equals("UH", ignoreCase = true) }
                        .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
                    val pasScore = ki3List.firstOrNull { it.kategori.equals("UJIAN_AKHIR", ignoreCase = true) }
                        ?.let { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }

                    val naKi3 = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
                        tugasList = tugasScores,
                        uhList = uhScores,
                        pasScore = pasScore,
                        bobotHarian = finalKkm.bobotHarian,
                        bobotUlangan = finalKkm.bobotUlangan,
                        bobotUjianAkhir = finalKkm.bobotUjianAkhir
                    )
                    val predikatKi3 = K13GradeCalculator.hitungPredikatK13(naKi3, finalKkm.nilaiKkm)

                    val ki4Scores = ki4List.map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
                    val naKi4 = K13GradeCalculator.hitungNilaiAkhirKeterampilan(ki4Scores)
                    val predikatKi4 = K13GradeCalculator.hitungPredikatK13(naKi4, finalKkm.nilaiKkm)

                    StudentGradeSummary(
                        siswaId = sId,
                        namaLengkap = student.namaLengkap,
                        nomorAbsen = student.nomorAbsen,
                        rataTugas = K13GradeCalculator.hitungRataRata(tugasScores),
                        rataUh = K13GradeCalculator.hitungRataRata(uhScores),
                        pasScore = pasScore,
                        naPengetahuan = naKi3,
                        predikatKi3 = predikatKi3,
                        naKeterampilan = naKi4,
                        predikatKi4 = predikatKi4,
                        isRemedialKi3 = naKi3 < finalKkm.nilaiKkm,
                        isRemedialKi4 = naKi4 < finalKkm.nilaiKkm
                    )
                }

                Triple(ki3List, ki4List, computedSummaries)
            }.collect { (ki3List, ki4List, summaries) ->
                _state.update {
                    it.copy(
                        penugasanId = penugasanId,
                        kelasId = kelasId,
                        namaKelas = kelas?.namaRombel ?: "Kelas $kelasId",
                        mapelId = mapelId,
                        namaMapel = mapel?.namaMapel ?: "Mata Pelajaran",
                        guruName = currentGuruName,
                        tahunAjaran = tahunAjaranStr,
                        semester = semesterStr,
                        tingkatKelas = tingkatKelas,
                        kkm = finalKkm,
                        komponenKi3 = ki3List,
                        komponenKi4 = ki4List,
                        students = studentItems,
                        summaries = summaries,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateScoreInBuffer(siswaId: String, komponenId: String, scoreStr: String) {
        val key = "${siswaId}_${komponenId}"
        gradeInputBuffer[key] = scoreStr
        recalculateSummaries()
    }

    private fun recalculateSummaries() {
        val currentState = _state.value
        val kkm = currentState.kkm
        val updatedSummaries = currentState.students.map { student ->
            val sId = student.siswaId

            val tugasScores = currentState.komponenKi3.filter { it.kategori.equals("TUGAS", ignoreCase = true) }
                .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
            val uhScores = currentState.komponenKi3.filter { it.kategori.equals("UH", ignoreCase = true) }
                .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
            val pasScore = currentState.komponenKi3.firstOrNull { it.kategori.equals("UJIAN_AKHIR", ignoreCase = true) }
                ?.let { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }

            val naKi3 = K13GradeCalculator.hitungNilaiAkhirPengetahuan(
                tugasList = tugasScores,
                uhList = uhScores,
                pasScore = pasScore,
                bobotHarian = kkm.bobotHarian,
                bobotUlangan = kkm.bobotUlangan,
                bobotUjianAkhir = kkm.bobotUjianAkhir
            )
            val predikatKi3 = K13GradeCalculator.hitungPredikatK13(naKi3, kkm.nilaiKkm)

            val ki4Scores = currentState.komponenKi4.map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
            val naKi4 = K13GradeCalculator.hitungNilaiAkhirKeterampilan(ki4Scores)
            val predikatKi4 = K13GradeCalculator.hitungPredikatK13(naKi4, kkm.nilaiKkm)

            StudentGradeSummary(
                siswaId = sId,
                namaLengkap = student.namaLengkap,
                nomorAbsen = student.nomorAbsen,
                rataTugas = K13GradeCalculator.hitungRataRata(tugasScores),
                rataUh = K13GradeCalculator.hitungRataRata(uhScores),
                pasScore = pasScore,
                naPengetahuan = naKi3,
                predikatKi3 = predikatKi3,
                naKeterampilan = naKi4,
                predikatKi4 = predikatKi4,
                isRemedialKi3 = naKi3 < kkm.nilaiKkm,
                isRemedialKi4 = naKi4 < kkm.nilaiKkm
            )
        }

        _state.update { it.copy(summaries = updatedSummaries) }
    }

    /**
     * Membuat kolom komponen nilai baru (KI-3 atau KI-4).
     */
    fun createKomponen(
        aspek: String,
        kategori: String,
        judul: String,
        onSuccess: () -> Unit = {}
    ) {
        val currentState = _state.value
        if (currentState.penugasanId.isBlank()) {
            _state.update { it.copy(feedbackMessage = "Penugasan mengajar tidak valid.", isError = true) }
            return
        }

        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            val result = gradingRepository.createKomponen(
                penugasanId = currentState.penugasanId,
                aspek = aspek,
                kategori = kategori,
                judul = judul.trim(),
                tanggalInput = todayStr
            )

            when (result) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            feedbackMessage = "Kolom nilai '${judul}' berhasil ditambahkan!",
                            isError = false
                        )
                    }
                    onSuccess()
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            feedbackMessage = result.message,
                            isError = true
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * Menyimpan batch nilai untuk komponen tertentu dari buffer ke database lokal dan outbox.
     */
    fun saveKomponenGrades(
        komponenId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, feedbackMessage = null) }

            val currentState = _state.value
            val scoresMap = mutableMapOf<String, Float>()

            for (student in currentState.students) {
                val key = "${student.siswaId}_${komponenId}"
                val scoreStr = gradeInputBuffer[key]
                val scoreFloat = scoreStr?.toFloatOrNull()
                if (scoreFloat != null) {
                    scoresMap[student.siswaId] = scoreFloat
                }
            }

            val result = gradingRepository.saveBatchGrades(komponenId, scoresMap)
            when (result) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Nilai siswa berhasil disimpan ke database & outbox!",
                            isError = false
                        )
                    }
                    onSuccess()
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = result.message,
                            isError = true
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun dismissFeedback() {
        _state.update { it.copy(feedbackMessage = null) }
    }

    /**
     * Menghasilkan teks CSV rekapitulasi nilai K13 untuk seluruh siswa.
     */
    fun generateCsvContent(): String {
        val s = _state.value
        val metadata = CsvHeaderMetadata(
            madrasahName = "MTs Roudhotut Thowalib",
            namaMapel = s.namaMapel,
            namaKelas = s.namaKelas,
            tahunAjaran = s.tahunAjaran,
            semester = s.semester,
            guruName = s.guruName,
            kkm = s.kkm.nilaiKkm
        )

        val studentMap = s.students.associateBy { it.siswaId }
        val rows = s.summaries.map { summary ->
            val student = studentMap[summary.siswaId]

            val sId = summary.siswaId
            val praktikScores = s.komponenKi4.filter { it.kategori.equals("PRAKTIK", ignoreCase = true) }
                .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
            val proyekScores = s.komponenKi4.filter { it.kategori.equals("PROYEK", ignoreCase = true) }
                .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }
            val portofolioScores = s.komponenKi4.filter { it.kategori.equals("PORTOFOLIO", ignoreCase = true) }
                .map { gradeInputBuffer["${sId}_${it.id}"]?.toFloatOrNull() }

            CsvStudentRowData(
                summary = summary,
                nisn = student?.nisn,
                nisLokal = student?.nisLokal.orEmpty(),
                avgPraktik = K13GradeCalculator.hitungRataRata(praktikScores),
                avgProyek = K13GradeCalculator.hitungRataRata(proyekScores),
                avgPortofolio = K13GradeCalculator.hitungRataRata(portofolioScores)
            )
        }

        return CsvExporter.generateK13GradesCsv(metadata, rows)
    }
}
