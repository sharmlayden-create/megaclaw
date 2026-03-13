package com.megaclaw.data

data class RecommendCard(
    val title: String,
    val thumbnailResId: Int,       // local placeholder drawable
    val thumbnailUrl: String,      // remote thumbnail url (future real image)
    val cardTintArgb: Long,        // random light/dark tint per refresh
    val appPackageName: String,
    val appName: String,
    val deepLinkUri: String
)
