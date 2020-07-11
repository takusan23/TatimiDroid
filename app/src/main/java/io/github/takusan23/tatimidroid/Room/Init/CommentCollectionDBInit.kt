package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.takusan23.tatimidroid.Room.Database.CommentCollectionDB

/**
 * [CommentCollectionDB]（コメントコレクションのデータベース）を生成するだけのクラス。Migrationsのせいで長いんだわ
 * @param context こんてきすと
 * */
class CommentCollectionDBInit(context: Context) {
    // データベース初期化
    val commentCollectionDB = Room.databaseBuilder(context, CommentCollectionDB::class.java, "CommentCollection.db")
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite移行。移行後のデータベースを作成する。カラムは移行前と同じ
                database.execSQL(
                    """
                        CREATE TABLE comment_collection_db_tmp (
                          _id INTEGER NOT NULL PRIMARY KEY, 
                          comment TEXT NOT NULL,
                          yomi TEXT NOT NULL,
                          description TEXT NOT NULL
                        )
                        """
                )
                // 移行後のデータベースへデータを移す
                database.execSQL(
                    """
                        INSERT INTO comment_collection_db_tmp (_id, comment, yomi, description)
                        SELECT _id, comment, yomi, description FROM comment_collection_db
                        """
                )
                // 前あったデータベースを消す
                database.execSQL("DROP TABLE comment_collection_db")
                // 移行後のデータベースの名前を移行前と同じにして移行完了
                database.execSQL("ALTER TABLE comment_collection_db_tmp RENAME TO comment_collection_db")
            }
        }).build()
}