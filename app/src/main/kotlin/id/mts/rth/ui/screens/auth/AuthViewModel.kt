package id.mts.rth.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.core.security.UserSession
import id.mts.rth.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InitialSetupFormState(
    val serverUrl: String = SessionPreferences.DEFAULT_SERVER_URL,
    val nipOrUsername: String = "",
    val password: String = "",
    val pin: String = "",
    val confirmPin: String = "",
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val sessionState: StateFlow<UserSession?> = authRepository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // State untuk form Initial Setup
    private val _setupForm = MutableStateFlow(InitialSetupFormState())
    val setupForm: StateFlow<InitialSetupFormState> = _setupForm.asStateFlow()

    private val _setupResult = MutableStateFlow<ResultState<Unit>>(ResultState.Idle)
    val setupResult: StateFlow<ResultState<Unit>> = _setupResult.asStateFlow()

    // State untuk layar PinLock
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    fun updateServerUrl(url: String) {
        _setupForm.value = _setupForm.value.copy(serverUrl = url, errorMessage = null)
    }

    fun updateNipOrUsername(nip: String) {
        _setupForm.value = _setupForm.value.copy(nipOrUsername = nip, errorMessage = null)
    }

    fun updatePassword(pass: String) {
        _setupForm.value = _setupForm.value.copy(password = pass, errorMessage = null)
    }

    fun updatePin(pin: String) {
        _setupForm.value = _setupForm.value.copy(pin = pin, errorMessage = null)
    }

    fun updateConfirmPin(confirmPin: String) {
        _setupForm.value = _setupForm.value.copy(confirmPin = confirmPin, errorMessage = null)
    }

    fun performInitialSetup() {
        val form = _setupForm.value

        if (form.serverUrl.isBlank()) {
            _setupForm.value = form.copy(errorMessage = "IP Server tidak boleh kosong.")
            return
        }
        if (form.nipOrUsername.isBlank()) {
            _setupForm.value = form.copy(errorMessage = "NIP atau Username wajib diisi.")
            return
        }
        if (form.password.isBlank()) {
            _setupForm.value = form.copy(errorMessage = "Password wajib diisi.")
            return
        }
        if (form.pin.length != 6 || !form.pin.all { it.isDigit() }) {
            _setupForm.value = form.copy(errorMessage = "PIN harus berupa 6 digit angka.")
            return
        }
        if (form.pin != form.confirmPin) {
            _setupForm.value = form.copy(errorMessage = "Konfirmasi PIN tidak cocok.")
            return
        }

        viewModelScope.launch {
            _setupResult.value = ResultState.Loading
            val result = authRepository.loginAndInitialSeed(
                serverUrl = form.serverUrl.trim(),
                nipOrUsername = form.nipOrUsername.trim(),
                password = form.password,
                pin6Digit = form.pin
            )
            _setupResult.value = result
        }
    }

    fun onPinChanged(newPin: String) {
        _pinInput.value = newPin
        _pinError.value = null
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val isValid = authRepository.verifyOfflinePin(pin)
            if (isValid) {
                _pinInput.value = ""
                _pinError.value = null
                onSuccess()
            } else {
                _pinInput.value = ""
                val currentSession = sessionState.value
                val remaining = SessionPreferences.MAX_FAILED_PIN_ATTEMPTS - (currentSession?.failedPinCount ?: 0)
                _pinError.value = if (remaining > 0) {
                    "PIN salah! Sisa percobaan: $remaining kali."
                } else {
                    "PIN salah 5 kali! Sesi terkunci. Silakan hubungkan ke Wi-Fi madrasah untuk login ulang."
                }
            }
        }
    }
}
