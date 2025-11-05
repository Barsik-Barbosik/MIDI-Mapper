package barsikbarbosik.midimapper

import android.media.midi.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException

private const val TAG = "MidiRouter"

class MidiRouter(private val midiManager: MidiManager) {

    data class OpenedDevice(
        val info: MidiDeviceInfo,
        val device: MidiDevice,
        val inputPorts: List<MidiInputPort>,   // ports we can write *to* (send to device)
        val outputPorts: List<MidiOutputPort>  // ports we can read *from* (receive from device)
    )

    private val opened = mutableMapOf<String, OpenedDevice>() // key by device id string

    fun openDevice(info: MidiDeviceInfo, onOpened: (OpenedDevice?) -> Unit) {
        val id = info.toString()
        if (opened.containsKey(id)) {
            onOpened(opened[id])
            return
        }

        // open async on main looper
        midiManager.openDevice(info, { device ->
            if (device == null) {
                Log.w(TAG, "Failed to open device $info")
                onOpened(null)
                return@openDevice
            }
            val inputPorts = mutableListOf<MidiInputPort>()
            val outputPorts = mutableListOf<MidiOutputPort>()

            // open all available input ports (for sending TO device)
            val inCount = info.inputPortCount
            for (i in 0 until inCount) {
                val p = device.openInputPort(i)
                if (p != null) inputPorts.add(p)
            }

            // open all available output ports (for receiving FROM device)
            val outCount = info.outputPortCount
            for (i in 0 until outCount) {
                val p = device.openOutputPort(i)
                if (p != null) outputPorts.add(p)
            }

            val od = OpenedDevice(info, device, inputPorts, outputPorts)
            opened[id] = od
            onOpened(od)
        }, Handler(Looper.getMainLooper()))
    }

    /**
     * Connect sourceDevice's output port index `srcOutputIndex` to targetDevice's input port index
     * `tgtInputIndex`.
     *
     * Returns a connection object you can later call `disconnect()` on to stop forwarding.
     */
    fun connect(
        src: OpenedDevice,
        srcOutputIndex: Int,
        tgt: OpenedDevice,
        tgtInputIndex: Int
    ): RouteConnection? {
        if (srcOutputIndex !in src.outputPorts.indices) {
            Log.w(TAG, "Invalid src output index")
            return null
        }
        if (tgtInputIndex !in tgt.inputPorts.indices) {
            Log.w(TAG, "Invalid tgt input index")
            return null
        }

        val outputPort = src.outputPorts[srcOutputIndex]
        val targetInputPort = tgt.inputPorts[tgtInputIndex]

        // Forwarder: receives bytes from the source output port and writes them to target input port
        val forwarder = object : MidiReceiver() {
            override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
                try {
                    // pass through timestamps if present, else 0
                    targetInputPort.send(data, offset, count, if (timestamp != 0L) timestamp else 0L)
                } catch (e: IOException) {
                    Log.e(TAG, "Forward send failed: ${e.message}", e)
                }
            }
        }

        // connect forwarder to source output port
        outputPort.connect(forwarder)

        return RouteConnection(outputPort, forwarder)
    }

    data class RouteConnection(
        val srcOutputPort: MidiOutputPort,
        val forwarder: MidiReceiver
    ) {
        fun disconnect() {
            try {
                srcOutputPort.disconnect(forwarder)
            } catch (ignored: Exception) {}
        }
    }

    fun closeDevice(info: MidiDeviceInfo) {
        val id = info.toString()
        val od = opened.remove(id) ?: return
        // close ports
        od.inputPorts.forEach { try { it.close() } catch (_: Exception) {} }
        od.outputPorts.forEach { try { it.close() } catch (_: Exception) {} }
        try { od.device.close() } catch (_: Exception) {}
    }

    fun closeAll() {
        for ((_, v) in opened.toMap()) {
            closeDevice(v.info)
        }
        opened.clear()
    }
}
