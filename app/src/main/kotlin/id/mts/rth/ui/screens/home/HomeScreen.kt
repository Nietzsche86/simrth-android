package id.mts.rth.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.ui.components.SyncTopAppBar
import id.mts.rth.ui.theme.EmeraldLight
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusAlphaContainer
import id.mts.rth.ui.theme.StatusAlphaOnContainer
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer
import id.mts.rth.ui.theme.StatusHadirOnContainer
import id.mts.rth.ui.theme.StatusIzin
import id.mts.rth.ui.theme.StatusIzinContainer
import id.mts.rth.ui.theme.StatusIzinOnContainer
import id.mts.rth.ui.theme.StatusSakit
import id.mts.rth.ui.theme.StatusSakitContainer
import id.mts.rth.ui.theme.StatusSakitOnContainer

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAttendance: (kelasId: String, mapelId: String) -> Unit = { _, _ -> },
    onNavigateToGrading: (kelasId: String, mapelId: String) -> Unit = { _, _ -> },
    onNavigateToStudents: () -> Unit = {},
    onNavigateToPinLock: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showAllTeaching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SyncTopAppBar(
                title = "SIMRTH",
                subtitle = "${state.activeTahunAjaran} • ${state.activeSemester}",
                outboxCount = state.outboxCount,
                isSyncing = state.isSyncing,
                onSyncClick = { viewModel.triggerManualSync() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profil & Salam Guru Card
            TeacherGreetingCard(
                userName = state.userName,
                userRole = state.userRole,
                todayFormatted = state.todayDateFormatted,
                onLockClick = onNavigateToPinLock
            )

            // 2. Banner Pesan Notifikasi Sinkronisasi (Jika Ada)
            if (state.syncMessage != null) {
                SyncMessageBanner(
                    message = state.syncMessage.orEmpty(),
                    isError = state.isSyncError,
                    onDismiss = { viewModel.dismissSyncMessage() }
                )
            }

            // 3. Ringkasan Status Outbox
            OutboxSummaryCard(
                outboxCount = state.outboxCount,
                isSyncing = state.isSyncing,
                onSyncClick = { viewModel.triggerManualSync() }
            )

            // 4. Jadwal Mengajar Hari Ini Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Jadwal Hari Ini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${state.todayScheduleList.size} Kelas",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (state.allTeachingList.isNotEmpty()) {
                    Text(
                        text = if (showAllTeaching) "Tutup Semua" else "Lihat Semua",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.clickable { showAllTeaching = !showAllTeaching }
                    )
                }
            }

            if (state.todayScheduleList.isNotEmpty()) {
                state.todayScheduleList.forEach { schedule ->
                    ScheduleCardItem(
                        item = schedule,
                        onPresensiClick = { onNavigateToAttendance(schedule.kelasId, schedule.mapelId) },
                        onNilaiClick = { onNavigateToGrading(schedule.kelasId, schedule.mapelId) }
                    )
                }
            } else {
                EmptyTodayScheduleCard(
                    dateText = state.todayDateFormatted,
                    onViewAllClick = { showAllTeaching = true }
                )
            }

            // Daftar Semua Penugasan / Mengajar (Jika Toggle Aktif)
            if (showAllTeaching && state.allTeachingList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Seluruh Rombel & Penugasan Mengajar",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                state.allTeachingList.forEach { schedule ->
                    ScheduleCardItem(
                        item = schedule,
                        showDayBadge = true,
                        onPresensiClick = { onNavigateToAttendance(schedule.kelasId, schedule.mapelId) },
                        onNilaiClick = { onNavigateToGrading(schedule.kelasId, schedule.mapelId) }
                    )
                }
            }

            // 5. Pintasan Cepat Fitur Madrasah
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pintasan Menu Cepat",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Presensi",
                    icon = Icons.Default.Assignment,
                    containerColor = StatusHadirContainer,
                    iconColor = StatusHadir,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToAttendance("", "") }
                )
                QuickActionItem(
                    title = "Nilai K13",
                    icon = Icons.Default.Edit,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToGrading("", "") }
                )
                QuickActionItem(
                    title = "Siswa EMIS",
                    icon = Icons.Default.Person,
                    containerColor = StatusIzinContainer,
                    iconColor = StatusIzin,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStudents
                )
                QuickActionItem(
                    title = "Kunci PIN",
                    icon = Icons.Default.Lock,
                    containerColor = StatusAlphaContainer,
                    iconColor = StatusAlpha,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPinLock
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TeacherGreetingCard(
    userName: String,
    userRole: String,
    todayFormatted: String,
    onLockClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmeraldTeal,
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Assalamu'alaikum,",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                        Text(
                            text = userName.ifBlank { "Ustadz / Ustadzah" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Kunci PIN",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Peran: $userRole",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.85f)
                    )
                )
            }
        }
    }
}

@Composable
private fun OutboxSummaryCard(
    outboxCount: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    val isClean = outboxCount == 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isClean) StatusHadirContainer else StatusSakitContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isClean) StatusHadir.copy(alpha = 0.2f) else StatusSakit.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isClean) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = if (isClean) StatusHadirOnContainer else StatusSakitOnContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isClean) "Data Tersinkronisasi" else "$outboxCount Mutasi Belum Tersinkronkan",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isClean) StatusHadirOnContainer else StatusSakitOnContainer
                    )
                )
                Text(
                    text = if (isClean) {
                        "Seluruh data presensi & nilai telah tersimpan di server madrasah."
                    } else {
                        "Tersimpan lokal di HP. Sinkronkan via Wi-Fi madrasah."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isClean) StatusHadirOnContainer.copy(alpha = 0.85f) else StatusSakitOnContainer.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                )
            }

            if (!isClean) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusSakit,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncMessageBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) StatusAlphaContainer else StatusHadirContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) StatusAlpha else StatusHadir,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isError) StatusAlphaOnContainer else StatusHadirOnContainer,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = if (isError) StatusAlphaOnContainer else StatusHadirOnContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleCardItem(
    item: TodayScheduleUiModel,
    showDayBadge: Boolean = false,
    onPresensiClick: () -> Unit,
    onNilaiClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = item.namaKelas,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.namaMapel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (item.jamKe != "-") "Jam ke ${item.jamKe} • ${item.jamMulai} WIB" else "Jadwal Reguler",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (showDayBadge) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = item.namaHari,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPresensiClick,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Presensi Kelas", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onNilaiClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Nilai K13", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayScheduleCard(
    dateText: String,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tidak Ada Jam Mengajar Hari Ini",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = "Tidak ada jadwal tatap muka untuk $dateText.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onViewAllClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Lihat Semua Rombel Binaan", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color = containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}
