package com.example.livebroadcast.ui.module

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.livebroadcast.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 显示当前时间，本意是为了观察延迟
 *
 *
 * @param modifier [Modifier]
 * @param scrollBehavior
 * @param onSettingClick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTimeTitle(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingClick: () -> Unit,
) {
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            delay(1000L)
            currentTime.longValue = System.currentTimeMillis()
        }
    }

    LargeTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = """
                    ${stringResource(id = R.string.time_component_time_now)} ${
                    timeToFormat(
                        currentTime.longValue
                    )
                }
                """.trimIndent()
            )
        },
        actions = {
            IconButton(onClick = onSettingClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_settings_24),
                    contentDescription = null
                )
            }
        }
    )
}

// 格式化时间
private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

/**
 * 格式化并返回 UnixTime
 *
 * @param unixTime UnixTime (ms)
 * @return
 */
private fun timeToFormat(unixTime: Long) = simpleDateFormat.format(unixTime)