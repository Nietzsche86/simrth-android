package id.mts.rth.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AttendanceRepository
import id.mts.rth.data.repository.AttendanceStudentItem
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.StudentAttendanceDetailInput
import id.mts.rth.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StudentAttendanceUiModel(
    val siswaId: String,
    val nomorAbsen: Int,
    val nisLokal: String,
    val nisn: String?,
    val namaLengkap: String,
    val jenisKelamin: String,
    val status: AttendanceStatus = AttendanceStatus.HADIR,
    val catatan: String = ""
)

data class AttendanceScreenState(
    val penugasanId: String = "",
    val kelasId: String = "",
    val namaKelas: String = "Memuat...",
    val mapelId: String = "",
    val namaMapel: String = "Memuat...",
    val tanggal: String = "", // Format: "YYYY-MM-DD"
    val formattedTanggal: String = "", // Format: "Senin, 3 November 2026"
    val jamKe: String = "1-2",
    val materiPokok: String = "",
    val students: List<StudentAttendanceUiModel> = emptyList(),
    val existingSesiId: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
) {
    val totalHadir: Int get() = students.count { it.status == AttendanceStatus.HADIR }
    val totalSakit: Int get() = students.count { it.status == AttendanceStatus.SAKIT }
    val totalIzin: Int get() = students.count { it.status == AttendanceStatus.IZIN }
    val totalAlpha: Int get() = students.count { it.status == AttendanceStatus.ALPHA }
    val totalSiswa: Int get() = students.size
}

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val academicRepository: AcademicRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceScreenState())
    val state: StateFlow<AttendanceScreenState> = _state.asStateFlow()

    fun initialize(
        penugasanIdParam: String? = null,
        kelasIdParam: String? = null,
        mapelIdParam: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val today = LocalDate.now()
            val tanggalStr = today.toString() // YYYY-MM-DD
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
            val formattedTanggalStr = today.format(dateFormatter)

            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id ?: ""

            // Cari Penugasan Guru
            var resolvedPenugasan: PenugasanGuruEntity? = null
            if (!penugasanIdParam.isNullOrBlank()) {
                resolvedPenugasan = academicRepository.getPenugasanById(penugasanIdParam)
            }

            if (resolvedPenugasan == null && !kelasIdParam.isNullOrBlank() && !mapelIdParam.isNullOrBlank()) {
                val session = authRepository.sessionFlow.first()
                val guruId = session.userId.orEmpty()
                val guruPenugasanList = academicRepository.getPenugasanByGuru(guruId, tahunAjaranId).first()
                resolvedPenugasan = guruPenugasanList.firstOrNull {
                    it.kelasId == kelasIdParam && it.mapelId == mapelIdParam
                }

                // Jika guru adalah admin atau penugasan tidak langsung cocok
                if (resolvedPenugasan == null) {
                    val allPenugasan = academicRepository.getPenugasanByTahunAjaran(tahunAjaranId).first()
                    resolvedPenugasan = allPenugasan.firstOrNull {
                        it.kelasId == kelasIdParam && it.mapelId == mapelIdParam
                    }
                }
            }

            val kelasId = resolvedPenugasan?.kelasId ?: kelasIdParam.orEmpty()
            val mapelId = resolvedPenugasan?.mapelId ?: mapelIdParam.orEmpty()
            val penugasanId = resolvedPenugasan?.id.orEmpty()

            val kelas = if (kelasId.isNotBlank()) academicRepository.getKelasById(kelasId) else null
            val mapel = if (mapelId.isNotBlank()) academicRepository.getMapelById(mapelId) else null

            // Cek apakah sudah ada sesi presensi tersimpan untuk hari ini
            val existingSessions = if (penugasanId.isNotBlank()) {
                attendanceRepository.getSessionByPenugasanAndDate(penugasanId, tanggalStr).first()
            } else {
                emptyList()
            }
            val latestSession = existingSessions.firstOrNull()

            // Ambil daftar siswa kelas
            val studentItems = if (kelasId.isNotBlank()) {
                attendanceRepository.getStudentsByKelas(kelasId, tahunAjaranId).first()
            } else {
                emptyList()
            }

            // Jika sesi sudah ada, muat detail status sebelumnya
            val studentUiList = if (latestSession != null) {
                val details = attendanceRepository.getDetailsBySesiIdList(latestSession.id)
                val detailMap = details.associateBy { it.siswaId }

                studentItems.map { student ->
                    val savedDetail = detailMap[student.siswaId]
                    val status = if (savedDetail != null) {
                        AttendanceStatus.fromCode(savedDetail.statusKehadiran)
                    } else {
                        AttendanceStatus.HADIR
                    }
                    StudentAttendanceUiModel(
                        siswaId = student.siswaId,
                        nomorAbsen = student.nomorAbsen,
                        nisLokal = student.nisLokal,
                        nisn = student.nisn,
                        namaLengkap = student.namaLengkap,
                        jenisKelamin = student.jenisKelamin,
                        status = status,
                        catatan = savedDetail?.catatan.orEmpty()
                    )
                }
            } else {
                // Sesi baru: Seluruh siswa default HADIR (H)
                studentItems.map { student ->
                    StudentAttendanceUiModel(
                        siswaId = student.siswaId,
                        nomorAbsen = student.nomorAbsen,
                        nisLokal = student.nisLokal,
                        nisn = student.nisn,
                        namaLengkap = student.namaLengkap,
                        jenisKelamin = student.jenisKelamin,
                        status = AttendanceStatus.HADIR,
                        catatan = ""
                    )
                }
            }

            _state.update {
                it.copy(
                    penugasanId = penugasanId,
                    kelasId = kelasId,
                    namaKelas = kelas?.namaRombel ?: "Kelas $kelasId",
                    mapelId = mapelId,
                    namaMapel = mapel?.namaMapel ?: "Mata Pelajaran",
                    tanggal = tanggalStr,
                    formattedTanggal = formattedTanggalStr,
                    jamKe = latestSession?.jamKe ?: "1-2",
                    materiPokok = latestSession?.materiPokok.orEmpty(),
                    students = studentUiList,
                    existingSesiId = latestSession?.id,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Memutar status kehadiran siswa sekuensial: H -> S -> I -> A -> H.
     */
    fun toggleStudentStatus(siswaId: String) {
        _state.update { current ->
            val updated = current.students.map { student ->
                if (student.siswaId == siswaId) {
                    student.copy(status = student.status.nextStatus())
                } else {
                    student
                }
            }
            current.copy(students = updated)
        }
    }

    /**
     * Mengatur status kehadiran untuk seluruh siswa sekaligus.
     */
    fun setAllStatus(status: AttendanceStatus) {
        _state.update { current ->
            val updated = current.students.map { it.copy(status = status) }
            current.copy(students = updated)
        }
    }

    fun updateStudentNote(siswaId: String, note: String) {
        _state.update { current ->
            val updated = current.students.map { student ->
                if (student.siswaId == siswaId) {
                    student.copy(catatan = note)
                } else {
                    student
                }
            }
            current.copy(students = updated)
        }
    }

    fun updateMateriPokok(materi: String) {
        _state.update { it.copy(materiPokok = materi) }
    }

    fun updateJamKe(jam: String) {
        _state.update { it.copy(jamKe = jam) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, saveSuccess = false) }
    }

    /**
     * Menyimpan data presensi dan materi KBM ke Room SQLite dan Sync Outbox.
     */
    fun saveAttendance(onSuccess: () -> Unit = {}) {
        val currentState = _state.value
        if (currentState.penugasanId.isBlank()) {
            _state.update { it.copy(errorMessage = "Penugasan mengajar tidak valid atau tidak ditemukan.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            val detailInputs = currentState.students.map {
                StudentAttendanceDetailInput(
                    siswaId = it.siswaId,
                    statusKehadiran = it.status.code,
                    catatan = it.catatan.ifBlank { null }
                )
            }

            val result = attendanceRepository.saveAttendance(
                sesiId = currentState.existingSesiId,
                penugasanId = currentState.penugasanId,
                tanggal = currentState.tanggal,
                jamKe = currentState.jamKe.ifBlank { "1-2" },
                materiPokok = currentState.materiPokok.ifBlank { null },
                details = detailInputs
            )

            when (result) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            existingSesiId = result.data
                        )
                    }
                    onSuccess()
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isSaving = false) }
                }
            }
        }
    }
}
