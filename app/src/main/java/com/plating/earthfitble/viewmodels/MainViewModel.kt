package com.plating.earthfitble.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.plating.earthfitble.UARTBleManager
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val cameraBleManager:UARTBleManager = UARTBleManager(getApplication())
    private val loadCellBleManager:UARTBleManager = UARTBleManager(getApplication())

    private var cameraDevice: BluetoothDevice? = null
    private var loadCellDevice: BluetoothDevice? = null

    private var _cameraDeviceLiveData = MutableLiveData<BluetoothDevice>()
    private var _loadCellDeviceLiveData = MutableLiveData<BluetoothDevice>()

    val cameraDeviceLiveData:LiveData<BluetoothDevice>
    get() = _cameraDeviceLiveData
    val loadCellDeviceLiveData:LiveData<BluetoothDevice>
    get() = _loadCellDeviceLiveData

    fun getCameraConnectionState(): LiveData<ConnectionState> {
        return cameraBleManager.state
    }

    fun getCameraReceivedData(): LiveData<Data> {
        return cameraBleManager.getReceivedData()
    }

    fun getLoadCellConnectionState(): LiveData<ConnectionState> {
        return loadCellBleManager.state
    }

    fun getLoadCellReceivedData(): LiveData<Data> {

        return loadCellBleManager.getReceivedData()
    }

    fun getCameraMtu() = cameraBleManager.getCurrentMTU()
    /**
     * Connect to the given peripheral.
     *
     * @param target the target device.
     */
    fun cameraConnect(target: DiscoveredBluetoothDevice) {
        // Prevent from calling again when called again (screen orientation changed).
        if (cameraDevice == null) {
            cameraDevice = target.device
            _cameraDeviceLiveData.postValue(target.device)
            cameraReconnect()
        }
    }

    fun loadCellConnect(target: DiscoveredBluetoothDevice){
        if (loadCellDevice == null){
            loadCellDevice = target.device
            _loadCellDeviceLiveData.postValue(target.device)
            loadCellReconnect()
        }
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    private fun cameraReconnect() {
        if (cameraDevice != null) {
            cameraBleManager.connect(cameraDevice!!)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()

        }
    }
    private fun loadCellReconnect(){
        if (loadCellDevice != null) {
            loadCellBleManager.connect(loadCellDevice!!)
                    .retry(3, 100)
                    .useAutoConnect(false)
                    .enqueue()
        }
    }

    fun sendMessageToCamera(text:String){
        cameraBleManager.send(text)
    }
    fun sendMessageToLoadCell(text:String){
        loadCellBleManager.send(text)
    }

    /**
     * Disconnect from peripheral.
     */
    fun cameraDisconnect() {
        cameraDevice = null
        cameraBleManager.disconnect().enqueue()
    }
    fun loadCellDisconnect(){
        loadCellDevice = null
        loadCellBleManager.disconnect().enqueue()
    }

    override fun onCleared() {
        super.onCleared()
        if (cameraBleManager.isConnected) {
            cameraDisconnect()
        }
        if (loadCellBleManager.isConnected){
            loadCellDisconnect()
        }
    }
}