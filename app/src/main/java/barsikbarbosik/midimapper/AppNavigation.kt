package barsikbarbosik.midimapper

import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import barsikbarbosik.midimapper.ui.controls.RotaryKnob

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    knobValue: Int,
    learningMode: Boolean,
    learnedCc: Int?,
    customSysExMessage: String,
    onSetCustomSysExMessage: (String) -> Unit,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    onKnobValueChange: (Int) -> Unit,
    onToggleLearning: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "devices", modifier = modifier) {
        composable("devices") {
            DeviceSelectionScreen(
                devices = devices,
                connectionStatus = connectionStatus,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                navController = navController
            )
        }
        composable("knobs") {
            KnobsScreen(
                knobValue = knobValue,
                learningMode = learningMode,
                learnedCc = learnedCc,
                onKnobValueChange = onKnobValueChange,
                onToggleLearning = onToggleLearning,
                customSysExMessage = customSysExMessage,
                onSetCustomSysExMessage = onSetCustomSysExMessage,
                navController = navController
            )
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
    navController: NavController
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }

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
                "Select Source and Target MIDI Devices",
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
            Text("Go to Knobs")
        }
    }
}

@Composable
fun KnobsScreen(
    knobValue: Int,
    learningMode: Boolean,
    learnedCc: Int?,
    onKnobValueChange: (Int) -> Unit,
    onToggleLearning: () -> Unit,
    customSysExMessage: String,
    onSetCustomSysExMessage: (String) -> Unit,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotaryKnob(
                    value = knobValue,
                    onValueChange = onKnobValueChange,
                    modifier = Modifier.size(150.dp),
                    min = 0,
                    max = 127
                )
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text(
                        text = learnedCc?.let { "Learned CC: $it" } ?: "No CC learned",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onToggleLearning, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))) {
                        Text(if (learningMode) "Listening..." else "Learn CC")
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            TextField(
                value = customSysExMessage,
                onValueChange = onSetCustomSysExMessage,
                label = { Text("SysEx Message (%V = value)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B998B))
        ) {
            Text("Back")
        }
    }
}