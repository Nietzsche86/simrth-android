package id.mts.rth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun PredicateBadge(
    predikat: Char,
    modifier: Modifier = Modifier
) {
    val (containerColor, onContainerColor, borderColor) = when (predikat.uppercaseChar()) {
        'A' -> Triple(StatusHadirContainer, StatusHadirOnContainer, StatusHadir)
        'B' -> Triple(StatusIzinContainer, StatusIzinOnContainer, StatusIzin)
        'C' -> Triple(StatusSakitContainer, StatusSakitOnContainer, StatusSakit)
        else -> Triple(StatusAlphaContainer, StatusAlphaOnContainer, StatusAlpha)
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = predikat.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
fun KkmStatusBadge(
    isTuntas: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isTuntas) StatusHadirContainer else StatusAlphaContainer
    val contentColor = if (isTuntas) StatusHadirOnContainer else StatusAlphaOnContainer
    val borderColor = if (isTuntas) StatusHadir else StatusAlpha
    val text = if (isTuntas) "Tuntas" else "Remedial"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun ScoreBadge(
    score: Int,
    kkm: Int,
    modifier: Modifier = Modifier
) {
    val isTuntas = score >= kkm
    val textColor = if (isTuntas) StatusHadir else StatusAlpha

    Text(
        text = if (score > 0) score.toString() else "-",
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = if (score > 0) textColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
