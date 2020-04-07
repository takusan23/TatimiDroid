package io.github.takusan23.tatimidroid.NicoAPI

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * キャッシュ取得など。
 * APIじゃないけど置く場所ないのでここで
 * ブロードキャスト初期化も
 * */
class NicoVideoCache(val context: Context?) {

    // DownloadManager
    var downloadManager: DownloadManager =
        context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private lateinit var broadcastReceiver: BroadcastReceiver

    /**
     * キャッシュ用フォルダからデータ持ってくる。
     * 多分重いのでコルーチンです。
     * @return NicoVideoDataの配列
     * */
    fun loadCache(): Deferred<ArrayList<NicoVideoData>> = GlobalScope.async {
        val list = arrayListOf<NicoVideoData>()
        // ScopedStorage
        if (context?.getExternalFilesDir(null) != null) {
            val media = context.getExternalFilesDir(null)
            // 動画キャッシュようフォルダ作成
            val cacheFolder = File("${media?.path}/cache")
            if (!cacheFolder.exists()) {
                cacheFolder.mkdir()
            }
            // 一覧取得
            cacheFolder.listFiles().forEach {
                // それぞれの動画フォルダ
                val videoFolder = it
                // 動画ID
                val videoId = videoFolder.name
                // 動画情報JSONパース
                val jsonString = File("${videoFolder.path}/${videoId}.json")
                if (jsonString.exists()) {
                    val jsonObject = JSONObject(jsonString.readText())
                    // NicoVideo
                    val video = jsonObject.getJSONObject("video")
                    val isCache = true
                    val title = video.getString("title")
                    val thum = "${videoFolder.path}/${videoId}.jpg"
                    val date =
                        NicoVideoHTML().postedDateTimeToUnixTime(video.getString("postedDateTime"))
                    val viewCount = video.getInt("viewCount").toString()
                    val commentCount =
                        jsonObject.getJSONObject("thread").getInt("commentCount").toString()
                    val mylistCount = video.getInt("mylistCount").toString()
                    val data =
                        NicoVideoData(isCache, false, title, videoId, thum, date, viewCount, commentCount, mylistCount, "")
                    list.add(data)
                } else {
                    /**
                     * 動画情報JSON、サムネイルがない場合で読み込みたいときに使う。主にニコ生TSを見るときに使って。
                     * */
                    val isCache = true
                    val isMylist = false
                    val title = getCacheFolderVideoFileName(videoId) ?: it.name
                    val videoId = it.name
                    val thum = ""
                    val date = it.lastModified()
                    val viewCount = "0"
                    val commentCount = "0"
                    val mylistCount = "0"
                    val mylistItemId = ""
                    // 動画からサムネイルを取得する

                    val data =
                        NicoVideoData(isCache, false, title, videoId, thum, date, viewCount, commentCount, mylistCount, mylistItemId)
                    list.add(data)
                }
            }
        }
        return@async list
    }


    /**
     * キャッシュ取得
     * 注意：キャッシュ取得中はハートビート処理しないと切られます、多分。
     * @param videoId 動画ID
     * @param json NicoVideoHTML#parseJSON()の値
     * @param 動画URL。公式動画は無理です。HLSの保存ってよくわからん。
     * @param userSession ユーザーセッション
     * @param nicoHistory レスポンスヘッダーのCookieにあるnicohistoryを入れてね。
     * */
    fun getCache(videoId: String, json: String, url: String, userSession: String, nicoHistory: String) {
        // 公式動画は落とせない。
        if (!isEncryption(json)) {
            // 保存先
            val path = getCacheFolderPath()
            // 動画IDフォルダー作成
            val videoIdFolder = File("$path/$videoId")
            videoIdFolder.mkdir()
            // コメント取得
            getCacheComment(videoIdFolder, videoId, json, userSession)
            // 動画情報取得
            saveVideoInfo(videoIdFolder, videoId, json)
            // 動画のサ胸取得
            getThumbnail(videoIdFolder, videoId, json, userSession)
            // 動画取得
            getVideoCache(videoIdFolder, videoId, url, userSession, nicoHistory)
        } else {
            showToast(context?.getString(R.string.encryption_not_download) ?: "")
        }
    }

