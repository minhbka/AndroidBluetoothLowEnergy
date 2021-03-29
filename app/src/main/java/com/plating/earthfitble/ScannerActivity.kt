package com.plating.earthfitble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.plating.earthfitble.adapter.DevicesAdapter
import com.plating.earthfitble.databinding.ActivityScannerBinding
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import com.plating.earthfitble.utils.Utils
import com.plating.earthfitble.utils.invisible
import com.plating.earthfitble.utils.show
import com.plating.earthfitble.utils.hide
import com.plating.earthfitble.viewmodels.ScannerStateLiveData
import com.plating.earthfitble.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity(), DevicesAdapter.OnItemClickListener {

    companion object{
        private const val REQUEST_ACCESS_FINE_LOCATION = 1022
    }
    private lateinit var binding:ActivityScannerBinding
    lateinit var scannerViewModel:ScannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =  DataBindingUtil.setContentView(this, R.layout.activity_scanner)
//        binding = ActivityScannerBinding.inflate(layoutInflater)

        scannerViewModel = ViewModelProvider(this).get(ScannerViewModel::class.java)
        scannerViewModel.getScannerState()!!.observe(this, this::startScan)

        binding.recyclerViewBleDevices.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        val animator = binding.recyclerViewBleDevices.itemAnimator
        if (animator is SimpleItemAnimator){
            animator.supportsChangeAnimations = false
        }

        val adapter = DevicesAdapter(this, scannerViewModel.getDevices())
        adapter.setOnItemClickListener(this)
        binding.recyclerViewBleDevices.adapter = adapter

        setUpClickListener()

    }


    private fun setUpClickListener(){
        binding.noDevices.actionEnableLocation.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableIntent)
        }

        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener {
            Utils.markLocationPermissionRequested(this)
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_FINE_LOCATION
            )
        }

        binding.noLocationPermission.actionPermissionSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    override fun onRestart() {
        super.onRestart()
        clear()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        scannerViewModel.stopScan()
    }

    override fun onItemClick(device: DiscoveredBluetoothDevice) {
        println("DEBUG: Selected device: ${device.name}, ${device.address}, ${device.scanResult.scanRecord?.serviceUuids}")
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.putExtra(MainActivity.EXTRA_DEVICE, device)
        startActivity(mainIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION){
            scannerViewModel.refresh()
        }
    }
    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        scannerViewModel.getDevices()!!.clear()
        scannerViewModel.getScannerState()!!.clearRecords()
    }
    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private fun startScan(state: ScannerStateLiveData) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionGranted(this)){

            binding.noLocationPermission.root.hide()

            // Bluetooth must be enabled.
            if (state.isBluetoothEnabled()) {
                binding.bluetoothOff.root.hide()

                // We are now OK to start scanning.
                scannerViewModel.startScan()
                binding.stateScanning.show()
                if (!state.hasRecords()) {
                    binding.noDevices.root.show()

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        binding.noDevices.noLocation.invisible()

                    } else {
                        binding.noDevices.noLocation.show()
                    }
                } else {
                    binding.noDevices.root.hide()
                }
            } else {
                binding.bluetoothOff.root.show()
                binding.stateScanning.invisible()
                binding.noDevices.root.hide()

                clear()
            }
        }
        else {

            binding.noLocationPermission.root.show()
            binding.bluetoothOff.root.hide()
            binding.stateScanning.invisible()
            binding.noDevices.root.hide()


            val deniedForever: Boolean = Utils.isLocationPermissionDeniedForever(this)
            if (deniedForever){
                binding.noLocationPermission.actionGrantLocationPermission.hide()
                binding.noLocationPermission.actionPermissionSettings.show()
            }
            else {
                binding.noLocationPermission.actionGrantLocationPermission.show()
                binding.noLocationPermission.actionPermissionSettings.hide()
            }
        }
    }
}