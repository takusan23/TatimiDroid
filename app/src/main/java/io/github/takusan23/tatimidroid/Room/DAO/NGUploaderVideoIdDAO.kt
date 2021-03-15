package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Database.NGUploaderVideoIdDB

/**
 * NG投稿者が投稿した動画IDが入ったデータベースにアクセスするときに使う
 * */
@Dao
interface NGUploaderVideoIdDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_uploader_video_id")
    fun getAll(): List<NGUploaderVideoIdDB>

    /** データ更新 */
    @Update
    fun update(ngUploaderVideoIdDB: NGUploaderVideoIdDB)

    /** データ追加 */
    @Insert
    fun insert(ngUploaderVideoIdDB: NGUploaderVideoIdDB)

    /** データ削除 */
    @Delete
    fun delete(ngUploaderVideoIdDB: NGUploaderVideoIdDB)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM ng_uploader_video_id")
    fun deleteAll()
}