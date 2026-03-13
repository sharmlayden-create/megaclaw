package com.megaclaw.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.megaclaw.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.megaclaw.ui.components.ChatBubble
import com.megaclaw.ui.components.GradientBorderInputBar
import com.megaclaw.ui.components.SuggestionCards
import com.megaclaw.ui.components.WelcomeHero

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x4DFFB4B4), // rgba(255,180,180,0.3)
                        Color(0xFFFFF8F8), // background base
                    ),
                    center = Offset(0.5f, 0.5f),
                    radius = 1200f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar spacer
            Spacer(modifier = Modifier.height(60.dp))

            // Conversation area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Welcome hero - hide when messages exist
                item {
                    AnimatedVisibility(
                        visible = !viewModel.hasMessages,
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        WelcomeHero()
                    }
                }

                items(
                    items = viewModel.messages,
                    key = { it.id }
                ) { message ->
                    ChatBubble(
                        message = message,
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 40.dp, vertical = 10.dp)
                    )
                }
            }

            // Suggestion cards - show only when no messages
            AnimatedVisibility(
                visible = !viewModel.hasMessages,
                exit = shrinkVertically() + fadeOut()
            ) {
                SuggestionCards(
                    cards = viewModel.currentCards,
                    onRefresh = { viewModel.refreshCards() },
                    modifier = Modifier
                )
            }

            // Input bar
            GradientBorderInputBar(
                onSend = { viewModel.send(it) }
            )
        }

        AnimatedVisibility(
            visible = viewModel.hasMessages,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            IconButton(onClick = { viewModel.clearMessages() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_custom_back),
                    contentDescription = "返回",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF1D1D1F)
                )
            }
        }
    }
}
