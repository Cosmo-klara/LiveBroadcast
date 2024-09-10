package com.example.livebroadcast.ui.screen

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.example.livebroadcast.R
import com.example.livebroadcast.ScreenMirroringService
import com.example.livebroadcast.settingData.SettingData
import com.example.livebroadcast.setting.Key
import com.example.livebroadcast.setting.dataStore
import com.example.livebroadcast.utility.DisplayTool
import com.example.livebroadcast.utility.IntentTool
import com.example.livebroadcast.utility.IpAddressTool
import com.example.livebroadcast.utility.PermissionTool
import com.example.livebroadcast.ui.module.CurrentTimeTitle
import com.example.livebroadcast.ui.module.IntroductionCard
import com.example.livebroadcast.ui.module.InternalAudioPermissionCard
import com.example.livebroadcast.ui.module.MirroringButton
import com.example.livebroadcast.ui.module.NotificationPermissionCard
import com.example.livebroadcast.ui.module.StreamInfo
import com.example.livebroadcast.ui.module.StreamingTypeCard
import com.example.livebroadcast.ui.module.UrlCard
import kotlinx.coroutines.launch

/**
 * 主屏幕
 *
 * @param onSettingClick 按“设置”时
 * @param onNavigate 画面切换时，传递路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 在 Flow 中接收 IP 地址
    val idAddress =
        remember { IpAddressTool.collectIpAddress(context) }.collectAsState(initial = null)

    // 镜像数据
    val mirroringData =
        remember { SettingData.loadDataStore(context) }.collectAsState(initial = null)

    // DataStore监控
    val dataStore = remember { context.dataStore.data }.collectAsState(initial = null)

    // 绑定镜像服务获取实例
    val mirroringService = remember {
        ScreenMirroringService.bindScreenMirrorService(
            context,
            lifecycleOwner.lifecycle
        )
    }.collectAsState(initial = null)

    // 是否镜像
    val isScreenMirroring = mirroringService.value?.isScreenMirroring?.collectAsState()

    // Android 10之前不支持麦克风录音权限，一律为 false；Android 10及以后如果没有权限则为 true。
    val isGrantedRecordAudio = remember {
        mutableStateOf(
            if (PermissionTool.isAndroidQAndHigher) {
                !PermissionTool.isGrantedRecordPermission(context)
            } else false
        )
    }

    // 通知权限是否存在，将前台服务执行中的通知发布出来
    val isGrantedPostNotification = remember {
        mutableStateOf(
            if (PermissionTool.isAndroidTiramisuAndHigher) {
                !PermissionTool.isGrantedPostNotificationPermission(context)
            } else false
        )
    }

    // 启动服务以获取权限
    val mediaProjectionManager =
        remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val requestCapture =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // 镜像服务已启动
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val (height, width) = DisplayTool.getDisplaySize((context as Activity))
                mirroringService.value?.startScreenMirroring(
                    result.resultCode,
                    result.data!!,
                    height,
                    width
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.home_screen_permission_result_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 当前时间
            CurrentTimeTitle(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onSettingClick = onSettingClick
            )
        }
    ) { paddingValues ->

        LazyColumn(modifier = Modifier.padding(paddingValues)) {

            item {
                MirroringButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    isScreenMirroring = isScreenMirroring?.value == true,
                    onStartClick = {
                        // 请求捕获权限
                        requestCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                    },
                    onStopClick = {
                        // 终止
                        mirroringService.value?.stopScreenMirroringAndServerRestart()
                    }
                )
            }

            item {
                if (dataStore.value?.contains(Key.IS_HIDE_HELLO_CARD) == false) {
                    IntroductionCard(
                        modifier = Modifier.fillMaxWidth(),
                        onHelloClick = { onNavigate(MainScreenNavigationLinks.IntroductionScreen) },
                        onClose = {
                            // 关闭后不再展示
                            scope.launch {
                                context.dataStore.edit { it[Key.IS_HIDE_HELLO_CARD] = true }
                            }
                        }
                    )
                }
            }

            if (mirroringData.value != null && idAddress.value != null) {
                item {
                    // 创建 URL
                    val url = "http://${idAddress.value}:${mirroringData.value?.portNumber}"
                    UrlCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        url = url,
                        onShareClick = {
                            context.startActivity(IntentTool.createShareIntent(url))
                        },
                        onOpenBrowserClick = {
                            context.startActivity(IntentTool.createOpenBrowserIntent(url))
                        }
                    )
                }
            }

            // 内部音频的麦克风权限
            if (isGrantedRecordAudio.value) {
                item {
                    InternalAudioPermissionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        permissionResult = { isGranted ->
                            isGrantedRecordAudio.value = !isGranted
                            // 启用录制内部音频的设置
                            scope.launch {
                                val updatedData =
                                    mirroringData.value?.copy(isRecordInternalAudio = true)
                                        ?: return@launch
                                SettingData.setDataStore(context, updatedData)
                            }
                        }
                    )
                }
            }

            // 申请通知权限。
            if (isGrantedPostNotification.value) {
                item {
                    NotificationPermissionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        permissionResult = { isGranted ->
                            isGrantedPostNotification.value = !isGranted
                        }
                    )
                }
            }

            // 选择流式处理方法
            if (mirroringData.value != null) {
                item {
                    StreamingTypeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        currentType = mirroringData.value!!.streamingType,
                        onClick = { type ->
                            // 保存设置
                            scope.launch {
                                SettingData.setDataStore(
                                    context,
                                    mirroringData.value!!.copy(streamingType = type)
                                )
                            }
                        }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                ) {
                    // 编码器
                    if (mirroringData.value != null) {
                        StreamInfo(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            mirroringData = mirroringData.value!!,
                            isGrantedAudioPermission = isGrantedRecordAudio.value,
                            onSettingClick = { onNavigate(MainScreenNavigationLinks.MirroringSettingScreen) }
                        )
                    }
                }
            }

        }
    }
}