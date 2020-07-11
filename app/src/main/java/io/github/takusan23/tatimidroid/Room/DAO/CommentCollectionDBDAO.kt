package io.github.takusan23.tatimidroid.Room.DAO

import androidx.room.*
import io.github.takusan23.tatimidroid.Room.Entity.CommentCollectionDBEntity

/**
 * データベースへアクセスするときに使う関数を定義する
 * */
@Dao
interface CommentCollectionDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM comment_collection_db")
    fun getAll(): List<CommentCollectionDBEntity>

    /** データ更新 */
    @Update
    fun update(commentCollectionDBEntity: CommentCollectionDBEntity)

    /** データ追加 */
    @Insert
    fun insert(commentCollectionDBEntity: CommentCollectionDBEntity)

    /** データ削除 */
    @Delete
    fun delete(commentCollectionDBEntity: CommentCollectionDBEntity)

    /** データをIDを使って検索 */
    @Query("SELECT * FROM comment_collection_db WHERE _id = :id")
    fun findById(id: Int): CommentCollectionDBEntity

    /**  指定したコメントを消す */
    @Query("DELETE FROM comment_collection_db WHERE comment = :comment")
    fun deleteByComment(comment: String)

}