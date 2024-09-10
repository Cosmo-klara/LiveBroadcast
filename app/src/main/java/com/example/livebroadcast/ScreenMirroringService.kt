package com.example.livebroadcast

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.livebroadcast.dashWebM.DashStreaming
import com.example.livebroadcast.settingData.SettingData
import com.example.livebroadcast.settingData.StreamingType
import com.example.livebroadcast.handler.StreamingInterface
import com.example.livebroadcast.utility.IpAddressTool
import com.example.livebroadcast.utility.QrCodeGeneratorTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class ScreenMirroringService : Service() {
    // 协程
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var mirroringJob: Job? = null
    private val _isScreenMirroring = MutableStateFlow(false)

    private val localBinder = LocalBinder(this)

    // 检索 DataStore 中的设置
    private val settingDataFlow by lazy { SettingData.loadDataStore(this@ScreenMirroringService) }

    // 在镜像中使用
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null

    // 镜像的接口
    private var streaming: StreamingInterface? = null

    // 是否镜像
    val isScreenMirroring = _isScreenMirroring.asStateFlow()

    override fun onBind(intent: Intent): IBinder = localBinder

    override fun onCreate() {
        super.onCreate()
        // 监视和反映数据存储（DataStore）设置
        coroutineScope.launch {
            settingDataFlow.collect { mirroringSettingData ->
                // 如果正在进行镜像，终止
                if (isScreenMirroring.value) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ScreenMirroringService,
                            R.string.service_stop_because_setting_update,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                mirroringJob?.cancelAndJoin()
                streaming?.destroy()

                // 创建实例并启动 Web 服务器
                streaming = when (mirroringSettingData.streamingType) {

                    StreamingType.MpegDash -> DashStreaming(
                        context = this@ScreenMirroringService,
                        parentFolder = getExternalFilesDir(null)!!,
                        settingData = mirroringSettingData
                    )
                    /**
                     * 在此处可以拓展编码方式供使用者选择，形式如上，参考 [StreamingType] 中的 enum class
                     */
                }.apply { startServer() }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 如果正在进行镜像，不终止服务
        // 如果没有，退出并释放资源
        if (!isScreenMirroring.value) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        mirroringJob?.cancel()
        streaming?.destroy()
    }

    /**
     * 开始镜像
     *
     * @param displayHeight
     * @param displayWidth
     */
    fun startScreenMirroring(
        resultCode: Int,
        resultData: Intent,
        displayHeight: Int,
        displayWidth: Int
    ) {
        coroutineScope.launch {
            mirroringJob?.cancelAndJoin()
            mirroringJob = launch {

                // 发出通知以使其成为前台服务
                notifyIpAddress()
                launch {
                    val mirroringSettingData = settingDataFlow.first()
                    IpAddressTool.collectIpAddress(this@ScreenMirroringService)
                        .collect { ipAddress ->
                            val url = "http://$ipAddress:${mirroringSettingData.portNumber}"
                            notifyIpAddress(
                                contentText = "${getString(R.string.ip_address)}：$url",
                                url = url
                            )
                            Log.d(TAG, url)
                        }
                }

                try {
                    mediaProjection =
                        mediaProjectionManager.getMediaProjection(resultCode, resultData)
                    _isScreenMirroring.value = true
                    // 准备和启动镜像
                    // 协程在编码期间暂停
                    streaming?.prepareAndStartEncode(mediaProjection!!, displayHeight, displayWidth)
                } finally {
                    // 取消协程时释放资源
                    // 前台服务解除
                    _isScreenMirroring.value = false
                    mediaProjection?.stop()
                    ServiceCompat.stopForeground(
                        this@ScreenMirroringService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE
                    )
                }
            }
        }
    }

    // 终止镜像，删除前台服务
    fun stopScreenMirroringAndServerRestart() {
        mirroringJob?.cancel()
    }

    /**
     * 创建和发送通知
     *
     * @param contentText 通知正文
     * @param url 用作二维码的 URL（如果有）
     */
    private fun notifyIpAddress(
        contentText: String = getString(R.string.service_notification_content),
        url: String? = null
    ) {
        // 通知频道
        val notificationManagerCompat = NotificationManagerCompat.from(this@ScreenMirroringService)
        if (notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(
                NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).apply {
                setName(getString(R.string.notification_channel_title))
            }.build()
            notificationManagerCompat.createNotificationChannel(channel)
        }
        // 创建通知
        val notification =
            NotificationCompat.Builder(this@ScreenMirroringService, NOTIFICATION_CHANNEL_ID)
                .also { builder ->
                    builder.setContentTitle(getString(R.string.service_notification_title))
                    builder.setContentText(contentText)
                    builder.setSmallIcon(R.drawable.livebroadcast_android)
                    builder.setContentIntent(
                        PendingIntent.getActivity(
                            this,
                            1,
                            Intent(this, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    if (url != null) {
                        val bitmap = QrCodeGeneratorTool.generateQrCode(url)
                        builder.setLargeIcon(bitmap)
                    }
                }.build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            // Android 10 及以下版本似乎未使用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
        )
    }

    private class LocalBinder(service: ScreenMirroringService) : Binder() {
        val serviceRef = WeakReference(service)
        val service: ScreenMirroringService
            get() = serviceRef.get()!!
    }

    companion object {
        private val TAG = ScreenMirroringService::class.simpleName

        // 通知 ID
        private const val NOTIFICATION_ID = 4545

        // 通知通道 ID
        private const val NOTIFICATION_CHANNEL_ID = "running_screen_mirror_service"

        /**
         * 绑定镜像服务并获取该服务的实例
         * 跟踪其生命周期并自动释放它们
         *
         * 在镜像过程中，如果从应用程序画面切换，将在[onTaskRemoved]中确保不会终止镜像。
         * @param context [Context]
         * @param lifecycle [Lifecycle]
         */
        fun bindScreenMirrorService(
            context: Context,
            lifecycle: Lifecycle
        ) = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val screenMirroringService = (service as LocalBinder).service
                    trySend(screenMirroringService)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }
            // 监控绑定和解绑的生命周期
            val lifecycleObserver = object : DefaultLifecycleObserver {
                val intent = Intent(context, ScreenMirroringService::class.java)
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    context.startService(intent)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    context.unbindService(serviceConnection)
                }
            }
            lifecycle.addObserver(lifecycleObserver)
            awaitClose { lifecycle.removeObserver(lifecycleObserver) }
        }
    }
}