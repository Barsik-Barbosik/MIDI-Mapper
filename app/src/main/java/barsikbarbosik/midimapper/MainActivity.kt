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
                val knobValues by viewModel.knobValues.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        devices = devices,
                        connectionStatus = connectionStatus,
                        knobValues = knobValues,
                        onConnect = { src, tgt -> viewModel.connectDevices(src, tgt) },
                        onDisconnect = { viewModel.disconnectDevices() },
                        onKnobValueChange = { index, value ->
                            viewModel.setKnobValue(
                                index,
                                value
                            )
                        },
                    )
                }
            }
        }
    }
}
