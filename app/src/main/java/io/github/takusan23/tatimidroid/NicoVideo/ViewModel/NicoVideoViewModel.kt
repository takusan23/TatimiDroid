package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.FregmentData.TabLayoutData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRecommendAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoruAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.Tool.isConnectionMobileDataInternet
import io.github.takusan23.tatimidroid.Tool.isLoginMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONObject

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment]のViewModel。
 *
 * いままでは画面回転前にデータ詰めてたんだけどViewModelを使えばFragmentのライフサイクルに関係なく生存する。
 *
 * でも何をおいておけば良いのかよくわからんので散らばってる。
 *
 * @param videoId 動画ID
 * @param isCache キャッシュで再生するか。ただし最終的には[isOfflinePlay]がtrueの時キャッシュ利用再生になります
 * @param isEco エコノミー再生ならtrue
 * @param useInternet キャッシュが有っても強制的にインターネットを経由して取得する場合はtrue
 * */
class NicoVideoViewModel(application: Application, val videoId: String, val isCache: Boolean, val isEco: Boolean, useInternet: Boolean) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** 結局インターネットで取得するのかローカルなのか。trueならキャッシュ再生 */
    var isOfflinePlay = false

    /** ViewPager2に動的に追加したFragment。 */
    val dynamicAddFragmentList = arrayListOf<TabLayoutData>()

    /** Fragment(Activity)へメッセージを送信するためのLiveData。Activity終了など */
    val messageLiveData = MutableLiveData<String>()

    /** Snackbarを表示しろってメッセージを送るLiveData */
    val snackbarLiveData = MutableLiveData<String>()

    /** ニコ動APIまとめ */
    val nicoVideoHTML = NicoVideoHTML()

    /** キャッシュまとめ */
    val nicoVideoCache = NicoVideoCache(context)

    /** ニコるくんAPI */
    val nicoruAPI = NicoruAPI()

    /** Smileサーバーの動画を再生するのに使う */
    var nicoHistory = ""

    /** ニコ動のJSON。コメントサーバーの情報や動画鯖の情報など */
    val nicoVideoJSON = MutableLiveData<JSONObject>()

    /** 旧サーバー（Smile鯖）の場合はfalse。DMC（画質変更ができる）ならtrue */
    var isDMCServer = true

    /** SessionAPIを叩いたレスポンスJSON */
    var sessionAPIJSON = JSONObject()

    /** 現在の画質 */
    var currentVideoQuality: String? = ""

    /** 現在の音質 */
    var currentAudioQuality: String? = ""

    /** 動画URL */
    val contentUrl = MutableLiveData<String>()

    /** 動画情報LiveData */
    val nicoVideoData = MutableLiveData<NicoVideoData>()

    /** コメントAPIの結果。 */
    var rawCommentList = arrayListOf<CommentJSONParse>()

    /** NGを適用したコメント。LiveDataで監視できます */
    var commentList = MutableLiveData<ArrayList<CommentJSONParse>>()

    /** NG配列 */
    private var ngList = listOf<NGDBEntity>()

    /** 関連動画配列。なんで個々にあるのかは不明 */
    val recommendList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** ニコるくんAPI叩く時に使う。どうにかしたい。 */
    var isPremium = false
    var threadId = ""
    var userId = ""

    /** 現在再生中の位置 */
    var currentPosition = 0L

    /** 全画面再生 */
    var isFullScreenMode = false

    /** 16:9の動画の際はtrue */
    var is169AspectLate = true

    init {
        onCleared()
        // どの方法で再生するか
        // キャッシュを優先的に使う設定有効？
        val isPriorityCache = prefSetting.getBoolean("setting_nicovideo_cache_priority", false)
        // キャッシュ再生が有効ならtrue
        isOfflinePlay = when {
            useInternet -> false // オンライン
            isCache -> true // キャッシュ再生
            NicoVideoCache(context).existsCacheVideoInfoJSON(videoId) && isPriorityCache -> true // キャッシュ優先再生が可能
            else -> false // オンライン
        }
        // 強制エコノミーの設定有効なら
        val isPreferenceEconomyMode = prefSetting.getBoolean("setting_nicovideo_economy", false)
        // エコノミー再生するなら
        val isEconomy = isEco
        // 再生準備を始める
        when {
            // キャッシュを優先的に使う&&キャッシュ取得済みの場合 もしくは　キャッシュ再生時
            isOfflinePlay -> cachePlay()
            // エコノミー再生？
            isEconomy || isPreferenceEconomyMode -> coroutine(true, "", "", true)
            // それ以外：インターネットで取得
            else -> coroutine()
        }
        viewModelScope.launch {
            // NGデータベースを監視する
            NGDBInit.getInstance(context).ngDBDAO().flowGetNGAll().collect {
                ngList = it
                // コメントフィルター通す
                commentFilter()
            }
        }
    }

    /** キャッシュから再生する */
    private fun cachePlay() {
        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(context, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            messageLiveData.postValue(getString(R.string.xml_comment_play))
            return
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    contentUrl.postValue("${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName")
                    // 動画情報
                    if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                        val jsonObject = JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId))
                        nicoVideoJSON.postValue(jsonObject)
                        nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(jsonObject, isOfflinePlay))
                    }
                    // コメント取得
                    launch {
                        val commentJSONFilePath = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                        val loadCommentAsync = nicoVideoHTML.parseCommentJSON(commentJSONFilePath, videoId)
                        // フィルターで3ds消したりする。が、コメントは並列で読み込んでるので、並列で作業してるコメント取得を待つ（合流する）
                        rawCommentList = ArrayList(loadCommentAsync)
                        commentFilter(true)
                    }
                } else {
                    // 動画が見つからなかった
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.not_found_video))
                        messageLiveData.postValue(getString(R.string.not_found_video))
                    }
                    return@launch
                }
            }
        }
    }

    /** インターネットから取得して再生する */
    fun coroutine(isGetComment: Boolean = true, videoQualityId: String = "", audioQualityId: String = "", smileServerLowRequest: Boolean = false) {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            // smileサーバーの動画は多分最初の視聴ページHTML取得のときに?eco=1をつけないと低画質リクエストできない
            val eco = if (smileServerLowRequest) "1" else ""
            // ログインしないならそもそもuserSessionの値を空にすれば！？
            val userSession = if (isLoginMode(context)) userSession else ""
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco)
            // 失敗したら落とす
            if (!response.isSuccessful) {
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val jsonObject = withContext(Dispatchers.Default) {
                nicoVideoHTML.parseJSON(response.body?.string())
            }
            nicoVideoJSON.postValue(jsonObject)
            isDMCServer = nicoVideoHTML.isDMCServer(jsonObject)
            // DMC鯖ならハートビート処理が必要なので。でもほぼDMC鯖からの配信じゃない？
            if (isDMCServer) {
                // 公式アニメは暗号化されてて見れないので落とす。最近プレ垢限定でアニメ配信してるんだっけ？
                if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                    showToast(context.getString(R.string.encryption_video_not_play))
                    // FragmentにおいたLiveDataのオブザーバーへActivity落とせってメッセージを送る
                    messageLiveData.postValue(context.getString(R.string.encryption_video_not_play))
                    return@launch
                } else {
                    // 再生可能
                    // モバイルデータで最低画質をリクエスト！
                    var videoQuality = videoQualityId
                    var audioQuality = audioQualityId
                    // 画質が指定している場合はモバイルデータ接続で最低画質の設定は無視
                    if (videoQuality.isEmpty() && audioQuality.isEmpty()) {
                        // モバイルデータ接続のときは強制的に低画質にする
                        if (prefSetting.getBoolean("setting_nicovideo_low_quality", false)) {
                            if (isConnectionMobileDataInternet(context)) {
                                // モバイルデータ
                                val videoQualityList = nicoVideoHTML.parseVideoQualityDMC(nicoVideoJSON.value!!)
                                val audioQualityList = nicoVideoHTML.parseAudioQualityDMC(nicoVideoJSON.value!!)
                                videoQuality = videoQualityList.getJSONObject(videoQualityList.length() - 1).getString("id")
                                audioQuality = audioQualityList.getJSONObject(audioQualityList.length() - 1).getString("id")
                            }
                            if (videoQuality.isNotEmpty()) {
                                showSnackBar("${getString(R.string.quality)}：$videoQuality")
                            }
                        }
                    }
                    // https://api.dmc.nico/api/sessions のレスポンス
                    val sessionAPIResponse = nicoVideoHTML.callSessionAPI(jsonObject, videoQuality, audioQuality)
                    if (sessionAPIResponse != null) {
                        sessionAPIJSON = sessionAPIResponse
                        // 動画URL
                        contentUrl.postValue(nicoVideoHTML.getContentURI(nicoVideoJSON.value!!, sessionAPIJSON))
                        // ハートビート処理。これしないと切られる。
                        nicoVideoHTML.heartBeat(nicoVideoJSON.value!!, sessionAPIJSON)
                        // 選択中の画質、音質控える
                        currentVideoQuality = nicoVideoHTML.getCurrentVideoQuality(sessionAPIJSON)
                        currentAudioQuality = nicoVideoHTML.getCurrentAudioQuality(sessionAPIJSON)
                    }
                }
            } else {
                // Smileさば。動画URL取得。自動or低画質は最初の視聴ページHTMLのURLのうしろに「?eco=1」をつければ低画質が送られてくる
                contentUrl.postValue(nicoVideoHTML.getContentURI(nicoVideoJSON.value!!, null))
            }
            // データクラスへ詰める
            nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(nicoVideoJSON.value!!, isOfflinePlay))
            // データベースへ書き込む
            insertDB()
            // コメント取得など
            if (isGetComment) {
                val commentJSON = async {
                    nicoVideoHTML.getComment(videoId, userSession, nicoVideoJSON.value!!)
                }
                rawCommentList = withContext(Dispatchers.Default) {
                    ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.await()?.body?.string()!!, videoId))
                }
                // フィルターで3ds消したりする
                commentFilter(true)
            }
            // 関連動画
            launch { getRecommend() }
            // ニコるくん
            launch { initNicoru() }
        }
    }


    /** 関連動画取得 */
    private suspend fun getRecommend() = withContext(Dispatchers.Default) {
        // 関連動画取得。
        val watchRecommendationRecipe = nicoVideoJSON.value!!.getString("watchRecommendationRecipe")
        val nicoVideoRecommendAPI = NicoVideoRecommendAPI()
        val recommendAPIResponse = nicoVideoRecommendAPI.getVideoRecommend(watchRecommendationRecipe)
        if (!recommendAPIResponse.isSuccessful) {
            // 失敗時
            showToast("${getString(R.string.error)}\n${recommendAPIResponse.code}")
            return@withContext
        }
        // パース
        withContext(Dispatchers.Default) {
            recommendList.postValue(nicoVideoRecommendAPI.parseVideoRecommend(recommendAPIResponse.body?.string()))
        }
    }

    /** ニコるくんのための準備 */
    private suspend fun initNicoru() = withContext(Dispatchers.Default) {
        // ニコるくん
        isPremium = nicoVideoHTML.isPremium(nicoVideoJSON.value!!)
        threadId = nicoVideoHTML.getThreadId(nicoVideoJSON.value!!)
        userId = nicoVideoHTML.getUserId(nicoVideoJSON.value!!)
        if (isPremium) {
            val nicoruResponse = nicoruAPI.getNicoruKey(userSession, threadId)
            if (!nicoruResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${nicoruResponse.code}")
                return@withContext
            }
            // nicoruKey!!!
            withContext(Dispatchers.Default) {
                nicoruAPI.parseNicoruKey(nicoruResponse.body?.string())
            }
        }
    }

    /** コメントNGを適用したりする */
    fun commentFilter(isShowToast: Boolean = false) {
        // 3DSけす？
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        // NGコメント。ngList引数が省略時されてるときはDBから取り出す
        val ngCommentList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // NGユーザー。ngList引数が省略時されてるときはDBから取り出す
        val ngUserList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // はい、NGでーす
        val filteredList = rawCommentList
            .filter { commentJSONParse -> if (is3DSCommentHidden) !commentJSONParse.mail.contains("device:3DS") else true }
            .filter { commentJSONParse -> !ngCommentList.contains(commentJSONParse.comment) }
            .filter { commentJSONParse -> !ngUserList.contains(commentJSONParse.userId) } as ArrayList<CommentJSONParse>
        commentList.postValue(filteredList)
        if (isShowToast) {
            showToast("${getString(R.string.get_comment_count)}：${filteredList.size}")
        }
    }

    /** 履歴データベースへ書き込む */
    private fun insertDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val unixTime = System.currentTimeMillis() / 1000
            // 入れるデータ
            val publisherId = nicoVideoHTML.getUploaderId(nicoVideoJSON.value!!)
            val nicoHistoryDBEntity = NicoHistoryDBEntity(
                type = "video",
                serviceId = videoId,
                userId = publisherId,
                title = nicoVideoJSON.value?.getJSONObject("video")?.getString("title") ?: videoId,
                unixTime = unixTime,
                description = ""
            )
            // 追加
            NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
        }
    }

    /**
     * SnackBar表示関数。予めFragmentでLiveDataをオブザーバーでつなげておいてね
     * */
    private fun showSnackBar(message: String?) {
        snackbarLiveData.postValue(message)
    }

    /** Toast表示関数 */
    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    /** ViewModel終了時 */
    override fun onCleared() {
        super.onCleared()
        nicoVideoHTML.destory()
    }

}