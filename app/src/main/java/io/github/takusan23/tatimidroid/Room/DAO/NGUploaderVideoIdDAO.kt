package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Database.NGUploaderVideoIdDB
import io.github.takusan23.tatimidroid.Room.Entity.NGUploaderVideoIdEntity

/**
 * NG投稿者が投稿した動画IDが入ったデータベースにアクセスするときに使う
 * */
@Dao
interface NGUploaderVideoIdDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_uploader_video_id")
    fun getAll(): List<NGUploaderVideoIdEntity>

    /** データ更新 */
    @Update
    fun update(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データ追加 */
    @Insert
    fun insert(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データ削除 */
    @Delete
    fun delete(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM ng_uploader_video_id")
    fun deleteAll()
}