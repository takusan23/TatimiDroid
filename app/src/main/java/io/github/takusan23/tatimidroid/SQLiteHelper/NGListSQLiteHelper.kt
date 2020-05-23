package io.github.takusan23.tatimidroid.SQLiteHelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * NGコメント/NGユーザーのデータベース
 * */
class NGListSQLiteHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // データーベースのバージョン
        private val DATABASE_VERSION = 1

        // データーベース名
        private val DATABASE_NAME = "NGList.db"
        private val TABLE_NAME = "ng_list"
        private val TYPE = "type"   // comment か user
        private val VALUE = "value" // コメントなら内容。ユーザーならユーザーID
        private val DESCRIPTION = "description" // 将来のために（今の所使う予定なし）
        private val _ID = "_id"


        // , を付け忘れるとエラー
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                TYPE + " TEXT ," +
                VALUE + " TEXT ," +
                DESCRIPTION + " TEXT" +
                ")"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        //テーブル作成
        p0?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        onUpgrade(p0, p1, p2)
    }

}