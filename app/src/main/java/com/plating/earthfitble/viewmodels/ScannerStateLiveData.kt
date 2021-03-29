package com.plating.earthfitble.viewmodels

import androidx.lifecycle.LiveData

class ScannerStateLiveData(
    private var bluetoothEnabled: Boolean,
    private var locationEnabled: Boolean
):LiveData<ScannerStateLiveData>() {
    private var scanningStarted :Boolean = false
    private var hasRecords:Boolean = false
    init {
        postValue(this)
    }

    fun refresh() {
        postValue(this)
    }

    fun scanningStarted(){
        scanningStarted = true
        postValue(this)
    }

    fun scanningStopped(){
        scanningStarted = false
        postValue(this)
    }

    fun bluetoothEnabled() {
        bluetoothEnabled = true
        postValue(this)
    }

    @Synchronized
    fun bluetoothDisabled() {
        bluetoothEnabled = false
        hasRecords = false
        postValue(this)
    }

    fun setUpLocationEnabled(enabled: Boolean) {
        locationEnabled = enabled
        postValue(this)
    }
    fun recordFound() {
        hasRecords = true
        postValue(this)
    }

    /**
     * Returns whether scanning is in progress.
     */
    fun isScanning(): Boolean {
        return scanningStarted
    }

    /**
     * Returns whether any records matching filter criteria has been found.
     */
    fun hasRecords(): Boolean {
        return hasRecords
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothEnabled
    }

    /**
     * Returns whether Location is enabled.
     */
    fun isLocationEnabled(): Boolean {
        return locationEnabled
    }

    /**
     * Notifies the observer that scanner has no records to show.
     */
    fun clearRecords() {
        hasRecords = false
        postValue(this)
    }
}