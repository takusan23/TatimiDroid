package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * ニコニ広告APIを叩く。
 * */
class NicoAdAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

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
        okHttpClient.newCall(request).execute()
    }

}