package id.mts.rth.ui.screens.grading

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.domain.model.StudentGradeSummary
import id.mts.rth.ui.components.KkmStatusBadge
import id.mts.rth.ui.components.PredicateBadge
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer
import id.mts.rth.ui.theme.StatusHadirOnContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradingOverviewScreen(
    viewModel: GradingViewModel,
    onNavigateToEntry: (komponenId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Launcher untuk Storage Access Framework (SAF) Simpan CSV
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(viewModel.generateCsvContent().toByteArray(Charsets.UTF_8))
                        }
                    }
                    snackbarHostState.showSnackbar("File CSV rekap nilai berhasil disimpan!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Gagal menyimpan file CSV: ${e.localizedMessage}")
                }
            }
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        if (state.feedbackMessage != null) {
            snackbarHostState.showSnackbar(state.feedbackMessage.orEmpty())
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Penilaian: ${state.namaKelas}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "${state.namaMapel} • KKM: ${state.kkm.nilaiKkm}",
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
                    // Tombol Tambah Kolom Nilai
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Kolom",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Tombol Ekspor CSV
                    IconButton(
                        onClick = {
                            val fileName = "Rekap_Nilai_${state.namaKelas}_${state.namaMapel}.csv".replace(" ", "_")
                            createDocumentLauncher.launch(fileName)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor CSV",
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
                // Tab Navigation: KI-3, KI-4, Rekap Nilai Akhir
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldTeal
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("KI-3 Pengetahuan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("KI-4 Keterampilan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Rekap Akhir", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                when (selectedTabIndex) {
                    0 -> ComponentListTab(
                        components = state.komponenKi3,
                        totalStudents = state.students.size,
                        aspekTitle = "KI-3 Pengetahuan (Tugas, UH, PAS/PAT)",
                        onComponentClick = onNavigateToEntry,
                        onAddClick = { showAddDialog = true }
                    )
                    1 -> ComponentListTab(
                        components = state.komponenKi4,
                        totalStudents = state.students.size,
                        aspekTitle = "KI-4 Keterampilan (Praktik, Proyek, Portofolio)",
                        onComponentClick = onNavigateToEntry,
                        onAddClick = { showAddDialog = true }
                    )
                    2 -> FinalRecapTab(
                        summaries = state.summaries,
                        kkm = state.kkm.nilaiKkm,
                        onExportClick = {
                            val fileName = "Rekap_Nilai_${state.namaKelas}_${state.namaMapel}.csv".replace(" ", "_")
                            createDocumentLauncher.launch(fileName)
                        }
                    )
                }
            }
        }
    }

    // Dialog Tambah Kolom Komponen Nilai Baru
    if (showAddDialog) {
        AddGradeColumnDialog(
            defaultAspek = if (selectedTabIndex == 1) "KI4" else "KI3",
            onDismiss = { showAddDialog = false },
            onConfirm = { aspek, kategori, judul ->
                viewModel.createKomponen(aspek, kategori, judul) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun ComponentListTab(
    components: List<KomponenNilaiEntity>,
    totalStudents: Int,
    aspekTitle: String,
    onComponentClick: (komponenId: String) -> Unit,
    onAddClick: () -> Unit
) {
    if (components.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Grade,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Belum Ada Kolom Nilai",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tekan tombol di bawah untuk menambah kolom nilai baru.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Kolom Nilai")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = aspekTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            items(components, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onComponentClick(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.kategori.take(3),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.judul,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Kategori: ${item.kategori} • Tanggal: ${item.tanggalInput}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        IconButton(onClick = { onComponentClick(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Input Nilai",
                                tint = EmeraldTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalRecapTab(
    summaries: List<StudentGradeSummary>,
    kkm: Int,
    onExportClick: () -> Unit
) {
    val totalSiswa = summaries.size
    val tuntasCount = summaries.count { !it.isRemedialKi3 && !it.isRemedialKi4 }
    val tuntasPercent = if (totalSiswa > 0) (tuntasCount * 100) / totalSiswa else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Kartu Ringkasan Ketuntasan KKM
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StatusHadirContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ketuntasan Belajar Kelas",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusHadirOnContainer
                            )
                        )
                        Text(
                            text = "$tuntasCount dari $totalSiswa Siswa Tuntas KKM ($kkm)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusHadirOnContainer.copy(alpha = 0.85f)
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusHadir
                    ) {
                        Text(
                            text = "$tuntasPercent%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Header Daftar Siswa
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rekap Nilai Siswa ($totalSiswa)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(onClick = onExportClick) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor CSV", fontSize = 12.sp)
                }
            }
        }

        // Baris Rekap per Siswa
        items(summaries, key = { it.siswaId }) { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nomor Absen
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", s.nomorAbsen),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Nama Siswa
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.namaLengkap,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tgs: ${s.rataTugas?.let { "%.1f".format(it) } ?: "-"} • UH: ${s.rataUh?.let { "%.1f".format(it) } ?: "-"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Skor KI-3 Pengetahuan
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "KI-3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.naPengetahuan.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (s.isRemedialKi3) StatusAlpha else StatusHadir
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            PredicateBadge(predikat = s.predikatKi3)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Skor KI-4 Keterampilan
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "KI-4",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.naKeterampilan.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (s.isRemedialKi4) StatusAlpha else StatusHadir
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            PredicateBadge(predikat = s.predikatKi4)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGradeColumnDialog(
    defaultAspek: String,
    onDismiss: () -> Unit,
    onConfirm: (aspek: String, kategori: String, judul: String) -> Unit
) {
    var aspek by remember { mutableStateOf(defaultAspek) }
    var kategori by remember {
        mutableStateOf(if (defaultAspek == "KI3") "TUGAS" else "PRAKTIK")
    }
    var judul by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val ki3Categories = listOf("TUGAS", "UH", "UJIAN_AKHIR")
    val ki4Categories = listOf("PRAKTIK", "PROYEK", "PORTOFOLIO")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Tambah Kolom Nilai", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Pilih Aspek
                Text(text = "Pilih Aspek Penilaian:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = aspek == "KI3",
                        onClick = {
                            aspek = "KI3"
                            kategori = "TUGAS"
                        },
                        label = { Text("KI-3 Pengetahuan") }
                    )
                    FilterChip(
                        selected = aspek == "KI4",
                        onClick = {
                            aspek = "KI4"
                            kategori = "PRAKTIK"
                        },
                        label = { Text("KI-4 Keterampilan") }
                    )
                }

                // Pilih Kategori
                Text(text = "Kategori Nilai:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentList = if (aspek == "KI3") ki3Categories else ki4Categories
                    for (cat in currentList) {
                        FilterChip(
                            selected = kategori == cat,
                            onClick = { kategori = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Input Judul
                OutlinedTextField(
                    value = judul,
                    onValueChange = {
                        judul = it
                        errorMessage = null
                    },
                    label = { Text("Nama / Judul Kolom") },
                    placeholder = {
                        Text(if (aspek == "KI3") "Misal: Tugas 1 / UH Bab 2" else "Misal: Praktik Tilawah")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (judul.isBlank()) {
                        errorMessage = "Nama kolom tidak boleh kosong."
                        return@Button
                    }
                    onConfirm(aspek, kategori, judul.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
