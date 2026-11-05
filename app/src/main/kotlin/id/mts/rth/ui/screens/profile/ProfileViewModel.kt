package id.mts.rth.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AuthRepository
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
import kotlinx.coroutines.launch

data class TeacherProfileUiState(
    val guru: GuruEntity? = null,
    val roleName: String = "",
    val isWaliKelas: Boolean = false,
    val namaKelasBinaan: String? = null,
    val mapelTaughtNames: List<String> = emptyList(),
    val isChangingPin: Boolean = false,
    val oldPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = true
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository
) : ViewModel() {

    private val _isChangingPin = MutableStateFlow(false)
    private val _oldPin = MutableStateFlow("")
    private val _newPin = MutableStateFlow("")
    private val _confirmPin = MutableStateFlow("")
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _isError = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TeacherProfileUiState> = authRepository.sessionFlow.flatMapLatest { session ->
        val userId = session.userId.orEmpty()
        academicRepository.getActiveTahunAjaran().flatMapLatest { activeTahun ->
            val tahunId = activeTahun?.id.orEmpty()
            combine(
                combine(
                    academicRepository.getAllKelas(),
                    academicRepository.getAllMapel(),
                    academicRepository.getPenugasanByGuru(userId, tahunId)
                ) { kelasList, mapelList, penugasanList ->
                    Triple(kelasList, mapelList, penugasanList)
                },
                combine(
                    _isChangingPin,
                    _oldPin,
                    _newPin,
                    _confirmPin
                ) { isChanging, oldP, newP, confP ->
                    listOf(isChanging, oldP, newP, confP)
                },
                combine(
                    _feedbackMessage,
                    _isError
                ) { feedback, err ->
                    Pair(feedback, err)
                }
            ) { (kelasList, mapelList, penugasanList), pinStateList, (feedback, err) ->
                val isChanging = pinStateList[0] as Boolean
                val oldP = pinStateList[1] as String
                val newP = pinStateList[2] as String
                val confP = pinStateList[3] as String

                val waliKelas = kelasList.firstOrNull { it.waliKelasGuruId == userId }
                val isWali = waliKelas != null || session.activeRole.equals("GURU_WALI", ignoreCase = true)

                val mapelMap = mapelList.associateBy { it.id }
                val mapelNames = penugasanList.mapNotNull { mapelMap[it.mapelId]?.namaMapel }.distinct()

                val dummyGuru = GuruEntity(
                    id = userId,
                    nip = if (session.activeRole == "ADMIN") "198001012005011001" else "198505122010011002",
                    npk = "NPK" + userId.takeLast(3),
                    nuptk = null,
                    nik = "3301010101850001",
                    namaLengkap = session.userName.ifBlank { "Pengguna SIMRTH" },
                    gelarDepan = null,
                    gelarBelakang = null,
                    jenisKelamin = "L",
                    noWa = "081234567890",
                    statusKepegawaian = "PNS / Yayasan",
                    role = session.activeRole,
                    username = session.userName.lowercase(),
                    password = "",
                    isDeleted = false,
                    updatedAt = System.currentTimeMillis()
                )

                TeacherProfileUiState(
                    guru = dummyGuru,
                    roleName = when (session.activeRole.uppercase()) {
                        "ADMIN" -> "Administrator Madrasah"
                        "GURU_WALI" -> "Guru & Wali Kelas"
                        else -> if (isWali) "Guru & Wali Kelas" else "Guru Mata Pelajaran"
                    },
                    isWaliKelas = isWali,
                    namaKelasBinaan = waliKelas?.namaRombel,
                    mapelTaughtNames = mapelNames,
                    isChangingPin = isChanging,
                    oldPin = oldP,
                    newPin = newP,
                    confirmPin = confP,
                    feedbackMessage = feedback,
                    isError = err,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TeacherProfileUiState()
    )

    fun startChangePin() {
        _oldPin.value = ""
        _newPin.value = ""
        _confirmPin.value = ""
        _feedbackMessage.value = null
        _isChangingPin.value = true
    }

    fun cancelChangePin() {
        _isChangingPin.value = false
        _feedbackMessage.value = null
    }

    fun updateOldPin(value: String) {
        if (value.length <= 6) _oldPin.value = value
    }

    fun updateNewPin(value: String) {
        if (value.length <= 6) _newPin.value = value
    }

    fun updateConfirmPin(value: String) {
        if (value.length <= 6) _confirmPin.value = value
    }

    fun submitPinChange() {
        viewModelScope.launch {
            if (_oldPin.value.length != 6) {
                _feedbackMessage.value = "PIN lama harus 6 digit angka."
                _isError.value = true
                return@launch
            }
            if (_newPin.value.length != 6) {
                _feedbackMessage.value = "PIN baru harus 6 digit angka."
                _isError.value = true
                return@launch
            }
            if (_newPin.value != _confirmPin.value) {
                _feedbackMessage.value = "Konfirmasi PIN baru tidak cocok."
                _isError.value = true
                return@launch
            }

            when (val res = authRepository.updatePin(_oldPin.value, _newPin.value)) {
                is ResultState.Success -> {
                    _feedbackMessage.value = "PIN offline berhasil diperbarui!"
                    _isError.value = false
                    _isChangingPin.value = false
                    _oldPin.value = ""
                    _newPin.value = ""
                    _confirmPin.value = ""
                }
                is ResultState.Error -> {
                    _feedbackMessage.value = res.message
                    _isError.value = true
                }
                else -> Unit
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun dismissFeedback() {
        _feedbackMessage.value = null
    }
}
