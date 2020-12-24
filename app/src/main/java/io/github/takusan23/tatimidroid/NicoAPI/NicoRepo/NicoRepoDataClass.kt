package io.github.takusan23.tatimidroid.NicoAPI.NicoRepo

/**
 * ニコレポのデータクラス
 * @param isVideo 動画ならtrue、生放送ならfalse
 * @param message ニコレポで表示するメッセージ
 * @param title 動画、生放送のタイトル
 * @param accountName アカウント名
 * @param contentId 動画ID、生放送ID
 * @param date じかん
 * @param thumbUrl サムネURL
 * */
data class NicoRepoDataClass(
    val isVideo: Boolean,
    val message: String,
    val contentId: String,
    val date: Long,
    val accountName: String,
    val thumbUrl: String,
    val title: String,
)