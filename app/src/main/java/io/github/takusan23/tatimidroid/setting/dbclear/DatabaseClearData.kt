package io.github.takusan23.tatimidroid.setting.dbclear

import android.content.Context
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.init.*

/**
 * データベース削除で使うデータクラス
 *
 * @param database データベース
 * @param databaseDescription データベースの説明
 * @param isDelete 削除する場合はtrue
 * */
data class DatabaseClearData(
    val database: RoomDatabase,
    val databaseDescription: String,
    val isDelete: Boolean,
) {

    companion object {

        /**
         * データベース削除一覧で表示する配列を返す関数
         * @param context Context
         * */
        fun getDatabaseList(context: Context): List<DatabaseClearData> {
            return listOf(
                DatabaseClearData(KotehanDBInit.getInstance(context), "コテハンデータベース", false),
                DatabaseClearData(NGDBInit.getInstance(context),"NGユーザーデータベース",false),
                DatabaseClearData(NGUploaderUserIdDBInit.getInstance(context),"NG投稿者データベース",false),
                DatabaseClearData(NGUploaderVideoIdDBInit.getInstance(context),"NG投稿者が投稿した動画データベース",false),
                DatabaseClearData(NicoHistoryDBInit.getInstance(context),"端末内履歴データベース",false),
                DatabaseClearData(SearchHistoryDBInit.getInstance(context),"検索履歴データベース",false),
            )
        }

    }

}