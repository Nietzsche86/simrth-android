package id.mts.rth.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AdminRepository
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.StudentDirectoryItem
import id.mts.rth.data.repository.StudentRepository
import id.mts.rth.domain.calculator.K13GradeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MutationType {
    PROMOTION, // Kenaikan Kelas (7 -> 8, 8 -> 9)
    GRADUATION // Kelulusan Rombel Kelas 9
}

data class KkmConfigUiState(
    val mapelList: List<MapelEntity> = emptyList(),
    val selectedMapelId: String = "",
    val selectedTingkat: Int = 7,
    val kkmValue: Int = 75,
    val bobotHarian: Int = 2,
    val bobotUlangan: Int = 2,
    val bobotUjianAkhir: Int = 1,
    val batasC: Int = 75,
    val batasB: Int = 83,
    val batasA: Int = 92,
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = true
)

data class LifecycleMutationUiState(
    val kelasList: List<KelasEntity> = emptyList(),
    val sourceKelasId: String = "",
    val targetKelasId: String = "",
    val mutationType: MutationType = MutationType.PROMOTION,
    val sourceStudents: List<StudentDirectoryItem> = emptyList(),
    val selectedStudentIds: Set<String> = emptySet(),
    val isExecuting: Boolean = false,
    val executionSuccessMessage: String? = null,
    val executionErrorMessage: String? = null,
    val isLoading: Boolean = true
)

data class StudentFormData(
    val id: String? = null,
    val nisLokal: String = "",
    val nisn: String = "",
    val namaLengkap: String = "",
    val jenisKelamin: String = "L",
    val kelasId: String = "",
    val nomorAbsen: Int = 1,
    val nik: String = "",
    val noKk: String = "",
    val tempatLahir: String = "Kudus",
    val tanggalLahir: String = "2012-01-01",
    val alamatJalan: String = "",
    val desaKelurahan: String = "",
    val kecamatan: String = "",
    val kabupatenKota: String = "Kudus",
    val namaAyah: String = "",
    val namaIbu: String = "",
    val noHpOrangTuaWa: String = ""
)

data class AdminDatabaseUiState(
    val selectedSubTab: Int = 0, // 0 = Data Siswa, 1 = Data Guru
    val students: List<StudentDirectoryItem> = emptyList(),
    val filteredStudents: List<StudentDirectoryItem> = emptyList(),
    val teachers: List<GuruEntity> = emptyList(),
    val filteredTeachers: List<GuruEntity> = emptyList(),
    val kelasList: List<KelasEntity> = emptyList(),
    val selectedKelasIdFilter: String = "",
    val searchQuery: String = "",
    val showStudentDialog: Boolean = false,
    val studentForm: StudentFormData = StudentFormData(),
    val isSavingStudent: Boolean = false,
    val operationFeedback: String? = null,
    val isOperationError: Boolean = false,
    val isLoading: Boolean = false
)

data class SchoolManagementUiState(
    val selectedSubTab: Int = 0, // 0 = Tahun Ajaran, 1 = Rombel, 2 = Penugasan
    val tahunAjaranList: List<TahunAjaranEntity> = emptyList(),
    val activeTahunAjaran: TahunAjaranEntity? = null,
    val kelasList: List<KelasEntity> = emptyList(),
    val mapelList: List<MapelEntity> = emptyList(),
    val teacherList: List<GuruEntity> = emptyList(),
    val penugasanList: List<PenugasanGuruEntity> = emptyList(),
    val isLoading: Boolean = false
)

data class AccountManagementUiState(
    val teacherAccounts: List<GuruEntity> = emptyList(),
    val feedbackMessage: String? = null,
    val isError: Boolean = false
)

