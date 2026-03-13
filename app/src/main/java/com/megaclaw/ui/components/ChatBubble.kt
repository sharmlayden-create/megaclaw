package com.megaclaw.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.megaclaw.data.ChatMessage
import com.megaclaw.data.Role

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == Role.USER
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.7f).dp

    // Animate in with bubbleIn effect (translateY + scale)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(300)
            )
        ) {
            if (isUser) {
                UserBubble(text = message.content, maxWidth = maxWidth)
            } else {
                AiBubble(text = message.content, maxWidth = maxWidth)
            }
        }
    }
}

@Composable
private fun UserBubble(text: String, maxWidth: androidx.compose.ui.unit.Dp) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 4.dp      // right-bottom small radius
        ),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 25.6.sp,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun AiBubble(text: String, maxWidth: androidx.compose.ui.unit.Dp) {
    Surface(
        color = Color.White.copy(alpha = 0.8f),
        shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 4.dp,   // left-bottom small radius
            bottomEnd = 22.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.5f)
        ),
        modifier = Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(22.dp),
            ambientColor = Color(0x0DFF0000),
            spotColor = Color(0x0DFF0000),
        )
    ) {
        Text(
            text = text,
            color = Color(0xFF111111),
            fontSize = 16.sp,
            lineHeight = 25.6.sp,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
