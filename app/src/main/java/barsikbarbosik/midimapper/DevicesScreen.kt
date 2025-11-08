package barsikbarbosik.midimapper


import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.layout.Arrangement
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
import barsikbarbosik.midimapper.ui.controls.RotaryKnob

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<MidiDeviceInfo>,
    connectionStatus: String,
    knobValue: Int,
    learningMode: Boolean,
    learnedCc: Int?,
    onConnect: (MidiDeviceInfo, MidiDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    onKnobValueChange: (Int) -> Unit,
    onToggleLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }

    var selectedSource by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var selectedTarget by remember { mutableStateOf<MidiDeviceInfo?>(null) }

    val isConnected = connectionStatus.startsWith("Connected")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Select Source and Target MIDI Devices", style = MaterialTheme.typography.titleMedium)

        /* ---------------- Source Dropdown ---------------- */
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
                enabled = !isConnected,   // ✅ disable when connected
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

        /* ---------------- Target Dropdown ---------------- */
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
                enabled = !isConnected,   // ✅ disable when connected
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

        /* ---------------- Connect Button ---------------- */
        Button(
            onClick = {
                if (selectedSource != null && selectedTarget != null) {
                    onConnect(selectedSource!!, selectedTarget!!)
                }
            },
            enabled = selectedSource != null && selectedTarget != null && !isConnected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }

        Divider()

        /* ---------------- Status Text ---------------- */
        Text(
            text = connectionStatus,
            color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
        )

        /* ---------------- Disconnect Button ---------------- */
        if (isConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Disconnect", color = Color.White)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            /*-- KNOB --*/
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
                    Button(onClick = onToggleLearning, enabled = isConnected) {
                        Text(if (learningMode) "Listening..." else "Learn CC")
                    }
                }
            }
        }
    }
}
