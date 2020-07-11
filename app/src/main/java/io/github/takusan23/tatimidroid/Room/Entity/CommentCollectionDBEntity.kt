package io.github.takusan23.tatimidroid.Room.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * コメントコレクション（初期状態無効になってる機能）のデータベースにに保存するデータ
 * この機能いら無くない？消したい
 * @param id 主キーです
 * @param comment コメント
 * @param yomi コメントの読みです。入力補助時に使われます。
 * @param description 説明。多分使わない。
 * */
@Entity(tableName = "comment_collection_db")
data class CommentCollectionDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "comment") val comment: String,
    @ColumnInfo(name = "yomi") val yomi: String,
    @ColumnInfo(name = "description") val description: String
)