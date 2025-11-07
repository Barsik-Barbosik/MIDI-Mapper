package barsikbarbosik.midimapper

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MidiViewModel(context: Context) : ViewModel() {

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private val _devices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _connectionStatus = MutableStateFlow("No connection")
    val connectionStatus = _connectionStatus.asStateFlow()

    private var inputConnection: MidiInputPort? = null
    private var outputConnection: MidiOutputPort? = null
    private var srcDevice: MidiDevice? = null
    private var tgtDevice: MidiDevice? = null

    init {
        refreshDevices()
        midiManager.registerDeviceCallback(object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo?) = refreshDevices()
            override fun onDeviceRemoved(device: MidiDeviceInfo?) = refreshDevices()
        }, null)
    }

    private fun refreshDevices() {
        _devices.value = midiManager.devices.toList()
    }

    fun connectDevices(source: MidiDeviceInfo, target: MidiDeviceInfo) {
        disconnectDevices() // ensure no stale connections

        midiManager.openDevice(source, object : MidiManager.OnDeviceOpenedListener {
            override fun onDeviceOpened(src: MidiDevice?) {
                if (src == null) {
                    Log.e("MidiMapper", "Failed to open source")
                    return
                }
                srcDevice = src
                midiManager.openDevice(target, object : MidiManager.OnDeviceOpenedListener {
                    override fun onDeviceOpened(tgt: MidiDevice?) {
                        if (tgt == null) {
                            Log.e("MidiMapper", "Failed to open target")
                            return
                        }
                        tgtDevice = tgt
                        try {
                            val outPort = src.openOutputPort(0)
                            val inPort = tgt.openInputPort(0)
                            if (outPort != null && inPort != null) {
                                val framer = MidiFramer(object : MidiReceiver() {
                                    override fun onSend(
                                        data: ByteArray,
                                        offset: Int,
                                        count: Int,
                                        timestamp: Long
                                    ) {
                                        inPort.send(data, offset, count, timestamp)
                                    }
                                })
                                outPort.connect(framer)
                                outputConnection = outPort
                                inputConnection = inPort
                                _connectionStatus.value =
                                    "Connected: ${source.properties.getString(MidiDeviceInfo.PROPERTY_NAME)} → ${
                                        target.properties.getString(
                                            MidiDeviceInfo.PROPERTY_NAME
                                        )
                                    }"
                                Log.i("MidiMapper", _connectionStatus.value)
                            }
                        } catch (e: Exception) {
                            Log.e("MidiMapper", "Error connecting", e)
                        }
                    }
                }, null)
            }
        }, null)
    }

    fun disconnectDevices() {
        try {
            outputConnection?.close()
            inputConnection?.close()
            srcDevice?.close()
            tgtDevice?.close()
            _connectionStatus.value = "No connection"
        } catch (e: Exception) {
            Log.e("MidiMapper", "Error disconnecting", e)
        } finally {
            inputConnection = null
            outputConnection = null
            srcDevice = null
            tgtDevice = null
        }
    }
}
