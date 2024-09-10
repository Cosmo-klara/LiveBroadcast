package com.example.livebroadcast.ui.module

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
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
import com.example.livebroadcast.settingData.SettingData
import com.example.livebroadcast.utility.NumberConvertTool

/**
 * 显示刷新间隔、比特率、fps 等
 *
 * @param modifier [Modifier]
 * @param mirroringData [SettingData]
 * @param isGrantedAudioPermission 如果具有录制权限为 true
 * @param onSettingClick
 */
@Composable
fun StreamInfo(
    modifier: Modifier = Modifier,
    mirroringData: SettingData,
    isGrantedAudioPermission: Boolean,
    onSettingClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(4.dp),
            text = stringResource(id = R.string.stream_info_title),
            style = TextStyle(fontWeight = FontWeight.Bold),
            fontSize = 18.sp,
            color = LocalContentColor.current
        )
        Spacer(modifier = Modifier.size(8.dp))

        Column(modifier = Modifier.padding(4.dp)) {
            Text(fontSize = 18.sp, text = stringResource(id = R.string.stream_info_internal_audio))
            Text(text = stringResource(id = if (mirroringData.isRecordInternalAudio) R.string.stream_info_enable else R.string.stream_info_disable))
            if (isGrantedAudioPermission) {
                Text(text = stringResource(id = R.string.stream_info_internal_audio_permission_error))
            }
        }
        Column(modifier = Modifier.padding(4.dp)) {
            Text(fontSize = 18.sp, text = stringResource(id = R.string.stream_info_interval))
            Text(text = (mirroringData.intervalMs / 1000).toInt().toString())
        }
        Column(modifier = Modifier.padding(4.dp)) {
            Text(fontSize = 18.sp, text = stringResource(id = R.string.stream_info_video_bitrate))
            Text(text = NumberConvertTool.convert(mirroringData.videoBitRate))
        }
        Column(modifier = Modifier.padding(4.dp)) {
            Text(fontSize = 18.sp, text = stringResource(id = R.string.stream_info_video_fps))
            Text(text = mirroringData.videoFrameRate.toString())
        }
        if (mirroringData.isCustomResolution) {
            Column(modifier = Modifier.padding(4.dp)) {
                Text(
                    fontSize = 18.sp,
                    text = stringResource(id = R.string.stream_info_video_resolution)
                )
                Text(text = "${mirroringData.videoWidth} x ${mirroringData.videoHeight}")
            }
        }
        Column(modifier = Modifier.padding(4.dp)) {
            Text(fontSize = 18.sp, text = stringResource(id = R.string.stream_info_audio_bitrate))
            Text(text = NumberConvertTool.convert(mirroringData.audioBitRate))
        }

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.End),
            onClick = onSettingClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_settings_24),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.stream_info_setting_button))
        }
    }
}
