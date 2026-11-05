package id.mts.rth.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer

@Composable
fun SchoolManagementScreen(
    viewModel: AdminViewModel,
    onNavigateToKkmConfig: () -> Unit = {},
    onNavigateToMutation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.schoolMgmtState.collectAsState()

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 3 Sub-tabs
            TabRow(
                selectedTabIndex = state.selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.selectedSubTab]),
                        color = EmeraldTeal
                    )
                }
            ) {
                Tab(
                    selected = state.selectedSubTab == 0,
                    onClick = { viewModel.setSchoolMgmtSubTab(0) },
                    text = {
                        Text(
                            text = "Tahun Ajaran",
                            fontWeight = if (state.selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = state.selectedSubTab == 1,
                    onClick = { viewModel.setSchoolMgmtSubTab(1) },
                    text = {
                        Text(
                            text = "Rombel / Kelas",
                            fontWeight = if (state.selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = state.selectedSubTab == 2,
                    onClick = { viewModel.setSchoolMgmtSubTab(2) },
                    text = {
                        Text(
                            text = "Penugasan Guru",
                            fontWeight = if (state.selectedSubTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            when (state.selectedSubTab) {
                0 -> {
                    // Tab 0: Tahun Ajaran
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.tahunAjaranList, key = { it.id }) { ta ->
                            TahunAjaranCardItem(ta = ta)
                        }
                    }
                }
                1 -> {
                    // Tab 1: Rombel / Kelas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Quick Action Tools: KKM Config & Kenaikan/Kelulusan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToKkmConfig,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Atur KKM", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onNavigateToMutation,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mutasi Siswa", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.kelasList, key = { it.id }) { kelas ->
                                val wali = state.teacherList.firstOrNull { it.id == kelas.waliKelasGuruId }
                                KelasCardItem(kelas = kelas, waliNama = wali?.namaLengkap)
                            }
                        }
                    }
                }
                2 -> {
                    // Tab 2: Penugasan Guru / Staff
                    val teacherMap = state.teacherList.associateBy { it.id }
                    val mapelMap = state.mapelList.associateBy { it.id }
                    val kelasMap = state.kelasList.associateBy { it.id }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.penugasanList, key = { it.id }) { penugasan ->
                            val guru = teacherMap[penugasan.guruId]
                            val mapel = mapelMap[penugasan.mapelId]
                            val kelas = kelasMap[penugasan.kelasId]
                            PenugasanCardItem(
                                penugasan = penugasan,
                                guruNama = guru?.namaLengkap ?: penugasan.guruId,
                                mapelNama = mapel?.namaMapel ?: penugasan.mapelId,
                                kelasNama = kelas?.namaRombel ?: penugasan.kelasId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TahunAjaranCardItem(ta: TahunAjaranEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (ta.isActive) StatusHadirContainer else Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (ta.isActive) StatusHadir else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tahun ${ta.tahun} (${ta.semester})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (ta.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(StatusHadir, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AKTIF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Periode: ${ta.tanggalMulai} s.d. ${ta.tanggalSelesai}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun KelasCardItem(kelas: KelasEntity, waliNama: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EmeraldTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Class,
                    contentDescription = null,
                    tint = EmeraldTeal,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rombel Kelas ${kelas.namaRombel} (Tingkat ${kelas.tingkat})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wali Kelas: ${waliNama ?: "Belum Ditugaskan"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun PenugasanCardItem(
    penugasan: PenugasanGuruEntity,
    guruNama: String,
    mapelNama: String,
    kelasNama: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EmeraldTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = EmeraldTeal,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$mapelNama - Kelas $kelasNama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Guru Pengampu: $guruNama",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Jadwal: ${penugasan.jadwalJson}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                    maxLines = 1
                )
            }
        }
    }
}
