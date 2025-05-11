package ch.heigvd.iict.dma.labo4.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ch.heigvd.iict.dma.labo4.ble.DMABleManager
import ch.heigvd.iict.dma.labo4.ble.DMAServiceListener
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.*

class BleViewModel(application: Application) : AndroidViewModel(application), DMAServiceListener, ConnectionObserver {

    // PERMISSIONS MANAGEMENT

    private val _blePermissionsGranted = MutableLiveData(false)
    val blePermissionsGranted : LiveData<Boolean> get() = _blePermissionsGranted

    fun blePermissionsGrantedUpdate(granted : Boolean) {
        _blePermissionsGranted.postValue(granted)
    }

    /*
     *  SCAN
     */

    private val _isScanning = MutableLiveData(false)
    val isScanning : LiveData<Boolean> get() = _isScanning

    private val _bleScanResults = MutableLiveData(mutableListOf<ScanResult>())
    val bleScanResults : LiveData<List<ScanResult>> = _bleScanResults.map {  l -> l.toList() }

    fun scanIsActive(isActive : Boolean) {
        _isScanning.postValue(isActive)
    }

    fun addScannedDevice(newScannedDevice : ScanResult) {
        val scanResults = _bleScanResults.value!!
        if(! scanResults.any { it.device.address == newScannedDevice.device.address }) {
            scanResults.add(newScannedDevice)
            _bleScanResults.postValue(scanResults)
        }
    }

    fun clearScannedDevices() {
        val scanResults = _bleScanResults.value!!
        scanResults.clear()
        _bleScanResults.postValue(scanResults)
    }

    /*
     *  LIVE DATA DMA SERVICE
     */
    private val _isConnected = MutableLiveData(false)
    val isConnected : LiveData<Boolean> get() = _isConnected

    private val _currentTime = MutableLiveData<Calendar>(null)
    private val _temperature = MutableLiveData<Float>(null)
    private val _buttonClick = MutableLiveData<Int>(null)

    val currentTime : LiveData<Calendar> get() = _currentTime
    val temperature : LiveData<Float> get() = _temperature
    val buttonClick : LiveData<Int> get() = _buttonClick

    // BLE MANAGER
    private var ble = DMABleManager(application.applicationContext, this).apply {
        connectionObserver = this@BleViewModel
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                .retry(1, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        if (isConnected.value!!)
            ble.requestDisconnection()
    }

    //
    fun setTime(): Boolean {
        if (!isConnected.value!!) return false
        // TODO send a current datetime value
        if (!isConnected.value!!) return false
        val now = Calendar.getInstance()
        return ble.writeCurrentTime(now)
    }

    fun sendValue(value: Int): Boolean {
        if (!isConnected.value!!) return false
        // TODO send a int value
        return ble.writeInt(value)
    }

    fun readTemperature(): Boolean {
        return  if (!isConnected.value!!)
                    false
                else
                    ble.readTemperature()
    }

    /*
     *  DMA service listener
     */
    override fun dateUpdate(date: Calendar) {
        _currentTime.postValue(date)
    }

    override fun temperatureUpdate(temperature: Float) {
        _temperature.postValue(temperature)
    }

    override fun clickCountUpdate(clickCount: Int) {
        _buttonClick.postValue(clickCount)
    }

    /*
     *  Connection Observer
     */
    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d(TAG, "onDeviceConnecting")
        _isConnected.postValue(false)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.d(TAG, "onDeviceConnected")
        _isConnected.postValue(true)
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d(TAG, "onDeviceDisconnecting")
        _isConnected.postValue(false)
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.d(TAG, "onDeviceReady")
    }

    override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
        Log.d(TAG, "onDeviceFailedToConnect")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
        if(reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
            Log.d(TAG, "onDeviceDisconnected - not supported")
            Toast.makeText(getApplication(), "Device not supported - implement method isRequiredServiceSupported()", Toast.LENGTH_LONG).show()
        }
        else
            Log.d(TAG, "onDeviceDisconnected")
        _isConnected.postValue(false)
    }

    /*
     *  ViewModel end of livecycle
     *  we force eventual existing connection to disconnect
     */
    override fun onCleared() {
        super.onCleared()
        if(isConnected.value!!)
            ble.disconnect()
    }

    companion object {
        private val TAG = BleViewModel::class.java.simpleName
    }

}