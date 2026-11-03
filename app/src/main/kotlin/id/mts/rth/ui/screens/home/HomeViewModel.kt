package id.mts.rth.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.mts.rth.core.common.ResultState
import id.mts.rth.data.local.dao.SyncOutboxDao
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.SyncEngineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class JadwalSlot(
    val hari: Int = 1, // 1 = Senin, 2 = Selasa, 3 = Rabu, 4 = Kamis, 5 = Jumat, 6 = Sabtu, 7 = Ahad
    val jamKe: String = "",
    val mulai: String = ""
)

data class TodayScheduleUiModel(
    val penugasanId: String,
    val kelasId: String,
    val namaKelas: String,
    val mapelId: String,
    val namaMapel: String,
    val hari: Int,
    val namaHari: String,
    val jamKe: String,
    val jamMulai: String
)

data class HomeScreenState(
    val userName: String = "",
    val userRole: String = "",
    val serverUrl: String = "",
    val activeTahunAjaran: String = "Tahun Ajaran Aktif",
    val activeSemester: String = "Semester",
    val activeTahunAjaranId: String? = null,
    val outboxCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val syncMessage: String? = null,
    val isSyncError: Boolean = false,
    val todayScheduleList: List<TodayScheduleUiModel> = emptyList(),
    val allTeachingList: List<TodayScheduleUiModel> = emptyList(),
    val todayDayOfWeek: Int = 1,
    val todayDateFormatted: String = "",
    val isLoading: Boolean = true
)

private data class SyncStatus(
    val isSyncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

private data class FourTuple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository,
    private val syncEngineRepository: SyncEngineRepository,
    private val syncOutboxDao: SyncOutboxDao
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _syncStatus = MutableStateFlow(SyncStatus())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val penugasanFlow = combine(
        authRepository.sessionFlow,
        academicRepository.getActiveTahunAjaran()
    ) { session, tahun ->
        Pair(session, tahun)
    }.flatMapLatest { (session, tahun) ->
        if (tahun == null) {
            flowOf(emptyList())
        } else if (session.activeRole.equals("ADMIN", ignoreCase = true)) {
            academicRepository.getPenugasanByTahunAjaran(tahun.id)
        } else if (!session.userId.isNullOrBlank()) {
            academicRepository.getPenugasanByGuru(session.userId, tahun.id)
        } else {
            flowOf(emptyList())
        }
    }

    val state: StateFlow<HomeScreenState> = combine(
        combine(
            authRepository.sessionFlow,
            academicRepository.getActiveTahunAjaran(),
            syncOutboxDao.getOutboxCount(),
            _syncStatus
        ) { session, tahun, outboxCount, syncStatus ->
            FourTuple(session, tahun, outboxCount, syncStatus)
        },
        combine(
            academicRepository.getAllKelas(),
            academicRepository.getAllMapel(),
            penugasanFlow
        ) { kelasList, mapelList, penugasanList ->
            Triple(kelasList, mapelList, penugasanList)
        }
    ) { base, details ->
        val (session, tahun, outboxCount, syncStatus) = base
        val (kelasList, mapelList, penugasanList) = details

        val todayDate = LocalDate.now()
        val currentDayOfWeek = todayDate.dayOfWeek.value
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        val formattedDate = todayDate.format(dateFormatter)

        val kelasMap = kelasList.associateBy { it.id }
        val mapelMap = mapelList.associateBy { it.id }

        val todayList = mutableListOf<TodayScheduleUiModel>()
        val allList = mutableListOf<TodayScheduleUiModel>()

        for (p in penugasanList) {
            val kelas = kelasMap[p.kelasId]
            val mapel = mapelMap[p.mapelId]
            val namaKelas = kelas?.namaRombel ?: "Kelas ?"
            val namaMapel = mapel?.namaMapel ?: "Mata Pelajaran ?"

            val slots = parseJadwalSlots(p.jadwalJson)
            if (slots.isEmpty()) {
                val item = TodayScheduleUiModel(
                    penugasanId = p.id,
                    kelasId = p.kelasId,
                    namaKelas = namaKelas,
                    mapelId = p.mapelId,
                    namaMapel = namaMapel,
                    hari = 1,
                    namaHari = "Senin",
                    jamKe = "-",
                    jamMulai = "-"
                )
                allList.add(item)
            } else {
                for (slot in slots) {
                    val item = TodayScheduleUiModel(
                        penugasanId = p.id,
                        kelasId = p.kelasId,
                        namaKelas = namaKelas,
                        mapelId = p.mapelId,
                        namaMapel = namaMapel,
                        hari = slot.hari,
                        namaHari = getNamaHari(slot.hari),
                        jamKe = slot.jamKe,
                        jamMulai = slot.mulai
                    )
                    allList.add(item)
                    if (slot.hari == currentDayOfWeek) {
                        todayList.add(item)
                    }
                }
            }
        }

        HomeScreenState(
            userName = session.userName ?: "Guru SIMRTH",
            userRole = session.activeRole,
            serverUrl = session.serverUrl,
            activeTahunAjaran = tahun?.tahun ?: "Tahun Ajaran",
            activeSemester = tahun?.semester ?: "Semester",
            activeTahunAjaranId = tahun?.id,
            outboxCount = outboxCount,
            isSyncing = syncStatus.isSyncing,
            lastSyncTime = session.lastSyncTimestamp,
            syncMessage = syncStatus.message,
            isSyncError = syncStatus.isError,
            todayScheduleList = todayList.sortedBy { it.jamKe },
            allTeachingList = allList.sortedWith(compareBy({ it.hari }, { it.jamKe })),
            todayDayOfWeek = currentDayOfWeek,
            todayDateFormatted = formattedDate,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeScreenState()
    )

    private fun parseJadwalSlots(jsonString: String): List<JadwalSlot> {
        return try {
            if (jsonString.isBlank()) emptyList()
            else json.decodeFromString<List<JadwalSlot>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getNamaHari(hari: Int): String = when (hari) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        7 -> "Ahad"
        else -> "Hari $hari"
    }

    fun dismissSyncMessage() {
        _syncStatus.update { it.copy(message = null) }
    }

    fun triggerManualSync() {
        if (_syncStatus.value.isSyncing) return

        viewModelScope.launch {
            _syncStatus.update { it.copy(isSyncing = true, message = null, isError = false) }
            when (val result = syncEngineRepository.performManualSync()) {
                is ResultState.Success -> {
                    val report = result.data
                    _syncStatus.update {
                        it.copy(
                            isSyncing = false,
                            message = "Sinkronisasi berhasil! ${report.pushedMutationsCount} mutasi dikirim, ${report.pulledEntitiesCount} diperbarui.",
                            isError = false
                        )
                    }
                }
                is ResultState.Error -> {
                    _syncStatus.update {
                        it.copy(
                            isSyncing = false,
                            message = result.message,
                            isError = true
                        )
                    }
                }
                else -> {
                    _syncStatus.update { it.copy(isSyncing = false) }
                }
            }
        }
    }
}
