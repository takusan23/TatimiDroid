package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

import java.io.Serializable

/**
 * 番組一覧で表示するRecyclerViewに渡すデータクラス
 * */
data class NicoLiveProgramData(
    val title: String,
    val communityName: String,
    val beginAt: String,
    val endAt: String,
    val programId: String,
    val broadCaster: String,
    val lifeCycle: String,
    val thum: String,
    val isOfficial: Boolean = false
) : Serializable