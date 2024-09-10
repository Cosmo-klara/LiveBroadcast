package com.example.livebroadcast.ui.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livebroadcast.R

// 关于此应用程序的组件

/**
 *
 * @param modifier [Modifier]
 * @param version 应用程序版本
 */
@Composable
fun AboutComponent(
    modifier: Modifier = Modifier,
    version: String,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .padding(8.dp)
                    .size(150.dp),
                painter = painterResource(id = R.drawable.livebroadcast_android),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }
        // 出现文字
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                modifier = Modifier
                    .padding(bottom = 4.dp),
                text = "${stringResource(id = R.string.app_name)} ( $version ) ",
                fontSize = 18.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.about_this_app_message),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AboutURL(
    isScrollable: Boolean,
    onGitHubClick: () -> Unit,
) {

    Row(
        modifier = if (isScrollable) {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        } else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        AboutScreen(text = stringResource(id = R.string.about_this_app_open_github), onClick = onGitHubClick)
    }
}

@Composable
private fun AboutScreen(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = CutCornerShape(10.dp, 0.dp, 0.dp, 0.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            Text(text = text)
        }
    }
}
