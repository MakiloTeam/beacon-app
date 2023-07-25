import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import android.content.Context
import android.net.http.HttpResponseCache.install
import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.kotlinx.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

actual fun getPlatformName(): String = "Android"


val macAddressAllowList = mutableListOf<String>(
    "FE:15:0B:AB:69:82",
    "F2:D7:51:1A:71:40",
    "C0:AF:DF:CF:3E:70",
    "D7:84:CE:2F:16:0D",
    "C3:EF:DC:C2:CC:D5",
    "FE:55:0C:F6:1D:56",
    "F7:2E:8E:F2:06:FB",
    "EA:68:3B:AA:74:C7",
    "D9:F7:4C:01:3F:A2",
)

class NetworkClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun loadData(): List<ApiBeacon> {
//        return client.get("https://pokeapi.co/api/v2/pokemon/ditto").body()
        return client.get("http://167.99.93.163/api/get/beacon-mac-addresses").body()
    }
}

val client = NetworkClient()

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

@RequiresApi(Build.VERSION_CODES.O)
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
            // load API data
            GlobalScope.launch {
                val data = client.loadData().map {
                    it.macAddress.chunked(2).joinToString(":")
                }
                println(data)
                macAddressAllowList.clear()
                macAddressAllowList.addAll(data)
            }
        }
    }

    LaunchedEffect(macAddress, rssi) {
        myWebView.beaconFound(macAddress, rssi)
    }

    // Assuming you have a WebView instance named 'webView'
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            myWebView
        }
    )
}

@Serializable
data class ApiBeacon(val uuid: String, var macAddress: String)
data class Beacon(val macAddress: String, var rssi: Int, var lastSeen: LocalDateTime)

val beaconList = mutableListOf<Beacon>()

@RequiresApi(Build.VERSION_CODES.O)
fun updateOrAddBeacon(macAddress: String, rssi: Int) {
    beaconList.forEach { it.rssi -= 1 }
    if (!macAddressAllowList.contains(macAddress)) {
        return
    }
    val existingBeacon = beaconList.find { it.macAddress == macAddress }
    if (existingBeacon != null) {
        // Beacon exists, update it
        existingBeacon.lastSeen = LocalDateTime.now()
        if (rssi > existingBeacon.rssi) {
            existingBeacon.rssi = rssi
        } else {
            existingBeacon.rssi -= 1
        }
        if (existingBeacon.rssi < -80) {
            beaconList.remove(existingBeacon)
        }
    } else {
        // Beacon does not exist, add it
        beaconList.add(Beacon(macAddress, rssi, LocalDateTime.now()))
    }
    beaconList.sortByDescending { it.rssi }
}


class MyWebView(context: Context) : WebView(context) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun beaconFound(macAddress: String, rssi: Int) {
        if (macAddress.isEmpty()) return
        updateOrAddBeacon(macAddress, rssi)
        val json = buildString {
            append("[")
            beaconList.forEachIndexed { index, beacon ->
                append(" {\"uuid\": \"${beacon.macAddress}\", \"rssi\": \"${beacon.rssi}\"}")
                if (index != beaconList.size-1) {
                    append(", ")
                }
            }
            append("]")
        }

        println("Web: beaconFound($json)")
        evaluateJavascript("beaconsFound($json);") { result ->
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
    // TODO: confirm which permissions we really need for different OS versions
    val permissionStates = rememberMultiplePermissionsState(
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADMIN)
    )

    if (permissionStates.allPermissionsGranted) {
        onPermissionGranted()
    } else if (permissionStates.allPermissionsGranted) {
        onPermissionDenied()
    }

    LaunchedEffect(permissionStates) {
        if (!permissionStates.allPermissionsGranted) {
            permissionStates.launchMultiplePermissionRequest()
        }
    }
}

