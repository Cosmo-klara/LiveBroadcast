package com.example.livebroadcast.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.livebroadcast.ui.screen.setting.AboutScreen
import com.example.livebroadcast.ui.screen.setting.MirroringSettingScreen
import com.example.livebroadcast.ui.screen.setting.SettingScreen
import com.example.livebroadcast.ui.theme.LiveBroadcastTheme


@Composable
fun MainScreen() {
    LiveBroadcastTheme {
        // 主屏幕路由
        val mainScreenNavigation = rememberNavController()

        NavHost(
            navController = mainScreenNavigation,
            startDestination = MainScreenNavigationLinks.HomeScreen
        ) {
            // 应用说明
            composable(MainScreenNavigationLinks.IntroductionScreen) {
                IntroductionScreen(
                    onNextClick = { mainScreenNavigation.popBackStack() },
                    onBack = { mainScreenNavigation.popBackStack() }
                )
            }
            // 主屏幕
            composable(MainScreenNavigationLinks.HomeScreen) {
                HomeScreen(
                    onSettingClick = { mainScreenNavigation.navigate(MainScreenNavigationLinks.SettingScreen) },
                    onNavigate = { mainScreenNavigation.navigate(it) }
                )
            }
            // 设置屏幕
            composable(MainScreenNavigationLinks.SettingScreen) {
                SettingScreen(
                    onBack = { mainScreenNavigation.popBackStack() },
                    onNavigate = { mainScreenNavigation.navigate(it) }
                )
            }
            // 屏幕共享设置
            composable(MainScreenNavigationLinks.MirroringSettingScreen) {
                MirroringSettingScreen(onBack = { mainScreenNavigation.popBackStack() })
            }
            // 关于程序
            composable(MainScreenNavigationLinks.AboutScreen) {
                AboutScreen(onBack = { mainScreenNavigation.popBackStack() })
            }
        }
    }
}