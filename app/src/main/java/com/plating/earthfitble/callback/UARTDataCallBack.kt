package com.plating.earthfitble.callback

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback
import no.nordicsemi.android.ble.data.Data

abstract class UARTDataCallBack: ProfileDataCallback, DataSentCallback{
    override fun onDataSent(device: BluetoothDevice, data: Data) {

    }

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
    }
}