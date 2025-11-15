package barsikbarbosik.midimapper

import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import barsikbarbosik.midimapper.ui.controls.RotaryKnob
import kotlinx.serialization.Serializable

@Serializable
data class KnobSettings(
    var name: String,
    var minValue: Int,
    var maxValue: Int,
    var sysex: String,
    var offset: Int = 0
)

@Serializable
data class MidiConfig(
    var configName: String,
    var knobSettings: List<KnobSettings>
)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    knobValues: List<Int>,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    onKnobValueChange: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var midiConfig by remember {
        mutableStateOf(
            SettingsManager.loadSettings(
                context,
                "default.json"
            )
        )
    }

    NavHost(navController = navController, startDestination = "devices", modifier = modifier) {
        composable("devices") {
            DeviceSelectionScreen(
                devices = devices,
                connectionStatus = connectionStatus,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                navController = navController,
                midiConfig = midiConfig,
                onMidiConfigChange = { midiConfig = it },
                onSaveConfig = { SettingsManager.saveSettings(context, midiConfig) },
                onLoadConfig = { midiConfig = SettingsManager.loadSettings(context, it) },
                getAvailableConfigs = { SettingsManager.getAvailableConfigs(context) }
            )
        }
        composable("knobs") {
            KnobsScreen(
                knobValues = knobValues,
                knobSettings = midiConfig.knobSettings,
                onKnobValueChange = onKnobValueChange,
                navController = navController
            )
        }
        composable(
            "knob-settings/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index")
            if (index != null) {
                KnobSettingsScreen(
                    navController = navController,
                    knobIndex = index,
                    knobSetting = midiConfig.knobSettings[index],
                    onSave = { newSettings ->
                        val newKnobSettings = midiConfig.knobSettings.toMutableList()
                        newKnobSettings[index] = newSettings
                        midiConfig = midiConfig.copy(knobSettings = newKnobSettings)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionScreen(
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    navController: NavController,
    midiConfig: MidiConfig,
    onMidiConfigChange: (MidiConfig) -> Unit,
    onSaveConfig: () -> Unit,
    onLoadConfig: (String) -> Unit,
    getAvailableConfigs: () -> List<String>
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }
    var expandedConfigs by remember { mutableStateOf(false) }

    var selectedSource by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var selectedTarget by remember { mutableStateOf<MidiDeviceInfo?>(null) }

    val isConnected = connectionStatus.startsWith("Connected")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            TextField(
                value = midiConfig.configName,
                onValueChange = { onMidiConfigChange(midiConfig.copy(configName = it)) },
                label = { Text("Config Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onSaveConfig,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Save config")
                }
                ExposedDropdownMenuBox(
                    expanded = expandedConfigs,
                    onExpandedChange = { expandedConfigs = !expandedConfigs },
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = { expandedConfigs = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Load")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Load config")
                    }
                    ExposedDropdownMenu(
                        expanded = expandedConfigs,
                        onDismissRequest = { expandedConfigs = false }
                    ) {
                        getAvailableConfigs().forEach { configName ->
                            DropdownMenuItem(
                                text = { Text(configName) },
                                onClick = {
                                    onLoadConfig(configName)
                                    expandedConfigs = false
                                }
                            )
                        }
                    }
                }
            }

            Divider()

            Text(
                "MIDI Connection",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = expandedSource && !isConnected,
                onExpandedChange = {
                    if (!isConnected) expandedSource = !expandedSource
                }
            ) {
                TextField(
                    value = selectedSource?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                        ?: "Select source",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isConnected,
                    label = { Text("Source") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expandedSource && !isConnected
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedSource && !isConnected,
                    onDismissRequest = { expandedSource = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                        ?: "Unknown"
                                )
                            },
                            onClick = {
                                selectedSource = device
                                expandedSource = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedTarget && !isConnected,
                onExpandedChange = {
                    if (!isConnected) expandedTarget = !expandedTarget
                }
            ) {
                TextField(
                    value = selectedTarget?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                        ?: "Select target",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isConnected,
                    label = { Text("Target") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expandedTarget && !isConnected
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTarget && !isConnected,
                    onDismissRequest = { expandedTarget = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                        ?: "Unknown"
                                )
                            },
                            onClick = {
                                selectedTarget = device
                                expandedTarget = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedSource != null && selectedTarget != null) {
                        onConnect(selectedSource!!, selectedTarget!!)
                    }
                },
                enabled = selectedSource != null && selectedTarget != null && !isConnected,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
            ) {
                Icon(Icons.Filled.Link, contentDescription = "Connect")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Connect")
            }

            Divider()

            Text(
                text = connectionStatus,
                color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            if (isConnected) {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(
                        Icons.Filled.LinkOff,
                        contentDescription = "Disconnect",
                        tint = Color.White
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Disconnect", color = Color.White)
                }
            }
        }

        Button(
            onClick = { navController.navigate("knobs") },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
        ) {
            Icon(Icons.Filled.ArrowForward, contentDescription = "Go to Knobs")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Go to Knobs")
        }
    }
}

@Composable
fun KnobsScreen(
    knobValues: List<Int>,
    knobSettings: List<KnobSettings>,
    onKnobValueChange: (Int, Int) -> Unit,
    navController: NavController
) {
    var isConfigurable by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(20) { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.pointerInput(isConfigurable) {
                            detectTapGestures(
                                onLongPress = {
                                    if (isConfigurable) {
                                        navController.navigate("knob-settings/$index")
                                    }
                                }
                            )
                        }
                    ) {
                        RotaryKnob(
                            value = knobValues.getOrElse(index) { 0 },
                            onValueChange = { newValue -> onKnobValueChange(index, newValue) },
                            modifier = Modifier.size(80.dp),
                            min = knobSettings.getOrElse(index) {
                                KnobSettings(
                                    "",
                                    0,
                                    127,
                                    "",
                                    0
                                )
                            }.minValue,
                            max = knobSettings.getOrElse(index) {
                                KnobSettings(
                                    "",
                                    0,
                                    127,
                                    "",
                                    0
                                )
                            }.maxValue,
                            offset = knobSettings.getOrElse(index) {
                                KnobSettings(
                                    "",
                                    0,
                                    127,
                                    "",
                                    0
                                )
                            }.offset
                        )
                    }
                    Text(knobSettings.getOrElse(index) {
                        KnobSettings(
                            "Knob ${index + 1}",
                            0,
                            127,
                            "",
                            0
                        )
                    }.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isConfigurable = !isConfigurable },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfigurable) Color(
                        0xFFF44336
                    ) else Color(0xFF1B998B)
                )
            ) {
                if (isConfigurable) {
                    Icon(Icons.Filled.Done, "Done")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Done")
                } else {
                    Icon(Icons.Filled.Tune, "Configure")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Configure")
                }
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
            ) {
                Icon(Icons.Filled.ArrowBack, "Back")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnobSettingsScreen(
    navController: NavController,
    knobIndex: Int,
    knobSetting: KnobSettings,
    onSave: (KnobSettings) -> Unit
) {
    var knobName by remember { mutableStateOf(knobSetting.name) }
    var minValue by remember { mutableStateOf(knobSetting.minValue.toString()) }
    var maxValue by remember { mutableStateOf(knobSetting.maxValue.toString()) }
    var offset by remember { mutableStateOf(knobSetting.offset.toString()) }
    var sysex by remember { mutableStateOf(knobSetting.sysex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configure Knob $knobIndex", style = MaterialTheme.typography.titleMedium)

        TextField(
            value = knobName,
            onValueChange = { knobName = it },
            label = { Text("Knob Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = minValue,
                onValueChange = { minValue = it },
                label = { Text("Min Value") },
                modifier = Modifier.weight(1f)
            )
            TextField(
                value = maxValue,
                onValueChange = { maxValue = it },
                label = { Text("Max Value") },
                modifier = Modifier.weight(1f)
            )
        }

        TextField(
            value = offset,
            onValueChange = { offset = it },
            label = { Text("Offset") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = sysex,
            onValueChange = { sysex = it },
            label = { Text("SysEx Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { /* TODO: Implement Learn logic */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
        ) {
            Icon(Icons.Filled.Sensors, "Learn")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Learn")
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val newSettings = KnobSettings(
                        name = knobName,
                        minValue = minValue.toIntOrNull() ?: 0,
                        maxValue = maxValue.toIntOrNull() ?: 127,
                        sysex = sysex,
                        offset = offset.toIntOrNull() ?: 0
                    )
                    onSave(newSettings)
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
            ) {
                Icon(Icons.Filled.Save, "Save")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Save")
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Icon(Icons.Filled.Cancel, "Cancel", tint = Color.White)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Cancel", color = Color.White)
            }
        }
    }
}
