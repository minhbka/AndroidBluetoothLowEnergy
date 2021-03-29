package com.plating.earthfitble.adapter

import androidx.recyclerview.widget.DiffUtil
import com.plating.earthfitble.model.DiscoveredBluetoothDevice

class DeviceDiffCallback(
    private val oldList: List<DiscoveredBluetoothDevice>?,
    private val newList: List<DiscoveredBluetoothDevice>?
):DiffUtil.Callback() {
    override fun getOldListSize() = if (oldList.isNullOrEmpty()) 0 else oldList.size

    override fun getNewListSize() = if (newList.isNullOrEmpty()) 0 else newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldList == null || newList == null) return false
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldList == null) return false
        val device = oldList[oldItemPosition]
        return device.hasRssiLevelChanged()
    }
}