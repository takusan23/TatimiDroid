package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException

/**
 * ニコニ広告APIを叩く。
 * */
class NicoAdAPI {

    /**
     * ニコニ広告のAPIを叩く。
     * @param liveId 生放送ID
     * @return OkHttpのレスポンス。
     * */
    suspend fun getNicoAd(liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/live/$liveId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

}