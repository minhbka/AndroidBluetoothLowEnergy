package com.plating.earthfitble.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.plating.earthfitble.R
import com.plating.earthfitble.adapter.DevicesAdapter
import com.plating.earthfitble.databinding.DeviceScanDialogBinding
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import com.plating.earthfitble.utils.Utils
import com.plating.earthfitble.viewmodels.ScannerStateLiveData
import com.plating.earthfitble.viewmodels.ScannerViewModel

class ScanDialogFragment:DialogFragment(), DevicesAdapter.OnItemClickListener {
    private var _binding:DeviceScanDialogBinding?=null
    private val binding get() = _binding!!
    lateinit var viewModel:ScannerViewModel
    lateinit var devicesAdapter: DevicesAdapter
    private var mCallBack : DeviceSelectedInterface?=null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DeviceScanDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ScannerViewModel::class.java)
        viewModel.getScannerState()!!.observe(this, this::startScan)
        try {
            mCallBack = activity as DeviceSelectedInterface
        }
        catch (ex:Exception){
            Log.d("DEBUG","Activity doesn't implement the ISelectedData interface")
        }

        val adapter = DevicesAdapter(this, viewModel.getDevices())
        adapter.setOnItemClickListener(this)
        binding.devicesRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this.requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        val animator = binding.devicesRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator){
            animator.supportsChangeAnimations = false
        }
        binding.devicesRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        clear()
        val params = dialog?.window!!.attributes
        val metrics = (context)?.resources?.displayMetrics!!
        val dialogHeight = metrics.heightPixels - (metrics.density * (100)).toInt()
        val dialogWidth = metrics.widthPixels - (metrics.density * (50)).toInt()
        params.height = dialogHeight
        params.width = dialogWidth


        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes = params as WindowManager.LayoutParams
        }
    }
    override fun onItemClick(device: DiscoveredBluetoothDevice) {
        Log.d("DEBUG", "Selected device: ${device.name}, ${device.address}, ${device.scanResult.scanRecord?.serviceUuids}")
        mCallBack?.onDeviceSelected(device)
        this.dismiss()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        viewModel.stopScan()
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        viewModel.getDevices()!!.clear()
        viewModel.getScannerState()!!.clearRecords()
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private fun startScan(state: ScannerStateLiveData) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionGranted(this.requireContext())){

            // binding.noLocationPermission.root.hide()

            // Bluetooth must be enabled.
            if (state.isBluetoothEnabled()) {
                //binding.bluetoothOff.root.hide()

                // We are now OK to start scanning.
                viewModel.startScan()
                //binding.stateScanning.show()
                if (!state.hasRecords()) {
                    //binding.noDevices.root.show()

                    if (!Utils.isLocationRequired(this.requireContext()) || Utils.isLocationEnabled(this.requireContext())) {
                        //binding.noDevices.noLocation.invisible()

                    } else {
                        //binding.noDevices.noLocation.show()
                    }
                } else {
                    // .noDevices.root.hide()
                }
            } else {
                //binding.bluetoothOff.root.show()
                //binding.stateScanning.invisible()
                //binding.noDevices.root.hide()

                clear()
            }
        }
        else {

            //binding.noLocationPermission.root.show()
            //binding.bluetoothOff.root.hide()
            //binding.stateScanning.invisible()
            //binding.noDevices.root.hide()


            val deniedForever: Boolean = Utils.isLocationPermissionDeniedForever(this.requireActivity())
            if (deniedForever){
                //binding.noLocationPermission.actionGrantLocationPermission.hide()
                //binding.noLocationPermission.actionPermissionSettings.show()
            }
            else {
                //binding.noLocationPermission.actionGrantLocationPermission.show()
                //binding.noLocationPermission.actionPermissionSettings.hide()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface DeviceSelectedInterface {
    fun onDeviceSelected(device: DiscoveredBluetoothDevice)
}