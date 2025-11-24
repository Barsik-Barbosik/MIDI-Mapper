package barsikbarbosik.midimapper

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MidiViewModel(private val context: Context) : ViewModel() {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private val _devices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    val devices: StateFlow<List<MidiDeviceInfo>> = _devices.asStateFlow()

    private val _selectedSourceDevice = MutableStateFlow<MidiDeviceInfo?>(null)
    val selectedSourceDevice: StateFlow<MidiDeviceInfo?> = _selectedSourceDevice.asStateFlow()

    private val _selectedTargetDevice = MutableStateFlow<MidiDeviceInfo?>(null)
    val selectedTargetDevice: StateFlow<MidiDeviceInfo?> = _selectedTargetDevice.asStateFlow()

    private val _midiConfig = MutableStateFlow(MidiConfig())
    val midiConfig: StateFlow<MidiConfig> = _midiConfig.asStateFlow()

    private val _learningKnobIndex = MutableStateFlow<Pair<Int, Int>?>(null)

    private val _learnedCc = MutableStateFlow<Int?>(null)
    val learnedCc: StateFlow<Int?> = _learnedCc.asStateFlow()

    private val _knobValues = MutableStateFlow(emptyList<Int>())
    val knobValues: StateFlow<List<Int>> = _knobValues.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Not Connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _receivedMidiMessages = MutableStateFlow<List<String>>(emptyList())
    val receivedMidiMessages: StateFlow<List<String>> = _receivedMidiMessages.asStateFlow()

    private var sourceOutputPort: MidiOutputPort? = null
    private var targetInputPort: MidiInputPort? = null
    @Volatile
    private var ignoringMidi = false

    val messageReceiver: MidiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (ignoringMidi) return

            viewModelScope.launch {
                val newMessage = msg.sliceArray(offset until offset + count)
                    .joinToString(" ") { "%02X".format(it) }
                val currentMessages = _receivedMidiMessages.value.toMutableList()
                currentMessages.add(newMessage)
                if (currentMessages.size > 5) {
                    currentMessages.removeAt(0)
                }
                _receivedMidiMessages.value = currentMessages

                val learningIndices = _learningKnobIndex.value
                if (learningIndices != null) {
                    val (pageIndex, knobIndex) = learningIndices
                    if (msg[offset].toInt() and 0xF0 == 0xB0) { // Control Change
                        val cc = msg[offset + 1].toInt()
                        _learnedCc.value = cc
                        val currentSettings =
                            _midiConfig.value.pages[pageIndex].knobSettings[knobIndex]
                        updateKnobSetting(
                            pageIndex,
                            knobIndex,
                            currentSettings.copy(cc = cc)
                        )
                        _learningKnobIndex.value = null
                    }
                } else { // Not in learning mode
                    val statusByte = msg[offset].toInt() and 0xFF
                    var forwardedAutomatically = true // Flag to determine if the message was forwarded
                    
                    if ((statusByte and 0xF0) == 0xB0) { // It's a Control Change message
                        val cc = msg[offset + 1].toInt()
                        val value = msg[offset + 2].toInt()

                        var globalKnobIndex = 0
                        var knobFound = false // Changed from knobFoundAndSysexHandled

                        // Search for a mapped knob
                        for (pageIndex in _midiConfig.value.pages.indices) {
                            val page = _midiConfig.value.pages[pageIndex]
                            for (knobIndexInPage in page.knobSettings.indices) {
                                val knobSetting = page.knobSettings[knobIndexInPage]
                                if (knobSetting.cc == cc) {
                                    // Found a mapped knob for the incoming CC
                                    onKnobValueChange(globalKnobIndex, value) // Update value and send SysEx if applicable

                                    if (knobSetting.sysex.isNotBlank()) {
                                        // If SysEx was sent by onKnobValueChange, don't forward the original CC.
                                        forwardedAutomatically = false
                                    } else {
                                        // If no SysEx was sent, then the original CC should be forwarded.
                                        forwardedAutomatically = true
                                    }
                                    knobFound = true
                                    break // Found the knob, no need to check other knobs
                                }
                                globalKnobIndex++
                            }
                            if (knobFound) {
                                break // Knob found, exit page loop
                            }
                        }
                    }

                    // If the message was not a SysEx-mapped CC, or if it was not a CC at all, forward it directly.
                    if (forwardedAutomatically) {
                        targetInputPort?.send(msg, offset, count, timestamp)
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
            loadMidiConfig("Default.json")
        }
    }

    fun connect(sourceDevice: MidiDeviceInfo, targetDevice: MidiDeviceInfo) {
        ignoringMidi = true
        val sourcePort = sourceDevice.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
        midiManager.openDevice(sourceDevice, { device ->
            sourceOutputPort = device.openOutputPort(sourcePort.portNumber)
            val framer = MidiFramer(messageReceiver)
            sourceOutputPort?.connect(framer)
            _connectionStatus.value =
                "Connected source:\n${sourceDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}\nConnected target:\n${
                    targetDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                }"
        }, null)

        val targetPort = targetDevice.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }
        midiManager.openDevice(targetDevice, { device ->
            targetInputPort = device.openInputPort(targetPort.portNumber)
        }, null)

        viewModelScope.launch {
            delay(500) // Ignore midi messages for 500ms
            ignoringMidi = false
        }
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
        val loadedConfig = SettingsManager.loadSettings(context, configName)
        _midiConfig.value = loadedConfig

        _selectedSourceDevice.value = _devices.value.find { device ->
            device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) == loadedConfig.selectedSourceDeviceName
        }
        _selectedTargetDevice.value = _devices.value.find { device ->
            device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) == loadedConfig.selectedTargetDeviceName
        }
        updateKnobValues()
    }

    private fun updateKnobValues() {
        val totalKnobs = _midiConfig.value.pages.sumOf { it.knobSettings.size }
        _knobValues.value = List(totalKnobs) { 0 }
    }

    fun getAvailableConfigs(): List<String> {
        return SettingsManager.getAvailableConfigs(context)
    }

    fun saveMidiConfig(source: MidiDeviceInfo?, target: MidiDeviceInfo?) {
        val updatedConfig = _midiConfig.value.copy(
            selectedSourceDeviceName = source?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME),
            selectedTargetDeviceName = target?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
        )
        SettingsManager.saveSettings(context, updatedConfig)
        _midiConfig.value = updatedConfig // Update ViewModel's midiConfig with new device names
    }

    fun updateConfigName(name: String) {
        _midiConfig.value = _midiConfig.value.copy(configName = name)
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}