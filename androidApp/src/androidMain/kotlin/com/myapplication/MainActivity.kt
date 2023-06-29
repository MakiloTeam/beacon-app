package com.myapplication

import MainView
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MainActivity : AppCompatActivity() {
    private lateinit var beaconDetector: BeaconDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val macAddress = remember { mutableStateOf("") }
            val rssi = remember { mutableStateOf(0) }
            val beaconDetectionCallback = remember {
                object : BeaconDetectionCallback {
                    override fun onBeaconDetected(address: String, rssiValue: Int) {
                        macAddress.value = address
                        rssi.value = rssiValue
                    }
                }
            }
            val beaconDetector = remember {
                BeaconDetector(this, beaconDetectionCallback)
            }
            MainView(
                macAddress = macAddress,
                rssi = rssi,
                onStartScanningClicked = {
                    beaconDetector.startDetection(context)
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconDetector.stopDetection()
    }
}

interface BeaconDetectionCallback {
    fun onBeaconDetected(address: String, rssi: Int)
}

class BeaconDetector(private val context: Context, private val callback: BeaconDetectionCallback) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                // Check if the device is a beacon based on its properties
                if (isBeacon(device)) {
                    // Found a beacon, invoke the callback
                    callback.onBeaconDetected(result.device.address, result.rssi)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("TAG", "onScanFailed $errorCode")
        }
    }

    fun startDetection(context: Context) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)

        if (!bluetoothAdapter?.isEnabled!!) {
            throw Exception("No bluetooth")
        } else {
            startScanning()
        }
    }

    fun stopDetection() {
        context.unregisterReceiver(bluetoothStateReceiver)
        stopScanning()
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilters = mutableListOf<ScanFilter>()
        // TODO: add more devices
        scanFilters.add(
            ScanFilter.Builder()
                .setDeviceAddress("D9:F7:4C:01:3F:A2")
                .build()
        )

        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)

        // Stop scanning after a specified period (e.g., 10 seconds)
        Handler().postDelayed({
            stopScanning()
        }, 20000)
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun isBeacon(device: BluetoothDevice?): Boolean {
        // Implement your beacon detection logic here
        // You can check the device's properties like name, address, etc.
        // and determine if it's a beacon or not
        // For example, you could check the device name or a specific service UUID

        return true
        // TODO: filter by MAC address instead?
//        return device?.name?.contains("MyBeacon") == true
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_ON -> startScanning()
                    BluetoothAdapter.STATE_OFF -> stopScanning()
                }
            }
        }
    }
}