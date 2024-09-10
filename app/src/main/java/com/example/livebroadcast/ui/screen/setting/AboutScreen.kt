package com.example.livebroadcast.ui.screen.setting

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import com.example.livebroadcast.R
import com.example.livebroadcast.ui.module.AboutComponent
import com.example.livebroadcast.ui.module.AboutURL

/**
 * 关于界面
 *
 * @param onBack 返回
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val isPortRate = remember { context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
    val version =
        remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(it)
        ) {
            IconButton(
                modifier = Modifier.align(alignment = Alignment.Start),
                onClick = onBack
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_arrow_back_24),
                    contentDescription = null
                )
            }

            AboutComponent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                version = version
            )

            AboutURL(
                isScrollable = isPortRate,
                onGitHubClick = { openBrowser(context, GitHubUrl) },
            )
        }
    }
}

private const val GitHubUrl = "https://github.com/Cosmo-klara?tab=repositories"

private fun openBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}