    /**
     * 動画が暗号化されているか
     * dmcInfoが無いときもfalse
     * 暗号化されているときはtrue
     * されてないときはfalse
     * @param json js-initial-watch-dataのdata-api-data
     * */
    fun isEncryption(json: String): Boolean {
        return when {
            JSONObject(json).getJSONObject("video").isNull("dmcInfo") -> false
            JSONObject(json).getJSONObject("video").getJSONObject("dmcInfo")
                .has("encryption") -> true
            else -> false
        }
    }

    /**
     * キャッシュを削除する
     * @param videoId 動画ID
     * */
    fun deleteCache(videoId: String?) {
        if (videoId == null) {
            return
        }
        // 削除
        val videoIdFolder = File("${getCacheFolderPath()}/$videoId")
        videoIdFolder.listFiles().forEach {
            it.delete()
        }
        videoIdFolder.delete()
    }

    /**
     * 動画キャッシュ取得。privateにしてないのは動画だけ更新できるようにって思ってるけどそんなこと無いな。
     * これだけDownloadManagerを使ってみた。プログレスバーあるのは有能！
     * @param videoIdFolder 保存先フォルダ。
     * @param videoId 動画ID
     * @param 動画URL。公式動画は無理です。HLSの保存ってよくわからん。
     * @param userSession ユーザーセッション
     * @param nicoHistory レスポンスヘッダーのCookieにあるnicohistoryを入れてね。
     * */
    fun getVideoCache(videoIdFolder: File, videoId: String, url: String, userSession: String, nicoHistory: String) {
        // 動画mp4ファイル作成
        val videoIdMp4 = File("${videoIdFolder.path}/$videoId.mp4")
        // リクエスト作成
        val downloadRequest = DownloadManager.Request(url.toUri()).apply {
            addRequestHeader("Cookie", nicoHistory)
            addRequestHeader("User-Agent", "TatimiDroid;@takusan_23")
            addRequestHeader("Cookie", "user_session=$userSession")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(context?.getString(R.string.get_cache_video))
            setDestinationUri(videoIdMp4.toUri())
        }
        downloadManager.enqueue(downloadRequest)
    }

    /**
     * js-initial-watch-dataのdata-api-dataを保存する。キャッシュ取得で動画情報を保存するときに使う。
     * privateな関数ではないので再取得にも使えます。
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * */
    fun saveVideoInfo(videoIdFolder: File, videoId: String, json: String) {
        // 動画情報JSON作成
        val videoJSONFile = File("${videoIdFolder.path}/$videoId.json")
        // Kotlinくっそ簡単やんけ！
        videoJSONFile.writeText(json)
        showToast("$videoId\n${context?.getString(R.string.get_cache_video_info_ok)}")
    }

