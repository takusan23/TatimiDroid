package io.github.takusan23.tatimidroid.NicoAPI.JK

/**
 * ニコニコ実況のデータクラス。
 * @param title 番組名
 * @param channnelId http://jk.nicovideo.jp/watch/に続くやつ。jk1とか
 * @param ikioi 勢い
 * */
data class NicoJKData(var title: String, val channnelId: String, var ikioi: String)
