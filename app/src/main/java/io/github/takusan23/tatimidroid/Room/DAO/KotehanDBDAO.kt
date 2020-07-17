package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity

/**
 * コテハンDBを操作する関数。
 * */
@Dao
interface KotehanDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM kotehan")
    fun getAll(): List<KotehanDBEntity>

    /** データ更新 */
    @Update
    fun update(kotehanDBEntity: KotehanDBEntity)

    /** データ追加 */
    @Insert
    fun insert(kotehanDBEntity: KotehanDBEntity)

    /** データ削除 */
    @Delete
    fun delete(kotehanDBEntity: KotehanDBEntity)

    /** ユーザーIDからコテハンを取り出す */
    @Query("SELECT * FROM kotehan WHERE user_id = :userId")
    fun findKotehanByUserId(userId: String): KotehanDBEntity?

}
