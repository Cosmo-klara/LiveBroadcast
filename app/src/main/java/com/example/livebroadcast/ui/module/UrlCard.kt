package com.example.livebroadcast.ui.module

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livebroadcast.R
import com.example.livebroadcast.utility.QrCodeGeneratorTool


/**
 * 显示用于镜像查看的 URL
 *
 * @param modifier [Modifier]
 * @param url 用于查看的 URL
 * @param onShareClick
 * @param onOpenBrowserClick
 */

@Composable
fun UrlCard(
    modifier: Modifier = Modifier,
    url: String,
    onShareClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
) {
    // 二维码是否生成
    val isShowQrCode = remember { mutableStateOf(false) }
    // 二维码的位图
    val qrCodeBitmap = remember(url) { QrCodeGeneratorTool.generateQrCode(url) }

    // 卡片布局
    Card(modifier = modifier) {
        // 垂直排列
        Column(modifier = Modifier.padding(8.dp)) {
            // 标题
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = R.string.url_card_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp
            )
            // URL
            Text(
                modifier = Modifier.padding(4.dp),
                text = url,
                fontSize = 18.sp
            )

            // 渲染二维码图像
            if (isShowQrCode.value) {
                Surface(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = null
                    )
                }
            }

            // 描述
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = R.string.url_card_description),
            )

            // 水平滚动布局
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // 分享按钮
                Button(onClick = onShareClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_share_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_share))
                }

                Spacer(modifier = Modifier.size(8.dp))
                // 渲染二维码按钮
                OutlinedButton(onClick = { isShowQrCode.value = !isShowQrCode.value }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_qr_code_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_qr_code))
                }

                Spacer(modifier = Modifier.size(8.dp))
                // 打开浏览器按钮
                OutlinedButton(onClick = onOpenBrowserClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.url_card_open_browser))
                }
            }
        }
    }
}