package id.mts.rth.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mts.rth.ui.components.PinNumPad
import id.mts.rth.ui.theme.StatusAlpha
import id.mts.rth.ui.theme.StatusAlphaContainer
import id.mts.rth.ui.theme.StatusAlphaOnContainer

@Composable
fun PinLockScreen(
    viewModel: AuthViewModel,
    onUnlockSuccess: () -> Unit,
    onForceRelogin: () -> Unit
) {
    val session by viewModel.sessionState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val pinError by viewModel.pinError.collectAsState()

    val isLockedOut = session?.isLockedOut == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Icon Lock
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (isLockedOut) StatusAlphaContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isLockedOut) StatusAlpha else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = session?.userName ?: "Guru SIMRTH",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "Peran: ${session?.activeRole ?: "GURU"} • Masukkan PIN Offline",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLockedOut) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StatusAlphaContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Aplikasi Terkunci!",
                            fontWeight = FontWeight.Bold,
                            color = StatusAlphaOnContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Salah memasukkan PIN 5 kali berturut-turut. Sesi offline telah dihapus. Hubungkan ke Wi-Fi madrasah untuk login ulang menggunakan password akun Anda.",
                            color = StatusAlphaOnContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onForceRelogin,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAlpha),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hubungkan Ulang Akun")
                        }
                    }
                }
            } else {
                PinNumPad(
                    pin = pinInput,
                    onPinChange = { viewModel.onPinChanged(it) },
                    onPinComplete = { enteredPin ->
                        viewModel.verifyPin(enteredPin, onUnlockSuccess)
                    },
                    isError = pinError != null,
                    errorMessage = pinError
                )
            }
        }
    }
}
