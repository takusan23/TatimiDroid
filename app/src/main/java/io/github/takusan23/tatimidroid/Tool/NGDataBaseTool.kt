package io.github.takusan23.tatimidroid.Tool

import android.content.ContentValues
import android.content.Context
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import io.github.takusan23.tatimidroid.Tool.DataClass.NGData

/**
 * NGユーザー/コメントのデータベースにアクセスするクラス？
 * Room使い方分かんねえな
 * */
class NGDataBaseTool(val context: Context?) {

    // データベース用意
    private var ngListSQLiteHelper = NGListSQLiteHelper(context)
    private var sqLiteDatabase = ngListSQLiteHelper.writableDatabase

    init {
        ngListSQLiteHelper.setWriteAheadLoggingEnabled(false) // 先読みNG
    }

    /** NGユーザー一覧(NGData)の配列 */
    val ngUserList = readDB("user", arrayListOf())

    /** NGコメント一覧(NGData)の配列 */
    val ngCommentList = readDB("comment", arrayListOf())

    /** NGユーザーのID(String)の配列 */
    var ngUserStringList = ngUserList.map { ngData -> ngData.value }

    /** NGコメントのコメント(String)の配列 */
    var ngCommentStringList = ngCommentList.map { ngData -> ngData.value }

    /**
     * NGユーザーを追加する
     * @param comment コメントの中身
     * */
    fun addNGUser(userId: String) = writeDB("user", userId)

    /**
     * NGコメントを追加する
     * @param userId ユーザーID
     * */
    fun addNGComment(comment: String) = writeDB("comment", comment)

    /**
     * NGユーザーを開放（NG解除）する
     * @param userId ユーザーID
     * */
    fun deleteNGUser(userId: String) = deleteDB(userId)


    /**
     * NGコメントを解除する
     * @param comment コメント本文
     * */
    fun deleteNGComment(comment: String) = deleteDB(comment)

    /**
     * NGコメント一覧の配列を更新する。
     * 注意：基本的にこの関数を呼ぶことはないです。データベースに追加や削除を行ったとき、勝手にこの関数を呼ぶようにしてありますので。
     * */
    fun notifyNGList() {
        readDB("user", ngUserList)
        readDB("comment", ngCommentList)
        ngUserStringList =ngUserList.map { ngData -> ngData.value }
        ngCommentStringList =ngCommentList.map { ngData -> ngData.value }
    }

    /**
     * NGデータベースから削除する
     * @param value 値。コメントならコメント本文だし、ユーザーならユーザーID
     * */
    private fun deleteDB(value: String) {
        sqLiteDatabase.delete("ng_list", "value=?", arrayOf(value))
        // 配列更新
        notifyNGList()
    }

    /**
     * DBに書き込む
     * @param type comment か user
     * @param value NGコメントならコメント本文、NGユーザーならユーザーID
     * */
    private fun writeDB(type: String, value: String) {
        val contentValues = ContentValues()
        contentValues.put("type", type)
        contentValues.put("value", value)
        contentValues.put("description", "")
        sqLiteDatabase.insert("ng_list", null, contentValues)
        // 配列更新
        notifyNGList()
    }

    /**
     * DBから取り出す。
     * @param list 返り値になる配列
     * @param type comment か user のどっちか
     * @return listの中身
     * */
    private fun readDB(type: String, list: ArrayList<NGData>): ArrayList<NGData> {
        list.clear()
        val cursor = sqLiteDatabase.query("ng_list", arrayOf("type", "value"), "type=?", arrayOf(type), null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            val data = NGData(
                isUser = type == "user",
                isComment = type == "comment",
                type = cursor.getString(0),
                value = cursor.getString(1)
            )
            list.add(data)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

}