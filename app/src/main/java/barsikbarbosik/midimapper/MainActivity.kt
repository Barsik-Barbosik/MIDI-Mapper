package barsikbarbosik.midimapper

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import barsikbarbosik.midimapper.ui.theme.MidiMapperTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var midiManager: MidiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        midiManager = getSystemService(MIDI_SERVICE) as MidiManager

        val viewModel = MidiViewModel(midiManager)

        setContent {
            MidiMapperTheme {
                val devices by viewModel.devices.observeAsState(emptyList())

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DevicesScreen(
                        devices = devices,
                        onConnect = { src, tgt ->
                            viewModel.connectDevices(src, tgt)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/* ---------- ViewModel that handles device discovery and connection ---------- */

class MidiViewModel(private val midiManager: MidiManager) : ViewModel() {

    private val _devices = MutableLiveData<List<MidiDeviceInfo>>(emptyList())
    val devices: LiveData<List<MidiDeviceInfo>> = _devices

    private var inputConnection: MidiInputPort? = null
    private var outputConnection: MidiOutputPort? = null

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = refreshDevices()
        override fun onDeviceRemoved(device: MidiDeviceInfo) = refreshDevices()
    }

    init {
        midiManager.registerDeviceCallback(deviceCallback, null)
        refreshDevices()
    }

    private fun refreshDevices() {
        _devices.postValue(midiManager.devices.toList())
    }

    fun connectDevices(source: MidiDeviceInfo, target: MidiDeviceInfo) {
        // Close previous connections if any
        inputConnection?.close()
        outputConnection?.close()

        midiManager.openDevice(source, object : MidiManager.OnDeviceOpenedListener {
            override fun onDeviceOpened(srcDevice: MidiDevice?) {
                if (srcDevice == null) {
                    Log.e("MidiMapper", "Failed to open source device")
                    return
                }

                midiManager.openDevice(target, object : MidiManager.OnDeviceOpenedListener {
                    override fun onDeviceOpened(tgtDevice: MidiDevice?) {
                        if (tgtDevice == null) {
                            Log.e("MidiMapper", "Failed to open target device")
                            return
                        }

                        try {
                            val outPortIndex = if (srcDevice.info.outputPortCount > 0) 0 else -1
                            val inPortIndex = if (tgtDevice.info.inputPortCount > 0) 0 else -1

                            if (outPortIndex == -1 || inPortIndex == -1) {
                                Log.e("MidiMapper", "Devices do not have compatible ports")
                                return
                            }

                            val srcOut = srcDevice.openOutputPort(outPortIndex)
                            val tgtIn = tgtDevice.openInputPort(inPortIndex)

                            if (srcOut != null && tgtIn != null) {
                                outputConnection = srcOut
                                inputConnection = tgtIn
                                srcOut.connect(tgtIn)
                                Log.i(
                                    "MidiMapper",
                                    "Connected source ${source.id} → target ${target.id}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MidiMapper", "Error connecting devices", e)
                        }
                    }
                }, null)
            }
        }, null)
    }

    override fun onCleared() {
        super.onCleared()
        midiManager.unregisterDeviceCallback(deviceCallback)
        inputConnection?.close()
        outputConnection?.close()
    }
}

/* ---------- Composable UI ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<MidiDeviceInfo>,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }

    var selectedSource by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var selectedTarget by remember { mutableStateOf<MidiDeviceInfo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select Source and Target MIDI Devices", style = MaterialTheme.typography.titleMedium)

        // Source dropdown
        ExposedDropdownMenuBox(
            expanded = expandedSource,
            onExpandedChange = { expandedSource = !expandedSource }
        ) {
            TextField(
                value = selectedSource?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: "Select Source",
                onValueChange = {},
                readOnly = true,
                label = { Text("Source Device") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedSource,
                onDismissRequest = { expandedSource = false }
            ) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                    ?: "Unknown"
                            )
                        },
                        onClick = {
                            selectedSource = device
                            expandedSource = false
                        }
                    )
                }
            }
        }

        // Target dropdown
        ExposedDropdownMenuBox(
            expanded = expandedTarget,
            onExpandedChange = { expandedTarget = !expandedTarget }
        ) {
            TextField(
                value = selectedTarget?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: "Select Target",
                onValueChange = {},
                readOnly = true,
                label = { Text("Target Device") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedTarget,
                onDismissRequest = { expandedTarget = false }
            ) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                    ?: "Unknown"
                            )
                        },
                        onClick = {
                            selectedTarget = device
                            expandedTarget = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (selectedSource != null && selectedTarget != null) {
                    onConnect(selectedSource!!, selectedTarget!!)
                }
            },
            enabled = selectedSource != null && selectedTarget != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect MIDI Devices")
        }
    }
}
