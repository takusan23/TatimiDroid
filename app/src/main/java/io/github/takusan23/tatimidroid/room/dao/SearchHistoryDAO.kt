package io.github.takusan23.tatimidroid.room.dao

import androidx.room.*
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import kotlinx.coroutines.flow.Flow

/**
 * 検索DBへアクセスするときに使う関数など
 * */
@Dao
interface SearchHistoryDAO {

    /** 全データ取得 */
    @Query("SELECT * FROM search_history")
    fun getAll(): List<SearchHistoryDBEntity>

    /** データ更新 */
    @Update
    fun update(searchHistoryDBEntity: SearchHistoryDBEntity)

    /** データ追加 */
    @Insert
    fun insert(searchHistoryDBEntity: SearchHistoryDBEntity)

    /** データ削除 */
    @Delete
    fun delete(searchHistoryDBEntity: SearchHistoryDBEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM search_history")
    fun deleteAll()

    /** ピン止めしてあるデータを取得する */
    @Query("SELECT * FROM search_history WHERE pin = 1")
    fun getPinnedSearchHistory(): List<SearchHistoryDBEntity>

    /** リアルタイムでピン止めしてあるデータを取得する */
    @Query("SELECT * FROM search_history WHERE pin = 1")
    fun realtimeGetPinnedSearchHistory(): Flow<List<SearchHistoryDBEntity>>

    /** リアルタイムで全データ取得 */
    @Query("SELECT * FROM search_history")
    fun realtimeGetAll(): Flow<List<SearchHistoryDBEntity>>

}