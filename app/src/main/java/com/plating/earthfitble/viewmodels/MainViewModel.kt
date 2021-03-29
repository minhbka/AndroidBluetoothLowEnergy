package com.plating.earthfitble.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.plating.earthfitble.UARTBleManager
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class MainViewModel(application: Application): AndroidViewModel(application) {
    private val uartBleManager:UARTBleManager = UARTBleManager(getApplication())
    private var device: BluetoothDevice? = null
    fun getConnectionState(): LiveData<ConnectionState> {
        return uartBleManager.state
    }

    fun getReceivedData(): LiveData<Data> {
        return uartBleManager.getReceivedData()
    }

    /**
     * Connect to the given peripheral.
     *
     * @param target the target device.
     */
    fun connect(target: DiscoveredBluetoothDevice) {
        // Prevent from calling again when called again (screen orientation changed).
        if (device == null) {
            device = target.device
//            val logSession = Logger
//                .newSession(getApplication(), null, target.address, target.name!!)
//            uartBleManager.setLogger(logSession)
            reconnect()
        }
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    fun reconnect() {
        if (device != null) {
            uartBleManager.connect(device!!)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun sendMessage(text:String){
        uartBleManager.send(text)
    }

    /**
     * Disconnect from peripheral.
     */
    private fun disconnect() {
        device = null
        uartBleManager.disconnect().enqueue()
    }

    override fun onCleared() {
        super.onCleared()
        if (uartBleManager.isConnected) {
            disconnect()
        }
    }
}