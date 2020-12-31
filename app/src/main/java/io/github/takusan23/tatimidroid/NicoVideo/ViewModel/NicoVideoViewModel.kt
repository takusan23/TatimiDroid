package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Adapter.Parcelable.TabLayoutData
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoLikeAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRecommendAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoruAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.User.UserData
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
 * @param videoId 動画ID。連続再生の[videoList]が指定されている場合はnullに出来ます。また、連続再生時にこの値に動画IDを入れるとその動画から再生を始めるようにします。
 * @param isCache キャッシュで再生するか。ただし最終的には[isOfflinePlay]がtrueの時キャッシュ利用再生になります。連続再生の[videoList]が指定されている場合はnullに出来ます。
 * @param isEco エコノミー再生ならtrue。なお、キャッシュを優先的に利用する設定等でキャッシュ再生になっている場合があるので[isOfflinePlay]を使ってください。なお連続再生時はすべての動画をエコノミーで再生します。
 * @param useInternet キャッシュが有っても強制的にインターネットを経由して取得する場合はtrue。
 * @param videoList 連続再生するなら配列を入れてね。nullでもいい
 * */
class NicoVideoViewModel(application: Application, videoId: String? = null, isCache: Boolean? = null, val isEco: Boolean, val useInternet: Boolean, startFullScreen: Boolean, val videoList: ArrayList<NicoVideoData>?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** 再生中の動画ID。動画変更の検知は[isOfflinePlay]をObserveしたほうが良いと思う（[playingVideoId]→[isOfflinePlay]の順番でLiveData通知が行く） */
    var playingVideoId = MutableLiveData<String>()

    /** 結局インターネットで取得するのかローカルなのか。trueならキャッシュ再生。ちなみにLiveDataの通知順だと、[playingVideoId]のほうが先にくる。 */
    var isOfflinePlay = MutableLiveData<Boolean>()

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

    /** 動画説明文LiveData */
    val nicoVideoDescriptionLiveData = MutableLiveData<String>()

    /** いいね済みかどうか。キャッシュ再生では使えない。 */
    val isLikedLiveData = MutableLiveData(false)

    /** いいねしたときのお礼メッセージを送信するLiveData */
    val likeThanksMessageLiveData = MutableLiveData<String>()

    /** ユーザー情報LiveData */
    val userDataLiveData = MutableLiveData<UserData>()

    /** タグ送信LiveData */
    val tagListLiveData = MutableLiveData<ArrayList<NicoLiveTagDataClass>>()

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
    var isFullScreenMode = startFullScreen

    /** ミニプレイヤーかどうか */
    var isMiniPlayerMode = MutableLiveData(false)

    /** 連続再生かどうか。連続再生ならtrue */
    val isPlayListMode = videoList != null

    /** 連続再生時に、再生中の動画が[videoList]から見てどこの位置にあるかが入っている */
    val playListCurrentPosition = MutableLiveData(0)

    /** 連続再生時に逆順再生が有効になっているか。trueなら逆順 */
    val isReversed = MutableLiveData(false)

    /** 連続再生の最初の並び順が入っている */
    val originVideoSortList = videoList?.map { nicoVideoData -> nicoVideoData.videoId }

    /** 連続再生時にシャッフル再生が有効になってるか。trueならシャッフル再生 */
    val isShuffled = MutableLiveData(false)

    /** コメントのみ表示する場合はtrue */
    var isCommentOnlyMode = prefSetting.getBoolean("setting_nicovideo_comment_only", false)

    /** 映像なしでコメントを流すコメント描画のみ、映像なしモード。ニコニコ実況みたいな */
    val isNotPlayVideoMode = MutableLiveData<Boolean>(false)

    /** プレイヤーの再生状態を通知するLiveData。これ経由で一時停止等を操作する。trueで再生 */
    val playerIsPlaying = MutableLiveData(false)

    /**
     * 現在の再生位置。LiveDataではないので定期的に値を入れてください。ミリ秒
     * [isNotPlayVideoMode]がtrueの場合（コメントのみを流すモードの時）は[initNotPlayVideoMode]を呼んで動画を再生している" つもり" になって値を入れてあげてください。
     * 呼ばないとコメントが流れません。
     * */
    var playerCurrentPositionMs = 0L

    /** 動画をシークする際に使うLiveData。再生時間の取得には[playerCurrentPositionMs]を使ってくれ。ミリ秒 */
    val playerSetSeekMs = MutableLiveData<Long>()

    /** リピートモードを設定するLiveData。これ経由でリピートモードの設定をする。trueでリピート */
    val playerIsRepeatMode = MutableLiveData(prefSetting.getBoolean("nicovideo_repeat_on", true))

    /** 動画の時間を通知するLiveData。ミリ秒 */
    val playerDurationMs = MutableLiveData<Long>()

    /** [isNotPlayVideoMode]がtrueのときにコルーチンを使うのでそれ */
    private val notVideoPlayModeCoroutineContext = Job()

    /** 動画の幅。ExoPlayerで取得して入れておいて */
    var videoWidth = 16

    /** 動画の高さ。同じくExoPlayerで取得して入れておいて */
    var videoHeight = 9

    /** コメント一覧表示してくれ～LiveData */
    val commentListBottomSheetLiveData = MutableLiveData(false)

    init {

        // 最初の動画。連続再生と分岐
        if (isPlayListMode) {
            val videoData = videoList!![0]
            val startVideoId = videoId ?: videoData.videoId
            // 指定した動画がキャッシュ再生かどうか
            val startVideoIdCanCachePlay = videoList.find { nicoVideoData -> nicoVideoData.videoId == startVideoId }?.isCache ?: false
            load(startVideoId, startVideoIdCanCachePlay, isEco, useInternet)
        } else {
            load(videoId!!, isCache!!, isEco, useInternet)
        }

        // NGデータベースを監視する
        viewModelScope.launch {
            NGDBInit.getInstance(context).ngDBDAO().flowGetNGAll().collect {
                ngList = it
                // コメントフィルター通す
                commentFilter()
            }
        }

    }

    /**
     * 再生する関数。
     * @param videoId 動画ID
     * */
    fun load(videoId: String, isCache: Boolean, isEco: Boolean, useInternet: Boolean) {
        onCleared()
        notVideoPlayModeCoroutineContext.cancelChildren()
        playerCurrentPositionMs = 0

        // 動画ID変更を通知
        playingVideoId.value = videoId
        if (videoList != null) {
            playListCurrentPosition.value = videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == videoId }
        }
        // どの方法で再生するか
        // キャッシュを優先的に使う設定有効？
        val isPriorityCache = prefSetting.getBoolean("setting_nicovideo_cache_priority", false)
        // キャッシュ再生が有効ならtrue
        isOfflinePlay.value = when {
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
            isOfflinePlay.value ?: false -> cachePlay()
            // エコノミー再生？
            isEconomy || isPreferenceEconomyMode -> coroutine(true, "", "", true)
            // それ以外：インターネットで取得
            else -> coroutine()
        }
    }

    /** キャッシュから再生する */
    private fun cachePlay() {
        val videoId = playingVideoId.value ?: return
        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(context, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            messageLiveData.postValue(getString(R.string.xml_comment_play))
            return
        } else {
            viewModelScope.launch(Dispatchers.IO) {

                // 動画ファイルが有るか
                if (nicoVideoCache.hasCacheVideoFile(videoId)) {
                    val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                    contentUrl.postValue("${nicoVideoCache.getCacheFolderPath()}/${playingVideoId.value}/$videoFileName")
                } else {
                    // 動画無しでコメントだけを流すモードへ切り替える
                    withContext(Dispatchers.Main) {
                        showToast(context.getString(R.string.nicovideo_not_play_video_mode))
                        messageLiveData.postValue(context.getString(R.string.nicovideo_not_play_video_mode))
                        isNotPlayVideoMode.postValue(true)
                    }
                }

                // 動画情報JSONがあるかどうか
                if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                    val jsonObject = JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId))
                    nicoVideoJSON.postValue(jsonObject)
                    nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(jsonObject, isOfflinePlay.value ?: false))
                    // 動画説明文
                    nicoVideoDescriptionLiveData.postValue(jsonObject.getJSONObject("video").getString("description"))
                    // ユーザー情報LiveData
                    userDataLiveData.postValue(nicoVideoHTML.parseUserData(jsonObject))
                    // タグLiveData
                    tagListLiveData.postValue(nicoVideoHTML.parseTagDataList(jsonObject))
                }

                // コメントが有るか
                if (xmlCommentJSON.commentJSONFileExists(videoId)) {
                    // コメント取得。
                    launch {
                        val commentJSONFilePath = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                        val loadCommentAsync = nicoVideoHTML.parseCommentJSON(commentJSONFilePath, videoId)
                        // フィルターで3ds消したりする。が、コメントは並列で読み込んでるので、並列で作業してるコメント取得を待つ（合流する）
                        rawCommentList = ArrayList(loadCommentAsync)
                        commentFilter(true)

                        // 動画なしモードと発覚した場合は自前で再生時間等を作成する
                        if (isNotPlayVideoMode.value == true) {
                            initNotPlayVideoMode()
                        }

                    }
                }

            }
        }
    }

    /**
     * インターネットから取得して再生する
     * @param videoId 動画ID
     * @param isGetComment コメントを取得する場合はtrue。基本true
     * @param videoQualityId 画質変更をする場合は入れてね。こんなの「archive_h264_4000kbps_1080p」
     * @param audioQualityId 音質変更をする場合は入れてね。
     * @param smileServerLowRequest Smile鯖で低画質をリクエストする場合はtrue。
     * */
    fun coroutine(isGetComment: Boolean = true, videoQualityId: String = "", audioQualityId: String = "", smileServerLowRequest: Boolean = false) {
        val videoId = playingVideoId.value ?: return
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
            // 動画説明文
            nicoVideoDescriptionLiveData.postValue(jsonObject.getJSONObject("video").getString("description"))
            // いいね済みかどうか
            isLikedLiveData.postValue(nicoVideoHTML.isLiked(jsonObject))
            // ユーザー情報LiveData
            userDataLiveData.postValue(nicoVideoHTML.parseUserData(jsonObject))
            // タグLiveData
            tagListLiveData.postValue(nicoVideoHTML.parseTagDataList(jsonObject))

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
            nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(nicoVideoJSON.value!!, isOfflinePlay.value ?: false))
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

    /**
     * 動画なしコメントのみを流すモードの初期化
     * ExoPlayerがいないので動画の時間を自分で進めるしか無い。
     * */
    private fun initNotPlayVideoMode() {
        viewModelScope.launch(notVideoPlayModeCoroutineContext) {
            // duration計算する
            val lastVpos = commentList.value?.maxOf { commentJSONParse -> commentJSONParse.vpos.toLong() }
            if (lastVpos != null) {
                playerDurationMs.postValue(lastVpos * 10) // 100vpos = 1s なので 10かけて 1000ms = 1s にする
            }
            while (true) {
                // プログレスバー進める
                delay(100)
                // 再生中 でなお 動画の時間がわかってるとき のみプログレスバーを進める
                if (playerIsPlaying.value == true && playerDurationMs.value != null) {
                    if (playerCurrentPositionMs < playerDurationMs.value!!) {
                        // 動画の長さのほうが大きい時は加算
                        playerCurrentPositionMs += 100
                    } else {
                        if (playerIsRepeatMode.value == true) {
                            // リピートモードなら0にして再生を続ける。
                            playerIsPlaying.value = true
                            playerCurrentPositionMs = 0L
                        } else {
                            // おしまい
                            playerIsPlaying.value = false
                        }
                    }
                }
            }
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

        /**
         * かんたんコメントを消す。forkの値が2の場合はかんたんコメントになる。
         * どうでもいいんだけどあの機能、関係ないところでうぽつとかできるから控えめに言ってあらし機能だろあれ。てかROM専は何してもコメントしないぞ
         * */
        val isHideKantanComment = prefSetting.getBoolean("nicovideo_comment_kantan_comment_hidden", false)

        // NGコメント。ngList引数が省略時されてるときはDBから取り出す
        val ngCommentList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // NGユーザー。ngList引数が省略時されてるときはDBから取り出す
        val ngUserList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // はい、NGでーす
        val filteredList = rawCommentList
            .filter { commentJSONParse -> if (is3DSCommentHidden) !commentJSONParse.mail.contains("device:3DS") else true }
            .filter { commentJSONParse -> if (isHideKantanComment) commentJSONParse.fork != 2 else true } // fork == 2 が かんたんコメント
            .filter { commentJSONParse -> !ngCommentList.contains(commentJSONParse.comment) }
            .filter { commentJSONParse -> !ngUserList.contains(commentJSONParse.userId) } as ArrayList<CommentJSONParse>
        commentList.postValue(filteredList)
        if (isShowToast) {
            showToast("${getString(R.string.get_comment_count)}：${filteredList.size}")
        }
    }

    /** 履歴データベースへ書き込む */
    private fun insertDB() {
        val videoId = playingVideoId.value ?: return
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

    /** 連続再生時に次の動画に行く関数 */
    fun nextVideo() {
        if (isPlayListMode && videoList != null) {
            // 連続再生時のみ利用可能
            val currentPos = playListCurrentPosition.value ?: return
            val nextVideoPos = if (currentPos + 1 < videoList.size) {
                // 次の動画がある
                currentPos + 1
            } else {
                // 最初の動画にする
                0
            }
            val videoData = videoList[nextVideoPos]
            load(videoData.videoId, videoData.isCache, isEco, useInternet)
        }
    }

    /** 連続再生時に動画IDを指定して切り替える関数 */
    fun playlistGoto(videoId: String) {
        if (isPlayListMode && videoList != null) {
            // 動画情報を見つける
            val videoData = videoList.find { nicoVideoData -> nicoVideoData.videoId == videoId } ?: return
            load(videoData.videoId, videoData.isCache, isEco, useInternet)
        }
    }

    /**
     * いいねする関数
     * 結果はLiveDataへ送信されます
     * */
    fun postLike() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            val likeAPI = NicoLikeAPI()
            val likeResponse = withContext(Dispatchers.IO) {
                // いいね なのか いいね解除 なのか
                likeAPI.postLike(userSession, playingVideoId.value!!)
            }
            if (!likeResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${likeResponse.code}")
                return@launch
            }
            // いいね登録
            val thanksMessage = withContext(Dispatchers.Default) {
                // お礼メッセージパース
                likeAPI.parseLike(likeResponse.body?.string())
            }
            // 文字列 "null" の可能性
            val message = if (thanksMessage == "null") getString(R.string.like_ok) else thanksMessage
            likeThanksMessageLiveData.postValue(message)
            // 登録した
            isLikedLiveData.postValue(true)
        }
    }

    /**
     * いいねを解除する関数
     * 結果はLiveDataでわかります。
     * */
    fun removeLike() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            val likeAPI = NicoLikeAPI()
            val likeResponse = withContext(Dispatchers.IO) {
                // いいね なのか いいね解除 なのか
                likeAPI.deleteLike(userSession, playingVideoId.value!!)
            }
            if (!likeResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${likeResponse.code}")
                return@launch
            }
            // 解除した
            isLikedLiveData.postValue(false)
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
        nicoVideoHTML.destroy()
    }

}