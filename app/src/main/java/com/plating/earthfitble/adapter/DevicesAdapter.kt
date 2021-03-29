package com.plating.earthfitble.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.plating.earthfitble.R
import com.plating.earthfitble.ScannerActivity
import com.plating.earthfitble.databinding.DeviceItemBinding
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import com.plating.earthfitble.viewmodels.DevicesLiveData

class DevicesAdapter(
        activity: ScannerActivity,
        devicesLiveData: DevicesLiveData?

):RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private var devices : List<DiscoveredBluetoothDevice>? = null
    private var onItemClickListener : OnItemClickListener? = null
    init {
        setHasStableIds(true)
        devicesLiveData!!.observe(activity, { newDevices ->
            println("NewDevices: $newDevices")
            val result = DiffUtil.calculateDiff(
                    DeviceDiffCallback(devices, newDevices), false)
            devices = newDevices
            result.dispatchUpdatesTo(this)
        })
    }
    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding:DeviceItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.device_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices!![position]
        holder.bind(device)
        holder.binding.deviceContainer.setOnClickListener {
            onItemClickListener!!.onItemClick(device)
        }
    }

    override fun getItemId(position: Int): Long {
        return devices!![position].hashCode().toLong()
    }

    override fun getItemCount() = if (devices.isNullOrEmpty()) 0 else devices!!.size

    fun isEmpty(): Boolean {
        return itemCount == 0
    }

    class ViewHolder(val binding: DeviceItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(device: DiscoveredBluetoothDevice){
            binding.deviceAddress.text = device.address
            binding.deviceName.text = if (device.name.isNullOrEmpty()) "Unknown Device" else device.name
            val rssiPercent = (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f)).toInt()
            binding.rssi.setImageLevel(rssiPercent)
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick(device: DiscoveredBluetoothDevice)
    }
}