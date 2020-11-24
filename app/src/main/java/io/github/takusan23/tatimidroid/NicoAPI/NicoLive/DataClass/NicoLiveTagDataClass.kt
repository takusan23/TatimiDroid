package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

/**
 * タグAPI叩いたときに帰ってくるJSON解析結果を入れるデータクラス
 *
 * @param tagName タグ名
 * @param isLocked ロックされてる場合。[type]が"category"の時はtrue？
 * @param type "category"？
 * @param isDeletable 消せるか。
 * @param hasNicoPedia 二コ百あってもfalseじゃね？
 * @param nicoPediaUrl 二コ百URL。相対URL
 * */
data class NicoLiveTagDataClass(
    val tagName: String,
    val isLocked: Boolean,
    val type: String,
    val isDeletable: Boolean,
    val hasNicoPedia: Boolean,
    val nicoPediaUrl: String,
)