package com.megaclaw.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.megaclaw.data.RecommendCard

@Composable
fun SuggestionCards(
    cards: List<RecommendCard>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardWidth = 280.dp
    val cardHeight = 210.dp
    val cardSpacing = 16.dp
    val cardsRowWidth = cardWidth * 4 + cardSpacing * 3
    val visibleCards = cards.take(4)
    val switchSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 260, easing = FastOutSlowInEasing)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(cardsRowWidth),
                horizontalAlignment = Alignment.End
            ) {
                RefreshButton(
                    onClick = onRefresh,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Fixed centered 4 cards + refresh switch animation
                AnimatedContent(
                    targetState = visibleCards,
                    transitionSpec = {
                        (fadeIn(animationSpec = switchSpec) + scaleIn(initialScale = 0.95f, animationSpec = switchSpec))
                            .togetherWith(
                                fadeOut(animationSpec = switchSpec) + scaleOut(targetScale = 0.95f, animationSpec = switchSpec)
                            )
                    },
                    label = "cardsSwitch"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
                        it.forEach { card ->
                            SuggestionCardItem(
                                card = card,
                                width = cardWidth,
                                height = cardHeight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var spinStep by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "refreshScale"
    )
    val rotation by animateFloatAsState(
        targetValue = spinStep * 360f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "refreshRotation"
    )

    Row(
        modifier = Modifier
            .then(modifier)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(percent = 50),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(percent = 50)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE6E6E8),
                shape = RoundedCornerShape(percent = 50)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            spinStep += 1
                            onClick()
                        }
                    }
                )
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "换一换",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            Icons.Default.Refresh,
            contentDescription = "换一批",
            modifier = Modifier
                .size(16.dp)
                .scale(1f)
                .blur(0.dp)
                .rotate(rotation),
            tint = Color(0xFF1D1D1F)
        )
    }
}

@Composable
private fun SuggestionCardItem(
    card: RecommendCard,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )

    // Load real app icon from package manager
    val appIcon: Drawable? = remember(card.appPackageName) {
        try {
            context.packageManager.getApplicationIcon(card.appPackageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    Card(
        modifier = Modifier
            .scale(scale)
            .width(width)
            .height(height)
            .clickable(interactionSource = interactionSource, indication = null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.deepLinkUri))
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast
                        .makeText(context, "未安装 ${card.appName}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
        // Title (moved above image)
        Text(
            text = card.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )

        // App icon + name row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = card.appName,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = card.appName,
                fontSize = 12.sp,
                color = Color(0xFF86868B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Thumbnail image (moved below text, smaller)
        Image(
            painter = painterResource(card.thumbnailResId),
            contentDescription = card.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        }
    }
}
