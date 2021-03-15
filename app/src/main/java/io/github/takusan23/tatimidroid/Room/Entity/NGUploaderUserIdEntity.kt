package io.github.takusan23.tatimidroid.Room.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NG投稿者IDデータベースに入れるデータのデータクラス
 *
 * @param id 主キー
 * @param userId NGにした投稿者のユーザーID
 * @param addDateTime 追加日時（UnixTimeのミリ秒）
 * @param lastUpdateTime 最新更新日時（UnixTimeのミリ秒）
 * @param description 将来的に使う？
 * */
@Entity(tableName = "ng_uploader_user_id")
data class NGUploaderUserIdEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "last_update_time") val lastUpdateTime: Long,
    @ColumnInfo(name = "add_date_time") val addDateTime: Long,
    @ColumnInfo(name = "description") val description: String,
)