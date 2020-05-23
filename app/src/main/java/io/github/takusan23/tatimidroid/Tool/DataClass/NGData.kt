package io.github.takusan23.tatimidroid.Tool.DataClass

/**
 * NGユーザーのデータクラス
 * @param isComment NGの種類がコメントなのか
 * @param isUser NGの種類がユーザーなのか
 * @param type user か comment
 * @param value 内容
 * */
data class NGData(val isUser: Boolean, val isComment: Boolean, val type: String, val value: String)