package id.mts.rth.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.MapelEntity
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
        academicRepository.getAllKelas(),
        _mutationSourceKelasId,
        _mutationTargetKelasId,
        _mutationType,
        sourceStudentsFlow,
        _selectedStudentIds
    ) { kelasList, sourceId, targetId, mutType, students, selectedIds ->
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
}
