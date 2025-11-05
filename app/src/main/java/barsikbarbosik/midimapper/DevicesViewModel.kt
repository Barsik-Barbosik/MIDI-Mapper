package barsikbarbosik.midimapper

import android.app.Application
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DevicesViewModel(app: Application) : AndroidViewModel(app) {
    private val midiManager = app.getSystemService(MidiManager::class.java)
    private val _devices = MutableLiveData<List<MidiDeviceInfo>>(emptyList())
    val devices: LiveData<List<MidiDeviceInfo>> = _devices

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(info: MidiDeviceInfo) {
            refresh()
        }
        override fun onDeviceRemoved(info: MidiDeviceInfo) {
            refresh()
        }
    }

    init {
        midiManager.registerDeviceCallback(callback, null)
        refresh()
    }

    private fun refresh() {
        _devices.postValue(midiManager.devices.toList())
    }

    override fun onCleared() {
        midiManager.unregisterDeviceCallback(callback)
        super.onCleared()
    }
}
