package barsikbarbosik.midimapper

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MidiViewModel(private val context: Context) : ViewModel() {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private val _devices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    val devices: StateFlow<List<MidiDeviceInfo>> = _devices.asStateFlow()

    private val _midiConfig = MutableStateFlow(MidiConfig())
    val midiConfig: StateFlow<MidiConfig> = _midiConfig.asStateFlow()

    private val _learningKnobIndex = MutableStateFlow<Int?>(null)

    private val _learnedCc = MutableStateFlow<Int?>(null)
    val learnedCc: StateFlow<Int?> = _learnedCc.asStateFlow()

    private val _knobValues = MutableStateFlow(List(20) { 0 })
    val knobValues: StateFlow<List<Int>> = _knobValues.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Not Connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var sourceOutputPort: MidiOutputPort? = null
    private var targetInputPort: MidiInputPort? = null

    val messageReceiver: MidiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            viewModelScope.launch {
                val learningIndex = _learningKnobIndex.value
                if (learningIndex != null) {
                    if (msg[offset].toInt() and 0xF0 == 0xB0) { // Control Change
                        val cc = msg[offset + 1].toInt()
                        _learnedCc.value = cc
                        updateKnobSetting(
                            learningIndex,
                            _midiConfig.value.knobSettings[learningIndex].copy(cc = cc)
                        )
                        _learningKnobIndex.value = null
                    }
                } else {
                    if (msg[offset].toInt() and 0xF0 == 0xB0) { // Control Change
                        val cc = msg[offset + 1].toInt()
                        val value = msg[offset + 2].toInt()
                        val knobIndex = _midiConfig.value.knobSettings.indexOfFirst { it.cc == cc }
                        if (knobIndex != -1) {
                            onKnobValueChange(knobIndex, value)
                        }
                    }
                }
            }
        }
    }

    init {
        _devices.value = midiManager.devices.toList()

        midiManager.registerDeviceCallback(object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                _devices.value = midiManager.devices.toList()
            }

            override fun onDeviceRemoved(device: MidiDeviceInfo) {
                _devices.value = midiManager.devices.toList()
            }
        }, null)

        viewModelScope.launch {
            loadMidiConfig("default.json")
        }
    }

    fun connect(sourceDevice: MidiDeviceInfo, targetDevice: MidiDeviceInfo) {
        val sourcePort = sourceDevice.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
        midiManager.openDevice(sourceDevice, { device ->
            sourceOutputPort = device.openOutputPort(sourcePort.portNumber)
            val framer = MidiFramer(messageReceiver)
            sourceOutputPort?.connect(framer)
            _connectionStatus.value =
                "Connected to ${sourceDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME)} and ${
                    targetDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                }"
        }, null)

        val targetPort = targetDevice.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }
        midiManager.openDevice(targetDevice, { device ->
            targetInputPort = device.openInputPort(targetPort.portNumber)
        }, null)
    }

    fun disconnect() {
        sourceOutputPort?.close()
        targetInputPort?.close()
        sourceOutputPort = null
        targetInputPort = null
        _connectionStatus.value = "Not Connected"
    }

    fun startLearning(knobIndex: Int) {
        _learningKnobIndex.value = knobIndex
        _learnedCc.value = null
    }

    fun updateKnobSetting(knobIndex: Int, newSettings: KnobSettings) {
        val currentConfig = _midiConfig.value
        val newKnobSettings = currentConfig.knobSettings.toMutableList()
        if (knobIndex >= 0 && knobIndex < newKnobSettings.size) {
            newKnobSettings[knobIndex] = newSettings
        } else if (knobIndex == newKnobSettings.size) {
            newKnobSettings.add(newSettings)
        }
        _midiConfig.value = currentConfig.copy(knobSettings = newKnobSettings)
    }

    fun onKnobValueChange(knobIndex: Int, value: Int) {
        val currentKnobValues = _knobValues.value.toMutableList()
        currentKnobValues[knobIndex] = value
        _knobValues.value = currentKnobValues
        sendMidiMessage(knobIndex, value)
    }

    private fun sendMidiMessage(knobIndex: Int, value: Int) {
        val knobSetting = _midiConfig.value.knobSettings.getOrNull(knobIndex) ?: return
        val sysexString = knobSetting.sysex
        if (sysexString.isNotBlank()) {
            // Simple placeholder replacement
            val finalSysexString = sysexString.replace("vv", "%02x".format(value))
            val sysexBytes = finalSysexString.split(" ").map { it.toInt(16).toByte() }.toByteArray()
            targetInputPort?.send(sysexBytes, 0, sysexBytes.size)
        }
    }

    fun loadMidiConfig(configName: String) {
        _midiConfig.value = SettingsManager.loadSettings(context, configName)
    }

    fun getAvailableConfigs(): List<String> {
        return SettingsManager.getAvailableConfigs(context)
    }

    fun saveMidiConfig() {
        SettingsManager.saveSettings(context, _midiConfig.value)
    }

    fun updateConfigName(name: String) {
        _midiConfig.value = _midiConfig.value.copy(configName = name)
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
