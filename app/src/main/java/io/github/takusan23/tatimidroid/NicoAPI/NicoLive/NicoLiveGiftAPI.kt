package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveGiftHistoryData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveGiftRankingData
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * 投げ銭履歴、ランキングAPI を叩くクラス
 * */
class NicoLiveGiftAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ギフトランキングAPIを叩く
     * */
    suspend fun getGiftRanking(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/$liveId/ranking/contribution?limit=50")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftRanking]をパースする
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftRanking(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoLiveGiftRankingData>()
        val jsonObject = JSONObject(responseString)
        val rankingJSONArray = jsonObject.getJSONObject("data").getJSONArray("ranking")
        for (i in 0 until rankingJSONArray.length()) {
            val rankingJSONObject = rankingJSONArray.getJSONObject(i)
            val userId = rankingJSONObject.getInt("userId")
            val advertiserName = rankingJSONObject.getString("advertiserName")
            val totalContribution = rankingJSONObject.getInt("totalContribution")
            val rank = rankingJSONObject.getInt("rank")
            resultList.add(
                NicoLiveGiftRankingData(
                    userId = userId,
                    advertiserName = advertiserName,
                    totalContribution = totalContribution,
                    rank = rank
                )
            )
        }
        resultList
    }

    /**
     * ギフト履歴APIを叩く
     * */
    suspend fun getGiftHistory(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/$liveId/histories?limit=50")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftHistory]をパースする
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftHistory(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoLiveGiftHistoryData>()
        val jsonObject = JSONObject(responseString)
        val historyJSONArray = jsonObject.getJSONObject("data").getJSONArray("histories")
        for (i in 0 until historyJSONArray.length()) {
            val historyJSONObject = historyJSONArray.getJSONObject(i)
            val advertiserName = historyJSONObject.getString("advertiserName")
            val userId = historyJSONObject.getInt("userId")
            val adPoint = historyJSONObject.getInt("adPoint")
            val itemObject = historyJSONObject.getJSONObject("item")
            val itemName = itemObject.getString("name")
            val itemThumbUrl = itemObject.getString("thumbnailUrl")
            resultList.add(
                NicoLiveGiftHistoryData(
                    advertiserName = advertiserName,
                    userId = userId,
                    adPoint = adPoint,
                    itemName = itemName,
                    itemThumbUrl = itemThumbUrl
                )
            )
        }
        resultList
    }

}