    /**
     * 動画のサムネイルを取得する。OkHttp
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * @param userSession ユーザーセッション
     * */
    fun getThumbnail(videoIdFolder: File, videoId: String, json: String, userSession: String) {
        // JSONパース
        val jsonObject = JSONObject(json)
        val thumbnailURL = jsonObject.getJSONObject("video").getString("largeThumbnailURL")
        // 動画サムネファイル作成
        val videoIdThum = File("${videoIdFolder.path}/$videoId.jpg")
        // リクエスト
        val request = Request.Builder().apply {
            url(thumbnailURL)
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 保存
                    val byte = response.body?.bytes()
                    if (byte != null) {
                        videoIdThum.writeBytes(byte)
                        showToast("$videoId\n${context?.getString(R.string.get_cache_thum_ok)}")
                    }
                }
            }
        })
    }

    /**
     * 動画のコメント取得。NicoVideoHTML#getComment()を使ってる。ここだけコルーチンなの不自然ある。
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * @param userSession ユーザーセッション
     * */
    fun getCacheComment(videoIdFolder: File, videoId: String, json: String, userSession: String) {
        GlobalScope.launch {
            // POSTするJSON作成
            val response =
                NicoVideoHTML().getComment(videoId, userSession, JSONObject(json)).await()
            if (response != null && response.isSuccessful) {
                // 動画情報JSON作成
                val videoJSONFile = File("${videoIdFolder.path}/${videoId}_comment.json")
                // Kotlinくっそ簡単やんけ！
                videoJSONFile.writeText(response.body?.string()!!)
                showToast("$videoId\n${context?.getString(R.string.get_cache_comment_ok)}")
            }
        }
    }

    // キャッシュフォルダのパス取得
    fun getCacheFolderPath(): String? {
        val media = context?.getExternalFilesDir(null)
        // 動画キャッシュようフォルダ作成
        val cacheFolder = File("${media?.path}/cache")
        return cacheFolder.path
    }

    /**
     * ブロードキャスト初期化
     * */
    fun initBroadcastReceiver(call: (() -> Unit)? = null) {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                showToast("${context?.getString(R.string.get_cache_video_ok)}")
                if (call != null) {
                    call()
                }
            }
        }
        context?.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    // 終了時に呼んでね
    fun destroy() {
        context?.unregisterReceiver(broadcastReceiver)
    }

    // Toast表示
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * キャッシュフォルダから動画のパスを取得する
     * @param 動画ID
     * */
    fun getCacheFolderVideoFilePath(videoId: String): String {
        return "${getCacheFolderPath()}/$videoId/$videoId.mp4"
    }

    /**
     * キャッシュフォルダから動画情報JSONファイルの中身を取得する。JSONファイルの中身ですよ！
     * @param videoId 動画ID
     * */
    fun getCacheFolderVideoInfoText(videoId: String): String {
        return File("${getCacheFolderPath()}/$videoId/$videoId.json").readText()
    }

    /**
     * 動画情報JSONがあるか。あればtrue
     * */
    fun existsCacheVideoInfoJSON(videoId: String): Boolean {
        return File("${getCacheFolderPath()}/$videoId/$videoId.json").exists()
    }

    /**
     * キャッシュフォルダから動画のサムネイルのパスを取得する。
     * @param videoId 動画ID
     * */
    fun getCacheFolderVideoThumFilePath(videoId: String): String {
        return "${getCacheFolderPath()}/$videoId/$videoId.jpg"
    }

    /**
     * キャッシュフォルダから動画のコメントJSONファイルの中身を取得する。JSONファイルの中身ですよ！
     * */
    fun getCacheFolderVideoCommentText(videoId: String): String {
        return File("${getCacheFolderPath()}/$videoId/${videoId}_comment.json").readText()
    }

    /**
     * キャッシュフォルダから動画の名前を取得する関数。ニコ生のTSのときに使って。
     * @return 動画ファイルの名前。ない場合はnull
     * */
    fun getCacheFolderVideoFileName(videoId: String): String? {
        // 見つける
        val videoFolder = File("${getCacheFolderPath()}/$videoId").listFiles()
        for (i in videoFolder.indices) {
            if (videoFolder[i].extension == "mp4") {
                return videoFolder[i].name
            }
        }
        return null
    }

    /**
     * 動画情報、コメント再取得まとめたやつ。
     * 二回も書かないと行けないのでここに書いた。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @param context Context。ActivityなりTextViewとかのViewだとView#getContext()あるし。。。
     * @param completeFun 終了時に呼ばれる高階関数。
     * */
    fun getReGetVideoInfoComment(videoId: String, userSession: String, context: Context?, completeFun: (() -> Unit)? = null) {
        GlobalScope.launch {
            val nicoVideoHTML = NicoVideoHTML()
            // 動画HTML取得
            val response = nicoVideoHTML.getHTML(videoId, userSession).await()
            if (response.isSuccessful) {
                // 動画情報更新
                val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                val videoIdFolder =
                    File("${getCacheFolderPath()}/${videoId}")
                saveVideoInfo(videoIdFolder, videoId, jsonObject.toString())
                // コメント取得
                val commentResponse =
                    nicoVideoHTML.getComment(videoId, userSession, jsonObject)
                        .await()
                val commentString = commentResponse?.body?.string()
                if (commentResponse?.isSuccessful == true && commentString != null) {
                    // コメント更新
                    getCacheComment(videoIdFolder, videoId, jsonObject.toString(), userSession)
                    showToast(context?.getString(R.string.cache_update_ok) ?: "取得できたよ")
                    if (completeFun != null) {
                        completeFun()
                    }
                } else {
                    showToast("${context?.getString(R.string.error)}\n${response.code}")
                }
            } else {
                showToast("${context?.getString(R.string.error)}\n${response?.code}")
            }
        }
    }

}