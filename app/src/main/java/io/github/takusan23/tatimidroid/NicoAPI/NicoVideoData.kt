package io.github.takusan23.tatimidroid.NicoAPI

/**
 * 動画タイトル、動画ID、サムネとか
 * */
data class NicoVideoData(
    val isCache: Boolean, // キャッシュならtrue
    val isMylist: Boolean, // マイリストならtrue
    val title: String,
    val videoId: String,
    val thum: String,
    val date: Long,
    val viewCount: String,
    val commentCount: String,
    val mylistCount: String,
    val mylistItemId: String // マイリストのitem_idの値。マイリスト以外は空文字。
)
