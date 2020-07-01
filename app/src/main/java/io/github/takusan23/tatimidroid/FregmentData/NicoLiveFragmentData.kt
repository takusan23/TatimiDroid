package io.github.takusan23.tatimidroid.FregmentData

import io.github.takusan23.tatimidroid.CommentServerList
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import org.json.JSONObject
import java.io.Serializable

data class NicoLiveFragmentData(
    /** 公式番組かどうか。 */
    val isOfficial: Boolean,
    /** HTMLのJSON渡す */
    val nicoLiveJSON: String,
    /** 全部屋WebSocketアドレス */
    val commentServerList: ArrayList<NicoLiveComment.CommentServerData>,
    /** 画面回転前に全画面再生だったらtrue */
    val isFullScreenMode: Boolean = false
) : Serializable