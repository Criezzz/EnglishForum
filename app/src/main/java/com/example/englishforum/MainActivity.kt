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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.ThemeOption
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.feature.auth.LoginScreen
import com.example.englishforum.feature.auth.LoginViewModel
import com.example.englishforum.feature.auth.LoginViewModelFactory
import com.example.englishforum.feature.aipractice.AiPracticeRoute
import com.example.englishforum.feature.home.HomeScreen
import com.example.englishforum.feature.noti.NotiRoute
import com.example.englishforum.feature.postdetail.PostDetailRoute
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
    private val appContainer by lazy {
        (application as EnglishForumApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val appContainer = LocalAppContainer.current
    val sessionRepository = remember { appContainer.userSessionRepository }
    val authRepository = remember { appContainer.authRepository }
    val themeRepository = remember { appContainer.themePreferenceRepository }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "login" && currentRoute != "register" && currentRoute != "forgot" && currentRoute != "verify" && currentRoute != Destinations.Settings.route
    val userSession by sessionRepository.sessionFlow.collectAsState(initial = null)
    val themeOption by themeRepository.themeOptionFlow.collectAsState(initial = ThemeOption.FOLLOW_SYSTEM)

    LaunchedEffect(userSession, currentRoute) {
        val authRoutes = setOf("login", "register", "forgot", "verify")
        val session = userSession
        when {
            session == null && currentRoute != null && currentRoute !in authRoutes -> {
                navController.navigate("login") {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }

            session != null && !session.isEmailVerified && currentRoute != "verify" -> {
                navController.navigate("verify") {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                    launchSingleTop = true
                }
            }

            session != null && session.isEmailVerified && (currentRoute == "login" || currentRoute == "register" || currentRoute == "verify" || currentRoute == "forgot") -> {
                navController.navigate(Destinations.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
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
                    val loginViewModel: LoginViewModel = viewModel(
                        factory = remember(authRepository) { LoginViewModelFactory(authRepository) }
                    )
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onRequireVerification = {
                            navController.navigate("verify") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onRegisterClick = { navController.navigate("register") },
                        onForgotPasswordClick = { navController.navigate("forgot") }
                    )
                }

                composable("forgot") {
                    val forgotViewModel: com.example.englishforum.feature.auth.ForgotPasswordViewModel = viewModel(
                        factory = remember(authRepository) {
                            com.example.englishforum.feature.auth.ForgotPasswordViewModelFactory(authRepository)
                        }
                    )
                    com.example.englishforum.feature.auth.ForgotPasswordScreen(
                        viewModel = forgotViewModel,
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
                    val registerViewModel: com.example.englishforum.feature.auth.RegisterViewModel = viewModel(
                        factory = remember(authRepository) {
                            com.example.englishforum.feature.auth.RegisterViewModelFactory(authRepository)
                        }
                    )
                    com.example.englishforum.feature.auth.RegisterScreen(
                        viewModel = registerViewModel,
                        onVerificationRequired = {
                            navController.navigate("verify") {
                                popUpTo("register") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onRegisterSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }

                composable("verify") {
                    val verificationViewModel: com.example.englishforum.feature.auth.EmailVerificationViewModel = viewModel(
                        factory = remember(authRepository) {
                            com.example.englishforum.feature.auth.EmailVerificationViewModel.Factory(authRepository)
                        }
                    )
                    com.example.englishforum.feature.auth.EmailVerificationScreen(
                        viewModel = verificationViewModel,
                        onVerificationSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onBackToLogin = {
                            scope.launch { sessionRepository.clearSession() }
                            navController.navigate("login") {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Destinations.Home.route) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onPostClick = { postId ->
                            navController.navigate("post/$postId")
                        },
                        onCommentClick = { postId ->
                            navController.navigate("post/$postId")
                        }
                    )
                }
                composable(Destinations.Create.route) {
                    com.example.englishforum.feature.create.CreateRoute(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToPostDetail = { newPostId ->
                            navController.navigate("post/$newPostId")
                        }
                    )
                }
                composable(Destinations.Noti.route) {
                    NotiRoute(
                        modifier = Modifier.fillMaxSize(),
                        onNotificationClick = { postId, commentId ->
                            if (commentId != null) {
                                navController.navigate("post/$postId?commentId=$commentId")
                            } else {
                                navController.navigate("post/$postId")
                            }
                        }
                    )
                }
                composable(Destinations.Profile.route) {
                    ProfileScreen(
                        modifier = Modifier.fillMaxSize(),
                        userId = userSession?.userId,
                        onSettingsClick = { navController.navigate(Destinations.Settings.route) },
                        onPostClick = { postId ->
                            navController.navigate("post/$postId")
                        },
                        onReplyClick = { postId, commentId ->
                            if (commentId != null) {
                                navController.navigate("post/$postId?commentId=$commentId")
                            } else {
                                navController.navigate("post/$postId")
                            }
                        }
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
                composable(
                    route = "post/{postId}?commentId={commentId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("postId") {
                            type = androidx.navigation.NavType.StringType
                        },
                        androidx.navigation.navArgument("commentId") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")
                    val commentId = backStackEntry.arguments?.getString("commentId")
                    if (postId != null) {
                        PostDetailRoute(
                            modifier = Modifier.fillMaxSize(),
                            postId = postId,
                            commentId = commentId,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToAiPractice = { practicePostId ->
                                navController.navigate("aiPractice/$practicePostId")
                            }
                        )
                    }
                }
                composable(
                    route = "aiPractice/{postId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("postId") {
                            type = androidx.navigation.NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")
                    if (postId != null) {
                        AiPracticeRoute(
                            postId = postId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
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

@Preview(showBackground = true)
@Composable
fun PreviewMainApp() {
    val context = LocalContext.current
    CompositionLocalProvider(LocalAppContainer provides com.example.englishforum.core.di.DefaultAppContainer(context)) {
        EnglishForumTheme {
            MainApp()
        }
    }
}
