package com.plating.earthfitble.viewmodels

import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import com.plating.earthfitble.UARTBleManager
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*
import kotlin.collections.ArrayList

class DevicesLiveData(
    var filterUuidRequired: Boolean,
    var filterNearbyOnly: Boolean
): LiveData<List<DiscoveredBluetoothDevice>>() {
    companion object {
        val FILTER_UUID = ParcelUuid(UARTBleManager.SERVICE_UUID)
        val FILTER_NAME = "UART Service"
        const val FILTER_RSSI = -50
    }

    val devices = ArrayList<DiscoveredBluetoothDevice>()
    private var filteredDevices : List<DiscoveredBluetoothDevice> ?= null

    @Synchronized
    fun bluetoothDisabled() {
        devices.clear()
        filteredDevices = null
        postValue(null)
    }
    fun filterByUuid(uuidRequired: Boolean): Boolean {
        filterUuidRequired = uuidRequired
        return applyFilter()
    }


    fun filterByDistance(nearbyOnly: Boolean): Boolean {
        filterNearbyOnly = nearbyOnly
        return applyFilter()
    }

    @Synchronized
    fun deviceDiscovered(result: ScanResult):Boolean{
        var device : DiscoveredBluetoothDevice
        val index = indexOf(result)
        if (index == -1) {
            device = DiscoveredBluetoothDevice(result)
            devices.add(device)
        } else {
            device = devices[index]
        }

        // Update RSSI and name.
        device.update(result)

        // Return true if the device was on the filtered list or is to be added.
        return (filteredDevices != null && filteredDevices!!.contains(device)
                || matchesUuidFilter(result) && matchesNearbyFilter(device.highestRssi))
    }

    @Synchronized
    fun clear() {
        devices.clear()
        filteredDevices = null
        postValue(null)
    }

    @Synchronized
    fun applyFilter(): Boolean {
        val tmp = ArrayList<DiscoveredBluetoothDevice>()
        for (device in devices) {

            val result: ScanResult = device.scanResult

            if (matchesUuidFilter(result) && matchesNearbyFilter(device.highestRssi)) {
                tmp.add(device)
            }
        }
        filteredDevices = tmp
        postValue(filteredDevices)
        return !filteredDevices.isNullOrEmpty()
    }

    private fun indexOf(result: ScanResult): Int {
        for ((i, device) in devices.withIndex()) {
            if (device.matches(result)) return i
        }
        return -1
    }

    private fun matchesUuidFilter(result: ScanResult): Boolean {
        if (!filterUuidRequired) return true

        val record = result.scanRecord ?: return false
        val uuids = record.serviceUuids ?: return false
        return uuids.contains(FILTER_UUID)
    }


    private fun matchesNearbyFilter(rssi: Int): Boolean {
        return if (!filterNearbyOnly) true else rssi >= FILTER_RSSI
    }
}