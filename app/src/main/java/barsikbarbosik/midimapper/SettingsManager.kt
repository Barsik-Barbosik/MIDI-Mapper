package barsikbarbosik.midimapper

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SettingsManager {

    private const val SETTINGS_FILE = "knob_settings.json"

    fun saveSettings(context: Context, settings: List<KnobSettings>) {
        val json = Json.encodeToString(settings)
        val file = File(context.filesDir, SETTINGS_FILE)
        file.writeText(json)
    }

    fun loadSettings(context: Context): List<KnobSettings> {
        val file = File(context.filesDir, SETTINGS_FILE)
        return if (file.exists()) {
            val json = file.readText()
            Json.decodeFromString(json)
        } else {
            List(20) { i -> KnobSettings("Knob ${i + 1}", 0, 127, "") }
        }
    }
}
