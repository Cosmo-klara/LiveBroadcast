package com.example.livebroadcast.ui.module

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livebroadcast.R


/**
 * 设置项
 *
 * @param title 标题
 * @param description 说明
 * @param iconRes 图标资源图像
 * @param onClick 按下时
 * */
@Composable
fun SettingComponent(
    modifier: Modifier = Modifier,
    title: String,
    description: String = "",
    iconRes: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(38.dp)
                    .padding(end = 8.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(bottom = 4.dp),
                    fontSize = 18.sp
                )
                Text(text = description)
            }
        }
    }
}

/**
 * 使用开关进行设置
 *
 * @param modifier [Modifier]
 * @param title 标题
 * @param subTitle 标题下方文字
 * @param description 说明
 * @param iconRes 图标资源 ID
 * @param isEnable 是否打开开关
 * @param onValueChange 切换时调用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchSettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    description: String? = null,
    iconRes: Int,
    isEnable: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        onClick = { onValueChange(!isEnable) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(38.dp)
                    .padding(end = 8.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column {
                Row {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(bottom = 4.dp),
                            fontSize = 18.sp
                        )
                        if (subTitle != null) {
                            Text(text = subTitle)
                        }
                    }
                    Switch(
                        modifier = Modifier.padding(8.dp),
                        checked = isEnable,
                        onCheckedChange = onValueChange
                    )
                }
                if (description != null) {
                    SettingItemDescriptionCard(description = description)
                }
            }
        }
    }
}

/**
 * 只接受初始值，并特意将该值保留在内部
 * 之后，仅在更改时才调用它
 *
 * @param modifier [Modifier]
 * @param title 标题
 * @param description 说明
 * @param inputUnderText 输入下方的说明
 * @param iconRes 图标资源图像
 * @param initValue 初始设定值
 * @param onValueChange 切换时调用
 */
@Composable
fun TextBoxInitValueSettingItem(
    modifier: Modifier = Modifier,
    title: String,
    initValue: String,
    description: String? = null,
    inputUnderText: String? = null,
    iconRes: Int,
    keyboardType: KeyboardType = KeyboardType.Number,
    onValueChange: (String) -> Unit,
) {
    val value = remember { mutableStateOf(initValue) }
    TextBoxSettingItem(
        modifier = modifier,
        label = title,
        inputValue = value.value,
        description = description,
        inputUnderText = inputUnderText,
        iconRes = iconRes,
        keyboardType = keyboardType,
        onValueChange = { changeValue ->
            value.value = changeValue
            onValueChange(changeValue)
        },
    )
}

/**
 * 输入功能的设置项目
 *
 * @param modifier [Modifier]
 * @param label 标题
 * @param description 说明
 * @param inputUnderText 输入下方的说明
 * @param iconRes 图标资源图像
 * @param inputValue 要放入文本框中的值
 * @param onValueChange 切换时调用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextBoxSettingItem(
    modifier: Modifier = Modifier,
    label: String,
    inputValue: String,
    description: String? = null,
    inputUnderText: String? = null,
    iconRes: Int,
    keyboardType: KeyboardType = KeyboardType.Number,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(38.dp)
                    .padding(end = 8.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = inputValue,
                    label = { Text(text = label) },
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                )

                if (inputUnderText != null) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = inputUnderText
                    )
                }

                if (description != null) {
                    SettingItemDescriptionCard(description = description)
                }
            }
        }
    }
}

/**
 * 各设置项的解释部分
 *
 * @param modifier [Modifier]
 * @param description 说明
 */
@ExperimentalMaterial3Api
@Composable
fun SettingItemDescriptionCard(
    modifier: Modifier = Modifier,
    description: String,
) {
    OutlinedCard(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_info_24),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = description
            )
        }
    }
}