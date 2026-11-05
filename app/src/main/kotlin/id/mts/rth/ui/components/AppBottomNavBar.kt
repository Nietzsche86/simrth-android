package id.mts.rth.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.mts.rth.ui.theme.EmeraldTeal

data class NavTabItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

object NavTabDefinitions {
    // 1. Tab Guru Biasa (4 Tab)
    val GURU_TABS = listOf(
        NavTabItem("home", "Beranda", Icons.Default.Home),
        NavTabItem("attendance", "Presensi", Icons.Default.Assignment),
        NavTabItem("grading", "Nilai", Icons.Default.Edit),
        NavTabItem("profile", "Data Diri", Icons.Default.Person)
    )

    // 2. Tab Wali Kelas (5 Tab)
    val WALI_TABS = listOf(
        NavTabItem("home", "Beranda", Icons.Default.Home),
        NavTabItem("attendance", "Presensi", Icons.Default.Assignment),
        NavTabItem("grading", "Nilai", Icons.Default.Edit),
        NavTabItem("students", "Data Siswa", Icons.Default.People),
        NavTabItem("profile", "Data Diri", Icons.Default.Person)
    )

    // 3. Tab Admin Madrasah (5 Tab)
    val ADMIN_TABS = listOf(
        NavTabItem("home", "Dashboard", Icons.Default.Dashboard),
        NavTabItem("admin_database", "Database", Icons.Default.Storage),
        NavTabItem("admin_school", "Manajemen", Icons.Default.AccountBalance),
        NavTabItem("admin_accounts", "Akun", Icons.Default.ManageAccounts),
        NavTabItem("admin_profile", "Data Sekolah", Icons.Default.School)
    )
}

@Composable
fun AppBottomNavBar(
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    userRole: String,
    isWaliKelas: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = when {
        userRole.equals("ADMIN", ignoreCase = true) -> NavTabDefinitions.ADMIN_TABS
        isWaliKelas || userRole.equals("GURU_WALI", ignoreCase = true) -> NavTabDefinitions.WALI_TABS
        else -> NavTabDefinitions.GURU_TABS
    }

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTabId == tab.id
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.id) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldTeal,
                    selectedTextColor = EmeraldTeal,
                    indicatorColor = EmeraldTeal.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
