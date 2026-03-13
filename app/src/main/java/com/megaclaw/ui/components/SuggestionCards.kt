package com.megaclaw.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val cardWidth = 240.dp
    val cardHeight = 220.dp
    val cardSpacing = 16.dp
    val cardsRowWidth = cardWidth * 4 + cardSpacing * 3
    val visibleCards = cards.take(4)
    val switchSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 220)

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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "refreshScale"
    )

    Row(
        modifier = Modifier
            .then(modifier)
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(percent = 50),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            )
            .blur(0.4.dp)
            .background(
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(percent = 50)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(percent = 50)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "换一批",
            modifier = Modifier.size(14.dp),
            tint = Color(0xFF86868B)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "换一批",
            fontSize = 11.sp,
            color = Color(0xFF86868B)
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

    Column(
        modifier = Modifier
            .scale(scale)
            .width(width)
            .height(height)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .blur(0.4.dp)
            .background(
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.deepLinkUri))
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast
                        .makeText(context, "未安装 ${card.appName}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .padding(10.dp)
    ) {
        // Thumbnail image
        Image(
            painter = painterResource(card.thumbnailResId),
            contentDescription = card.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        // Title
        Text(
            text = card.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom row: app icon + name | arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon (real icon from PackageManager)
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = card.appName,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            if (appIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = card.appName,
                fontSize = 11.sp,
                color = Color(0xFF86868B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
