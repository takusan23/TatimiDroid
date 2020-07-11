package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.AutoAdmissionDBDAO
import io.github.takusan23.tatimidroid.Room.Entity.AutoAdmissionDBEntity

/**
 * 予約枠自動入場のデータベース
 * */
@Database(entities = [AutoAdmissionDBEntity::class], version = 2)
abstract class AutoAdmissionDB : RoomDatabase() {
    abstract fun autoAdmissionDBDAO(): AutoAdmissionDBDAO
}