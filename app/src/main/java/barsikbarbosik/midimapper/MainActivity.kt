package barsikbarbosik.midimapper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import barsikbarbosik.midimapper.ui.theme.MidiMapperTheme
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MidiViewModel> { MidiViewModelFactory(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                AndroidColor.TRANSPARENT
            )
        )

        setContent {
            MidiMapperTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { navController.navigate("main") }) {
                                        Icon(Icons.Filled.Home, "Home", tint = Color.White)
                                    }
                                    Text(
                                        text = currentBackStackEntry?.destination?.route?.replaceFirstChar { it.uppercase() }
                                            ?: "",
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(onClick = { navController.navigate("main") }) {
                                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                                    }
                                    IconButton(
                                        onClick = { navController.navigate("knobs") },
                                        enabled = true
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowForward,
                                            "Forward",
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black
                            )
                        )
                    }
                ) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        midiViewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}
