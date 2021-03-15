package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.NGUploaderUserIdDAO
import io.github.takusan23.tatimidroid.Room.Entity.NGUploaderVideoIdEntity

/**
 * NG投稿者が投稿した動画IDのデータベース
 *
 * 「NG投稿者」のデータベースはこっち：[io.github.takusan23.tatimidroid.Room.Database.NGUploaderUserIdDB]
 * */
@Database(entities = [NGUploaderVideoIdEntity::class], version = 1)
abstract class NGUploaderVideoIdDB : RoomDatabase() {
    abstract fun ngUploaderUserIdDAO(): NGUploaderUserIdDAO
}