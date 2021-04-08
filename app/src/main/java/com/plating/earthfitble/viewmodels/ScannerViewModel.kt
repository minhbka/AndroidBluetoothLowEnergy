package com.plating.earthfitble.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.location.LocationManager
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.plating.earthfitble.utils.Utils
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class ScannerViewModel(application: Application):AndroidViewModel(application) {

    companion object{
        const val PREFS_FILTER_UUID_REQUIRED = "filter_uuid"
        const val PREFS_FILTER_NEARBY_ONLY = "filter_nearby"
    }


    /**
     * MutableLiveData containing the list of devices.
     */
    private var devicesLiveData: DevicesLiveData? = null

    /**
     * MutableLiveData containing the scanner state.
     */
    private var scannerStateLiveData: ScannerStateLiveData? = null

    private var preferences: SharedPreferences? = null

    fun getDevices(): DevicesLiveData? {
        return devicesLiveData
    }

    fun getScannerState(): ScannerStateLiveData? {
        return scannerStateLiveData
    }

    init {
        Log.d("DEBUG", "Init view model")
        preferences = PreferenceManager.getDefaultSharedPreferences(application)

        val filterUuidRequired: Boolean = isUuidFilterEnabled()
        val filerNearbyOnly: Boolean = isNearbyFilterEnabled()

        scannerStateLiveData = ScannerStateLiveData(
            Utils.isBleEnabled(),
            Utils.isLocationEnabled(application)
        )
        devicesLiveData = DevicesLiveData(filterUuidRequired, filerNearbyOnly)
        registerBroadcastReceivers(application)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(bluetoothStateBroadcastReceiver)
            getApplication<Application>().unregisterReceiver(locationProviderChangedReceiver)
        }
        catch (ex:Exception){
            Log.e("DEBUG", "already unregistered")
        }

    }
    private fun isUuidFilterEnabled(): Boolean {
        return preferences!!.getBoolean(PREFS_FILTER_UUID_REQUIRED, true)
    }
    private fun isNearbyFilterEnabled(): Boolean {
        return preferences!!.getBoolean(PREFS_FILTER_NEARBY_ONLY, false)
    }

    /**
     * Forces the observers to be notified. This method is used to refresh the screen after the
     * location permission has been granted. In result, the observer in
     * [no.nordicsemi.android.blinky.ScannerActivity] will try to start scanning.
     */
    fun refresh() {
        scannerStateLiveData!!.refresh()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param uuidRequired if true, the list will display only devices with Led-Button Service UUID
     * in the advertising packet.
     */
    fun filterByUuid(uuidRequired: Boolean) {
        preferences!!.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply()
        if (devicesLiveData!!.filterByUuid(uuidRequired)) scannerStateLiveData!!.recordFound() else scannerStateLiveData!!.clearRecords()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param nearbyOnly if true, the list will show only devices with high RSSI.
     */
    fun filterByDistance(nearbyOnly: Boolean) {
        preferences!!.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply()
        if (devicesLiveData!!.filterByDistance(nearbyOnly)) scannerStateLiveData!!.recordFound() else scannerStateLiveData!!.clearRecords()
    }

    /**
     * Start scanning for Bluetooth devices.
     */
    fun startScan() {
        if (scannerStateLiveData!!.isScanning()) {
            return
        }

        // Scanning settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(500)
            .setUseHardwareBatchingIfSupported(false)
            .build()
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.startScan(null, settings, scanCallback)
        scannerStateLiveData!!.scanningStarted()
    }

    /**
     * Stop scanning for bluetooth devices.
     */
    fun stopScan() {
        if (scannerStateLiveData!!.isScanning() && scannerStateLiveData!!.isBluetoothEnabled()) {
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.stopScan(scanCallback)
            scannerStateLiveData!!.scanningStopped()
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // This callback will be called only if the scan report delay is not set or is set to 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(
                    getApplication()
                )
            ) Utils.markLocationNotRequired(getApplication())
            if (devicesLiveData!!.deviceDiscovered(result)) {
                devicesLiveData!!.applyFilter()
                scannerStateLiveData!!.recordFound()
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // This callback will be called only if the report delay set above is greater then 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(
                    getApplication()
                )
            ) Utils.markLocationNotRequired(getApplication())
            var atLeastOneMatchedFilter = false
            for (result in results) atLeastOneMatchedFilter = devicesLiveData!!.deviceDiscovered(
                result
            ) || atLeastOneMatchedFilter
            if (atLeastOneMatchedFilter) {
                devicesLiveData!!.applyFilter()
                scannerStateLiveData!!.recordFound()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // TODO This should be handled
            scannerStateLiveData!!.scanningStopped()
        }
    }

    /**
     * Register for required broadcast receivers.
     */
    private fun registerBroadcastReceivers(application: Application) {
        Log.d("DEBUG", "registerReceiver: $application")
        application.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        application.registerReceiver(
            locationProviderChangedReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider.
     */
    private val locationProviderChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = Utils.isLocationEnabled(context)
            scannerStateLiveData?.setUpLocationEnabled(enabled)
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter.
     */
    private val bluetoothStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )
            when (state) {
                BluetoothAdapter.STATE_ON -> scannerStateLiveData!!.bluetoothEnabled()
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    scannerStateLiveData!!.bluetoothDisabled()
                }
            }
        }
    }

}