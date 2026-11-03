package id.mts.rth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.domain.model.AttendanceStatus
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
fun AttendanceRowItem(
    nomorAbsen: Int,
    namaLengkap: String,
    nisLokal: String,
    status: AttendanceStatus,
    catatan: String? = null,
    onStatusToggle: () -> Unit,
    onEditNoteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPresent = status == AttendanceStatus.HADIR

    // Konfigurasi visual berdasarkan status rotasi sekuensial
    val (containerColor, onContainerColor, borderColor, icon, statusText) = when (status) {
        AttendanceStatus.HADIR -> StatusConfig(
            container = StatusHadirContainer,
            onContainer = StatusHadirOnContainer,
            border = StatusHadir,
            icon = Icons.Default.Check,
            label = "Hadir"
        )
        AttendanceStatus.SAKIT -> StatusConfig(
            container = StatusSakitContainer,
            onContainer = StatusSakitOnContainer,
            border = StatusSakit,
            icon = Icons.Default.Warning,
            label = "Sakit"
        )
        AttendanceStatus.IZIN -> StatusConfig(
            container = StatusIzinContainer,
            onContainer = StatusIzinOnContainer,
            border = StatusIzin,
            icon = Icons.Default.Info,
            label = "Izin"
        )
        AttendanceStatus.ALPHA -> StatusConfig(
            container = StatusAlphaContainer,
            onContainer = StatusAlphaOnContainer,
            border = StatusAlpha,
            icon = Icons.Default.Close,
            label = "Alpa"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) MaterialTheme.colorScheme.surface else containerColor.copy(alpha = 0.25f)
        ),
        border = if (!isPresent) BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPresent) 0.5.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nomor Absen Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", nomorAbsen),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nama Siswa & Identitas
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = namaLengkap,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "NIS: $nisLokal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )

                    if (!catatan.isNullOrBlank()) {
                        Surface(
                            color = borderColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Ket: $catatan",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = onContainerColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tombol Catatan Tambahan (Opsional)
            if (onEditNoteClick != null) {
                IconButton(
                    onClick = onEditNoteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit Catatan",
                        tint = if (!catatan.isNullOrBlank()) borderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tombol Status Single-Tap Rotasi Sekuensial (H -> S -> I -> A -> H)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onStatusToggle() },
                color = containerColor,
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

private data class StatusConfig(
    val container: Color,
    val onContainer: Color,
    val border: Color,
    val icon: ImageVector,
    val label: String
)
