import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(
    onButtonClicked: () -> Unit,
    webviewComponent: @Composable () -> Unit
) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
        )
    ) {
        var buttonClicked by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(!buttonClicked) {
                Button(
                    modifier = Modifier.fillMaxWidth(0.6f).height(100.dp).clip(CircleShape),
                    onClick = {
                    onButtonClicked()
                    buttonClicked = true
                }) {
                    Text(
                        fontSize = 26.sp,
                        text = "Start scanning"
                    )
                }
            }
            AnimatedVisibility(buttonClicked) {
                webviewComponent()
            }
        }
    }
}


expect fun getPlatformName(): String