package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Entity.NGUploaderUserIdEntity

/**
 * NG投稿者のユーザーIDが格納されたデータベースへアクセスするときに使う
 * */
@Dao
interface NGUploaderUserIdDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_uploader_user_id")
    fun getAll(): List<NGUploaderUserIdEntity>

    /** データ更新 */
    @Update
    fun update(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データ追加 */
    @Insert
    fun insert(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データ削除 */
    @Delete
    fun delete(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM ng_uploader_user_id")
    fun deleteAll()
}