package io.github.takusan23.tatimidroid.Room.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.Room.DAO.NicoHistoryDBDAO
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit

/**
 * 端末内履歴データベース
 * 使う際は[NicoHistoryDBInit.getInstance]を経由してね（SQLite->Room移行時にバージョンを上げるコードが書いてある）
 * */
@Database(entities = [NicoHistoryDBEntity::class], version = 4)
abstract class NicoHistoryDB : RoomDatabase() {
    abstract fun nicoHistoryDBDAO(): NicoHistoryDBDAO
}