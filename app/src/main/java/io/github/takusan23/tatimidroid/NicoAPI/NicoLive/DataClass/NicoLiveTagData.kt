package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

/**
 * タグのAPI叩いた結果データクラス
 *
 * @param isLocked タグ編集可能かどうか
 * @param tagList タグ一覧
 * */
data class NicoLiveTagData(
    val isLocked: Boolean,
    val tagList: ArrayList<NicoTagItemData>
)