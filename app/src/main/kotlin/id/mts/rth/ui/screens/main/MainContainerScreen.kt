package id.mts.rth.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import id.mts.rth.ui.components.AppBottomNavBar
import id.mts.rth.ui.navigation.Screen
import id.mts.rth.ui.screens.admin.AccountManagementScreen
import id.mts.rth.ui.screens.admin.AdminDatabaseScreen
import id.mts.rth.ui.screens.admin.AdminViewModel
import id.mts.rth.ui.screens.admin.SchoolAndAdminProfileScreen
import id.mts.rth.ui.screens.admin.SchoolManagementScreen
import id.mts.rth.ui.screens.attendance.AttendanceScreen
import id.mts.rth.ui.screens.attendance.AttendanceViewModel
import id.mts.rth.ui.screens.auth.AuthViewModel
import id.mts.rth.ui.screens.grading.GradingOverviewScreen
import id.mts.rth.ui.screens.grading.GradingViewModel
import id.mts.rth.ui.screens.home.HomeScreen
import id.mts.rth.ui.screens.home.HomeViewModel
import id.mts.rth.ui.screens.profile.ProfileViewModel
import id.mts.rth.ui.screens.profile.TeacherProfileScreen
import id.mts.rth.ui.screens.students.StudentListScreen
import id.mts.rth.ui.screens.students.StudentViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainContainerScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = koinViewModel(),
    attendanceViewModel: AttendanceViewModel = koinViewModel(),
    gradingViewModel: GradingViewModel = koinViewModel(),
    studentViewModel: StudentViewModel = koinViewModel(),
    adminViewModel: AdminViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val session by authViewModel.sessionState.collectAsState()
    val profileState by profileViewModel.state.collectAsState()

    val userRole = session?.activeRole ?: "GURU"
    val isWaliKelas = profileState.isWaliKelas || userRole.equals("GURU_WALI", ignoreCase = true)

    var currentTabId by rememberSaveable { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                selectedTabId = currentTabId,
                onTabSelected = { currentTabId = it },
                userRole = userRole,
                isWaliKelas = isWaliKelas
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTabId) {
                // Tab Bersama: Dashboard / Home
                "home" -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToAttendance = { kelasId, mapelId ->
                            if (kelasId.isNotEmpty() && mapelId.isNotEmpty()) {
                                navController.navigate(Screen.Attendance.createRoute(null, kelasId, mapelId))
                            } else {
                                currentTabId = "attendance"
                            }
                        },
                        onNavigateToGrading = { kelasId, mapelId ->
                            if (kelasId.isNotEmpty() && mapelId.isNotEmpty()) {
                                navController.navigate(Screen.GradingOverview.createRoute(null, kelasId, mapelId))
                            } else {
                                currentTabId = "grading"
                            }
                        },
                        onNavigateToStudents = {
                            if (userRole.equals("ADMIN", ignoreCase = true)) {
                                currentTabId = "admin_database"
                            } else {
                                currentTabId = "students"
                            }
                        },
                        onNavigateToPinLock = {
                            navController.navigate(Screen.PinLock.route)
                        },
                        onNavigateToKkmConfig = {
                            navController.navigate(Screen.KkmConfig.route)
                        },
                        onNavigateToMutation = {
                            navController.navigate(Screen.LifecycleMutation.route)
                        }
                    )
                }

                // Tab Guru & Wali: Presensi
                "attendance" -> {
                    AttendanceScreen(
                        viewModel = attendanceViewModel,
                        onNavigateBack = { currentTabId = "home" }
                    )
                }

                // Tab Guru & Wali: Nilai K13
                "grading" -> {
                    GradingOverviewScreen(
                        viewModel = gradingViewModel,
                        onNavigateToEntry = { komponenId ->
                            navController.navigate(Screen.GradingEntry.createRoute(komponenId))
                        },
                        onNavigateBack = { currentTabId = "home" }
                    )
                }

                // Tab Khusus Wali Kelas: Data Siswa Rombel Binaan
                "students" -> {
                    StudentListScreen(
                        viewModel = studentViewModel,
                        onStudentClick = { siswaId, kelasId, nomorAbsen ->
                            navController.navigate(Screen.StudentEmisDetail.createRoute(siswaId, kelasId, nomorAbsen))
                        },
                        onNavigateBack = { currentTabId = "home" }
                    )
                }

                // Tab Guru & Wali: Data Diri
                "profile" -> {
                    TeacherProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToPinLock = {
                            navController.navigate(Screen.PinLock.route)
                        },
                        onLogoutSuccess = {
                            navController.navigate(Screen.InitialSetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // Tab Khusus Admin 1: Database (Siswa & Guru)
                "admin_database" -> {
                    AdminDatabaseScreen(
                        viewModel = adminViewModel
                    )
                }

                // Tab Khusus Admin 2: Manajemen Sekolah (Tahun Ajaran, Rombel, Penugasan)
                "admin_school" -> {
                    SchoolManagementScreen(
                        viewModel = adminViewModel,
                        onNavigateToKkmConfig = {
                            navController.navigate(Screen.KkmConfig.route)
                        },
                        onNavigateToMutation = {
                            navController.navigate(Screen.LifecycleMutation.route)
                        }
                    )
                }

                // Tab Khusus Admin 3: Manajemen Akun
                "admin_accounts" -> {
                    AccountManagementScreen(
                        viewModel = adminViewModel
                    )
                }

                // Tab Khusus Admin 4: Data Sekolah & Data Diri Admin
                "admin_profile" -> {
                    SchoolAndAdminProfileScreen(
                        viewModel = adminViewModel,
                        onNavigateToPinLock = {
                            navController.navigate(Screen.PinLock.route)
                        },
                        onLogoutSuccess = {
                            navController.navigate(Screen.InitialSetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // Fallback
                else -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToPinLock = { navController.navigate(Screen.PinLock.route) }
                    )
                }
            }
        }
    }
}
