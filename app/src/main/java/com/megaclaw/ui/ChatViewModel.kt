package com.megaclaw.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megaclaw.R
import com.megaclaw.data.ChatMessage
import com.megaclaw.data.RecommendCard
import com.megaclaw.data.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    val hasMessages: Boolean get() = messages.isNotEmpty()

    // Recommendation cards
    var currentCards by mutableStateOf(emptyList<RecommendCard>())
        private set

    private val cardTintPalette = listOf(
        0xFFEEF3FF,
        0xFFF2F6ED,
        0xFFFFF2E8,
        0xFFEFEAFF,
        0xFF1E2430,
        0xFF1A2A2A,
        0xFF2B1F2F,
        0xFF2B2520
    )

    private val allCards = listOf(
        RecommendCard(
            title = "15分钟全身拉伸训练",
            thumbnailResId = R.drawable.local_mock_1,
            thumbnailUrl = "https://picsum.photos/seed/stretch/320/200",
            cardTintArgb = 0xFFEEF3FF,
            appPackageName = "com.xingin.xhs",
            appName = "小红书",
            deepLinkUri = "xhsdiscover://home"
        ),
        RecommendCard(
            title = "新华三MegaBookPro拆箱",
            thumbnailResId = R.drawable.local_mock_2,
            thumbnailUrl = "https://picsum.photos/seed/unbox/320/200",
            cardTintArgb = 0xFFF2F6ED,
            appPackageName = "tv.danmaku.bili",
            appName = "哔哩哔哩",
            deepLinkUri = "bilibili://video/1"
        ),
        RecommendCard(
            title = "Agent Skill是什么？",
            thumbnailResId = R.drawable.local_mock_3,
            thumbnailUrl = "https://picsum.photos/seed/agent-skill/320/200",
            cardTintArgb = 0xFFFFF2E8,
            appPackageName = "com.douban.frodo",
            appName = "豆瓣",
            deepLinkUri = "douban://douban.com/"
        ),
        RecommendCard(
            title = "OPENCLAW为什么这么火",
            thumbnailResId = R.drawable.local_mock_4,
            thumbnailUrl = "https://picsum.photos/seed/openclaw/320/200",
            cardTintArgb = 0xFFEFEAFF,
            appPackageName = "tv.danmaku.bili",
            appName = "哔哩哔哩",
            deepLinkUri = "bilibili://video/2"
        ),
        RecommendCard(
            title = "晚间冥想放松 10 分钟",
            thumbnailResId = R.drawable.local_mock_5,
            thumbnailUrl = "https://picsum.photos/seed/meditation/320/200",
            cardTintArgb = 0xFF1E2430,
            appPackageName = "com.netease.cloudmusic",
            appName = "网易云音乐",
            deepLinkUri = "orpheus://"
        ),
        RecommendCard(
            title = "AI 产品经理入门清单",
            thumbnailResId = R.drawable.local_mock_1,
            thumbnailUrl = "https://picsum.photos/seed/pm-list/320/200",
            cardTintArgb = 0xFF1A2A2A,
            appPackageName = "com.zhihu.android",
            appName = "知乎",
            deepLinkUri = "zhihu://questions"
        ),
        RecommendCard(
            title = "如何系统化学习 Kotlin",
            thumbnailResId = R.drawable.local_mock_2,
            thumbnailUrl = "https://picsum.photos/seed/kotlin/320/200",
            cardTintArgb = 0xFF2B1F2F,
            appPackageName = "com.tencent.weread",
            appName = "微信读书",
            deepLinkUri = "weread://"
        ),
        RecommendCard(
            title = "x86 平板效率工具合集",
            thumbnailResId = R.drawable.local_mock_3,
            thumbnailUrl = "https://picsum.photos/seed/x86-tools/320/200",
            cardTintArgb = 0xFF2B2520,
            appPackageName = "com.android.chrome",
            appName = "Chrome",
            deepLinkUri = "https://www.google.com/search?q=x86+android+productivity+tools"
        )
    )

    init {
        refreshCards()
    }

    fun refreshCards() {
        currentCards = allCards
            .shuffled()
            .take(4)
            .map { card ->
                card.copy(cardTintArgb = cardTintPalette.random())
            }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(role = Role.USER, content = text.trim()))

        // Phase 1: mock AI reply
        viewModelScope.launch {
            delay(800)
            messages.add(
                ChatMessage(
                    role = Role.AI,
                    content = "收到您的消息，Megaclaw 正在处理中..."
                )
            )
        }
    }

    fun clearMessages() {
        messages.clear()
    }
}
