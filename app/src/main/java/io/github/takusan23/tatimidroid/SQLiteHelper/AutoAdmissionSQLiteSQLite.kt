package io.github.takusan23.tatimidroid.SQLiteHelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AutoAdmissionSQLiteSQLite(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // データーベースのバージョン
        private val DATABASE_VERSION = 1

        // データーベース名
        private val DATABASE_NAME = "AutoAdmission.db"
        val TABLE_NAME = "auto_admission"
        private val DESCRIPTION = "description"
        private val APP = "app"
        private val START = "start" // 開始時間。UnixTimeで頼んだ（System.currentTimeなんとかだとミリ秒になるので1000で割ってね）
        private val LIVEID = "liveid"
        private val NAME = "name"
        private val _ID = "_id"

        // 起動方法。appに入れる内容はこれのどれか
        val LAUNCH_TATIMIDROID_APP = "tatimidroid_app" // たちみどろいどで開く
        val LAUNCH_OFFICIAL_APP = "nicolive_app"    // ニコニコ生放送アプリで開く
        val LAUNCH_POPUP = "tatimidroid_popup"              // ポップアップ再生で開く
        val LAUNCH_BACKGROUND = "tatimidroid_background"    // バッググラウンド再生で開く

        // , を付け忘れるとエラー
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                NAME + " TEXT ," +
                LIVEID + " TEXT ," +
                START + " TEXT ," +
                APP + " TEXT ," +
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