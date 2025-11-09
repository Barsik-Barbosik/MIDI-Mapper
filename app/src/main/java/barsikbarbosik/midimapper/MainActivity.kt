package barsikbarbosik.midimapper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import barsikbarbosik.midimapper.ui.theme.MidiMapperTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MidiViewModel> { MidiViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MidiMapperTheme {
                val devices by viewModel.devices.collectAsState()
                val connectionStatus by viewModel.connectionStatus.collectAsState()
                val knobValue by viewModel.knobValue.collectAsState()
                val learningMode by viewModel.learningMode.collectAsState()
                val learnedCc by viewModel.learnedCcNumber.collectAsState()
                val customSysExMessage by viewModel.customSysExMessage.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DevicesScreen(
                        devices = devices,
                        connectionStatus = connectionStatus,
                        knobValue = knobValue,
                        learningMode = learningMode,
                        learnedCc = learnedCc,
                        customSysExMessage = customSysExMessage,
                        onSetCustomSysExMessage = { viewModel.setCustomSysExMessage(it) },
                        onConnect = { src, tgt -> viewModel.connectDevices(src, tgt) },
                        onDisconnect = { viewModel.disconnectDevices() },
                        onKnobValueChange = { value -> viewModel.setKnobValue(value) },
                        onToggleLearning = { viewModel.toggleLearningMode() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
