package com.plating.earthfitble

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.plating.earthfitble.databinding.ActivityMainBinding
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import com.plating.earthfitble.viewmodels.MainViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    companion object{
        const val EXTRA_DEVICE = "com.plating.earthfitble.EXTRA_DEVICE"
        const val BUFFER_SIZE = 1024*1024
    }
    lateinit var binding:ActivityMainBinding
    lateinit var mainViewModel:MainViewModel
    private var fileByteArray = ByteArray(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val intent = intent
        val device: DiscoveredBluetoothDevice? =
            intent.getParcelableExtra(EXTRA_DEVICE)
        val deviceName: String? = device?.name
        val deviceAddress: String? = device?.address
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        if (device!=null){
            mainViewModel.connect(device)
        }

        mainViewModel.getConnectionState().observe(this, { state ->
            when (state.state) {
                ConnectionState.State.CONNECTING -> {
                    Log.d("DEBUG", getString(R.string.state_connecting))
                }
                ConnectionState.State.INITIALIZING -> Log.d("DEBUG", getString(R.string.state_initializing))
                ConnectionState.State.READY -> {
                    Log.d("DEBUG", "State Ready")
                    onConnectionStateChanged(true)
                }
                ConnectionState.State.DISCONNECTED -> {
                    Log.d("DEBUG", "State Disconnected")
                    if (state is ConnectionState.Disconnected) {
                        val stateWithReason: ConnectionState.Disconnected =
                            state as ConnectionState.Disconnected
                        onConnectionStateChanged(false)
                        Log.d("DEBUG", "Disconnect Reason: ${stateWithReason.isLinkLoss}, ${stateWithReason.isTimeout}, ${stateWithReason.isNotSupported}")
                        if (stateWithReason.isNotSupported) {
                            Log.d("DEBUG", "State not supported")
                        }
                        else if (stateWithReason.isTimeout){
                            this@MainActivity.onBackPressed()
                        }
                    }

                }
                ConnectionState.State.DISCONNECTING -> {
                    Log.d("DEBUG", "State DISCONNECTING")
                    onConnectionStateChanged(false)
                }
            }

        })

        mainViewModel.getReceivedData().observe(this, {data->
            if (!data.getStringValue(0)!!.contains("FINISH SEND FILE")){
                data.value?.let {
                    fileByteArray += data.value!!
                }
            }
            else{
                Log.d("DEBUG", data.getStringValue(0)!!)
                Log.d("DEBUG", "FileSize: ${fileByteArray.size}")
                writeBytesAsJPG(fileByteArray)
            }

        })

        binding.sendHello.setOnClickListener {
            mainViewModel.sendMessage("HELLO JONGHWA")
        }

        binding.askDeviceToSend.setOnClickListener {
            fileByteArray = ByteArray(0)
            mainViewModel.sendMessage("SEND")
        }
    }

    private fun onConnectionStateChanged(connected: Boolean) {

    }

    private fun writeBytesAsJPG(bytes : ByteArray) {
        Log.d("DEBUG", "FileSize: ${bytes.size}")

        val myExternalFile:File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"picture.jpg")
        try {
            Log.d("DEBUG", "FileExist: ${myExternalFile.exists()}")
            Log.d("DEBUG", "FileSize: ${myExternalFile.path}")
            val os = FileOutputStream(myExternalFile)
            os.write(bytes)
            os.close()
        }
        catch (ex:Exception){
            Log.d("DEBUG", "Error: $ex")
        }

    }
}