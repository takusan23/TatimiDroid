package io.github.takusan23.tatimidroid.NicoAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

/**
 * マイリストAPI
 * */
class NicoVideoMyListAPI {

    /**
     * マイリストで使うトークンを取得するときに使うHTMLを取得する
     * ニコニコ新市場->動画引用だとこのTokenなしで取得できるAPIあるけど今回はPC版の方法で取得する
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun getMyListHTML(userSession: String): Deferred<Response> = GlobalScope.async {
        val url = "https://www.nicovideo.jp/my/mylist"
        val request = Request.Builder().apply {
            url(url)
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * HTMLの中からTokenを取り出す
     * @param getMyListHTML()の戻り値
     * @return マイリスト取得で利用するToken。見つからなかったらnull
     * */
    fun getToken(string: String?): String? {
        //正規表現で取り出す。
        val regex = "NicoAPI.token = \"(.+?)\";"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(string)
        if (matcher.find()) {
            val token = matcher.group(1)
            //println("トークン　$token")
            return token
        }
        return null
    }

    /**
     * マイリスト一覧を取得する。
     * 注意　とりあえずマイリストはこの中に含まれません。
     * @param token getHTML()とgetToken()を使って取ったトークン
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun getMyListList(token: String, userSession: String): Deferred<Response> = GlobalScope.async {
        val url = "https://www.nicovideo.jp/api/mylistgroup/list"
        // POSTする内容
        val post = FormBody.Builder()
            .add("token", token)
            .build()
        val request = Request.Builder().apply {
            url(url)
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(post)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * マイリストの中身を取得する
     * @param token getHTML()とgetToken()を使って取ったトークン。
     * @param mylistId マイリストのID。からの場合はとりあえずマイリストを取りに行きます
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun getMyListItems(token: String, mylistId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val post = FormBody.Builder().apply {
                add("token", token)
                //とりあえずマイリスト以外ではIDを入れる、
                if (mylistId.isNotEmpty()) {
                    add("group_id", mylistId)
                }
            }.build()
            //とりあえずマイリストと普通のマイリスト。
            val url = if (mylistId.isEmpty()) {
                "https://www.nicovideo.jp/api/deflist/list"
            } else {
                "https://www.nicovideo.jp/api/mylist/list"
            }
            val request = Request.Builder().apply {
                header("Cookie", "user_session=${userSession}")
                header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
                header("User-Agent", "TatimiDroid;@takusan_23")
                url(url)
                post(post)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * マイリストJSONパース
     * @param json getMyListItems()の戻り値
     * @return NicoVideoData配列
     * */
    fun parseMyListJSON(json: String?): ArrayList<NicoVideoData> {
        val myListList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val myListItem = jsonObject.getJSONArray("mylistitem")
        for (i in 0 until myListItem.length()) {
            val video = myListItem.getJSONObject(i)
            val itemData = video.getJSONObject("item_data")
            val title = itemData.getString("title")
            val videoId = itemData.getString("video_id")
            val thum = itemData.getString("thumbnail_url")
            val date = itemData.getLong("first_retrieve") * 1000 // ミリ秒へ
            val viewCount = itemData.getString("view_counter")
            val commentCount = itemData.getString("num_res")
            val mylistCount = itemData.getString("mylist_counter")
            val data =
                NicoVideoData(title, videoId, thum, date, viewCount, commentCount, mylistCount)
            myListList.add(data)
        }
        return myListList
    }

}