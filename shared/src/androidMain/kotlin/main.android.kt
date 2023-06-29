import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

actual fun getPlatformName(): String = "Android"

@Composable
fun MainView(
    onStartScanningClicked: () -> Unit,
    macAddress: MutableState<String>,
    rssi: MutableState<Int>
) {
    LocationPermissionRequest(
        onPermissionGranted = {
            BluetoothPermissionRequest(
                onPermissionGranted = {
                    App(
                        onButtonClicked = {
                            onStartScanningClicked()
                        },
                        webviewComponent = {
                            WebViewComponent(
                                "https://beacon-gallery-frontend.vercel.app/",
                                macAddress = macAddress.value,
                                rssi = rssi.value,
                            )
                        }
                    )
                },
                onPermissionDenied = {
                    Text("No bluetooth permission given")
                }
            )
        },
        onPermissionDenied = {
            Text("No location permission given")
        }
    )
}

@Composable
fun WebViewComponent(
    url: String,
    macAddress: String,
    rssi: Int
) {
    val context = LocalContext.current
    val myWebView = remember(url) {
        MyWebView(context).apply {
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
    }

    LaunchedEffect(macAddress, rssi) {
        myWebView.beaconFound(macAddress, rssi)
    }

    // Assuming you have a WebView instance named 'webView'
    AndroidView(
        modifier = Modifier.padding(top = 50.dp)
            .fillMaxWidth().fillMaxHeight(0.9f),
        factory = {
            myWebView
        },

        )
}

class MyWebView(context: Context) : WebView(context) {
    fun beaconFound(macAddress: String, rssi: Int) {
        evaluateJavascript("beaconFound(\"$macAddress\", $rssi);") { result ->
            // Handle the result if needed
            println("Web view result $result")
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionRequest(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (permissionState.status.isGranted) {
        onPermissionGranted()
    } else if (permissionState.status.shouldShowRationale) {
        onPermissionDenied()
    }

    LaunchedEffect(permissionState) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionRequest(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(Manifest.permission.BLUETOOTH_ADMIN)

    if (permissionState.status.isGranted) {
        onPermissionGranted()
    } else if (permissionState.status.shouldShowRationale) {
        onPermissionDenied()
    }

    LaunchedEffect(permissionState) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}

