package id.mts.rth.ui.screens.students

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import id.mts.rth.ui.theme.EmeraldTeal
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusAlphaContainer
import id.mts.rth.ui.theme.StatusAlphaOnContainer
import id.mts.rth.ui.theme.StatusHadir
import id.mts.rth.ui.theme.StatusHadirContainer
import id.mts.rth.ui.theme.StatusHadirOnContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEmisDetailScreen(
    viewModel: StudentViewModel,
    siswaId: String,
    kelasId: String,
    nomorAbsen: Int,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(siswaId, kelasId) {
        viewModel.loadStudentDetail(siswaId, kelasId, nomorAbsen)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profil EMIS Siswa",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "${detailState.studentCore?.namaLengkap ?: "Siswa"} • ${detailState.namaKelas}",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (detailState.isLoading) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val core = detailState.studentCore
                val detail = detailState.studentDetail

                // 1. Header Identitas Siswa (Selalu Tampil)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Foto Profil WebP
                        if (detailState.fotoFile != null && detailState.fotoFile!!.exists()) {
                            AsyncImage(
                                model = detailState.fotoFile,
                                contentDescription = core?.namaLengkap,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = core?.namaLengkap ?: "Nama Siswa",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "NIS: ${core?.nisLokal ?: "-"} • NISN: ${core?.nisn ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${detailState.namaKelas} (Absen ${detailState.nomorAbsen})",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Surface(
                                color = StatusHadirContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = core?.statusSiswa ?: "AKTIF",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusHadirOnContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Pemeriksaan RBAC: Peringatan Akses Terbatas jika Bukan Wali/Admin
                if (detailState.isAccessDenied) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusAlphaContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = StatusAlpha,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Akses Terbatas: Hanya Wali Kelas & Admin",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAlphaOnContainer
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = detailState.accessDeniedMessage.ifBlank {
                                    "Data lengkap EMIS (NIK, Kartu Keluarga, Data Orang Tua, dan Bantuan KIP) hanya dapat diakses oleh Wali Kelas yang membina kelas ini serta Administrator madrasah."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = StatusAlphaOnContainer.copy(alpha = 0.9f)
                                ),
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else if (detail != null) {
                    // 3. Detail Lengkap EMIS (Bagi Wali Kelas & Admin)

                    // Card Identitas Kependudukan (NIK, KK, Domisili)
                    EmisSectionCard(
                        title = "Data Identitas Kependudukan",
                        icon = Icons.Default.Badge
                    ) {
                        DetailItem(label = "Nomor Induk Kependudukan (NIK)", value = detail.nik)
                        DetailItem(label = "Nomor Kartu Keluarga (KK)", value = detail.noKk)
                        DetailItem(label = "Tempat, Tanggal Lahir", value = "${detail.tempatLahir}, ${detail.tanggalLahir}")
                        DetailItem(label = "Agama", value = detail.agama)
                        DetailItem(label = "Anak Ke / Jumlah Saudara", value = "Anak ke-${detail.anakKe} dari ${detail.jumlahSaudara} bersaudara")
                        DetailItem(
                            label = "Alamat Domisili",
                            value = "${detail.alamatJalan}, RT ${detail.rt} / RW ${detail.rw}, Desa ${detail.desaKelurahan}, Kec. ${detail.kecamatan}, ${detail.kabupatenKota}${if (!detail.kodePos.isNullOrBlank()) " (${detail.kodePos})" else ""}"
                        )
                        DetailItem(label = "Status Tempat Tinggal", value = detail.statusTempatTinggal)
                    }

                    // Card Biodata Orang Tua & Kontak
                    EmisSectionCard(
                        title = "Data Orang Tua & Kontak",
                        icon = Icons.Default.People
                    ) {
                        DetailItem(label = "Nama Ayah Kandung", value = detail.namaAyah ?: "-")
                        DetailItem(label = "NIK Ayah / Status", value = "${detail.nikAyah ?: "-"} (${detail.statusAyah ?: "Hidup"})")
                        DetailItem(label = "Pekerjaan / Penghasilan Ayah", value = "${detail.pekerjaanAyah ?: "-"} • ${detail.penghasilanAyah ?: "-"}")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.5f))

                        DetailItem(label = "Nama Ibu Kandung", value = detail.namaIbu ?: "-")
                        DetailItem(label = "NIK Ibu / Status", value = "${detail.nikIbu ?: "-"} (${detail.statusIbu ?: "Hidup"})")
                        DetailItem(label = "Pekerjaan / Penghasilan Ibu", value = "${detail.pekerjaanIbu ?: "-"} • ${detail.penghasilanIbu ?: "-"}")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.5f))

                        DetailItem(label = "Nomor HP / WhatsApp Orang Tua", value = detail.noHpOrangTuaWa)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tombol Pintas WhatsApp Orang Tua
                        Button(
                            onClick = {
                                openWhatsApp(
                                    context = context,
                                    phoneNumber = detail.noHpOrangTuaWa,
                                    studentName = core?.namaLengkap ?: "Siswa",
                                    className = detailState.namaKelas
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusHadir),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hubungi WhatsApp Orang Tua",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Card Program Bantuan Sosial (KIP / PKH)
                    EmisSectionCard(
                        title = "Bantuan Sosial & Afirmasi EMIS",
                        icon = Icons.Default.Shield
                    ) {
                        DetailItem(label = "Nomor Kartu Indonesia Pintar (KIP)", value = detail.noKip ?: "Tidak Terdaftar")
                        DetailItem(label = "Nomor KIS / PKH", value = detail.noKisPkh ?: "Tidak Terdaftar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmisSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

private fun openWhatsApp(
    context: Context,
    phoneNumber: String,
    studentName: String,
    className: String
) {
    val digits = phoneNumber.filter { it.isDigit() }
    val cleanNumber = when {
        digits.startsWith("0") -> "62" + digits.substring(1)
        digits.startsWith("62") -> digits
        else -> "62$digits"
    }

    val defaultMessage = "Assalamu'alaikum Warahmatullahi Wabarakatuh, kami dari pihak madrasah ingin menyampaikan informasi terkait ananda $studentName ($className)."
    val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(defaultMessage)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback jika tidak ada browser/WA terpasang
    }
}
