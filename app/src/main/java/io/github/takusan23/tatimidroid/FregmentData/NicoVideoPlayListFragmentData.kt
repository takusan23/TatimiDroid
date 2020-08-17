package io.github.takusan23.tatimidroid.FregmentData

import java.io.Serializable

/**
 * ニコ動連続再生で画面回転後にデータを引き継ぐためのデータクラス
 * */
data class NicoVideoPlayListFragmentData(
    val currentVideoId: String
) : Serializable