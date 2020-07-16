package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.KotehanDBDAO
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity

/**
 * コテハンデータベース
 * */
@Database(entities = [KotehanDBEntity::class], version = 1)
abstract class KotehanDB : RoomDatabase() {
    abstract fun kotehanDBDAO(): KotehanDBDAO
}