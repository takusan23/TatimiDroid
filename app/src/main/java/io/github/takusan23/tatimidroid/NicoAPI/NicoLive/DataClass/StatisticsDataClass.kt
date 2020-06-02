package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

/**
 * 総来場者数/コメント数/投げ銭ポイント/ニコニ広告ポイント数
 * */
data class StatisticsDataClass(val adPoints: Int, val comments: Int, val giftPoints: Int, val viewers: Int)
