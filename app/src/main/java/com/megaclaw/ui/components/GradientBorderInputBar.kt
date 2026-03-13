package com.megaclaw.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.megaclaw.R

// Gradient colors from prototype: yellow -> red -> purple -> blue -> green
private val GradientColors = listOf(
    Color(0xFFFFCC33),
    Color(0xFFFF6666),
    Color(0xFFCC66FF),
    Color(0xFF66CCFF),
    Color(0xFF99FF99),
    Color(0xFFFFCC33), // repeat first color for seamless loop
)

@Composable
fun GradientBorderInputBar(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    // Animate the gradient offset for the "flowing light" effect
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )

    // Glow opacity: 0.3 -> 0.6 over 3s (matches @keyframes glow)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Outer glow layer (blurred gradient shadow)
                    val cornerRadius = CornerRadius(40.dp.toPx())
                    val strokeWidth = 3.dp.toPx()

                    // Shift gradient colors based on animated offset
                    val shiftedColors = shiftColors(GradientColors, gradientOffset)

                    // Glow effect behind the border
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = shiftedColors,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f)
                        ),
                        cornerRadius = cornerRadius,
                        alpha = glowAlpha,
                        size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                        topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                    )

                    // Gradient border stroke
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = shiftedColors,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f)
                        ),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokeWidth),
                    )
                }
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White)
                .padding(3.dp) // matches the 3px padding in prototype
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(37.dp))
                    .background(Color.White)
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Megaclaw avatar (32dp)
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )

                // Text input
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = Color(0xFF333333),
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank()) {
                                onSend(text)
                                text = ""
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "输入你的问题 ~",
                                    style = TextStyle(
                                        fontSize = 17.sp,
                                        color = Color(0xFFAAAAAA),
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Send button with elastic appear/disappear
                AnimatedVisibility(
                    visible = text.isNotBlank(),
                    enter = scaleIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = { t ->
                                // cubic-bezier(0.175, 0.885, 0.32, 1.275) approximation (overshoot)
                                val c1 = 1.70158f
                                val c3 = c1 + 1f
                                1 + c3 * (t - 1f).let { it * it * it } + c1 * (t - 1f).let { it * it }
                            }
                        )
                    ),
                    exit = scaleOut()
                ) {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSend(text)
                                text = ""
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shifts the gradient color list by a normalized offset [0..1) to create flowing animation.
 */
private fun shiftColors(colors: List<Color>, offset: Float): List<Color> {
    val count = colors.size
    val shift = (offset * count).toInt() % count
    return colors.subList(shift, count) + colors.subList(0, shift)
}
