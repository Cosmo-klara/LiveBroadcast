package com.example.livebroadcast.utility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// 权限
object PermissionTool {

    /**
     * 若授予权限则返回 true
     *
     * @param context [Context]
     * @param permission 权限名称
     */
    private fun isGrantedPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 录制内部音频需要 Android 10 或更高版本。
     *
     * @return 适用于 Android 10 及更高版本
     */
    val isAndroidQAndHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * 适用于 Android 13 及更高版本
     * 我需要获得通知许可吗？运行前台服务时不需要，但需要在通知区域显示。
     */
    val isAndroidTiramisuAndHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * 检测是否具有麦克风权限
     *
     * @param context [Context]
     * @return 有权限则返回 true
     */
    fun isGrantedRecordPermission(context: Context) =
        isGrantedPermission(context, Manifest.permission.RECORD_AUDIO)

    /**
     * 检查是否具有通知权限
     * 仅适用于 Android 13 及以上
     *
     * @param context [context]
     * @return 低于 Android 13 一律返回 false
     */
    fun isGrantedPostNotificationPermission(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGrantedPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
}