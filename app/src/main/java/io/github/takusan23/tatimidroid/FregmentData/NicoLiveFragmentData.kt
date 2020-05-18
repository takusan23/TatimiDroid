package io.github.takusan23.tatimidroid.FregmentData

import io.github.takusan23.tatimidroid.CommentServerList
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import org.json.JSONObject
import java.io.Serializable

data class NicoLiveFragmentData(
    /** 公式番組かどうか。 */
    val isOfficial: Boolean,
    val nicoLiveJSON: String,
    val commentServerList: ArrayList<NicoLiveComment.CommentServerData>
) : Serializable