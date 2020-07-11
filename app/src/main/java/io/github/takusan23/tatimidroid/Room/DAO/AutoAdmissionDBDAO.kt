package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Entity.AutoAdmissionDBEntity

@Dao
interface AutoAdmissionDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM auto_admission")
    fun getAll(): List<AutoAdmissionDBEntity>

    /** データ更新 */
    @Update
    fun update(autoAdmissionDBEntity: AutoAdmissionDBEntity)

    /** データ追加 */
    @Insert
    fun insert(autoAdmissionDBEntity: AutoAdmissionDBEntity)

    /** データ削除 */
    @Delete
    fun delete(autoAdmissionDBEntity: AutoAdmissionDBEntity)

    /** 指定した番組IDの自動入場を取り消す */
    @Query("DELETE FROM auto_admission WHERE liveid = :liveId")
    fun deleteById(liveId: String)

    /** 指定した番組IDの配列を取得する。配列の数が0か1かで登録済みかどうかを判断可能に */
    @Query("SELECT * FROM auto_admission WHERE liveid = :liveId")
    fun getLiveIdList(liveId: String): List<AutoAdmissionDBEntity>

}