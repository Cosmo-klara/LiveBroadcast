package com.example.livebroadcast.ui.module

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livebroadcast.R

/**
 * 获取通知权限的组件
 *
 * @param modifier [Modifier]
 * @param permissionResult
 */
@Composable
fun NotificationPermissionCard(
    modifier: Modifier = Modifier,
    permissionResult: (Boolean) -> Unit,
) {
    // 权限回调
    val permissionRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGrant ->
            permissionResult(isGrant)
        }

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = R.string.post_notification_permission_card_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))

            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = R.string.post_notification_permission_card_description)
            )
            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.End),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_settings_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.post_notification_permission_card_button))
            }
        }
    }
}