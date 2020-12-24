package io.github.takusan23.tatimidroid.NicoAPI.User

import java.io.Serializable

/**
 * ユーザー取得APIのレスポンス。
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
    val largeIcon:String,
) : Serializable