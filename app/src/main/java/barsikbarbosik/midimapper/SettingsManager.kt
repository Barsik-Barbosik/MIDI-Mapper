package barsikbarbosik.midimapper

import android.content.Context
import android.os.Environment
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SettingsManager {

    private fun getConfigsDirectory(context: Context): File {
        val directory = context.getExternalFilesDir(null)
        val storageDir = directory ?: context.filesDir
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return storageDir
    }

    fun saveSettings(context: Context, config: MidiConfig) {
        val json = Json { encodeDefaults = true }
        val jsonString = json.encodeToString(config)
        val configsDir = getConfigsDirectory(context)
        val file = File(configsDir, "${config.configName}.json")
        file.writeText(jsonString)
    }

    fun loadSettings(context: Context, fileName: String): MidiConfig {
        val configsDir = getConfigsDirectory(context)
        val file = File(configsDir, fileName)
        return if (file.exists()) {
            val json = file.readText()
            if (json.isEmpty()) {
                return defaultConfig(fileName.removeSuffix(".json"))
            }
            try {
                Json.decodeFromString<MidiConfig>(json)
            } catch (e: SerializationException) {
                e.printStackTrace()
                defaultConfig(fileName.removeSuffix(".json"))
            }
        } else {
            defaultConfig(fileName.removeSuffix(".json"))
        }
    }

    fun getAvailableConfigs(context: Context): List<String> {
        val configsDir = getConfigsDirectory(context)
        return configsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    if (json.isNotEmpty()) {
                        Json.decodeFromString<MidiConfig>(json).configName
                    } else {
                        null
                    }
                } catch (e: SerializationException) {
                    null
                }
            }
            ?: emptyList()
    }

    private fun defaultConfig(configName: String): MidiConfig {
        return MidiConfig(
            configName = configName,
            knobSettings = List(20) { i -> KnobSettings("Knob ${i + 1}", 0, 127, "", 0) }
        )
    }
}
