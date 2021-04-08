package com.plating.earthfitble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.MtuCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.util.*


open class UARTBleManager(context: Context): ObservableBleManager(context) {
    companion object{
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24D00488")
        val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24D00488")
        val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24D00488")
    }
    private var useLongWrite = true
    private val receivedData:MutableLiveData<Data> = MutableLiveData()
    private var supported = false
    // Server characteristics

    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic:BluetoothGattCharacteristic? = null
    fun getReceivedData()  =  receivedData
    override fun getGattCallback(): BleManagerGattCallback {
        return UARTManagerGattCallback()
    }

    private val receivedCallback = DataReceivedCallback { device, data ->
        receivedData.value = data
    }

    private val mtuChangedCallback = MtuCallback { device, mtu ->
        Log.d("DEBUG", "Device: ${device.name} with MTU: $mtu")
    }


    override fun shouldClearCacheWhenDisconnected(): Boolean {
        return !supported
    }

    protected inner class UARTManagerGattCallback: BleManagerGattCallback(){

        override fun initialize() {
            Log.d("DEBUG", "initialize")
            setNotificationCallback(txCharacteristic)
                .with (receivedCallback)

            enableNotifications(txCharacteristic).enqueue()
            requestMtu(250).with(mtuChangedCallback).enqueue()
            requestMtu(260).with(mtuChangedCallback).enqueue()
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service : BluetoothGattService? = gatt.getService(SERVICE_UUID)
            if (service != null){
                rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID_RX)
                txCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID_TX)
            }
            var writeRequest =  false
            var writeCommand = false
            if (rxCharacteristic != null){
                val rxProperties = rxCharacteristic!!.properties
                writeRequest = (rxProperties and  BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
                writeCommand = (rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) >0

                if (writeRequest)
                    rxCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                else
                    useLongWrite = false
            }
            supported = rxCharacteristic != null && txCharacteristic != null && (writeRequest || writeCommand)
            return supported
        }


        override fun onDeviceDisconnected() {
            rxCharacteristic = null
            txCharacteristic = null
            useLongWrite = true
        }

    }
    fun getCurrentMTU() = mtu

    fun send(text: String?){
        if (rxCharacteristic == null)
            return
        if (!text.isNullOrEmpty()){
            val request = writeCharacteristic(rxCharacteristic, text.toByteArray())
                    .with { device, data ->
                        println("BLE_DEBUG: Sent ${data.getStringValue(0)}")
                    }
            if (!useLongWrite){
                request.split()
            }
            request.enqueue()
        }
    }


}
