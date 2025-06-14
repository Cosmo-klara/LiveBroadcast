package com.example.livebroadcast.ui.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livebroadcast.R
import com.example.livebroadcast.settingData.StreamingType

/**
 *
 * @param modifier [Modifier]
 * @param currentType 当前选择的[StreamingType]
 * @param onClick
 *
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingTypeCard(
    modifier: Modifier,
    currentType: StreamingType,
    onClick: (StreamingType) -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = R.string.streaming_setting_type_title),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = LocalContentColor.current,
                fontSize = 18.sp
            )
            StreamingTypeMenu(
                currentType = currentType,
                onClick = onClick
            )
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(id = currentType.message)
            )
        }
    }
}

/**
 * 选择菜单
 *
 * @param modifier
 * @param currentType
 * @param onClick
 *
 */

@ExperimentalMaterial3Api
@Composable
private fun StreamingTypeMenu(
    modifier: Modifier = Modifier,
    currentType: StreamingType,
    onClick: (StreamingType) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        border = BorderStroke(1.dp, LocalContentColor.current),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row {
            StreamingType.entries.forEach { type ->
                val isSelected = type == currentType
                val color = if (isSelected) MaterialTheme.colorScheme.primary else contentColorFor(
                    MaterialTheme.colorScheme.surface
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    contentColor = color,
                    border = if (isSelected) BorderStroke(2.dp, color) else null,
                    onClick = { onClick(type) }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(38.dp),
                            painter = painterResource(id = type.icon),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = type.title),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}