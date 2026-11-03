package id.mts.rth.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.domain.model.AttendanceStatus
import id.mts.rth.ui.components.AttendanceRowItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog edit catatan siswa
    var editingStudent by remember { mutableStateOf<StudentAttendanceUiModel?>(null) }
    var currentNoteText by remember { mutableStateOf("") }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Presensi berhasil disimpan ke Room & outbox!")
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            snackbarHostState.showSnackbar(state.errorMessage.orEmpty())
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Presensi: ${state.namaKelas}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "${state.namaMapel} • ${state.formattedTanggal}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.setAllStatus(AttendanceStatus.HADIR) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Semua Hadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldTeal,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveAttendance() },
                        enabled = !state.isSaving && !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Menyimpan...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simpan Presensi (${state.totalHadir}/${state.totalSiswa} Hadir)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldTeal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Bar Ringkasan Status Kehadiran Cepat
                AttendanceSummaryHeader(
                    totalHadir = state.totalHadir,
                    totalSakit = state.totalSakit,
                    totalIzin = state.totalIzin,
                    totalAlpha = state.totalAlpha
                )

                // 2. Input Jurnal Mengajar Singkat
                JournalInputCard(
                    jamKe = state.jamKe,
                    materiPokok = state.materiPokok,
                    onJamKeChange = { viewModel.updateJamKe(it) },
                    onMateriChange = { viewModel.updateMateriPokok(it) }
                )

                // 3. Daftar Siswa Rombel (Fast Single-Tap List)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.students, key = { it.siswaId }) { student ->
                        AttendanceRowItem(
                            nomorAbsen = student.nomorAbsen,
                            namaLengkap = student.namaLengkap,
                            nisLokal = student.nisLokal,
                            status = student.status,
                            catatan = student.catatan,
                            onStatusToggle = { viewModel.toggleStudentStatus(student.siswaId) },
                            onEditNoteClick = {
                                editingStudent = student
                                currentNoteText = student.catatan
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog Input/Edit Catatan Siswa
    if (editingStudent != null) {
        val student = editingStudent!!
        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = {
                Text(
                    text = "Catatan Kehadiran",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "${student.namaLengkap} (No. Absen: ${student.nomorAbsen})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentNoteText,
                        onValueChange = { currentNoteText = it },
                        placeholder = { Text("Contoh: Sakit demam berdarah / Izin lomba") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStudentNote(student.siswaId, currentNoteText)
                        editingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingStudent = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AttendanceSummaryHeader(
    totalHadir: Int,
    totalSakit: Int,
    totalIzin: Int,
    totalAlpha: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBadge(
                label = "Hadir",
                count = totalHadir,
                containerColor = StatusHadirContainer,
                contentColor = StatusHadirOnContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryBadge(
                label = "Sakit",
                count = totalSakit,
                containerColor = StatusSakitContainer,
                contentColor = StatusSakitOnContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryBadge(
                label = "Izin",
                count = totalIzin,
                containerColor = StatusIzinContainer,
                contentColor = StatusIzinOnContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryBadge(
                label = "Alpa",
                count = totalAlpha,
                containerColor = StatusAlphaContainer,
                contentColor = StatusAlphaOnContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun JournalInputCard(
    jamKe: String,
    materiPokok: String,
    onJamKeChange: (String) -> Unit,
    onMateriChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Jurnal Mengajar KBM",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = jamKe,
                    onValueChange = onJamKeChange,
                    label = { Text("Jam Ke", fontSize = 11.sp) },
                    placeholder = { Text("1-2") },
                    singleLine = true,
                    modifier = Modifier.weight(0.32f)
                )

                OutlinedTextField(
                    value = materiPokok,
                    onValueChange = onMateriChange,
                    label = { Text("Materi Pokok / Pembahasan", fontSize = 11.sp) },
                    placeholder = { Text("Contoh: Bab 2 - Sholat Berjamaah") },
                    singleLine = true,
                    modifier = Modifier.weight(0.68f)
                )
            }
        }
    }
}
