package com.runshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.ui.screens.*
import com.runshare.app.ui.theme.RunShareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RunShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunShareNavigation()
                }
            }
        }

        // 处理Deep Link
        handleDeepLink()
    }

    private fun handleDeepLink() {
        intent?.data?.let { uri ->
            if (uri.scheme == "runshare" && uri.host == "share") {
                // TODO: 处理分享链接
                val data = uri.getQueryParameter("data")
            }
        }
    }
}

@Composable
fun RunShareNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefsRepository = remember { PreferencesRepository(context) }
    val isLoggedIn by prefsRepository.isLoggedIn.collectAsState(initial = null)

    // 等待登录状态加载
    if (isLoggedIn == null) return

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Auth.route
    ) {
        // 登录/注册
        composable(Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // 主页
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onNavigateToCheckIn = {
                    navController.navigate(Screen.CheckIn.route)
                },
                onNavigateToGroup = {
                    navController.navigate(Screen.Group.route)
                }
            )
        }

        // 历史记录
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { runId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(runId))
                }
            )
        }

        // 历史详情
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(navArgument("runId") { type = NavType.LongType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId") ?: 0L
            HistoryDetailScreen(
                runId = runId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 排行榜
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 打卡
        composable(Screen.CheckIn.route) {
            CheckInScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 小组
        composable(Screen.Group.route) {
            GroupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Leaderboard : Screen("leaderboard")
    object CheckIn : Screen("checkin")
    object Group : Screen("group")
    object HistoryDetail : Screen("history/{runId}") {
        fun createRoute(runId: Long) = "history/$runId"
    }
}

