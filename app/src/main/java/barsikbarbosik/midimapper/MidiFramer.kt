package barsikbarbosik.midimapper

import android.media.midi.MidiReceiver

class MidiFramer(private val receiver: MidiReceiver) : MidiReceiver() {

    private var runningStatus = 0
    private val buffer = ByteArray(1024)
    private var pos = 0

    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        for (i in offset until offset + count) {
            val b = msg[i].toInt() and 0xFF

            when {
                // -----------------------------
                // REALTIME MESSAGES (always 1 byte)
                // -----------------------------
                b in 0xF8..0xFF -> {
                    receiver.send(byteArrayOf(b.toByte()), 0, 1, timestamp)
                }

                // -----------------------------
                // STATUS BYTE
                // -----------------------------
                b and 0x80 != 0 -> {
                    runningStatus = b
                    pos = 0
                }

                // -----------------------------
                // DATA BYTE WITHOUT STATUS (running status)
                // -----------------------------
                runningStatus == 0 -> {
                    // ignore until a status byte arrives
                }

                else -> {
                    buffer[pos++] = b.toByte()

                    val needed = when (runningStatus) {
                        in 0x80..0xEF -> { // channel voice messages
                            when (runningStatus and 0xF0) {
                                0xC0, 0xD0 -> 1 // Program Change, Mono Pressure
                                else -> 2       // NoteOn, NoteOff, CC, etc.
                            }
                        }

                        0xF1 -> 1 // MTC Quarter Frame
                        0xF2 -> 2 // Song Position
                        0xF3 -> 1 // Song Select
                        0xF6 -> 0 // Tune Request (no data)
                        0xF0 -> { // SysEx Start
                            // SYS EX: keep collecting until F7
                            if (b == 0xF7) {
                                buffer[pos++] = b.toByte()
                                sendSysEx(pos, timestamp)
                                pos = 0
                                runningStatus = 0
                            }
                            continue
                        }

                        else -> 0 // unsupported system
                    }

                    if (pos >= needed) {
                        val out = ByteArray(1 + needed)
                        out[0] = runningStatus.toByte()
                        System.arraycopy(buffer, 0, out, 1, needed)

                        receiver.send(out, 0, out.size, timestamp)
                        pos = 0
                    }
                }
            }
        }
    }

    private fun sendSysEx(len: Int, timestamp: Long) {
        receiver.send(buffer, 0, len, timestamp)
    }
}
