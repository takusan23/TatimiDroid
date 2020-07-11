package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.takusan23.tatimidroid.Room.Database.AutoAdmissionDB
import io.github.takusan23.tatimidroid.Room.Database.CommentCollectionDB

/**
 * 予約枠自動入場
 * */
class AutoAdmissionDBInit(context: Context) {
    // データベース初期化
    val commentCollectionDB = Room.databaseBuilder(context, AutoAdmissionDB::class.java, "AutoAdmission.db")
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite移行。移行後のデータベースを作成する。カラムは移行前と同じ
                database.execSQL(
                    """
                    CREATE TABLE auto_admission_tmp (
                    _id INTEGER NOT NULL PRIMARY KEY, 
                    name TEXT NOT NULL,
                    liveid TEXT NOT NULL,
                    start TEXT NOT NULL,
                    app TEXT NOT NULL,
                    description TEXT NOT NULL
                    )
                    """
                )
                // 移行後のデータベースへデータを移す
                database.execSQL(
                    """
                    INSERT INTO auto_admission_tmp (_id, name, liveid, start, app, description)
                    SELECT _id, name, liveid, start, app, description FROM auto_admission
                    """
                )
                // 前あったデータベースを消す
                database.execSQL("DROP TABLE auto_admission")
                // 移行後のデータベースの名前を移行前と同じにして移行完了
                database.execSQL("ALTER TABLE auto_admission_tmp RENAME TO auto_admission")
            }
        }).build()
}