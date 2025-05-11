package ch.heigvd.iict.dma.labo4

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.commit
import ch.heigvd.iict.dma.labo4.databinding.ActivityMainBinding
import ch.heigvd.iict.dma.labo4.ui.BleConnectedFragment
import ch.heigvd.iict.dma.labo4.ui.BleScanFragment
import ch.heigvd.iict.dma.labo4.viewmodels.BleViewModel

class MainActivity : AppCompatActivity() {

    private var handler = Handler(Looper.getMainLooper())

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val bleViewModel : BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // we request permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBlePermissionLauncher.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else {
            requestBlePermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN))
        }

        // manage ui - 2 modes :
        // - if not connected to a device, we display "scan" fragment
        // - when connected we display "connected" fragment
        bleViewModel.isConnected.observe(this) {isConnected ->
            if(isConnected)
                supportFragmentManager.commit {
                    replace(R.id.main_fragment, BleConnectedFragment.newInstance())
                }
            else
                supportFragmentManager.commit {
                    replace(R.id.main_fragment, BleScanFragment.newInstance())
                }
        }

    }

    override fun onPause() {
        super.onPause()
        if (bleViewModel.isScanning.value!!) scanLeDevice(enable = false, automatic = true)
        if (isFinishing) bleViewModel.disconnect()
    }

    private val requestBlePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

        val isBLEGranted =  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false) &&
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false)
        else
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) &&
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
            permissions.getOrDefault(Manifest.permission.BLUETOOTH, false) &&
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_ADMIN, false)

        bleViewModel.blePermissionsGrantedUpdate(isBLEGranted)

    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice(enable: Boolean, automatic : Boolean = false) {
        val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (enable) {
            //reset display
            bleViewModel.clearScannedDevices()

            //config
            val builderScanSettings = ScanSettings.Builder()
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            builderScanSettings.setReportDelay(0)

            // we scan for any BLE device
            // we don't filter them based on advertised services...

            // TODO ajouter un filtre pour n'afficher que les devices proposant
            // le service "SYM" (UUID: "3c0a1000-281d-4b48-b2a7-f15579a1c38f")

            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")).build()

            bluetoothScanner.startScan(listOf(filter), builderScanSettings.build(), leScanCallback)
            Log.d(TAG, "Start scanning...")
            bleViewModel.scanIsActive(true)

            //we scan only for 15 seconds
            handler.postDelayed({ scanLeDevice(enable = false, automatic = true) }, 15 * 1000L)
        }
        else {
            if(automatic)
                Log.d(TAG, "Stop scanning (automatic)")
            else
                Log.d(TAG, "Stop scanning (manual)")

            bluetoothScanner.stopScan(leScanCallback)
            bleViewModel.scanIsActive(false)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread { bleViewModel.addScannedDevice(result) }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}