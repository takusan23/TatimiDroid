package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * タイムシフト予約をするAPIまとめ
 * 登録済みか確認する関数は
 * */
class NicoLiveTimeShiftAPI {

    /**
     * タイムシフト登録APIを叩く。コルーチンです。
     * 注意：このAPIを使うときは登録以外にも登録済みか判断するときに使うっぽい
     *      登録済みの場合はステータスコードが500になる
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    fun registerTimeShift(liveId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val postFormData = FormBody.Builder().apply {
                // 番組IDからlvを抜いた値を指定する
                add("vid", liveId.replace("lv", ""))
                add("overwrite", "0")
            }.build()
            val request = Request.Builder().apply {
                url("https://live.nicovideo.jp/api/timeshift.reservations")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                header("Content-Type", "application/x-www-form-urlencoded")
                header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
                post(postFormData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * タイムシフト登録リストからタイムシフトを削除するAPIを叩く。コルーチンです。
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    fun deleteTimeShift(liveId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                // 番組IDからlvを抜いた値を指定する
                url("https://live.nicovideo.jp/api/timeshift.reservations?vid=${liveId.replace("lv", "")}")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                header("Content-Type", "application/x-www-form-urlencoded")
                header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
                delete()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

}