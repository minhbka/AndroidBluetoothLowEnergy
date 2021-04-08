package com.plating.earthfitble

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.plating.earthfitble.databinding.ActivityMainBinding
import com.plating.earthfitble.model.DiscoveredBluetoothDevice
import com.plating.earthfitble.viewmodels.MainViewModel
import kotlinx.coroutines.tasks.await
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity(), DeviceSelectedInterface {
    companion object{
        const val EXTRA_DEVICE = "com.plating.earthfitble.EXTRA_DEVICE"
        const val BUFFER_SIZE = 1024*1024
        const val CAMERA_BLE_NAME = "Camera BLE"
        const val LOAD_CELL_BLE_NAME = "Load Cell BLE"
    }
    private lateinit var auth: FirebaseAuth
    lateinit var binding:ActivityMainBinding
    lateinit var mainViewModel:MainViewModel
    private var fileByteArray = ByteArray(0)
    private var fileName = ""
    private val storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        mainViewModel.getCameraConnectionState().observe(this, { cameraConnectionState ->

            when (cameraConnectionState.state) {
                ConnectionState.State.CONNECTING -> {
                    Log.d("DEBUG", "Camera ${getString(R.string.state_connecting)}")
                }
                ConnectionState.State.INITIALIZING -> Log.d("DEBUG", "Camera ${getString(R.string.state_initializing)}")
                ConnectionState.State.READY -> {
                    Log.d("DEBUG", "Camera Ready, mtu: ${mainViewModel.getCameraMtu()}")

                    onCameraConnectionStateChanged(true)
                }
                ConnectionState.State.DISCONNECTED -> {
                    Log.d("DEBUG", "Camera Disconnected")
                    if (cameraConnectionState is ConnectionState.Disconnected) {
                        val stateWithReason: ConnectionState.Disconnected =
                            cameraConnectionState as ConnectionState.Disconnected
                        onCameraConnectionStateChanged(false)
                        Log.d("DEBUG", "Camera disconnect Reason: ${stateWithReason.isLinkLoss}, ${stateWithReason.isTimeout}, ${stateWithReason.isNotSupported}")
                        if (stateWithReason.isNotSupported) {
                            Log.d("DEBUG", "Camera state not supported")
                        }
                        else if (stateWithReason.isTimeout){
                            mainViewModel.cameraDisconnect()
                        }
                    }

                }
                ConnectionState.State.DISCONNECTING -> {
                    Log.d("DEBUG", "Camera disconnecting")
                    onCameraConnectionStateChanged(false)
                }
            }

        })

        mainViewModel.getLoadCellConnectionState().observe(this, { loadCellConnectionState ->
            when (loadCellConnectionState.state) {
                ConnectionState.State.CONNECTING -> {
                    Log.d("DEBUG", "Load Cell ${getString(R.string.state_connecting)}")
                }
                ConnectionState.State.INITIALIZING -> Log.d("DEBUG", "Load Cell ${getString(R.string.state_initializing)}")
                ConnectionState.State.READY -> {
                    Log.d("DEBUG", "Load Cell Ready")

                    onLoadCellConnectionStateChanged(true)
                }
                ConnectionState.State.DISCONNECTED -> {
                    Log.d("DEBUG", "Load Cell Disconnected")
                    if (loadCellConnectionState is ConnectionState.Disconnected) {
                        val stateWithReason: ConnectionState.Disconnected =
                                loadCellConnectionState as ConnectionState.Disconnected
                        onLoadCellConnectionStateChanged(false)
                        Log.d("DEBUG", "Load Cell disconnect reason: ${stateWithReason.isLinkLoss}, ${stateWithReason.isTimeout}, ${stateWithReason.isNotSupported}")
                        if (stateWithReason.isNotSupported) {
                            Log.d("DEBUG", "Load Cell state not supported")
                        }
                        else if (stateWithReason.isTimeout){
                            mainViewModel.loadCellDisconnect()
                        }
                    }

                }
                ConnectionState.State.DISCONNECTING -> {
                    Log.d("DEBUG", "Load Cell disconnecting")
                    onLoadCellConnectionStateChanged(false)
                }
            }
        })
        var segmentIndex = 0

        mainViewModel.getCameraReceivedData().observe(this, {data->
            Log.d("DEBUG", "segmentIndex: ${segmentIndex++}, Size: ${data.value?.size}")
            if(data.getStringValue(0)!!.startsWith("FILE/")){
                fileName = data.getStringValue(0)!!.substring(5)
                Log.d("DEBUG", "fileName: ${data.getStringValue(0)!!}")
            }
            else if (!data.getStringValue(0)!!.contains("FINISH SEND")){
                data.value?.let {
                    fileByteArray += data.value!!
                }
            }

            else{
                writeBytesAsJPG(fileByteArray)
                segmentIndex = 0
                fileByteArray = ByteArray(0)
            }

        })
        mainViewModel.getLoadCellReceivedData().observe(this, {
            it?.let { data ->
                Log.d("DEBUG", "Load Cell: ${data.getStringValue(0)!!}")
            }
        })

        binding.sendHelloCamera.setOnClickListener {
            mainViewModel.sendMessageToCamera("TAKE:${getDateString()}")
        }
        binding.sendHelloLoadCell.setOnClickListener {
            mainViewModel.sendMessageToLoadCell("HELLO LOADCELL")
        }
        binding.askCameraToSend.setOnClickListener {
            fileByteArray = ByteArray(0)
            segmentIndex = 0
            mainViewModel.sendMessageToCamera("SEND")
        }
        binding.askLoadCellToSend.setOnClickListener {
            mainViewModel.sendMessageToLoadCell("SEND")
        }
        binding.scanButton.setOnClickListener {
            val fm = supportFragmentManager
            ScanDialogFragment().show(fm, null)
        }
        binding.cameraDisconnect.setOnClickListener {
            mainViewModel.cameraDisconnect()
            onCameraConnectionStateChanged(false)
        }
        binding.loadCellDisconnect.setOnClickListener {
            mainViewModel.loadCellDisconnect()
            onLoadCellConnectionStateChanged(false)
        }

    }

    override fun onStart() {
        super.onStart()
        Log.d("DEBUG", "On start")
        auth = Firebase.auth
        auth.signInWithEmailAndPassword("minh@plating.co.kr", "helloworld").addOnCompleteListener {
            if (it.isSuccessful){
                Log.d("DEBUG", "Login Success")
            }
            else{
                Log.e("DEBUG", "Login Failed: ${it.exception}")
            }
        }
    }
    private fun getDateString():String{
        val tz = TimeZone.getTimeZone("GMT+09:00")
        val c = Calendar.getInstance(tz)
        return "${c.get(Calendar.YEAR)}"+ String.format("%02d", c.get(Calendar.MONTH)+1) +
        String.format("%02d", c.get(Calendar.DAY_OF_MONTH)) +
        String.format("%02d" , c.get(Calendar.HOUR_OF_DAY)) +
        String.format("%02d" , c.get(Calendar.MINUTE)) +
        String.format("%02d" , c.get(Calendar.SECOND))
    }
    override fun onDeviceSelected(device: DiscoveredBluetoothDevice) {
        val deviceName: String? = device.name
        val deviceAddress: String = device.address
        if (deviceName == CAMERA_BLE_NAME) mainViewModel.cameraConnect(device)
        else if (deviceName == LOAD_CELL_BLE_NAME) mainViewModel.loadCellConnect(device)

    }
    private fun onCameraConnectionStateChanged(connected: Boolean) {
        binding.sendHelloCamera.isEnabled = connected
        binding.askCameraToSend.isEnabled = connected
        binding.cameraDisconnect.isEnabled = connected
    }

    private fun onLoadCellConnectionStateChanged(connected: Boolean){
        binding.sendHelloLoadCell.isEnabled = connected
        binding.askLoadCellToSend.isEnabled = connected
        binding.loadCellDisconnect.isEnabled = connected
    }

    private fun writeBytesAsJPG(bytes : ByteArray) {
        Log.d("DEBUG", "FileSize: ${bytes.size}")
        val currentSecond = System.currentTimeMillis()/1000

        val writeFile = if (fileName.isBlank()){
            "picture_$currentSecond.jpg"
        } else{
            "picture_$fileName"
        }
        val myExternalFile:File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),writeFile)
        try {

            Log.d("DEBUG", "FilePath: ${myExternalFile.path}")
            val os = FileOutputStream(myExternalFile)
            os.write(bytes)
            os.close()
            fileName = ""
            val fileUri = Uri.fromFile(myExternalFile)
            val uploadRef =  storage.reference.child("test/$writeFile")
            uploadRef.putFile(fileUri, storageMetadata {
                contentType = "image/jpg"
            })

        }
        catch (ex:Exception){
            fileName = ""
            Log.d("DEBUG", "Error: $ex")
        }

    }
}