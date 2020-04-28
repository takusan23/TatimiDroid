package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * マイリストAPIを叩く。
 * こっちはスマホWebブラウザ版のAPI。token取得が不要で便利そう（小並感
 * */
class NicoVideoSPMyListAPI {

    /**
     * マイリスト一覧のAPIを叩く関数
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    fun getMyListList(userSession: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/mylists")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * マイリスト一覧のAPIのレスポンスをパースする関数
     * @param responseString getMyListList()のレスポンス
     *
     * */
    fun parseMyListList(responseString: String?): ArrayList<MyListData> {
        val myListListDataList = arrayListOf<MyListData>()
        val jsonObject = JSONObject(responseString)
        val mylists = jsonObject.getJSONObject("data").getJSONArray("mylists")
        for (i in 0 until mylists.length()) {
            val list = mylists.getJSONObject(i)
            val title = list.getString("name")
            val id = list.getString("id")
            val itemsCount = list.getInt("itemsCount")
            val data = MyListData(title, id, itemsCount)
            myListListDataList.add(data)
        }
        return myListListDataList
    }

    /**
     * マイリストの中身APIを叩く関数。
     * @param myListId マイリストのID。MyListData#idで取得して
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス。パースはparseMyListItems()でできると思う
     * */
    fun getMyListItems(myListId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://nvapi.nicovideo.jp/v1/users/me/mylists/$myListId/items?pageSize=500")
                header("Cookie", "user_session=$userSession")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("x-frontend-id", "3")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * とりあえずマイリストの中身取得APIを叩く関数。
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス。パースはparseMyListItems()でできると思う
     * */
    fun getToriaezuMyListList(userSession: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/deflist/items?pageSize=500")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * マイリストの中身APIをパースする関数
     * @param responseString getMyListItems()のレスポンス
     * @return NicoVideoDataの配列
     * */
    fun parseMyListItems(responseString: String?): ArrayList<NicoVideoData> {
        val videoItems = arrayListOf<NicoVideoData>()
        val items = JSONObject(responseString).getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val itemObject = items.getJSONObject(i)
            // マイリスト操作系
            val myListItemId = itemObject.getString("itemId")
            val myListAddedDate = toUnixTime(itemObject.getString("addedAt"))
            if (!itemObject.isNull("video")) {
                val videoObject = itemObject.getJSONObject("video")
                // 動画情報
                val videoId = videoObject.getString("id")
                val title = videoObject.getString("title")
                val date = toUnixTime(videoObject.getString("registeredAt"))
                val duration = videoObject.getLong("duration")
                // 再生数等
                val countObject = videoObject.getJSONObject("count")
                val viewCount = countObject.getString("view")
                val commentCount = countObject.getString("comment")
                val mylistCount = countObject.getString("mylist")
                // さむね
                val thum = if (videoObject.getJSONObject("thumbnail").isNull("largeUrl")) {
                    videoObject.getJSONObject("thumbnail").getString("url")
                } else {
                    videoObject.getJSONObject("thumbnail").getString("largeUrl")
                }
                val data =
                    NicoVideoData(false, true, title, videoId, thum, date, viewCount, commentCount, mylistCount, myListItemId, myListAddedDate, duration, null, null)
                videoItems.add(data)
            }
        }
        return videoItems
    }

    /**
     * マイリストから動画を削除するAPIを叩く関数。
     * @param myListId マイリストのID。
     * @param itemId アイテムID（NicoVideoData#mylistItemId）。動画IDではないので注意
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス
     * */
    fun deleteMyListVideo(myListId: String, itemId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://nvapi.nicovideo.jp/v1/users/me/mylists/$myListId/items?itemIds=$itemId")
                header("Cookie", "user_session=$userSession")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("x-frontend-id", "3")
                header("x-request-with", "nicovideo")
                delete()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

    data class MyListData(val title: String, val id: String, val itemsCount: Int)

}