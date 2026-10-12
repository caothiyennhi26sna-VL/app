package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThinkingSessionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ThinkingViewModel

sealed class AppDestination {
    data object Home : AppDestination()
    data class Session(val sessionId: Long) : AppDestination()
    data object Journal : AppDestination()
    data object Settings : AppDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavHost()
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    viewModel: ThinkingViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Home) }

    val showBottomNav = currentDestination !is AppDestination.Session

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentDestination is AppDestination.Home,
                        onClick = { currentDestination = AppDestination.Home },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination is AppDestination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Trang chủ"
                            )
                        },
                        label = { Text("Gợi mở") },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentDestination is AppDestination.Journal,
                        onClick = { currentDestination = AppDestination.Journal },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination is AppDestination.Journal) Icons.Filled.Book else Icons.Outlined.Book,
                                contentDescription = "Nhật ký"
                            )
                        },
                        label = { Text("Đúc kết") },
                        modifier = Modifier.testTag("nav_journal")
                    )

                    NavigationBarItem(
                        selected = currentDestination is AppDestination.Settings,
                        onClick = { currentDestination = AppDestination.Settings },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination is AppDestination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Cài đặt"
                            )
                        },
                        label = { Text("Cài đặt") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier.padding(innerPadding)
        ) { destination ->
            when (destination) {
                is AppDestination.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSession = { sessionId ->
                            currentDestination = AppDestination.Session(sessionId)
                        },
                        onNavigateToJournal = { currentDestination = AppDestination.Journal },
                        onNavigateToSettings = { currentDestination = AppDestination.Settings }
                    )
                }

                is AppDestination.Session -> {
                    ThinkingSessionScreen(
                        sessionId = destination.sessionId,
                        viewModel = viewModel,
                        onNavigateBack = {
                            viewModel.clearCurrentSession()
                            currentDestination = AppDestination.Home
                        }
                    )
                }

                is AppDestination.Journal -> {
                    JournalScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.Home },
                        onSelectSession = { sessionId ->
                            currentDestination = AppDestination.Session(sessionId)
                        }
                    )
                }

                is AppDestination.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.Home }
                    )
                }
            }
        }
    }
}
