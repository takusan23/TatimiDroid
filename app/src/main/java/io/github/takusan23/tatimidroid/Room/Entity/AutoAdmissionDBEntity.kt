package io.github.takusan23.tatimidroid.Room.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 予約枠自動入場のからむ
 * @param id 主キー
 * @param name 番組名
 * @param startTime 番組開始時間。UnixTimeです。（System#currentTimeなんとかだとミリ秒になるので1000で割ってね）
 * @param liveId 番組ID
 * @param lanchApp 起動アプリを指定。[AutoAdmissionDBEntity.LAUNCH_POPUP] 等を参照してね
 * @param description なにかに使う
 * */
@Entity(tableName = "auto_admission")
class AutoAdmissionDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "liveid") val liveId: String,
    @ColumnInfo(name = "start") val startTime: String,
    @ColumnInfo(name = "app") val lanchApp: String,
    @ColumnInfo(name = "description") val description: String
) {
    /**
     * 起動方法。[AutoAdmissionDBEntity.lanchApp]に入れる内容はこれのどれか
     * */
    companion object {
        /** たちみどろいどで開く */
        val LAUNCH_TATIMIDROID_APP = "tatimidroid_app"

        /** ニコニコ生放送アプリで開く */
        val LAUNCH_OFFICIAL_APP = "nicolive_app"

        /** ポップアップ再生で開く */
        val LAUNCH_POPUP = "tatimidroid_popup"

        /** バッググラウンド再生で開く */
        val LAUNCH_BACKGROUND = "tatimidroid_background"
    }
}