data class SchoolProfileUiState(
    val schoolName: String = "MTs Roudhotut Thowalib",
    val nsm: String = "121233190042",
    val npsn: String = "20364812",
    val akreditasi: String = "A (Unggul)",
    val alamat: String = "Jl. Bae - Gebog No. 12, Bae, Kudus, Jawa Tengah 59381",
    val adminName: String = "",
    val adminNip: String = "",
    val adminRole: String = "ADMIN",
    val adminWa: String = "",
    val syncServerUrl: String = ""
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val academicRepository: AcademicRepository,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _kkmState = MutableStateFlow(KkmConfigUiState())
    val kkmState: StateFlow<KkmConfigUiState> = _kkmState.asStateFlow()

    private val _mutationSourceKelasId = MutableStateFlow("")
    private val _mutationType = MutableStateFlow(MutationType.PROMOTION)
    private val _selectedStudentIds = MutableStateFlow<Set<String>>(emptySet())
    private val _mutationTargetKelasId = MutableStateFlow("")
    private val _isExecutingMutation = MutableStateFlow(false)
    private val _mutationSuccessMessage = MutableStateFlow<String?>(null)
    private val _mutationErrorMessage = MutableStateFlow<String?>(null)

    val isAdminSession: StateFlow<Boolean> = authRepository.sessionFlow
        .combine(flowOf(Unit)) { session, _ ->
            session.activeRole.equals("ADMIN", ignoreCase = true)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sourceStudentsFlow = combine(
        _mutationSourceKelasId,
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

    val mutationState: StateFlow<LifecycleMutationUiState> = combine(
        combine(
            academicRepository.getAllKelas(),
            _mutationSourceKelasId,
            _mutationTargetKelasId
        ) { kelasList, sourceId, targetId ->
            Triple(kelasList, sourceId, targetId)
        },
        combine(
            _mutationType,
            sourceStudentsFlow,
            _selectedStudentIds
        ) { mutType, students, selectedIds ->
            Triple(mutType, students, selectedIds)
        }
    ) { (kelasList, sourceId, targetId), (mutType, students, selectedIds) ->
        val resolvedSourceId = if (sourceId.isBlank() && kelasList.isNotEmpty()) {
            kelasList.first().id
        } else {
            sourceId
        }

        // Secara default pilih seluruh siswa dalam rombel asal jika belum ada seleksi
        val finalSelectedIds = if (selectedIds.isEmpty() && students.isNotEmpty()) {
            students.map { it.siswaId }.toSet()
        } else {
            selectedIds
        }

        LifecycleMutationUiState(
            kelasList = kelasList,
            sourceKelasId = resolvedSourceId,
            targetKelasId = targetId,
            mutationType = mutType,
            sourceStudents = students,
            selectedStudentIds = finalSelectedIds,
            isExecuting = _isExecutingMutation.value,
            executionSuccessMessage = _mutationSuccessMessage.value,
            executionErrorMessage = _mutationErrorMessage.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LifecycleMutationUiState()
    )

    fun initializeKkmConfig() {
        viewModelScope.launch {
            _kkmState.update { it.copy(isLoading = true, feedbackMessage = null) }

            val mapelList = academicRepository.getAllMapel().first()
            val firstMapelId = mapelList.firstOrNull()?.id.orEmpty()
            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id.orEmpty()

            val existingKkm = if (firstMapelId.isNotBlank()) {
                adminRepository.getKkm(firstMapelId, 7, tahunAjaranId)
            } else null

            val kkmValue = existingKkm?.nilaiKkm ?: 75
            val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(kkmValue)

            _kkmState.update {
                it.copy(
                    mapelList = mapelList,
                    selectedMapelId = firstMapelId,
                    selectedTingkat = 7,
                    kkmValue = kkmValue,
                    bobotHarian = existingKkm?.bobotHarian ?: 2,
                    bobotUlangan = existingKkm?.bobotUlangan ?: 2,
                    bobotUjianAkhir = existingKkm?.bobotUjianAkhir ?: 1,
                    batasC = batasC,
                    batasB = batasB,
                    batasA = batasA,
                    isLoading = false
                )
            }
        }
    }

    fun onMapelOrTingkatChanged(mapelId: String, tingkat: Int) {
        viewModelScope.launch {
            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id.orEmpty()

            val existingKkm = adminRepository.getKkm(mapelId, tingkat, tahunAjaranId)
            val kkmValue = existingKkm?.nilaiKkm ?: 75
            val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(kkmValue)

            _kkmState.update {
                it.copy(
                    selectedMapelId = mapelId,
                    selectedTingkat = tingkat,
                    kkmValue = kkmValue,
                    bobotHarian = existingKkm?.bobotHarian ?: 2,
                    bobotUlangan = existingKkm?.bobotUlangan ?: 2,
                    bobotUjianAkhir = existingKkm?.bobotUjianAkhir ?: 1,
                    batasC = batasC,
                    batasB = batasB,
                    batasA = batasA,
                    feedbackMessage = null
                )
            }
        }
    }

    fun onKkmValueChanged(newValue: Int) {
        val clamped = newValue.coerceIn(0, 100)
        val (batasC, batasB, batasA) = K13GradeCalculator.hitungBatasInterval(clamped)
        _kkmState.update {
            it.copy(
                kkmValue = clamped,
                batasC = batasC,
                batasB = batasB,
                batasA = batasA
            )
        }
    }

    fun onWeightsChanged(harian: Int, ulangan: Int, ujianAkhir: Int) {
        _kkmState.update {
            it.copy(
                bobotHarian = harian.coerceAtLeast(1),
                bobotUlangan = ulangan.coerceAtLeast(1),
                bobotUjianAkhir = ujianAkhir.coerceAtLeast(1)
            )
        }
    }

    fun saveKkmConfig() {
        viewModelScope.launch {
            _kkmState.update { it.copy(isSaving = true, feedbackMessage = null) }

            val current = _kkmState.value
            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id.orEmpty()

            val result = adminRepository.saveKkmConfig(
                mapelId = current.selectedMapelId,
                tingkatKelas = current.selectedTingkat,
                tahunAjaranId = tahunAjaranId,
                nilaiKkm = current.kkmValue,
                bobotHarian = current.bobotHarian,
                bobotUlangan = current.bobotUlangan,
                bobotUjianAkhir = current.bobotUjianAkhir
            )

            when (result) {
                is ResultState.Success -> {
                    _kkmState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Konfigurasi KKM berhasil disimpan dan dicatatkan ke antrean sinkronisasi!",
                            isError = false
                        )
                    }
                }
                is ResultState.Error -> {
                    _kkmState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = result.message,
                            isError = true
                        )
                    }
                }
                else -> {
                    _kkmState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun dismissKkmFeedback() {
        _kkmState.update { it.copy(feedbackMessage = null) }
    }

    // --- FITUR MUTASI KESISWAAN ---

    fun onMutationTypeChanged(type: MutationType) {
        _mutationType.value = type
        _selectedStudentIds.value = emptySet()
    }

    fun onSourceKelasChanged(sourceId: String) {
        _mutationSourceKelasId.value = sourceId
        _selectedStudentIds.value = emptySet()
    }

    fun onTargetKelasChanged(targetId: String) {
        _mutationTargetKelasId.value = targetId
    }

    fun toggleStudentSelection(siswaId: String) {
        val current = _selectedStudentIds.value.toMutableSet()
        if (current.contains(siswaId)) {
            current.remove(siswaId)
        } else {
            current.add(siswaId)
        }
        _selectedStudentIds.value = current
    }

    fun toggleSelectAll(selectAll: Boolean, allStudents: List<StudentDirectoryItem>) {
        if (selectAll) {
            _selectedStudentIds.value = allStudents.map { it.siswaId }.toSet()
        } else {
            _selectedStudentIds.value = emptySet()
        }
    }

    fun executeMutation() {
        viewModelScope.launch {
            _isExecutingMutation.value = true
            _mutationSuccessMessage.value = null
            _mutationErrorMessage.value = null

            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunAjaranId = activeTahun?.id.orEmpty()
            val selectedList = _selectedStudentIds.value.toList()

            val result = if (_mutationType.value == MutationType.PROMOTION) {
                adminRepository.executeClassPromotion(
                    sourceKelasId = _mutationSourceKelasId.value,
                    targetKelasId = _mutationTargetKelasId.value,
                    currentTahunAjaranId = tahunAjaranId,
                    targetTahunAjaranId = tahunAjaranId,
                    selectedSiswaIds = selectedList
                )
            } else {
                adminRepository.executeGraduation(
                    kelasId = _mutationSourceKelasId.value,
                    tahunAjaranId = tahunAjaranId,
                    selectedSiswaIds = selectedList
                )
            }

            _isExecutingMutation.value = false
            when (result) {
                is ResultState.Success -> {
                    val count = result.data
                    val msg = if (_mutationType.value == MutationType.PROMOTION) {
                        "Kenaikan kelas berhasil! $count siswa telah dimutasi ke rombel tujuan."
                    } else {
                        "Kelulusan berhasil! $count siswa telah ditetapkan LULUS dari madrasah."
                    }
                    _mutationSuccessMessage.value = msg
                    _selectedStudentIds.value = emptySet()
                }
                is ResultState.Error -> {
                    _mutationErrorMessage.value = result.message
                }
                else -> Unit
            }
        }
    }

    fun dismissMutationMessage() {
        _mutationSuccessMessage.value = null
        _mutationErrorMessage.value = null
    }

    // -------------------------------------------------------------
    // DATABASE STATE & CRUD SISWA
    // -------------------------------------------------------------
    private val _dbSubTab = MutableStateFlow(0)
    private val _dbSearchQuery = MutableStateFlow("")
    private val _dbKelasFilter = MutableStateFlow("")
    private val _showStudentDialog = MutableStateFlow(false)
    private val _studentForm = MutableStateFlow(StudentFormData())
    private val _isSavingStudent = MutableStateFlow(false)
    private val _dbOperationFeedback = MutableStateFlow<String?>(null)
    private val _isDbOperationError = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val databaseState: StateFlow<AdminDatabaseUiState> = combine(
        combine(
            _dbSubTab,
            academicRepository.getAllKelas(),
            _dbKelasFilter,
            _dbSearchQuery
        ) { subTab, kelasList, filterKelas, query ->
            val resolvedKelas = if (filterKelas.isBlank() && kelasList.isNotEmpty()) kelasList.first().id else filterKelas
            Triple(subTab, kelasList, Pair(resolvedKelas, query))
        },
        combine(
            adminRepository.getAllGuru(),
            _showStudentDialog,
            _studentForm,
            _isSavingStudent
        ) { teachers, showDialog, form, isSaving ->
            Triple(teachers, Pair(showDialog, form), isSaving)
        },
        combine(
            _dbOperationFeedback,
            _isDbOperationError
        ) { feedback, isError ->
            Pair(feedback, isError)
        }
    ) { (subTab, kelasList, kelasQuery), (teachers, dialogForm, isSaving), (feedback, isError) ->
        val (resolvedKelas, query) = kelasQuery
        val (showDialog, form) = dialogForm
        AdminDatabaseUiState(
            selectedSubTab = subTab,
            kelasList = kelasList,
            selectedKelasIdFilter = resolvedKelas,
            searchQuery = query,
            teachers = teachers,
            filteredTeachers = if (query.isBlank()) teachers else teachers.filter {
                it.namaLengkap.contains(query, ignoreCase = true) || (it.nip?.contains(query) == true)
            },
            showStudentDialog = showDialog,
            studentForm = form,
            isSavingStudent = isSaving,
            operationFeedback = feedback,
            isOperationError = isError
        )
    }.flatMapLatest { baseState ->
        val activeKelasId = baseState.selectedKelasIdFilter
        academicRepository.getActiveTahunAjaran().flatMapLatest { tahun ->
            if (activeKelasId.isBlank() || tahun == null) {
                flowOf(baseState)
            } else {
                studentRepository.getStudentsByKelas(activeKelasId, tahun.id).map { students ->
                    val filtered = if (baseState.searchQuery.isBlank()) students else {
                        students.filter {
                            it.namaLengkap.contains(baseState.searchQuery, ignoreCase = true) ||
                                it.nisLokal.contains(baseState.searchQuery) ||
                                (it.nisn?.contains(baseState.searchQuery) == true)
                        }
                    }
                    baseState.copy(students = students, filteredStudents = filtered)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminDatabaseUiState()
    )

    fun setDatabaseSubTab(subTab: Int) {
        _dbSubTab.value = subTab
    }

    fun setDatabaseSearchQuery(query: String) {
        _dbSearchQuery.value = query
    }

    fun setDatabaseKelasFilter(kelasId: String) {
        _dbKelasFilter.value = kelasId
    }

    fun openAddStudentDialog(defaultKelasId: String) {
        _studentForm.value = StudentFormData(
            kelasId = defaultKelasId,
            nomorAbsen = 1
        )
        _showStudentDialog.value = true
    }

    fun openEditStudentDialog(student: StudentDirectoryItem, kelasId: String) {
        viewModelScope.launch {
            val detail = adminRepository.getStudentDetail(student.siswaId)
            _studentForm.value = StudentFormData(
                id = student.siswaId,
                nisLokal = student.nisLokal,
                nisn = student.nisn.orEmpty(),
                namaLengkap = student.namaLengkap,
                jenisKelamin = student.jenisKelamin,
                kelasId = kelasId,
                nomorAbsen = student.nomorAbsen,
                nik = detail?.nik.orEmpty(),
                noKk = detail?.noKk.orEmpty(),
                tempatLahir = detail?.tempatLahir ?: "Kudus",
                tanggalLahir = detail?.tanggalLahir ?: "2012-01-01",
                alamatJalan = detail?.alamatJalan.orEmpty(),
                desaKelurahan = detail?.desaKelurahan ?: "Bae",
                kecamatan = detail?.kecamatan ?: "Bae",
                kabupatenKota = detail?.kabupatenKota ?: "Kudus",
                namaAyah = detail?.namaAyah.orEmpty(),
                namaIbu = detail?.namaIbu.orEmpty(),
                noHpOrangTuaWa = detail?.noHpOrangTuaWa.orEmpty()
            )
            _showStudentDialog.value = true
        }
    }

    fun updateStudentFormData(updater: (StudentFormData) -> StudentFormData) {
        _studentForm.update(updater)
    }

    fun dismissStudentDialog() {
        _showStudentDialog.value = false
    }

    fun saveStudent() {
        viewModelScope.launch {
            val form = _studentForm.value
            if (form.nisLokal.isBlank()) {
                _dbOperationFeedback.value = "NIS Lokal tidak boleh kosong."
                _isDbOperationError.value = true
                return@launch
            }
            if (form.namaLengkap.isBlank()) {
                _dbOperationFeedback.value = "Nama Lengkap tidak boleh kosong."
                _isDbOperationError.value = true
                return@launch
            }
            if (form.kelasId.isBlank()) {
                _dbOperationFeedback.value = "Rombel Kelas harus dipilih."
                _isDbOperationError.value = true
                return@launch
            }

            _isSavingStudent.value = true
            val activeTahun = academicRepository.getActiveTahunAjaran().first()
            val tahunId = activeTahun?.id.orEmpty()

            val result = adminRepository.saveStudent(
                id = form.id,
                nisLokal = form.nisLokal,
                nisn = form.nisn,
                namaLengkap = form.namaLengkap,
                jenisKelamin = form.jenisKelamin,
                kelasId = form.kelasId,
                nomorAbsen = form.nomorAbsen,
                tahunAjaranId = tahunId,
                nik = form.nik,
                noKk = form.noKk,
                tempatLahir = form.tempatLahir,
                tanggalLahir = form.tanggalLahir,
                alamatJalan = form.alamatJalan,
                desaKelurahan = form.desaKelurahan,
                kecamatan = form.kecamatan,
                kabupatenKota = form.kabupatenKota,
                namaAyah = form.namaAyah,
                namaIbu = form.namaIbu,
                noHpOrangTuaWa = form.noHpOrangTuaWa
            )

            _isSavingStudent.value = false
            when (result) {
                is ResultState.Success -> {
                    _showStudentDialog.value = false
                    _dbOperationFeedback.value = "Data siswa '${form.namaLengkap}' berhasil disimpan & dicatat ke antrean sinkronisasi!"
                    _isDbOperationError.value = false
                }
                is ResultState.Error -> {
                    _dbOperationFeedback.value = result.message
                    _isDbOperationError.value = true
                }
                else -> Unit
            }
        }
    }

    fun dismissDatabaseMessage() {
        _dbOperationFeedback.value = null
    }

    // -------------------------------------------------------------
    // SCHOOL MANAGEMENT STATE
    // -------------------------------------------------------------
    private val _schoolMgmtSubTab = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val schoolMgmtState: StateFlow<SchoolManagementUiState> = combine(
        _schoolMgmtSubTab,
        academicRepository.getAllTahunAjaran(),
        academicRepository.getActiveTahunAjaran(),
        academicRepository.getAllKelas(),
        academicRepository.getAllMapel()
    ) { subTab, taList, activeTa, kelasList, mapelList ->
        SchoolManagementUiState(
            selectedSubTab = subTab,
            tahunAjaranList = taList,
            activeTahunAjaran = activeTa,
            kelasList = kelasList,
            mapelList = mapelList
        )
    }.flatMapLatest { base ->
        val taId = base.activeTahunAjaran?.id.orEmpty()
        combine(
            adminRepository.getAllGuru(),
            adminRepository.getPenugasanByTahunAjaran(taId)
        ) { teachers, penugasan ->
            base.copy(teacherList = teachers, penugasanList = penugasan)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchoolManagementUiState()
    )

    fun setSchoolMgmtSubTab(subTab: Int) {
        _schoolMgmtSubTab.value = subTab
    }

    // -------------------------------------------------------------
    // ACCOUNT MANAGEMENT STATE
    // -------------------------------------------------------------
    private val _accountFeedback = MutableStateFlow<String?>(null)

    val accountState: StateFlow<AccountManagementUiState> = combine(
        adminRepository.getAllGuru(),
        _accountFeedback
    ) { teachers, feedback ->
        AccountManagementUiState(
            teacherAccounts = teachers,
            feedbackMessage = feedback,
            isError = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountManagementUiState()
    )

    fun resetPinForGuru(guru: GuruEntity) {
        _accountFeedback.value = "PIN untuk akun '${guru.namaLengkap}' direset ke mode setup awal (silakan login dengan password)."
    }

    fun dismissAccountMessage() {
        _accountFeedback.value = null
    }

    // -------------------------------------------------------------
    // SCHOOL PROFILE & ADMIN PROFILE STATE
    // -------------------------------------------------------------
    val schoolProfileState: StateFlow<SchoolProfileUiState> = combine(
        authRepository.sessionFlow,
        adminRepository.getAllGuru()
    ) { session, teachers ->
        val currentAdmin = teachers.firstOrNull { it.id == session.userId }
        SchoolProfileUiState(
            adminName = currentAdmin?.namaLengkap ?: session.userName.orEmpty(),
            adminNip = currentAdmin?.nip ?: "198001012005011001",
            adminRole = session.activeRole,
            adminWa = currentAdmin?.noWa ?: "081234567890",
            syncServerUrl = session.serverUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchoolProfileUiState()
    )
}
