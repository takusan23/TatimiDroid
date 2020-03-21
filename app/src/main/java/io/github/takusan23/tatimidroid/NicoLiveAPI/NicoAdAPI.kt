package io.github.takusan23.tatimidroid.NicoLiveAPI

import okhttp3.*
import java.io.IOException

/**
 * ニコニ広告APIを叩く。
 * */
class NicoAdAPI() {

    // ニコニ広告の情報取得。宣伝トータル、アクテイブなど
    fun getNicoAdInfo(liveId: String, error: (() -> Unit)?, response: (Response) -> Unit) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/live/$liveId")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (error != null) {
                    error()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response(response)
            }
        })
    }

}