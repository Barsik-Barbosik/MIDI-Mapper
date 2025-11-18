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
import androidx.compose.runtime.collectAsState
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
                val midiConfig by viewModel.midiConfig.collectAsState()

                val route = currentBackStackEntry?.destination?.route
                val pageCount = midiConfig.pages.size
                val isPage = route?.startsWith("page/") == true
                val pageIndex = if (isPage) currentBackStackEntry?.arguments?.getInt("pageIndex") else null

                val title = when {
                    pageIndex != null -> midiConfig.pages.getOrNull(pageIndex)?.name
                    route != null -> route.replaceFirstChar { it.uppercase() }
                    else -> ""
                } ?: ""

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
                                        text = title,
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = {
                                            when {
                                                pageIndex != null && pageIndex > 0 -> navController.navigate("page/${pageIndex - 1}")
                                                pageIndex == 0 -> navController.navigate("main")
                                            }
                                        },
                                        enabled = pageIndex != null
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowBack, "Back",
                                            tint = if (pageIndex != null) Color.White else Color.DarkGray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            when {
                                                pageIndex != null && pageIndex < pageCount - 1 -> navController.navigate(
                                                    "page/${pageIndex + 1}"
                                                )

                                                pageIndex == null && route == "main" && pageCount > 0 -> navController.navigate(
                                                    "page/0"
                                                )
                                            }
                                        },
                                        enabled = (pageIndex != null && pageIndex < pageCount - 1) || (pageIndex == null && route == "main" && pageCount > 0)
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowForward,
                                            "Forward",
                                            tint = if ((pageIndex != null && pageIndex < pageCount - 1) || (pageIndex == null && route == "main" && pageCount > 0)) Color.White else Color.DarkGray
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
