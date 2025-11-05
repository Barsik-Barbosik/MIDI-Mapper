package barsikbarbosik.midimapper

import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import barsikbarbosik.midimapper.ui.theme.MidiMapperTheme

class MainActivity : ComponentActivity() {

    private val model: DevicesViewModel by viewModels()
    private lateinit var midiManager: MidiManager
    private lateinit var midiRouter: MidiRouter

    // keep connection references if you want to disconnect later
    private val activeConnections = mutableStateListOf<MidiRouter.RouteConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        midiManager = getSystemService(MidiManager::class.java)
        midiRouter = MidiRouter(midiManager)

        setContent {
            MidiMapperTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DevicesScreen(model = model, onConnect = { srcInfo, tgtInfo ->
                        connectDevices(srcInfo, tgtInfo)
                    })
                }
            }
        }
    }

    private fun connectDevices(srcInfo: MidiDeviceInfo, tgtInfo: MidiDeviceInfo) {
        // open both devices (async). When both opened, connect port 0->0 by default
        midiRouter.openDevice(srcInfo) { srcOpened ->
            if (srcOpened == null) return@openDevice
            midiRouter.openDevice(tgtInfo) { tgtOpened ->
                if (tgtOpened == null) return@openDevice

                // choose port indices 0 by default (you could choose via UI)
                val srcOutIndex = 0.coerceAtMost(srcOpened.outputPorts.size - 1)
                val tgtInIndex = 0.coerceAtMost(tgtOpened.inputPorts.size - 1)

                val conn = midiRouter.connect(srcOpened, srcOutIndex, tgtOpened, tgtInIndex)
                if (conn != null) {
                    activeConnections.add(conn)
                }
            }
        }
    }

    override fun onDestroy() {
        // cleanup
        activeConnections.forEach { it.disconnect() }
        midiRouter.closeAll()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(model: DevicesViewModel, onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit) {
    val devices by model.devices.observeAsState(emptyList())

    var selectedSourceIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTargetIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Midi Mapper") })
    }) { inner ->
        Column(Modifier
            .padding(inner)
            .padding(8.dp)) {
            Text("Available MIDI devices:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                itemsIndexed(devices) { idx, info ->
                    val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                        ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                        ?: "MidiDevice ${idx}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // toggle selection as source/target by long press logic simplified:
                                // first click selects source, second selects target, third clears source
                                if (selectedSourceIndex == null) selectedSourceIndex = idx
                                else if (selectedTargetIndex == null && selectedSourceIndex != idx) selectedTargetIndex =
                                    idx
                                else if (selectedSourceIndex == idx) selectedSourceIndex = null
                                else if (selectedTargetIndex == idx) selectedTargetIndex = null
                            }
                            .padding(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name)
                            Text(
                                "Inputs:${info.inputPortCount} Outputs:${info.outputPortCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (selectedSourceIndex == idx) {
                            Text("SOURCE", modifier = Modifier.padding(8.dp))
                        } else if (selectedTargetIndex == idx) {
                            Text("TARGET", modifier = Modifier.padding(8.dp))
                        }
                    }
                    Divider()
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                enabled = selectedSourceIndex != null && selectedTargetIndex != null && selectedSourceIndex != selectedTargetIndex,
                onClick = {
                    val src = devices[selectedSourceIndex!!]
                    val tgt = devices[selectedTargetIndex!!]
                    onConnect(src, tgt)
                }
            ) {
                Text("Connect selected source → target")
            }
            Spacer(Modifier.height(8.dp))
            Text("How to select: tap a device to pick source, tap another to pick target. Tap again to unselect.")
        }
    }
}
