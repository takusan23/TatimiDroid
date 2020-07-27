package io.github.takusan23.tatimidroid.FregmentData

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import java.io.Serializable

data class NicoLiveFragmentData(
    /** 公式番組かどうか。 */
    val isOfficial: Boolean,
    /** HTMLのJSON渡す */
    val nicoLiveJSON: String,
    /** 画面回転前に全画面再生だったらtrue */
    val isFullScreenMode: Boolean = false,
    /** 流量制限コメントサーバーの情報。公式にはない */
    var storeCommentServerData: NicoLiveComment.CommentServerData? = null
) : Serializable