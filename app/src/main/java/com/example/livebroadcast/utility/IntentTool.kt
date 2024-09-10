package com.example.livebroadcast.utility

import android.content.Intent
import androidx.core.net.toUri

// Intent
object IntentTool {

    /**
     * 创建一个用于分享的Intent
     * 允许用户通过系统选择器分享指定的URL，参考 [https://cloud.tencent.com/developer/article/1743463]
     *
     * @param url 要分享的URL字符串
     * @return 用于分享操作的[Intent]实例
     */
    fun createShareIntent(url: String): Intent {
        return Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }, null)
    }

    /**
     * 创建一个用于在浏览器中打开URL的Intent
     *
     * @param url 要在浏览器中打开的URL字符串
     * @return 用于启动浏览器并查看URL的[Intent]实例
     */
    fun createOpenBrowserIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, url.toUri())
    }
}