package id.mts.rth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.mts.rth.ui.screens.admin.AdminViewModel
import id.mts.rth.ui.screens.admin.KkmConfigScreen
import id.mts.rth.ui.screens.admin.LifecycleMutationScreen
import id.mts.rth.ui.screens.attendance.AttendanceScreen
import id.mts.rth.ui.screens.attendance.AttendanceViewModel
import id.mts.rth.ui.screens.auth.AuthViewModel
import id.mts.rth.ui.screens.auth.InitialSetupScreen
import id.mts.rth.ui.screens.auth.PinLockScreen
import id.mts.rth.ui.screens.grading.GradingEntryScreen
import id.mts.rth.ui.screens.grading.GradingOverviewScreen
import id.mts.rth.ui.screens.grading.GradingViewModel
import id.mts.rth.ui.screens.home.HomeScreen
import id.mts.rth.ui.screens.home.HomeViewModel
import id.mts.rth.ui.screens.main.MainContainerScreen
import id.mts.rth.ui.screens.students.StudentEmisDetailScreen
import id.mts.rth.ui.screens.students.StudentListScreen
import id.mts.rth.ui.screens.students.StudentViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val session by authViewModel.sessionState.collectAsState()

    // Tentukan rute awal: Jika belum pernah setup/login -> InitialSetup, jika sudah -> PinLock
    val startDestination = if (session?.authToken.isNullOrBlank() || session?.pinHash.isNullOrBlank()) {
        Screen.InitialSetup.route
    } else {
        Screen.PinLock.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Setup Awal
        composable(Screen.InitialSetup.route) {
            InitialSetupScreen(
                viewModel = authViewModel,
                onSetupSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.InitialSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Layar Kunci PIN Offline
        composable(Screen.PinLock.route) {
            PinLockScreen(
                viewModel = authViewModel,
                onUnlockSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PinLock.route) { inclusive = true }
                    }
                },
                onForceRelogin = {
                    navController.navigate(Screen.InitialSetup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 3. Kontainer Utama Aplikasi (Berbasis Bottom Navigation Adaptif)
        composable(Screen.Home.route) {
            MainContainerScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // 4. Presensi Cepat
        composable(
            route = Screen.Attendance.route,
            arguments = listOf(
                navArgument("penugasanId") { type = NavType.StringType; defaultValue = "" },
                navArgument("kelasId") { type = NavType.StringType; defaultValue = "" },
                navArgument("mapelId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val penugasanId = backStackEntry.arguments?.getString("penugasanId")
            val kelasId = backStackEntry.arguments?.getString("kelasId")
            val mapelId = backStackEntry.arguments?.getString("mapelId")

            val attendanceViewModel: AttendanceViewModel = koinViewModel()
            LaunchedEffect(penugasanId, kelasId, mapelId) {
                attendanceViewModel.initialize(penugasanId, kelasId, mapelId)
            }

            AttendanceScreen(
                viewModel = attendanceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Penilaian K13 Overview
        composable(
            route = Screen.GradingOverview.route,
            arguments = listOf(
                navArgument("penugasanId") { type = NavType.StringType; defaultValue = "" },
                navArgument("kelasId") { type = NavType.StringType; defaultValue = "" },
                navArgument("mapelId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val penugasanId = backStackEntry.arguments?.getString("penugasanId")
            val kelasId = backStackEntry.arguments?.getString("kelasId")
            val mapelId = backStackEntry.arguments?.getString("mapelId")

            val gradingViewModel: GradingViewModel = koinViewModel()
            LaunchedEffect(penugasanId, kelasId, mapelId) {
                gradingViewModel.initialize(penugasanId, kelasId, mapelId)
            }

            GradingOverviewScreen(
                viewModel = gradingViewModel,
                onNavigateToEntry = { komponenId ->
                    navController.navigate(Screen.GradingEntry.createRoute(komponenId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Input Nilai Vertikal
        composable(
            route = Screen.GradingEntry.route,
            arguments = listOf(
                navArgument("komponenId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val komponenId = backStackEntry.arguments?.getString("komponenId").orEmpty()
            val gradingViewModel: GradingViewModel = koinViewModel()

            GradingEntryScreen(
                viewModel = gradingViewModel,
                komponenId = komponenId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. Direktori Siswa
        composable(Screen.StudentList.route) {
            val studentViewModel: StudentViewModel = koinViewModel()
            StudentListScreen(
                viewModel = studentViewModel,
                onStudentClick = { siswaId, kelasId, nomorAbsen ->
                    navController.navigate(Screen.StudentEmisDetail.createRoute(siswaId, kelasId, nomorAbsen))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 8. Detail EMIS Siswa
        composable(
            route = Screen.StudentEmisDetail.route,
            arguments = listOf(
                navArgument("siswaId") { type = NavType.StringType },
                navArgument("kelasId") { type = NavType.StringType },
                navArgument("nomorAbsen") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val siswaId = backStackEntry.arguments?.getString("siswaId").orEmpty()
            val kelasId = backStackEntry.arguments?.getString("kelasId").orEmpty()
            val nomorAbsen = backStackEntry.arguments?.getInt("nomorAbsen") ?: 0

            val studentViewModel: StudentViewModel = koinViewModel()
            StudentEmisDetailScreen(
                viewModel = studentViewModel,
                siswaId = siswaId,
                kelasId = kelasId,
                nomorAbsen = nomorAbsen,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 9. Pengaturan KKM Khusus Admin
        composable(Screen.KkmConfig.route) {
            val adminViewModel: AdminViewModel = koinViewModel()
            KkmConfigScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 10. Mutasi Siklus Kesiswaan Khusus Admin
        composable(Screen.LifecycleMutation.route) {
            val adminViewModel: AdminViewModel = koinViewModel()
            LifecycleMutationScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
