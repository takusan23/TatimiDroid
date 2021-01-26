package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

/**
 * 投げ銭のランキングAPIのデータクラス
 *
 * @param userId ユーザーID
 * @param advertiserName ユーザー名
 * @param totalContribution　貢献度？よくわからん
 * @param rank 何位か
 * */
data class NicoLiveGiftRankingData(
    val userId: Int,
    val advertiserName: String,
    val totalContribution: Int,
    val rank: Int,
)