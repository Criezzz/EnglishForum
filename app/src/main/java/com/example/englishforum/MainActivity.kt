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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.englishforum.core.model.ThemeOption
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.data.auth.DataStoreUserSessionRepository
import com.example.englishforum.data.auth.FakeAuthRepository
import com.example.englishforum.data.settings.ThemePreferenceRepository
import com.example.englishforum.feature.auth.LoginScreen
import com.example.englishforum.feature.auth.LoginViewModel
import com.example.englishforum.feature.auth.LoginViewModelFactory
import com.example.englishforum.feature.profile.ProfileScreen
import com.example.englishforum.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

private sealed class Destinations(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    data object Home : Destinations("home", R.string.nav_home, Icons.Filled.Home)
    data object Create : Destinations("create", R.string.nav_create, Icons.Filled.AddCircle)
    data object Noti : Destinations("noti", R.string.nav_notifications, Icons.Filled.Notifications)
    data object Profile : Destinations("profile", R.string.nav_profile, Icons.Filled.Person)
    data object Settings : Destinations("settings", R.string.settings_title)

    companion object {
        val bottomBar = listOf(Home, Create, Noti, Profile)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val sessionRepository = remember { DataStoreUserSessionRepository(context) }
    val authRepository = remember { FakeAuthRepository(sessionRepository) }
    val themeRepository = remember { ThemePreferenceRepository(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "login" && currentRoute != "register" && currentRoute != "forgot" && currentRoute != Destinations.Settings.route
    val userSession by sessionRepository.sessionFlow.collectAsState(initial = null)
    val themeOption by themeRepository.themeOptionFlow.collectAsState(initial = ThemeOption.FOLLOW_SYSTEM)

    LaunchedEffect(userSession, currentRoute) {
        if (userSession != null && currentRoute == "login") {
            navController.navigate(Destinations.Home.route) {
                popUpTo("login") { inclusive = true }
            }
        } else if (userSession == null && currentRoute != null && currentRoute != "login") {
            navController.navigate("login") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    EnglishForumTheme(themeOption = themeOption) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    MainBottomBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "login",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") {
                    val vm: LoginViewModel = viewModel(factory = LoginViewModelFactory(authRepository))
                    LoginScreen(
                        viewModel = vm,
                        onLoginSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onRegisterClick = { navController.navigate("register") },
                        onForgotPasswordClick = { navController.navigate("forgot") }
                    )
                }

                composable("forgot") {
                    val vm: com.example.englishforum.feature.auth.ForgotPasswordViewModel = viewModel()
                    com.example.englishforum.feature.auth.ForgotPasswordScreen(
                        viewModel = vm,
                        onBackToLogin = {
                            navController.popBackStack()
                        },
                        onResetSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("register") {
                    val vm: com.example.englishforum.feature.auth.RegisterViewModel = viewModel()
                    com.example.englishforum.feature.auth.RegisterScreen(
                        viewModel = vm,
                        onRegisterSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }

                composable(Destinations.Home.route) {
                    PlaceholderScreen(titleRes = R.string.nav_home)
                }
                composable(Destinations.Create.route) {
                    PlaceholderScreen(titleRes = R.string.nav_create)
                }
                composable(Destinations.Noti.route) {
                    PlaceholderScreen(titleRes = R.string.nav_notifications)
                }
                composable(Destinations.Profile.route) {
                    ProfileScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSettingsClick = { navController.navigate(Destinations.Settings.route) }
                    )
                }
                composable(Destinations.Settings.route) {
                    SettingsScreen(
                        currentTheme = themeOption,
                        onThemeChange = { option ->
                            scope.launch {
                                themeRepository.setThemeOption(option)
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onLogoutClick = {
                            scope.launch {
                                sessionRepository.clearSession()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        Destinations.bottomBar.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon!!,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(titleRes: Int) {
    Text(text = stringResource(titleRes), modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
fun PreviewMainApp() {
    EnglishForumTheme {
        MainApp()
    }
}
