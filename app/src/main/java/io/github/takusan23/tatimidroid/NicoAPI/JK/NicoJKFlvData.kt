package io.github.takusan23.tatimidroid.NicoAPI.JK

/**
 * ニコニコ実況のgetFlvからコメントサーバーに接続するのに必要な値だけ
 * */
data class NicoJKFlvData(
    val threadId: String,
    val messageServer: String,
    val messageServerPort: String,
    val baseTime: String,
    val userId: String,
    val channelName: String,
    val isPremium: Int
)
