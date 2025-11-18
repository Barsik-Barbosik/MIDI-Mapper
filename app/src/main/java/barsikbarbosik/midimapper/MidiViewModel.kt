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

    private val _learningKnobIndex = MutableStateFlow<Pair<Int, Int>?>(null)

    private val _learnedCc = MutableStateFlow<Int?>(null)
    val learnedCc: StateFlow<Int?> = _learnedCc.asStateFlow()

    private val _knobValues = MutableStateFlow(emptyList<Int>())
    val knobValues: StateFlow<List<Int>> = _knobValues.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Not Connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var sourceOutputPort: MidiOutputPort? = null
    private var targetInputPort: MidiInputPort? = null

    val messageReceiver: MidiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            viewModelScope.launch {
                val learningIndices = _learningKnobIndex.value
                if (learningIndices != null) {
                    val (pageIndex, knobIndex) = learningIndices
                    if (msg[offset].toInt() and 0xF0 == 0xB0) { // Control Change
                        val cc = msg[offset + 1].toInt()
                        _learnedCc.value = cc
                        val currentSettings = _midiConfig.value.pages[pageIndex].knobSettings[knobIndex]
                        updateKnobSetting(
                            pageIndex,
                            knobIndex,
                            currentSettings.copy(cc = cc)
                        )
                        _learningKnobIndex.value = null
                    }
                } else {
                    if (msg[offset].toInt() and 0xF0 == 0xB0) { // Control Change
                        val cc = msg[offset + 1].toInt()
                        val value = msg[offset + 2].toInt()
                        var knobsBefore = 0
                        for (page in _midiConfig.value.pages) {
                            val knobIndexInPage = page.knobSettings.indexOfFirst { it.cc == cc }
                            if (knobIndexInPage != -1) {
                                val globalKnobIndex = knobsBefore + knobIndexInPage
                                onKnobValueChange(globalKnobIndex, value)
                                break
                            }
                            knobsBefore += page.knobSettings.size
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

    fun startLearning(pageIndex: Int, knobIndex: Int) {
        _learningKnobIndex.value = pageIndex to knobIndex
        _learnedCc.value = null
    }

    fun updateKnobSetting(pageIndex: Int, knobIndex: Int, newSettings: KnobSettings) {
        val currentConfig = _midiConfig.value
        val newPages = currentConfig.pages.toMutableList()
        val page = newPages.getOrNull(pageIndex) ?: return
        val newKnobSettings = page.knobSettings.toMutableList()

        if (knobIndex >= 0 && knobIndex < newKnobSettings.size) {
            newKnobSettings[knobIndex] = newSettings
            val newPage = page.copy(knobSettings = newKnobSettings)
            newPages[pageIndex] = newPage
            _midiConfig.value = currentConfig.copy(pages = newPages)
        }
    }

    fun addPage() {
        val currentConfig = _midiConfig.value
        val newPage = KnobPage(
            name = "User Page ${currentConfig.pages.size + 1}",
            knobSettings = List(20) { index ->
                KnobSettings(
                    "Knob ${index + 1}",
                    0,
                    127,
                    "",
                    0,
                    null
                )
            }
        )
        _midiConfig.value = currentConfig.copy(pages = currentConfig.pages + newPage)
        updateKnobValues()
    }

    fun onKnobValueChange(knobIndex: Int, value: Int) {
        val currentKnobValues = _knobValues.value.toMutableList()
        if (knobIndex < currentKnobValues.size) {
            currentKnobValues[knobIndex] = value
            _knobValues.value = currentKnobValues
            sendMidiMessage(knobIndex, value)
        }
    }

    private fun sendMidiMessage(knobIndex: Int, value: Int) {
        var tempKnobIndex = knobIndex
        var knobSetting: KnobSettings? = null
        for (page in _midiConfig.value.pages) {
            if (tempKnobIndex < page.knobSettings.size) {
                knobSetting = page.knobSettings[tempKnobIndex]
                break
            }
            tempKnobIndex -= page.knobSettings.size
        }

        if (knobSetting == null) return
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
        updateKnobValues()
    }

    private fun updateKnobValues() {
        val totalKnobs = _midiConfig.value.pages.sumOf { it.knobSettings.size }
        _knobValues.value = List(totalKnobs) { 0 }
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
