package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

import java.io.Serializable

/**
 * コメントサーバーのデータクラス
 * */
data class CommentServerData(
    val webSocketUri: String,
    val threadId: String,
    val roomName: String,
    val threadKey: String? = null
) : Serializable
