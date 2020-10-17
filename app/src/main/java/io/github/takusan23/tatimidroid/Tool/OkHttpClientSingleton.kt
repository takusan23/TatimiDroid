package io.github.takusan23.tatimidroid.Tool

import okhttp3.OkHttpClient

/**
 * OkHttp曰く、「OkHttpClient」を使いまわし、すべてのリクエストで同じOkHttpClientを使うと最高のパフォーマンスが出る
 *
 * とのことなので使いまわしてみる
 * */
object OkHttpClientSingleton {

    /**
     * これをすべてのリクエストで使う共通のOkHttpClient
     * */
    val okHttpClient = OkHttpClient()

}