package com.example.englishforum

import android.net.Uri
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.englishforum.feature.postedit.PostEditRoute
import com.example.englishforum.feature.profile.ProfileScreen
import com.example.englishforum.feature.search.SearchRoute
import com.example.englishforum.feature.session.SessionMonitorState
import com.example.englishforum.feature.session.SessionMonitorViewModel
import com.example.englishforum.feature.session.SessionMonitorViewModelFactory
import com.example.englishforum.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

private sealed class Destinations(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    data object Home : Destinations("home", R.string.nav_home, Icons.Filled.Home)
    data object Search : Destinations("search", R.string.nav_search, Icons.Filled.Search)
    data object Create : Destinations("create", R.string.nav_create, Icons.Filled.AddCircle)
    data object Noti : Destinations("noti", R.string.nav_notifications, Icons.Filled.Notifications)
    data object Profile : Destinations("profile", R.string.nav_profile, Icons.Filled.Person)
    data object Settings : Destinations("settings", R.string.settings_title)

    companion object {
        val bottomBar = listOf(Home, Search, Create, Noti, Profile)
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
    val sessionValidator = remember { appContainer.sessionValidator }
    val networkMonitor = remember { appContainer.networkMonitor }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPostDetailRoute = currentRoute?.startsWith("post/") == true || currentRoute == "post/{postId}?commentId={commentId}"
    val isViewingOtherProfile = currentRoute?.let {
        it != Destinations.Profile.route && it.startsWith("profile")
    } == true
    val showBottomBar = currentRoute != "login" &&
        currentRoute != "register" &&
        currentRoute != "forgot" &&
        currentRoute != "verify" &&
        currentRoute != Destinations.Settings.route &&
        !isPostDetailRoute &&
        !isViewingOtherProfile
    val userSession by sessionRepository.sessionFlow.collectAsState(initial = null)
    val themeOption by themeRepository.themeOptionFlow.collectAsState(initial = ThemeOption.FOLLOW_SYSTEM)
    val sessionMonitorViewModel: SessionMonitorViewModel = viewModel(
        factory = remember(sessionRepository, sessionValidator, networkMonitor) {
            SessionMonitorViewModelFactory(
                userSessionRepository = sessionRepository,
                sessionValidator = sessionValidator,
                networkMonitor = networkMonitor
            )
        }
    )
    val sessionMonitorState by sessionMonitorViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionExpiredMessage = stringResource(R.string.session_expired_message)
    val sessionOfflineMessage = stringResource(R.string.session_offline_message)
    val sessionCheckErrorMessage = stringResource(R.string.session_check_error_message)
    val sessionRequiresVerificationMessage = stringResource(R.string.session_requires_verification_message)

    LaunchedEffect(sessionMonitorState) {
        when (val state = sessionMonitorState) {
            SessionMonitorState.Invalidated -> {
                snackbarHostState.showSnackbar(sessionExpiredMessage)
            }

            SessionMonitorState.Offline -> {
                snackbarHostState.showSnackbar(sessionOfflineMessage)
            }

            SessionMonitorState.RequiresVerification -> {
                snackbarHostState.showSnackbar(sessionRequiresVerificationMessage)
            }

            is SessionMonitorState.Error -> {
                snackbarHostState.showSnackbar(state.message ?: sessionCheckErrorMessage)
            }

            else -> Unit
        }
    }

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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                composable("login") { backStackEntry ->
                    val resetMessage by backStackEntry.savedStateHandle
                        .getStateFlow<String?>("passwordResetMessage", null)
                        .collectAsState()
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
                        onForgotPasswordClick = { navController.navigate("forgot") },
                        resetSuccessMessage = resetMessage,
                        onResetMessageShown = {
                            backStackEntry.savedStateHandle["passwordResetMessage"] = null
                        }
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
                            val resetMessage = context.getString(R.string.auth_password_reset_success_message)
                            scope.launch {
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "passwordResetMessage",
                                    resetMessage
                                )
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
                        },
                        onAuthorClick = { username ->
                            val encoded = Uri.encode(username)
                            navController.navigate("profile/$encoded")
                        }
                    )
                }
                composable(Destinations.Search.route) {
                    SearchRoute(
                        modifier = Modifier.fillMaxSize(),
                        onPostClick = { postId ->
                            navController.navigate("post/$postId")
                        },
                        onCommentClick = { postId ->
                            navController.navigate("post/$postId")
                        },
                        onAuthorClick = { username ->
                            val encoded = Uri.encode(username)
                            navController.navigate("profile/$encoded")
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
                        },
                        isOwnProfile = true
                    )
                }
                composable(
                    route = "profile/{username}",
                    arguments = listOf(
                        androidx.navigation.navArgument("username") {
                            type = androidx.navigation.NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val usernameArg = backStackEntry.arguments?.getString("username")?.let(Uri::decode)
                    val isOwnProfile = usernameArg?.let { username ->
                        val session = userSession
                        session != null && (
                            username.equals(session.username, ignoreCase = true) ||
                                username.equals(session.userId, ignoreCase = true)
                            )
                    } ?: false
                    ProfileScreen(
                        modifier = Modifier.fillMaxSize(),
                        userId = usernameArg,
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
                        },
                        isOwnProfile = isOwnProfile,
                        onBackClick = { navController.popBackStack() }
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
                            savedStateHandle = backStackEntry.savedStateHandle,
                            onNavigateToAiPractice = { practicePostId ->
                                navController.navigate("aiPractice/$practicePostId")
                            },
                            onEditPostClick = { editPostId ->
                                navController.navigate("post/$editPostId/edit")
                            },
                            onAuthorClick = { username ->
                                val encoded = Uri.encode(username)
                                navController.navigate("profile/$encoded")
                            }
                        )
                    }
                }
                composable(
                    route = "post/{postId}/edit",
                    arguments = listOf(
                        androidx.navigation.navArgument("postId") {
                            type = androidx.navigation.NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")
                    if (postId != null) {
                        PostEditRoute(
                            postId = postId,
                            onBackClick = { navController.popBackStack() },
                            onPostUpdated = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("post_edit_result", true)
                                navController.popBackStack()
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
