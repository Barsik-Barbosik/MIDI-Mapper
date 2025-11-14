package barsikbarbosik.midimapper

import android.content.Context
import android.os.Environment
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SettingsManager {

    private fun getSetupsDirectory(context: Context): File {
        val directory = context.getExternalFilesDir(null)
        val storageDir = directory ?: context.filesDir
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return storageDir
    }

    fun saveSettings(context: Context, setup: MidiSetup) {
        val json = Json { encodeDefaults = true }
        val jsonString = json.encodeToString(setup)
        val setupsDir = getSetupsDirectory(context)
        val file = File(setupsDir, "${setup.setupName}.json")
        file.writeText(jsonString)
    }

    fun loadSettings(context: Context, fileName: String): MidiSetup {
        val setupsDir = getSetupsDirectory(context)
        val file = File(setupsDir, fileName)
        return if (file.exists()) {
            val json = file.readText()
            if (json.isEmpty()) {
                return defaultSetup(fileName.removeSuffix(".json"))
            }
            try {
                Json.decodeFromString<MidiSetup>(json)
            } catch (e: SerializationException) {
                e.printStackTrace()
                defaultSetup(fileName.removeSuffix(".json"))
            }
        } else {
            defaultSetup(fileName.removeSuffix(".json"))
        }
    }

    fun getAvailableSetups(context: Context): List<String> {
        val setupsDir = getSetupsDirectory(context)
        return setupsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    if (json.isNotEmpty()) {
                        Json.decodeFromString<MidiSetup>(json).setupName
                    } else {
                        null
                    }
                } catch (e: SerializationException) {
                    null
                }
            }
            ?: emptyList()
    }

    private fun defaultSetup(setupName: String): MidiSetup {
        return MidiSetup(
            setupName = setupName,
            knobSettings = List(20) { i -> KnobSettings("Knob ${i + 1}", 0, 127, "", 0) }
        )
    }
}
