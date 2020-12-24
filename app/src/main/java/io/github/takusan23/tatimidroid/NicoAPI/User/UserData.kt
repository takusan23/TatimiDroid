package io.github.takusan23.tatimidroid.NicoAPI.User

import java.io.Serializable

/**
 * ユーザー取得APIのレスポンス。
 * @param description 説明文。どうやらHTML
 * @param isPremium プレ垢ならtrue。価値は人それぞれ
 * @param niconicoVersion 登録時のバージョン。GINZAとか
 * @param followeeCount フォロー中数
 * @param followerCount フォロワーの数
 * @param userId ユーザーID
 * @param nickName ユーザー名
 * @param isFollowing フォロー中？
 * @param currentLevel 現在のレベル
 * @param largeIcon アイコンのURL
 * */
data class UserData(
    val description: String,
    val isPremium: Boolean,
    val niconicoVersion: String,
    val followeeCount: Int,
    val followerCount: Int,
    val userId: Int,
    val nickName: String,
    val isFollowing: Boolean,
    val currentLevel: Int,
    val largeIcon: String,
) : Serializable