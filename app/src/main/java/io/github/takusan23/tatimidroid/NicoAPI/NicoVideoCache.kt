package io.github.takusan23.tatimidroid.NicoAPI

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheFilterDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DownloadPocket
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * キャッシュ取得など。
 * APIじゃないけど置く場所ないのでここで
 * */
class NicoVideoCache(val context: Context?) {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /** キャッシュ合計サイズ。注意：loadCache()を呼ぶまで0です */
    var cacheTotalSize = 0L

    /**
     * キャッシュ用フォルダからデータ持ってくる。
     * [Dispatchers.IO]だとスレッド数が多いらしい←？
     * 多分重いのでコルーチンです。
     * @return NicoVideoDataの配列
     * */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        cacheTotalSize = 0
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
            cacheFolder.listFiles()?.forEach {
                it.listFiles()?.forEach {
                    cacheTotalSize += it.length()
                }
                // それぞれの動画フォルダ
                val videoFolder = it
                // 動画ID
                val videoId = videoFolder.name
                // 動画情報JSONパース
                val jsonString = File("${videoFolder.path}/${videoId}.json")
                if (jsonString.exists()) {
                    // まれによく落ちるので。ファイルあるって言ってんのに何で無いっていうの？
                    val jsonObject = try {
                        JSONObject(jsonString.readText())
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        return@forEach
                    }
                    // NicoVideo
                    val video = jsonObject.getJSONObject("video")
                    val isCache = true
                    val title = video.getString("title")
                    val thum = "${videoFolder.path}/${videoId}.jpg"
                    val date = NicoVideoHTML().postedDateTimeToUnixTime(video.getString("postedDateTime"))
                    val viewCount = video.getInt("viewCount").toString()
                    val commentCount =
                        jsonObject.getJSONObject("thread").getInt("commentCount").toString()
                    val mylistCount = video.getInt("mylistCount").toString()
                    val cacheAddedDate = it.lastModified()
                    // 再生時間取得
                    val duration = video.getLong("duration")
                    // タグJSON
                    val tagsJSONArray = arrayListOf<String>().apply {
                        val jsonArray = jsonObject.getJSONArray("tags")
                        for (i in 0 until jsonArray.length()) {
                            add(jsonArray.getJSONObject(i).getString("name"))
                        }
                    }
                    // 投稿者
                    val uploaderName = NicoVideoHTML().getUploaderName(jsonObject)
                    // キャッシュ取得日時
                    val data = NicoVideoData(isCache = isCache, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = duration, cacheAddedDate = cacheAddedDate, uploaderName = uploaderName, videoTag = tagsJSONArray)
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
                    val viewCount = "-1"
                    val commentCount = "-1"
                    val mylistCount = "-1"
                    val mylistItemId = ""
                    val duration = 0L
                    // 動画からサムネイルを取得する
                    val data = NicoVideoData(isCache = isCache, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = mylistItemId, mylistAddedDate = null, duration = duration, cacheAddedDate = date)
                    list.add(data)
                }
            }
        }
        // 新たしい順にソート
        list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
        list
    }

    /**
     * 動画の再生時間を取得する。ミリ秒ではなく秒です。
     * 重そう（小並感
     * @param videoId 動画ID
     * */
    fun getVideoDurationSec(videoId: String): Long {
        // 動画パス
        val videoFile = File("${context?.getExternalFilesDir(null)?.path}/cache/$videoId/${getCacheFolderVideoFileName(videoId)}")
        if (!videoFile.exists()) {
            return 0L
        }
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(videoFile.path)
        val time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: return 0L
        mediaMetadataRetriever.release()
        return time.toLong() / 1000
    }

    /**
     * キャッシュ一覧をCacheFilterDataClassでふるいにかけて返す。
     * @param cacheNicoVideoDataList loadCache()の返り値
     * @param filter フィルターかけるときに使って。
     * */
    fun getCacheFilterList(cacheNicoVideoDataList: ArrayList<NicoVideoData>, filter: CacheFilterDataClass): ArrayList<NicoVideoData> {

        // 部分一致検索。大文字小文字を無視するので強制大文字
        var filterList = cacheNicoVideoDataList.filter { nicoVideoData ->
            nicoVideoData.title.toUpperCase(Locale.getDefault()).contains(filter.titleContains.toUpperCase(Locale.getDefault()))
        } as ArrayList<NicoVideoData>

        // 指定中のタグソート
        filterList = filterList.filter { nicoVideoData ->
            nicoVideoData.videoTag?.containsAll(filter.tagItems) ?: false // 含まれているか
        } as ArrayList<NicoVideoData>

        // やったぜ。投稿者：でソート
        if (filter.uploaderName.isNotEmpty()) {
            filterList = filterList.filter { nicoVideoData ->
                filter.uploaderName == nicoVideoData.uploaderName
            } as ArrayList<NicoVideoData>
        }

        // たちみどろいどで取得したキャッシュのみを再生
        if (filter.isTatimiDroidGetCache) {
            filterList = filterList.filter { nicoVideoData ->
                nicoVideoData.commentCount != "-1"
            } as ArrayList<NicoVideoData>
        }

        // 並び替え
        sort(filterList, NicoVideoCacheFilterBottomFragment.CACHE_FILTER_SORT_LIST.indexOf(filter.sort))

        return filterList
    }

    /**
     * キャッシュ取得
     * 注意：キャッシュ取得中はハートビート処理しないと切られます、多分。
     * @param videoId 動画ID
     * @param json NicoVideoHTML#parseJSON()の値
     * @param userSession ユーザーセッション
     * @param nicoHistory レスポンスヘッダーのCookieにあるnicohistoryを入れてね。
     * @param splitCount 分割数。並列リクエスト数。デフォ4
     * @param url 動画URL
     * */
    suspend fun getCache(videoId: String, json: String, url: String, userSession: String, nicoHistory: String, splitCount: Int = 4) = withContext(Dispatchers.IO) {
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
            // 動画取得で使う一時的に持っておくフォルダ
            val tmpVideoFileFolder = File(path, "${videoId}_pocket")
            // 動画取得
            getVideoDownloader(tmpVideoFileFolder, videoIdFolder, videoId, url, userSession, nicoHistory, splitCount)
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
        if (videoId == null) return
        // 削除
        val videoIdFolder = File("${getCacheFolderPath()}/$videoId")
        videoIdFolder.listFiles()?.forEach { it.delete() }
        videoIdFolder.delete()
    }

    /**
     * 動画をダウンロードする[DownloadPocket]クラスを返す
     * */
    suspend fun getVideoDownloader(tmpFileFolder: File, videoIdFolder: File, videoId: String, url: String, userSession: String, nicoHistory: String, splitCount: Int) = withContext(Dispatchers.IO) {
        // 動画mp4ファイル作成
        val resultVideoFile = File("${videoIdFolder.path}/${videoId}.mp4")
        // ヘッダー
        val headers = arrayListOf(
            Pair("User-Agent", "TatimiDroid;@takusan_23"),
            Pair("Cookie", "user_session=$userSession"),
            Pair("Cookie", nicoHistory),
        )
        // 並列ダウンロードするやつ
        return@withContext DownloadPocket(
            url = url,
            splitFileFolder = tmpFileFolder,
            resultVideoFile = resultVideoFile,
            headers = headers,
            splitCount = splitCount
        )
    }

    /**
     * js-initial-watch-dataのdata-api-dataを保存する。キャッシュ取得で動画情報を保存するときに使う。
     * privateな関数ではないので再取得にも使えます。
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * */
    suspend fun saveVideoInfo(videoIdFolder: File, videoId: String, json: String) = withContext(Dispatchers.Default) {
        // 動画情報JSON作成
        val videoJSONFile = File("${videoIdFolder.path}/$videoId.json")
        videoJSONFile.createNewFile()
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
    suspend fun getThumbnail(videoIdFolder: File, videoId: String, json: String, userSession: String) = withContext(Dispatchers.Default) {
        // JSONパース
        val jsonObject = JSONObject(json)
        val thumbnailURL = jsonObject.getJSONObject("video").getString("largeThumbnailURL")
        // 動画サムネファイル作成
        val videoIdThum = File("${videoIdFolder.path}/$videoId.jpg")
        videoIdThum.createNewFile()
        // リクエスト
        val request = Request.Builder().apply {
            url(thumbnailURL)
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            // 保存
            val byte = response.body?.bytes()
            if (byte != null) {
                videoIdThum.writeBytes(byte)
                showToast("$videoId\n${context?.getString(R.string.get_cache_thum_ok)}")
            }
        }
    }

    /**
     * 動画のコメント取得。NicoVideoHTML#getComment()を使ってる。非同期
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * @param userSession ユーザーセッション
     * */
    suspend fun getCacheComment(videoIdFolder: File, videoId: String, json: String, userSession: String) = withContext(Dispatchers.IO) {
        // POSTするJSON作成
        val response = NicoVideoHTML().getComment(videoId, userSession, JSONObject(json))
        if (response != null && response.isSuccessful) {
            // 動画コメントJSON作成
            val videoJSONFile = File("${videoIdFolder.path}/${videoId}_comment.json")
            videoJSONFile.createNewFile()
            // Kotlinくっそ簡単やんけ！
            videoJSONFile.writeText(response.body?.string()!!)
            showToast("$videoId\n${context?.getString(R.string.get_cache_comment_ok)}")
        }
    }

    /** キャッシュフォルダのパス取得 */
    fun getCacheFolderPath(): String? {
        val media = context?.getExternalFilesDir(null)
        // 動画キャッシュようフォルダ作成
        val cacheFolder = File(media, "cache")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir()
        }
        return cacheFolder.path
    }

    /** キャッシュ取得の際に一時的に使えるフォルダのパス取得 */
    fun getCacheTempFolderPath(): String? {
        val media = context?.externalCacheDir
        // 動画キャッシュようフォルダ作成
        val cacheFolder = File(media, "cache_tmp")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir()
        }
        return cacheFolder.path
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
        return "${getCacheFolderPath()}/$videoId/${getCacheFolderVideoFileName(videoId)}"
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
     * コメントJSONファイルのFileを返す
     * */
    fun getCacheFolderVideoCommentFile(videoId: String): File {
        return File("${getCacheFolderPath()}/$videoId/${videoId}_comment.json")
    }

    /**
     * キャッシュフォルダから動画のコメントJSONファイルの中身を取得する。JSONファイルの中身ですよ！
     * */
    fun getCacheFolderVideoCommentText(videoId: String): String {
        return getCacheFolderVideoCommentFile(videoId).readText()
    }

    /**
     * キャッシュフォルダから動画の名前を取得する関数。ニコ生のTSのときに使って。
     * @return 動画ファイルの名前。ない場合はnull
     * */
    fun getCacheFolderVideoFileName(videoId: String): String? {
        // 見つける
        val videoFolder = File("${getCacheFolderPath()}/$videoId").listFiles() ?: return null
        for (i in videoFolder.indices) {
            if (videoFolder[i].extension == "mp4") {
                return videoFolder[i].name
            }
        }
        return null
    }

    /**
     * 動画ファイルが存在するかどうか。
     * @param videoId 動画ID。
     * @return あればtrueを、なければfalse
     * */
    fun hasCacheVideoFile(videoId: String): Boolean {
        val videoFolder = File("${getCacheFolderPath()}/$videoId").listFiles()
        // mp4でフィルターかけて0じゃなければある判定。てか IntelliJ IDEA くん優秀すぎん？array()#any{}に置き換えられるとか知らんかったわ
        return videoFolder?.any { file -> file.extension == "mp4" } ?: false
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
            val response = nicoVideoHTML.getHTML(videoId, userSession)
            if (response.isSuccessful) {
                // 動画情報更新
                val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                val videoIdFolder = File("${getCacheFolderPath()}/${videoId}")
                saveVideoInfo(videoIdFolder, videoId, jsonObject.toString())
                // コメント取得
                val commentResponse = nicoVideoHTML.getComment(videoId, userSession, jsonObject)
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

    /**
     * 動画IDかどうか（smかsoかnmのもじが入ってるかどうか）
     * @param videoId 動画IDかどうか確かめたい文字列
     * @return 動画IDの場合はtrue。違ったらfalse
     * */
    fun checkVideoId(videoId: String): Boolean {
        return when {
            videoId.contains("sm") -> true
            videoId.contains("so") -> true
            videoId.contains("nm") -> true
            else -> false
        }
    }

    private fun sort(list: ArrayList<NicoVideoData>, position: Int) {
        // 選択
        when (position) {
            0 -> list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
            1 -> list.sortBy { nicoVideoData -> nicoVideoData.cacheAddedDate }
            2 -> list.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> list.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> list.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> list.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> list.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> list.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> list.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> list.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> list.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> list.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
    }

}