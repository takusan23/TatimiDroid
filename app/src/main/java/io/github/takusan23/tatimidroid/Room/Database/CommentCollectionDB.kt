package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.CommentCollectionDBDAO
import io.github.takusan23.tatimidroid.Room.Entity.CommentCollectionDBEntity

/**
 * コメントコレクションのデータベース。
 * SQLiteから移行する場合はバージョンを上げる必要がある
 * */
@Database(entities = [CommentCollectionDBEntity::class], version = 2)
abstract class CommentCollectionDB : RoomDatabase() {
    abstract fun commentCollectionDBDAO(): CommentCollectionDBDAO
}