package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.NGDBDAO
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity

/**
 * NGのデータベース。
 * クラス名が略しすぎてわからんて
 * */
@Database(entities = [NGDBEntity::class], version = 2)
abstract class NGDB : RoomDatabase() {
    abstract fun ngDBDAO(): NGDBDAO
}