package com.example.englishforum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.englishforum.core.ui.theme.EnglishForumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishForumTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.Home.route) {
                ScreenContent(title = "Home") //placeholder cho UI
            }
            composable(Destinations.Create.route) {
                ScreenContent(title = "Create") //placeholder cho UI
            }
            composable(Destinations.Noti.route) {
                ScreenContent(title = "Notifications") //placeholder cho UI
            }
            composable(Destinations.Profile.route) {
                ScreenContent(title = "Profile") //placeholder cho UI
            }
        }
    }
}

private sealed class Destinations(val route: String, val label: String) {
    data object Home : Destinations("home", "Home")
    data object Create : Destinations("create", "Create")
    data object Noti : Destinations("noti", "Noti")
    data object Profile : Destinations("profile", "Profile")

    companion object {
        val all = listOf(Home, Create, Noti, Profile)
    }
}

@Composable
private fun BottomBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        Destinations.all.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            val icon = when (destination) {
                Destinations.Home -> Icons.Filled.Home
                Destinations.Create -> Icons.Filled.AddCircle
                Destinations.Noti -> Icons.Filled.Notifications
                Destinations.Profile -> Icons.Filled.Person
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        // Multiple back stacks for each top-level destination
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun ScreenContent(title: String) { //đây là place holder tạm
    Text(text = title, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
fun PreviewMainApp() {
    EnglishForumTheme {
        MainApp()
    }
}