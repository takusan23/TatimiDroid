package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import io.github.takusan23.tatimidroid.Room.Database.KotehanDB

/**
 * コテハンデータベース初期化（初期設定的な）クラス。
 * */
class KotehanDBInit(context: Context) {
    val kotehanDB = Room.databaseBuilder(context, KotehanDB::class.java, "KotehanDB").build()
}