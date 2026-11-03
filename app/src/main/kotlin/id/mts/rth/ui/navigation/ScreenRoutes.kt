package id.mts.rth.ui.navigation

sealed class Screen(val route: String) {
    object InitialSetup : Screen("initial_setup")
    object PinLock : Screen("pin_lock")
    object Home : Screen("home")

    object Attendance : Screen("attendance?penugasanId={penugasanId}&kelasId={kelasId}&mapelId={mapelId}") {
        fun createRoute(
            penugasanId: String? = null,
            kelasId: String? = null,
            mapelId: String? = null
        ): String {
            val p = penugasanId.orEmpty()
            val k = kelasId.orEmpty()
            val m = mapelId.orEmpty()
            return "attendance?penugasanId=$p&kelasId=$k&mapelId=$m"
        }
    }

    object GradingOverview : Screen("grading_overview?penugasanId={penugasanId}&kelasId={kelasId}&mapelId={mapelId}") {
        fun createRoute(
            penugasanId: String? = null,
            kelasId: String? = null,
            mapelId: String? = null
        ): String {
            val p = penugasanId.orEmpty()
            val k = kelasId.orEmpty()
            val m = mapelId.orEmpty()
            return "grading_overview?penugasanId=$p&kelasId=$k&mapelId=$m"
        }
    }

    object GradingEntry : Screen("grading_entry/{komponenId}") {
        fun createRoute(komponenId: String): String = "grading_entry/$komponenId"
    }

    object StudentList : Screen("student_list")

    object StudentEmisDetail : Screen("student_emis_detail/{siswaId}/{kelasId}/{nomorAbsen}") {
        fun createRoute(siswaId: String, kelasId: String, nomorAbsen: Int): String =
            "student_emis_detail/$siswaId/$kelasId/$nomorAbsen"
    }

    object KkmConfig : Screen("kkm_config")

    object LifecycleMutation : Screen("lifecycle_mutation")
}
