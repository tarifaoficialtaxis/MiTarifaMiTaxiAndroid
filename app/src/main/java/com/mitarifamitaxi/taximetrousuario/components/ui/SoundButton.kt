import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SoundButton(
    isSoundEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(6.dp, shape = CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = if (isSoundEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = if (isSoundEnabled) "Sonido activado" else "Sonido desactivado",
            modifier = Modifier.size(34.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SoundButtonPreview() {
    Column {
        SoundButton(
            isSoundEnabled = true,
            onClick = { }
        )
        Spacer(Modifier.size(16.dp))
        SoundButton(
            isSoundEnabled = false,
            onClick = { }
        )
    }
}
