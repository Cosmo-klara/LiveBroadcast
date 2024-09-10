package com.example.livebroadcast.ui.module

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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
 *
 * @param modifier [Modifier]
 * @param onHelloClick
 * @param onClose
 */
@Composable
fun IntroductionCard(
    modifier: Modifier = Modifier,
    onHelloClick: () -> Unit,
    onClose: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onHelloClick
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(8.dp),
                painter = painterResource(id = R.drawable.livebroadcast_android),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.hello_card_title),
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = stringResource(id = R.string.hello_card_description))
            }
            IconButton(
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_close_24),
                    contentDescription = null
                )
            }
        }
    }
}