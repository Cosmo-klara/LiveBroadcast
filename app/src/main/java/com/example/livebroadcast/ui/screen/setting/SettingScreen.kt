package com.example.livebroadcast.ui.screen.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.livebroadcast.ui.module.SettingComponent
import com.example.livebroadcast.ui.screen.MainScreenNavigationLinks
import com.example.livebroadcast.R
/**
 * 设置界面
 *
 * @param onBack 返回
 * @param onNavigate 子界面切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // 使用 Scaffold 作为基本布局
    Scaffold(
        topBar = {
            // 大顶部栏 （标题，返回图标）
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.setting_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) {
            paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // 使用 LazyColumn 展示设置项，节省内存并优化性能（其实好像不用哈哈，太少了）
            LazyColumn {
                item {
                    SettingComponent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.setting_stream_title),
                        iconRes = R.drawable.ic_outline_videocam_24,
                        description = stringResource(id = R.string.setting_stream_description),
                        onClick = { onNavigate(MainScreenNavigationLinks.MirroringSettingScreen) }
                    )
                }
                item {
                    SettingComponent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.setting_about_this_app_title),
                        iconRes = R.drawable.ic_outline_info_24,
                        description = stringResource(id = R.string.setting_about_this_app_description),
                        onClick = { onNavigate(MainScreenNavigationLinks.AboutScreen) }
                    )
                }
            }
        }
    }
}