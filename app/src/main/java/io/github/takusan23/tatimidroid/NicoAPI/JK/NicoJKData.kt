package io.github.takusan23.tatimidroid.NicoAPI.JK

import java.io.Serializable

/**
 * ニコニコ実況のデータクラス。
 * @param title 番組名
 * @param channnelId http://jk.nicovideo.jp/watch/に続くやつ。jk1とか
 * @param ikioi 勢い
 * @param isNicoLiveVersion ニコ生を利用した新ニコニコ実況の場合はtrue
 * */
data class NicoJKData(
    var title: String,
    val channnelId: String,
    var ikioi: String,
    var isNicoLiveVersion: Boolean = false,
) : Serializable
