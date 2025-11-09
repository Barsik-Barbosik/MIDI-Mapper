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

    private val _knobValue = MutableStateFlow(64)
    val knobValue = _knobValue.asStateFlow()

    private val _learnedCcNumber = MutableStateFlow<Int?>(null)
    val learnedCcNumber = _learnedCcNumber.asStateFlow()

    private val _learningMode = MutableStateFlow(false)
    val learningMode = _learningMode.asStateFlow()

    private val _customSysExMessage =
        MutableStateFlow("F04419017F010203000000000000000000006A0100000000%VF7")
    val customSysExMessage = _customSysExMessage.asStateFlow()

    fun setCustomSysExMessage(message: String) {
        _customSysExMessage.value = message
    }

    fun toggleLearningMode() {
        _learningMode.value = !_learningMode.value
    }

    fun setKnobValue(newValue: Int) {
        _knobValue.value = newValue
        inputConnection?.let { inPort ->
            val hexValue = String.format("%02X", newValue)
            val sysExString = _customSysExMessage.value.replace("%V", hexValue)
            if (sysExString.isNotBlank()) {
                try {
                    val sysExMsg = hexStringToByteArray(sysExString)
                    inPort.send(sysExMsg, 0, sysExMsg.size)
                } catch (e: Exception) {
                    Log.e("MidiMapper", "Error sending SysEx message", e)
                }
            }
        }
    }

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
                                        if (count > 2 && data[offset].toInt() and 0xF0 == 0xB0) {
                                            val ccNumber = data[offset + 1].toInt()
                                            val ccValue = data[offset + 2].toInt()

                                            if (_learningMode.value) {
                                                _learnedCcNumber.value = ccNumber
                                                _knobValue.value = ccValue
                                                _learningMode.value = false
                                                return // Consume the message
                                            } else if (ccNumber == _learnedCcNumber.value) {
                                                setKnobValue(ccValue) // Update knob and send SysEx
                                                return // Consume the original CC message
                                            }
                                        }
                                        // Forward all other messages
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
            _learnedCcNumber.value = null
            _learningMode.value = false
        } catch (e: Exception) {
            Log.e("MidiMapper", "Error disconnecting", e)
        } finally {
            inputConnection = null
            outputConnection = null
            srcDevice = null
            tgtDevice = null
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] =
                ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }
}
