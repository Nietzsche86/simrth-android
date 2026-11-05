package id.mts.rth.ui.screens.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.repository.StudentDirectoryItem
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer
import id.mts.rth.ui.theme.StatusIzinContainer

@Composable
fun AdminDatabaseScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.databaseState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (state.selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.openAddStudentDialog(state.selectedKelasIdFilter) },
                    containerColor = EmeraldTeal,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Siswa Baru")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Sub Tabs: Data Siswa vs Data Guru
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
                    onClick = { viewModel.setDatabaseSubTab(0) },
                    text = {
                        Text(
                            text = "Data Siswa (${state.filteredStudents.size})",
                            fontWeight = if (state.selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = state.selectedSubTab == 1,
                    onClick = { viewModel.setDatabaseSubTab(1) },
                    text = {
                        Text(
                            text = "Data Guru (${state.filteredTeachers.size})",
                            fontWeight = if (state.selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Pesan Feedback Notifikasi (Jika ada mutasi baru)
            if (state.operationFeedback != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isOperationError) StatusAlpha.copy(alpha = 0.15f) else StatusHadirContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.operationFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (state.isOperationError) StatusAlpha else StatusHadir,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissDatabaseMessage() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Tutup", tint = Color.Gray)
                        }
                    }
                }
            }

            // Search Bar & Filter
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setDatabaseSearchQuery(it) },
                placeholder = {
                    Text(
                        if (state.selectedSubTab == 0) "Cari nama atau NIS siswa..." else "Cari nama atau NIP guru..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setDatabaseSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (state.selectedSubTab == 0) {
                // Filter Chip Rombel Kelas
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.kelasList) { kelas ->
                        val isSelected = state.selectedKelasIdFilter == kelas.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDatabaseKelasFilter(kelas.id) },
                            label = { Text("Kelas ${kelas.namaRombel}") }
                        )
                    }
                }

                // Daftar Siswa
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredStudents, key = { it.siswaId }) { student ->
                        StudentAdminRowItem(
                            student = student,
                            onEditClick = { viewModel.openEditStudentDialog(student, state.selectedKelasIdFilter) }
                        )
                    }
                }
            } else {
                // Daftar Guru
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredTeachers, key = { it.id }) { teacher ->
                        TeacherAdminRowItem(teacher = teacher)
                    }
                }
            }
        }
    }

    // Dialog Form Tambah / Edit Siswa
    if (state.showStudentDialog) {
        StudentFormDialog(
            form = state.studentForm,
            kelasList = state.kelasList,
            isSaving = state.isSavingStudent,
            onFormChange = { updated -> viewModel.updateStudentFormData { updated } },
            onSave = { viewModel.saveStudent() },
            onDismiss = { viewModel.dismissStudentDialog() }
        )
    }
}

@Composable
private fun StudentAdminRowItem(
    student: StudentDirectoryItem,
    onEditClick: () -> Unit
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EmeraldTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", student.nomorAbsen),
                    fontWeight = FontWeight.Bold,
                    color = EmeraldTeal,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.namaLengkap,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NIS: ${student.nisLokal} • NISN: ${student.nisn ?: "-"} • JK: ${student.jenisKelamin}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Siswa",
                    tint = EmeraldTeal
                )
            }
        }
    }
}

@Composable
private fun TeacherAdminRowItem(
    teacher: GuruEntity
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(StatusIzinContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = EmeraldTeal,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.namaLengkap,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NIP: ${teacher.nip ?: "-"} • Peran: ${teacher.role}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Status: ${teacher.statusKepegawaian} • WA: ${teacher.noWa}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun StudentFormDialog(
    form: StudentFormData,
    kelasList: List<KelasEntity>,
    isSaving: Boolean,
    onFormChange: (StudentFormData) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = !form.id.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Data Siswa" else "Tambah Siswa Baru",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Data Pokok Siswa (Wajib)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EmeraldTeal,
                        fontWeight = FontWeight.Bold
                    )
                )

                OutlinedTextField(
                    value = form.namaLengkap,
                    onValueChange = { onFormChange(form.copy(namaLengkap = it)) },
                    label = { Text("Nama Lengkap Siswa *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.nisLokal,
                        onValueChange = { onFormChange(form.copy(nisLokal = it)) },
                        label = { Text("NIS Lokal *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = form.nisn,
                        onValueChange = { onFormChange(form.copy(nisn = it)) },
                        label = { Text("NISN (Opsional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Jenis Kelamin Selector
                Text(text = "Jenis Kelamin *", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onFormChange(form.copy(jenisKelamin = "L")) }
                    ) {
                        RadioButton(
                            selected = form.jenisKelamin == "L",
                            onClick = { onFormChange(form.copy(jenisKelamin = "L")) }
                        )
                        Text("Laki-laki (L)")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onFormChange(form.copy(jenisKelamin = "P")) }
                    ) {
                        RadioButton(
                            selected = form.jenisKelamin == "P",
                            onClick = { onFormChange(form.copy(jenisKelamin = "P")) }
                        )
                        Text("Perempuan (P)")
                    }
                }

                // Pilihan Rombel Kelas
                Text(text = "Pilih Rombel Kelas *", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kelasList) { k ->
                        FilterChip(
                            selected = form.kelasId == k.id,
                            onClick = { onFormChange(form.copy(kelasId = k.id)) },
                            label = { Text("Kelas ${k.namaRombel}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = form.nomorAbsen.toString(),
                    onValueChange = { onFormChange(form.copy(nomorAbsen = it.toIntOrNull() ?: 1)) },
                    label = { Text("Nomor Absen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Biodata EMIS Kemenag (Opsional)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EmeraldTeal,
                        fontWeight = FontWeight.Bold
                    )
                )

                OutlinedTextField(
                    value = form.nik,
                    onValueChange = { onFormChange(form.copy(nik = it)) },
                    label = { Text("NIK Siswa (16 Digit)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.noHpOrangTuaWa,
                    onValueChange = { onFormChange(form.copy(noHpOrangTuaWa = it)) },
                    label = { Text("No. HP / WhatsApp Orang Tua") },
                    placeholder = { Text("081234567890") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.namaAyah,
                        onValueChange = { onFormChange(form.copy(namaAyah = it)) },
                        label = { Text("Nama Ayah") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = form.namaIbu,
                        onValueChange = { onFormChange(form.copy(namaIbu = it)) },
                        label = { Text("Nama Ibu") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = form.alamatJalan,
                    onValueChange = { onFormChange(form.copy(alamatJalan = it)) },
                    label = { Text("Alamat Rumah / Jalan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isEditMode) "Perbarui Siswa" else "Simpan Siswa")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Batal")
            }
        }
    )
}
