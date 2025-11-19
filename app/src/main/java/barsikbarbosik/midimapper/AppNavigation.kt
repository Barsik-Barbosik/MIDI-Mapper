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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NoteAdd
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import barsikbarbosik.midimapper.ui.controls.RotaryKnob
import kotlinx.serialization.Serializable

@Serializable
data class KnobSettings(
    var name: String,
    var minValue: Int,
    var maxValue: Int,
    var sysex: String,
    var offset: Int = 0,
    var cc: Int? = null
)

@Serializable
data class KnobPage(
    var name: String,
    var knobSettings: List<KnobSettings>
)

@Serializable
data class MidiConfig(
    var configName: String = "default",
    var pages: List<KnobPage> = listOf(
        KnobPage(
            name = "User Page 1",
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
    )
)


@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    midiViewModel: MidiViewModel = viewModel(factory = MidiViewModelFactory(LocalContext.current)),
    navController: NavHostController
) {
    val devices by midiViewModel.devices.collectAsState()
    val midiConfig by midiViewModel.midiConfig.collectAsState()
    val learnedCc by midiViewModel.learnedCc.collectAsState()
    val knobValues by midiViewModel.knobValues.collectAsState()
    val connectionStatus by midiViewModel.connectionStatus.collectAsState()
    val receivedMidiMessages by midiViewModel.receivedMidiMessages.collectAsState()

    // The following methods on midiViewModel will need to be updated or created:
    // - fun addPage()
    // - fun updateKnobSetting(pageIndex: Int, knobIndex: Int, newSettings: KnobSettings)
    // - fun startLearning(pageIndex: Int, knobIndex: Int)
    // The signature of onKnobValueChange can remain the same if using a global index.

    NavHost(navController = navController, startDestination = "main", modifier = modifier) {
        composable("main") {
            MainScreen(
                devices = devices,
                connectionStatus = connectionStatus,
                onConnect = midiViewModel::connect,
                onDisconnect = { midiViewModel.disconnect() },
                navController = navController,
                midiConfig = midiConfig,
                onConfigNameChange = { midiViewModel.updateConfigName(it) },
                onSaveConfig = { midiViewModel.saveMidiConfig() },
                onLoadConfig = { midiViewModel.loadMidiConfig(it) },
                getAvailableConfigs = { midiViewModel.getAvailableConfigs() },
                onAddPage = { midiViewModel.addPage() },
                receivedMidiMessages = receivedMidiMessages
            )
        }
        composable(
            "page/{pageIndex}",
            arguments = listOf(navArgument("pageIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex")
            if (pageIndex != null) {
                val page = midiConfig.pages.getOrNull(pageIndex)
                if (page != null) {
                    val knobsBefore =
                        midiConfig.pages.take(pageIndex).sumOf { it.knobSettings.size }
                    val pageKnobValues = knobValues.drop(knobsBefore).take(page.knobSettings.size)
                    KnobsScreen(
                        pageIndex = pageIndex,
                        knobValues = pageKnobValues,
                        knobSettings = page.knobSettings,
                        onKnobValueChange = { index, value ->
                            val globalKnobIndex = knobsBefore + index
                            midiViewModel.onKnobValueChange(
                                globalKnobIndex,
                                value
                            )
                        },
                        navController = navController
                    )
                }
            }
        }
        composable(
            "page/{pageIndex}/knob-settings/{knobIndex}",
            arguments = listOf(
                navArgument("pageIndex") { type = NavType.IntType },
                navArgument("knobIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex")
            val knobIndex = backStackEntry.arguments?.getInt("knobIndex")
            if (pageIndex != null && knobIndex != null) {
                KnobSettingsScreen(
                    navController = navController,
                    knobIndex = knobIndex,
                    knobSetting = midiConfig.pages.getOrNull(pageIndex)?.knobSettings?.getOrElse(
                        knobIndex
                    ) {
                        KnobSettings(
                            "",
                            0,
                            127,
                            "",
                            0,
                            null
                        )
                    } ?: KnobSettings(
                        "",
                        0,
                        127,
                        "",
                        0,
                        null
                    ),
                    onSave = { newSettings ->
                        midiViewModel.updateKnobSetting(pageIndex, knobIndex, newSettings)
                    },
                    onStartLearning = { midiViewModel.startLearning(pageIndex, it) },
                    learnedCc = learnedCc
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    navController: NavController,
    midiConfig: MidiConfig,
    onConfigNameChange: (String) -> Unit,
    onSaveConfig: () -> Unit,
    onLoadConfig: (String) -> Unit,
    getAvailableConfigs: () -> List<String>,
    onAddPage: () -> Unit,
    receivedMidiMessages: String
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }
    var expandedConfigs by remember { mutableStateOf(false) }

    var selectedSource by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var selectedTarget by remember { mutableStateOf<MidiDeviceInfo?>(null) }

    val isConnected = connectionStatus != "Not Connected"

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
                "Save/Load Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            TextField(
                value = midiConfig.configName,
                onValueChange = onConfigNameChange,
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
                    modifier = Modifier.weight(1f)
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
                            .menuAnchor()
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
                                    onLoadConfig("$configName.json")
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

            if (isConnected) {
                TextField(
                    value = receivedMidiMessages,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Received MIDI Messages") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = expandedSource,
                    onExpandedChange = { expandedSource = !expandedSource }
                ) {
                    TextField(
                        value = selectedSource?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                            ?: "Select source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedSource
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSource,
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
                    expanded = expandedTarget,
                    onExpandedChange = { expandedTarget = !expandedTarget }
                ) {
                    TextField(
                        value = selectedTarget?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                            ?: "Select target",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedTarget
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTarget,
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
                    enabled = selectedSource != null && selectedTarget != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Link, contentDescription = "Connect")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Connect")
                }
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

            Divider()

            Text(
                "Pages",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(midiConfig.pages) { index, page ->
                    Button(onClick = { navController.navigate("page/$index") }) {
                        Text(page.name)
                    }
                }
            }

            Button(onClick = onAddPage) {
                Icon(Icons.Filled.NoteAdd, "Add New User Page")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add New User Page")
            }
        }
    }
}

@Composable
fun KnobsScreen(
    pageIndex: Int,
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
            items(knobSettings.size) { index ->
                val setting = knobSettings[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.pointerInput(isConfigurable) {
                            detectTapGestures(
                                onLongPress = {
                                    if (isConfigurable) {
                                        navController.navigate("page/$pageIndex/knob-settings/$index")
                                    }
                                }
                            )
                        }
                    ) {
                        RotaryKnob(
                            value = knobValues.getOrElse(index) { 0 },
                            onValueChange = { newValue -> onKnobValueChange(index, newValue) },
                            modifier = Modifier.size(80.dp),
                            min = setting.minValue,
                            max = setting.maxValue,
                            offset = setting.offset
                        )
                    }
                    Text(setting.name, style = MaterialTheme.typography.bodySmall)
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
                    ) else MaterialTheme.colorScheme.primary
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnobSettingsScreen(
    navController: NavController,
    knobIndex: Int,
    knobSetting: KnobSettings,
    onSave: (KnobSettings) -> Unit,
    onStartLearning: (Int) -> Unit,
    learnedCc: Int?
) {
    var knobName by remember { mutableStateOf(knobSetting.name) }
    var minValue by remember { mutableStateOf(knobSetting.minValue.toString()) }
    var maxValue by remember { mutableStateOf(knobSetting.maxValue.toString()) }
    var offset by remember { mutableStateOf(knobSetting.offset.toString()) }
    var sysex by remember { mutableStateOf(knobSetting.sysex) }
    var cc by remember { mutableStateOf(knobSetting.cc?.toString() ?: "") }

    LaunchedEffect(learnedCc) {
        if (learnedCc != null) {
            cc = learnedCc.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configure Knob ${knobIndex + 1}", style = MaterialTheme.typography.titleMedium)

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
            value = cc,
            onValueChange = { cc = it },
            label = { Text("MIDI CC") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )

        TextField(
            value = sysex,
            onValueChange = { sysex = it },
            label = { Text("SysEx Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onStartLearning(knobIndex) },
            modifier = Modifier.fillMaxWidth()
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
                        offset = offset.toIntOrNull() ?: 0,
                        cc = cc.toIntOrNull()
                    )
                    onSave(newSettings)
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f)
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
