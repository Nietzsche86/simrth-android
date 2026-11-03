package id.mts.rth.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusAlphaContainer
import id.mts.rth.ui.theme.StatusAlphaOnContainer
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer
import id.mts.rth.ui.theme.StatusHadirOnContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifecycleMutationScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val isAdmin by viewModel.isAdminSession.collectAsState()
    val state by viewModel.mutationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.executionSuccessMessage) {
        if (state.executionSuccessMessage != null) {
            snackbarHostState.showSnackbar(state.executionSuccessMessage.orEmpty())
            viewModel.dismissMutationMessage()
        }
    }

    LaunchedEffect(state.executionErrorMessage) {
        if (state.executionErrorMessage != null) {
            snackbarHostState.showSnackbar(state.executionErrorMessage.orEmpty())
            viewModel.dismissMutationMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mutasi Siklus Kesiswaan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "Kenaikan Kelas & Kelulusan Massal",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldTeal,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (isAdmin && state.selectedStudentIds.isNotEmpty()) {
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
                        val isPromotion = state.mutationType == MutationType.PROMOTION
                        val btnEnabled = !state.isExecuting && (!isPromotion || state.targetKelasId.isNotBlank())

                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = btnEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPromotion) EmeraldTeal else StatusAlpha,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengeksekusi Mutasi...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = if (isPromotion) Icons.Default.ArrowForward else Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPromotion) {
                                        "Eksekusi Kenaikan (${state.selectedStudentIds.size} Siswa)"
                                    } else {
                                        "Eksekusi Kelulusan (${state.selectedStudentIds.size} Siswa)"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusAlphaContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusAlpha,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Akses Ditolak",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusAlphaOnContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hanya akun Administrator madrasah yang berwenang mengeksekusi mutasi kenaikan kelas dan kelulusan siswa.",
                            style = MaterialTheme.typography.bodySmall.copy(color = StatusAlphaOnContainer),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Pemilihan Jenis Mutasi (Kenaikan Kelas vs Kelulusan)
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
                        FilterChip(
                            selected = state.mutationType == MutationType.PROMOTION,
                            onClick = { viewModel.onMutationTypeChanged(MutationType.PROMOTION) },
                            label = { Text("Kenaikan Kelas Rombel") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldTeal,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = state.mutationType == MutationType.GRADUATION,
                            onClick = { viewModel.onMutationTypeChanged(MutationType.GRADUATION) },
                            label = { Text("Kelulusan Kelas 9") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusAlpha,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 2. Pemilihan Rombel Asal & Tujuan
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pilih Rombel Asal:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.kelasList.forEach { kelas ->
                                val isSelected = kelas.id == state.sourceKelasId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onSourceKelasChanged(kelas.id) },
                                    label = { Text("Kelas ${kelas.namaRombel}") }
                                )
                            }
                        }

                        if (state.mutationType == MutationType.PROMOTION) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pilih Rombel Tujuan Kenaikan:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldTeal
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.kelasList.forEach { kelas ->
                                    val isSelected = kelas.id == state.targetKelasId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onTargetKelasChanged(kelas.id) },
                                        label = { Text("Kelas ${kelas.namaRombel}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldTeal,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Header Counter & Toggle Select All Siswa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = state.selectedStudentIds.size == state.sourceStudents.size && state.sourceStudents.isNotEmpty()

                    Text(
                        text = "${state.selectedStudentIds.size} dari ${state.sourceStudents.size} Siswa Terpilih",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    TextButton(
                        onClick = { viewModel.toggleSelectAll(!allSelected, state.sourceStudents) }
                    ) {
                        Text(if (allSelected) "Batal Pilih Semua" else "Pilih Semua", fontSize = 12.sp)
                    }
                }

                // 4. Daftar Siswa Rombel Asal
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.sourceStudents, key = { it.siswaId }) { student ->
                        val isChecked = state.selectedStudentIds.contains(student.siswaId)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleStudentSelection(student.siswaId) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleStudentSelection(student.siswaId) },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldTeal)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", student.nomorAbsen),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.namaLengkap,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "NIS: ${student.nisLokal} • Status: ${student.statusSiswa}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Eksekusi Mutasi Ganda
    if (showConfirmDialog) {
        val isPromotion = state.mutationType == MutationType.PROMOTION
        val sourceKelasName = state.kelasList.firstOrNull { it.id == state.sourceKelasId }?.namaRombel ?: state.sourceKelasId
        val targetKelasName = state.kelasList.firstOrNull { it.id == state.targetKelasId }?.namaRombel ?: state.targetKelasId

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isPromotion) EmeraldTeal else StatusAlpha,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isPromotion) "Konfirmasi Kenaikan Kelas" else "Konfirmasi Kelulusan Siswa",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isPromotion) {
                        "Apakah Anda yakin ingin menaikkan ${state.selectedStudentIds.size} siswa dari Kelas $sourceKelasName ke Kelas $targetKelasName? Seluruh riwayat akan disimpan secara atomik dan dicatat ke antrean sinkronisasi."
                    } else {
                        "Apakah Anda yakin ingin menetapkan status LULUS untuk ${state.selectedStudentIds.size} siswa dari Kelas $sourceKelasName? Status keanggotaan rombel akan diakhiri dan status siswa berubah menjadi LULUS."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.executeMutation()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPromotion) EmeraldTeal else StatusAlpha
                    )
                ) {
                    Text("Ya, Eksekusi Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
