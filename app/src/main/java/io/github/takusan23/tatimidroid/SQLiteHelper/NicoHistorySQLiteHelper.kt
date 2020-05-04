package io.github.takusan23.tatimidroid.SQLiteHelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NicoHistorySQLiteHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // データーベースのバージョン
        private val DATABASE_VERSION = 1

        // データーベース名
        private val DATABASE_NAME = "NicoHistory.db"
        val TABLE_NAME = "history"
        private val TYPE = "type" //live or video
        private val SERVICE_ID = "service_id" //生放送ID、動画ID
        private val SERVICE_USER_ID = "user_id" //コミュID　など
        private val TITLE  = "title" //なまえ
        private val UNIXTIME = "date" // unix time
        private val DESCRIPTION = "description" // 将来つかうかも
        private val _ID = "_id"


        // , を付け忘れるとエラー
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                TYPE + " TEXT ," +
                SERVICE_ID + " TEXT ," +
                SERVICE_USER_ID + " TEXT ," +
                TITLE + " TEXT ," +
                UNIXTIME + " INTEGER ," +
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