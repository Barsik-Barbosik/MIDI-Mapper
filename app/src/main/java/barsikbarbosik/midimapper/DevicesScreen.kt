package barsikbarbosik.midimapper


import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }

    var selectedSource by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var selectedTarget by remember { mutableStateOf<MidiDeviceInfo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Text("MIDI Mapper", style = MaterialTheme.typography.titleLarge)
        Text("Select Source and Target MIDI Devices", style = MaterialTheme.typography.titleMedium)

        // Source dropdown
        ExposedDropdownMenuBox(
            expanded = expandedSource,
            onExpandedChange = { expandedSource = !expandedSource }) {
            TextField(
                value = selectedSource?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: "Select source",
                onValueChange = {},
                readOnly = true,
                label = { Text("Source") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSource) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedSource,
                onDismissRequest = { expandedSource = false }) {
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

        // Target dropdown
        ExposedDropdownMenuBox(
            expanded = expandedTarget,
            onExpandedChange = { expandedTarget = !expandedTarget }) {
            TextField(
                value = selectedTarget?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: "Select target",
                onValueChange = {},
                readOnly = true,
                label = { Text("Target") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTarget) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedTarget,
                onDismissRequest = { expandedTarget = false }) {
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
            Text("Connect")
        }

        // --- Status indicator + Disconnect ---
        Divider()
        Text(
            text = connectionStatus,
            color = if (connectionStatus.startsWith("Connected")) Color(0xFF2E7D32) else Color(
                0xFFC62828
            )
        )
        if (connectionStatus.startsWith("Connected")) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Disconnect", color = Color.White)
            }
        }
    }
}
