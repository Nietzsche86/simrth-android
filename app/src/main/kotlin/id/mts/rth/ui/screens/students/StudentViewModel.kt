package id.mts.rth.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.StudentDirectoryItem
import id.mts.rth.data.repository.StudentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class StudentListUiState(
    val kelasList: List<KelasEntity> = emptyList(),
    val selectedKelasId: String = "",
    val selectedKelasName: String = "",
    val isSupervisedClass: Boolean = false,
    val currentUserId: String = "",
    val currentUserRole: String = "",
    val searchQuery: String = "",
    val students: List<StudentDirectoryItem> = emptyList(),
    val filteredStudents: List<StudentDirectoryItem> = emptyList(),
    val isLoading: Boolean = true
)

data class StudentDetailUiState(
    val studentCore: SiswaCoreEntity? = null,
    val studentDetail: SiswaDetailEntity? = null,
    val nomorAbsen: Int = 0,
    val namaKelas: String = "",
    val fotoFile: File? = null,
    val isAccessDenied: Boolean = false,
    val accessDeniedMessage: String = "",
    val isLoading: Boolean = false
)

class StudentViewModel(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedKelasId = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")

    private val _detailState = MutableStateFlow(StudentDetailUiState())
    val detailState: StateFlow<StudentDetailUiState> = _detailState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val studentsForSelectedKelas = combine(
        _selectedKelasId,
        academicRepository.getActiveTahunAjaran()
    ) { kelasId, tahun ->
        Pair(kelasId, tahun)
    }.flatMapLatest { (kelasId, tahun) ->
        if (kelasId.isBlank() || tahun == null) {
            flowOf(emptyList())
        } else {
            studentRepository.getStudentsByKelas(kelasId, tahun.id)
        }
    }

    val listState: StateFlow<StudentListUiState> = combine(
        authRepository.sessionFlow,
        academicRepository.getAllKelas(),
        _selectedKelasId,
        _searchQuery,
        studentsForSelectedKelas
    ) { session, kelasList, selectedId, query, students ->
        val currentSelectedId = if (selectedId.isBlank() && kelasList.isNotEmpty()) {
            kelasList.first().id
        } else {
            selectedId
        }

        val currentKelas = kelasList.firstOrNull { it.id == currentSelectedId }
        val isSupervised = !session.userId.isNullOrBlank() && currentKelas?.waliKelasGuruId == session.userId

        val filtered = if (query.isBlank()) {
            students
        } else {
            val q = query.trim().lowercase()
            students.filter {
                it.namaLengkap.lowercase().contains(q) ||
                    it.nisLokal.lowercase().contains(q) ||
                    (it.nisn?.lowercase()?.contains(q) == true)
            }
        }

        StudentListUiState(
            kelasList = kelasList,
            selectedKelasId = currentSelectedId,
            selectedKelasName = currentKelas?.namaRombel ?: "",
            isSupervisedClass = isSupervised,
            currentUserId = session.userId.orEmpty(),
            currentUserRole = session.activeRole,
            searchQuery = query,
            students = students,
            filteredStudents = filtered,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudentListUiState()
    )

    fun selectKelas(kelasId: String) {
        _selectedKelasId.value = kelasId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Memuat data detail EMIS siswa dengan penjagaan hak akses RBAC.
     */
    fun loadStudentDetail(siswaId: String, kelasId: String, nomorAbsen: Int) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, isAccessDenied = false, accessDeniedMessage = "") }

            val core = studentRepository.getStudentCoreById(siswaId)
            val kelas = academicRepository.getKelasById(kelasId)
            val photoFile = studentRepository.getPhotoFile(core?.fotoFileName)

            val rbacResult = studentRepository.getStudentDetailWithRbac(siswaId, kelasId)

            when (rbacResult) {
                is ResultState.Success -> {
                    _detailState.update {
                        it.copy(
                            studentCore = core,
                            studentDetail = rbacResult.data,
                            nomorAbsen = nomorAbsen,
                            namaKelas = kelas?.namaRombel ?: "Kelas $kelasId",
                            fotoFile = photoFile,
                            isAccessDenied = false,
                            accessDeniedMessage = "",
                            isLoading = false
                        )
                    }
                }
                is ResultState.Error -> {
                    _detailState.update {
                        it.copy(
                            studentCore = core,
                            studentDetail = null,
                            nomorAbsen = nomorAbsen,
                            namaKelas = kelas?.namaRombel ?: "Kelas $kelasId",
                            fotoFile = photoFile,
                            isAccessDenied = true,
                            accessDeniedMessage = rbacResult.message,
                            isLoading = false
                        )
                    }
                }
                else -> {
                    _detailState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
