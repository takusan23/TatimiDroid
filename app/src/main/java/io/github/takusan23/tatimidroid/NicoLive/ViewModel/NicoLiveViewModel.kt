package io.github.takusan23.tatimidroid.NicoLive.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKFlvData
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.StatisticsDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.KotehanDBInit
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.Tool.isConnectionMobileDataInternet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import okhttp3.internal.toLongOrDefault
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * [io.github.takusan23.tatimidroid.NicoLive.CommentFragment]のViewModel
 *
 * いまだに何をおいておけば良いのかわからん
 *
 * @param liveId 番組ID
 * @param isJK 実況の時はtrue
 * @param isLoginMode HTML取得時にログインする場合はtrue
 * */
class NicoLiveViewModel(application: Application, val liveId: String, val isLoginMode: Boolean, val isJK: Boolean) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** HTML取得からWebSocket接続など */
    val nicoLiveHTML = NicoLiveHTML()

    /** ニコ生のコメントサーバーへ接続する */
    val nicoLiveComment = NicoLiveComment()

    /** ニコ実関係 */
    val nicoJK = NicoJKHTML()

    /** Snackbar表示用LiveData。無理やり複数行表示に対応してね */
    val snackbarLiveData = MutableLiveData<String>()

    /** Fragment(Activity)へメッセージを送信するためのLiveData。Activity終了など。*/
    val messageLiveData = MutableLiveData<String>()

    /** ニコ生のHTML内にあるJSONを入れる */
    val nicoLiveJSON = MutableLiveData<JSONObject>()

    /** 番組情報 */
    val nicoLiveProgramData = MutableLiveData<NicoLiveProgramData>()

    /** ニコニコ実況のデータ */
    val nicoJKGetFlv = MutableLiveData<NicoJKFlvData>()

    /** コメントを送るLiveData。ただ配列に入れる処理はこっちが担当するので、コメントが来た時に処理したい場合はどうぞ（RecyclerView更新など） */
    val commentReceiveLiveData = MutableLiveData<CommentJSONParse>()

    /** RecyclerViewを更新するLiveData？ */
    val updateRecyclerViewLiveData = MutableLiveData<String>()

    /** コメント配列 */
    val commentList = arrayListOf<CommentJSONParse>()

    /** 運営コメントを渡すLiveData。[CommentJSONParse]に変換可能なJSON文字列 か clear って文字が入るはず */
    val unneiCommentLiveData = MutableLiveData<String>()

    /** アンケートがあったら表示するLiveData。 */
    val enquateLiveData = MutableLiveData<String>()

    /** 来場者、コメント数等の来場者数を送るLiveData */
    val statisticsLiveData = MutableLiveData<StatisticsDataClass>()

    /** 部屋の名前と座席番号をつなげた文字列。getplayerstatusもデータクラスとかにしたい（とか言ってる間にgetPlayerStatus使えなくなりそう（使えなくてもノーダメだけど）） */
    val roomNameAndChairIdLiveData = MutableLiveData<String>()

    /** 一分間にコメントした人数（ユニークユーザー数ってやつ。同じIDは１として数える）。 */
    val activeCommentPostUserLiveData = MutableLiveData<String>()

    /** 経過時間をLiveDataで送る */
    val programTimeLiveData = MutableLiveData<String>()

    /** 番組終了時刻。フォーマット済み、HH:mm:ss */
    val formattedProgramEndTime = MutableLiveData<String>()

    /** 画質が切り替わったら飛ばすLiveData */
    val changeQualityLiveData = MutableLiveData<String>()

    /** [changeQualityLiveData]で二回目から使うので制御用 */
    private var isNotFirstQualityMessage = false

    /** 番組名 */
    var programTitle = ""

    /** コミュID */
    var communityId = ""

    /** サムネURL */
    var thumbnailURL = ""

    /** 現在の画質 */
    var currentQuality = ""

    /** 選択可能な画質 */
    var qualityListJSONArray = JSONArray()

    /** HLSアドレス */
    val hlsAddress = MutableLiveData<String>()

    /** 番組終了時刻。こっちはUnixTime。UI（Fragment）で使うこと無いしLiveDataじゃなくていっか！ */
    var programEndUnixTime = 0L

    /** 延長検知。視聴セッション接続後すぐに送られてくるので一回目はパス */
    private var isNotFirstEntyouKenti = false

    /** 運営コメントを消すときはtrue */
    var hideInfoUnnkome = false

    /** 匿名コメントを表示しない場合はtrue */
    var isTokumeiHide = false

    /** 匿名コメントで投稿する場合はtrue */
    var isPostTokumei = false

    /** NGコメント配列。Room+Flowで監視する */
    var ngCommentList = listOf<String>()

    /** NGのID配列。Room+Flowで監視する */
    var ngIdList = listOf<String>()

    /** 全画面再生時はtrue */
    var isFullScreenMode = false

    init {
        // エラーのとき（タイムアウトなど）はここでToastを出すなど
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        if (!isJK) {
            // 低遅延設定
            nicoLiveHTML.isLowLatency = prefSetting.getBoolean("nicolive_low_latency", false)
            // 初回の画質を低画質にする設定（モバイル回線とか強制低画質モードとか）
            val isMobileDataLowQuality = prefSetting.getBoolean("setting_mobiledata_quality_low", false) == isConnectionMobileDataInternet(context) // 有効時 でなお モバイルデータ接続時
            val isPreferenceLowQuality = prefSetting.getBoolean("setting_nicolive_quality_low", false)
            if (isMobileDataLowQuality || isPreferenceLowQuality) {
                nicoLiveHTML.startQuality = "super_low"
            }
            // ニコ生
            viewModelScope.launch(errorHandler + Dispatchers.Default) {
                // 情報取得
                val html = getNicoLiveHTML()
                val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(html)
                nicoLiveJSON.postValue(jsonObject)
                // 番組名取得など
                nicoLiveHTML.initNicoLiveData(jsonObject)
                programTitle = nicoLiveHTML.programTitle
                communityId = nicoLiveHTML.communityId
                thumbnailURL = nicoLiveHTML.thumb
                nicoLiveProgramData.postValue(nicoLiveHTML.getProgramData(jsonObject))
                // 履歴に追加
                launch { insertDB() }
                // WebSocketへ接続
                connectWebSocket(jsonObject)
                // getPlayerStatus叩く
                launch { getPlayerStatus() }
                // コメント人数を定期的に数える
                activeUserClear()
                // 経過時間
                setLiveTime()
            }
        } else {
            // ニコ実況。JK
            viewModelScope.launch(errorHandler + Dispatchers.Default) {
                // getflv叩く。
                val getFlvResponse = nicoJK.getFlv(liveId, userSession)
                if (!getFlvResponse.isSuccessful) {
                    // 失敗のときは落とす
                    messageLiveData.postValue("finish")
                    showToast("${getString(R.string.error)}\n${getFlvResponse.code}")
                    return@launch
                }
                // getflvパースする
                val getFlv = withContext(Dispatchers.Default) {
                    nicoJK.parseGetFlv(getFlvResponse.body?.string())!!
                }
                nicoJKGetFlv.postValue(getFlv)
                // 番組情報入れる
                programTitle = getFlv.channelName
                // 接続
                withContext(Dispatchers.Default) {
                    nicoJK.connectionCommentServer(getFlv, ::receiveCommentFun)
                }
            }
        }
        // NGデータベースを監視する
        viewModelScope.launch {
            val dao = NGDBInit.getInstance(context).ngDBDAO()
            dao.flowGetNGAll().collect { ngList ->
                // NGユーザー追加/削除を検知
                ngCommentList = ngList.filter { ngdbEntity -> ngdbEntity.type == "comment" }.map { ngdbEntity -> ngdbEntity.value }
                ngIdList = ngList.filter { ngdbEntity -> ngdbEntity.type == "user" }.map { ngdbEntity -> ngdbEntity.value }
                // 取得済みコメントからも排除
                commentList.toList().forEach { commentJSONParse ->
                    if (ngCommentList.contains(commentJSONParse.comment) || ngIdList.contains(commentJSONParse.userId)) {
                        commentList.remove(commentJSONParse)
                    }
                }
                // 一覧更新。ただUIに更新しろって送りたいだけなので適当送る
                updateRecyclerViewLiveData.postValue("update")
            }
        }
    }

    /** 座席番号と部屋の名前取得 */
    private suspend fun getPlayerStatus() = withContext(Dispatchers.IO) {
        // getPlayerStatus叩いて座席番号取得
        val getPlayerStatusResponse = nicoLiveHTML.getPlayerStatus(liveId, userSession)
        if (getPlayerStatusResponse.isSuccessful) {
            // なおステータスコード200でも中身がgetPlayerStatusのものかどうかはまだわからないので、、、
            val document =
                Jsoup.parse(getPlayerStatusResponse.body?.string())
            // 番組開始直後（開始数秒でアクセス）すると何故か視聴ページにリダイレクト（302）されるのでチェック
            val hasGetPlayerStatusTag = document.getElementsByTag("getplayerstatus ").isNotEmpty()
            // 番組が終わっててもレスポンスは200を返すのでチェック
            if (hasGetPlayerStatusTag && document.getElementsByTag("getplayerstatus ")[0].attr("status") == "ok") {
                val roomName = document.getElementsByTag("room_label")[0].text() // 部屋名
                val chairNo = document.getElementsByTag("room_seetno")[0].text() // 座席番号
                roomNameAndChairIdLiveData.postValue("$liveId - $roomName - $chairNo")
            } else {
                // getPlayerStatus取得失敗時
                snackbarLiveData.postValue(getString(R.string.error_getplayserstatus))
            }
        }
    }

    /**
     * コメント投稿関数。実況でも使う
     * コメント送信処理は[NicoLiveHTML.sendPOSTWebSocketComment]でやってる
     * @param comment コメント内容。「お大事に」とか
     * @param isUseNicocasAPI コメント投稿にニコキャスのAPIを使う場合はtrue
     * @param size コメントの大きさ。これらは省略が可能
     * @param position コメントの位置。これらは省略が可能
     * @param color コメントの色。これらは省略が可能
     * */
    suspend fun sendComment(comment: String, color: String = "white", size: String = "medium", position: String = "naka", isUseNicocasAPI: Boolean = false): Unit = withContext(Dispatchers.IO) {
        if (isJK) {
            // ニコニコ実況
            nicoJKGetFlv.value?.apply {
                nicoJK.postCommnet(comment, userId, baseTime.toLong(), threadId, userSession)
            }
        } else {
            if (comment != "\n") {
                if (!isUseNicocasAPI) {
                    // 視聴セッションWebSocketにコメントを送信する
                    nicoLiveHTML.sendPOSTWebSocketComment(comment, color, size, position)
                } else {
                    // コマンドをくっつける
                    val command = "$color $size $position"
                    // ニコキャスのAPIを叩いてコメントを投稿する
                    nicoLiveHTML.sendCommentNicocasAPI(comment, command, liveId, userSession, { showToast(getString(R.string.error)) }, { response ->
                        // 成功時
                        if (response.isSuccessful) {
                            //成功
                            snackbarLiveData.postValue(getString(R.string.comment_post_success))
                        } else {
                            showToast("${getString(R.string.error)}\n${response.code}")
                        }
                    })
                }
            }
        }
    }

    /**
     * アンケを押す
     * @param pos アンケの位置。多分一番目が0（配列みたいに）
     * ■■■■■■■■■■■
     * ■ 1 /             ■
     * ■ /               ■
     * ■  とても良かった  ■
     * ■                 ■
     * ■■ [ 98.6 ％ ] ■■   宇宙よりも遠い場所 2020/08/09 一挙アンケ
     * */
    fun enquatePOST(pos: Int) {
        val jsonObject = JSONObject().apply {
            put("type", "answerEnquete")
            put("data", JSONObject().apply {
                put("answer", pos)
            })
        }
        nicoLiveHTML.nicoLiveWebSocketClient.send(jsonObject.toString())
    }

    /** 経過時間計算 */
    private fun setLiveTime() {
        // 1秒ごとに
        viewModelScope.launch {
            while (true) {
                delay(1000)
                // 現在の時間
                val nowUnixTime = System.currentTimeMillis() / 1000L
                programTimeLiveData.postValue(calcLiveTime(nowUnixTime))
            }
        }
    }

    /** アクティブ人数を計算する。一分間間隔 */
    private fun activeUserClear() {
        // 1分でリセット
        viewModelScope.launch {
            // とりあえず一回目は10秒後計算
            delay(10000)
            calcToukei()
            while (true) {
                // あとは一分間間隔で計算する
                delay(60000)
                calcToukei()
            }
        }
    }

    /**
     * 統計情報を表示する。立ち見部屋の数が出なくなったの少し残念。人気番組の指数だったのに
     * @param showSnackBar SnackBarを表示する場合はtrue
     * */
    fun calcToukei(showSnackBar: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val calender = Calendar.getInstance()
            calender.add(Calendar.MINUTE, -1)
            val unixTime = calender.timeInMillis / 1000L
            // 今のUnixTime
            val nowUnixTime = System.currentTimeMillis() / 1000L
            // 範囲内のコメントを取得する
            val timeList = commentList.toList().filter { comment ->
                if (comment.date.toFloatOrNull() != null) {
                    comment.date.toLong() in unixTime..nowUnixTime
                } else {
                    false
                }
            }
            // 同じIDを取り除く
            val idList = timeList.distinctBy { comment -> comment.userId }
            // 数えた結果
            activeCommentPostUserLiveData.postValue("${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}")
            // SnackBarで統計を表示する場合
            if (showSnackBar) {
                // プレ垢人数
                val premiumCount = idList.count { commentJSONParse -> commentJSONParse.premium == "\uD83C\uDD7F" }
                // 生ID人数
                val userIdCount = idList.count { commentJSONParse -> !commentJSONParse.mail.contains("184") }
                // 平均コメント数
                val commentLengthAverageDouble = timeList.map { commentJSONParse -> commentJSONParse.comment.length }.average()
                val commentLengthAverage = if (!commentLengthAverageDouble.isNaN()) {
                    commentLengthAverageDouble.roundToInt()
                } else {
                    -1
                }
                // UnixTime(ms)をmm:ssのssだけを取り出すためのSimpleDataFormat。
                val simpleDateFormat = SimpleDateFormat("ss")
                // 秒間コメントを取得する。なお最大値
                val commentPerSecondMap = timeList.groupBy({ comment ->
                    // 一分間のコメント配列から秒、コメント配列のMapに変換するためのコード
                    // 例。51秒に投稿されたコメントは以下のように：51=[いいよ, がっつコラボ, ガッツ, 歓迎]
                    val programStartTime = nicoLiveHTML.programStartTime
                    val calc = comment.date.toLong() - programStartTime
                    simpleDateFormat.format(calc * 1000).toInt()
                }, { comment ->
                    comment
                }).maxByOrNull { map ->
                    // 秒Mapから一番多いのを取る。
                    map.value.size
                }
                // 数えた結果
                activeCommentPostUserLiveData.postValue("${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}")
                // 統計情報表示
                snackbarLiveData.postValue(
                    """${getString(R.string.one_minute_statistics)}
${getString(R.string.comment_per_second)}(${getString(R.string.max_value)}/${calcLiveTime(commentPerSecondMap?.value?.first()?.date?.toLong() ?: 0L)})：${commentPerSecondMap?.value?.size}
${getString(R.string.one_minute_statistics_premium)}：$premiumCount
${getString(R.string.one_minute_statistics_user_id)}：$userIdCount
${getString(R.string.one_minute_statistics_comment_length)}：$commentLengthAverage"""
                )
            }

        }
    }

    /**
     * 相対時間を計算する。25:25みたいなこと。
     * @param unixTime 基準時間。
     * */
    private fun calcLiveTime(unixTime: Long): String {
        // 経過時間 - 番組開始時間
        val calc = unixTime - nicoLiveHTML.programStartTime
        val date = Date(calc * 1000L)
        //時間はUNIX時間から計算する
        val hour = (calc / 60 / 60)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "$hour:${simpleDateFormat.format(date.time)}"
    }

    /**
     * WebSocketへ接続する関数
     * @param jsonObject [NicoLiveHTML.nicoLiveHTMLtoJSONObject]のJSONObject
     * */
    private fun connectWebSocket(jsonObject: JSONObject) {
        nicoLiveHTML.connectWebSocket(jsonObject) { command, message ->
            // WebSocketへ接続してHLSアドレス、コメント鯖の情報をもらう
            when (command) {
                "stream" -> {
                    // HLSアドレス取得
                    hlsAddress.postValue(nicoLiveHTML.getHlsAddress(message))
                    // 画質一覧と今の画質
                    currentQuality = nicoLiveHTML.getCurrentQuality(message)
                    // 選択可能な画質
                    qualityListJSONArray = nicoLiveHTML.getQualityListJSONArray(message)
                    // 二回目以降画質変更を通知する
                    if (isNotFirstQualityMessage) {
                        changeQualityLiveData.postValue(nicoLiveHTML.getQualityListJSONArray(message).toString())
                    }
                    isNotFirstQualityMessage = true
                }
                "room" -> {
                    // コメントサーバーの情報
                    if (isLoginMode) {
                        // ログイン時のみyourPostKeyが取れる
                        val commentMessageServerUri = nicoLiveHTML.getCommentServerWebSocketAddress(message)
                        val commentThreadId = nicoLiveHTML.getCommentServerThreadId(message)
                        val yourPostKey = nicoLiveHTML.getCommentYourPostKey(message)
                        val commentRoomName = getString(R.string.room_integration) // ユーザーならコミュIDだけどもう立ちみないので部屋統合で統一
                        // コメントサーバーへ接続する
                        nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, nicoLiveHTML.userId, yourPostKey, ::receiveCommentFun)
                        // 流量制限コメント鯖へ接続する
                        if (!nicoLiveHTML.isOfficial) {
                            viewModelScope.launch(Dispatchers.Default) {
                                connectionStoreCommentServer(nicoLiveHTML.userId, yourPostKey)
                            }
                        }
                    } else {
                        // 視聴モード以外は非ログインなのでyourPostKeyが取れない
                        val commentMessageServerUri = nicoLiveHTML.getCommentServerWebSocketAddress(message)
                        val commentThreadId = nicoLiveHTML.getCommentServerThreadId(message)
                        val commentRoomName = getString(R.string.room_integration) // ユーザーならコミュIDだけどもう立ちみないので部屋統合で統一
                        // コメントサーバーへ接続する
                        nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, null, null, ::receiveCommentFun)
                        // 流量制限コメント鯖へ接続する
                        if (!nicoLiveHTML.isOfficial) {
                            viewModelScope.launch(Dispatchers.Default) {
                                connectionStoreCommentServer(nicoLiveHTML.userId)
                            }
                        }
                    }
                }
                "postCommentResult" -> {
                    // コメント送信結果。
                    showCommentPOSTResultSnackBar(message)
                }
                "statistics" -> {
                    // 総来場者数、コメント数を表示させる
                    initStatisticsInfo(message)
                }
                "schedule" -> {
                    // 延長を検知
                    showSchedule(message)
                }
            }
            // containsで部分一致にしてみた。なんで部分一致なのかは私も知らん
            if (command.contains("disconnect")) {
                //番組終了
                programEnd(message)
            }
        }
    }

    /** 番組終了。なお一般追い出しの場合はWebSocketが切断される */
    private fun programEnd(message: String) {
        // 理由？
        val because = JSONObject(message).getJSONObject("data").getString("reason")
        // 原因が追い出しの場合はToast出す
        if (because == "CROWDED") {
            showToast("${getString(R.string.oidashi)}\uD83C\uDD7F") // パーキングの絵文字
        }
        // Activity終了
        if (prefSetting.getBoolean("setting_disconnect_activity_finish", false)) {
            messageLiveData.postValue("finish")
        }
    }

    /** 延長メッセージを受け取る */
    private fun showSchedule(message: String) {
        val scheduleData = nicoLiveHTML.getSchedule(message)
        //時間出す場所確保したので終了時刻書く。
        if (isNotFirstEntyouKenti) {
            // 終了時刻出す
            val time = nicoLiveHTML.getScheduleEndTime(message)
            val message = "${getString(R.string.entyou_message)}\n${getString(R.string.end_time)} $time"
            snackbarLiveData.postValue(message)
        } else {
            isNotFirstEntyouKenti = true
        }
        // 延長したら残り時間再計算する
        // 割り算！
        val calc = (scheduleData.endTime - scheduleData.beginTime) / 1000
        //時間/分
        val hour = calc / 3600
        var hourString = hour.toString()
        if (hourString.length == 1) {
            hourString = "0$hourString"
        }
        val minute = calc % 3600 / 60
        var minuteString = minute.toString()
        if (minuteString.length == 1) {
            minuteString = "0$minuteString"
        }
        formattedProgramEndTime.postValue("${hourString}:${minuteString}:00")
        // 番組終了時刻を入れる
        programEndUnixTime = scheduleData.endTime / 1000

    }

    /** 来場者数、コメント数などの統計情報を受け取る */
    private fun initStatisticsInfo(message: String) {
        statisticsLiveData.postValue(nicoLiveHTML.getStatistics(message))
    }

    /** コメントが送信できたか */
    private fun showCommentPOSTResultSnackBar(message: String) {
        viewModelScope.launch {
            val jsonObject = JSONObject(message)
            /**
             * 本当に送信できたかどうか。
             * 実は流量制限にかかってしまったのではないか（公式番組以外では流量制限コメント鯖（store鯖）に接続できるけど公式は無理）
             * 流量制限にかかると他のユーザーには見えない。ので本当に成功したか確かめる
             * */
            if (nicoLiveHTML.isOfficial) {
                val comment = jsonObject.getJSONObject("data").getJSONObject("chat").getString("content")
                delay(500)
                // 受信済みコメント配列から自分が投稿したコメント(yourpostが1)でかつ5秒前まで遡った配列を作る
                val nowTime = System.currentTimeMillis() / 1000
                val prevComment = commentList.filter { commentJSONParse ->
                    val time = nowTime - (commentJSONParse.date.toLongOrDefault(0))
                    time <= 5 && commentJSONParse.yourPost // 5秒前でなお自分が投稿したものを
                }.map { commentJSONParse -> commentJSONParse.comment }
                if (prevComment.contains(comment)) {
                    // コメント一覧に自分のコメントが有る
                    snackbarLiveData.postValue(getString(R.string.comment_post_success))
                } else {
                    // 無いので流量制限にかかった（他には見えない）
                    snackbarLiveData.postValue("${getString(R.string.comment_post_error)}\n${getString(R.string.comment_post_limit)}")
                }
            } else {
                // ユーザー番組ではコメント多いときもStore鯖に入るので検証はしない
                snackbarLiveData.postValue("${getString(R.string.comment_post_success)}\n${getString(R.string.comment_post_limit)}")
            }
        }
    }

    /** コメントを受け取る高階関数 */
    private fun receiveCommentFun(comment: String, roomName: String, isHistoryComment: Boolean) {
        val room = if (isJK) programTitle else roomName
        // JSONぱーす
        val commentJSONParse = CommentJSONParse(comment, room, liveId)
        // アンケートや運コメを表示させる。
        if (roomName != getString(R.string.room_limit)) {
            when {
                comment.contains("/vote") -> {
                    // アンケート
                    enquateLiveData.postValue(comment)
                }
                comment.contains("/disconnect") -> {
                    // disconnect受け取ったらSnackBar表示
                    snackbarLiveData.postValue(getString(R.string.program_disconnect))
                }
            }
            if (!hideInfoUnnkome) {
                //運営コメント
                if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
                    unneiCommentLiveData.postValue(comment)
                }
                //運営コメントけす
                if (commentJSONParse.comment.contains("/clear")) {
                    unneiCommentLiveData.postValue("clear")
                }
            }
        }
        // 匿名コメント落とすモード
        if (isTokumeiHide && commentJSONParse.mail.contains("184")) {
            return
        }
        // NGユーザー/コメントの場合は配列に追加しない
        when {
            ngIdList.contains(commentJSONParse.userId) -> return
            ngCommentList.contains(commentJSONParse.comment) -> return
        }
        // LiveData送信！！！
        commentReceiveLiveData.postValue(commentJSONParse)
        // コメント配列に追加
        commentList.add(0, commentJSONParse)
        // コテハン登録
        registerKotehan(commentJSONParse)
    }

    /**
     * コテハンがコメントに含まれている場合はコテハンDBに追加する関数
     * コテハンmap反映もしている。
     * */
    private fun registerKotehan(commentJSONParse: CommentJSONParse) {
        val comment = commentJSONParse.comment
        if (comment.contains("@") || comment.contains("＠")) {
            // @の位置を特定
            val index = when {
                comment.contains("@") -> comment.indexOf("@") + 1 // @を含めないように
                comment.contains("＠") -> comment.indexOf("＠") + 1 // @を含めないように
                else -> -1
            }
            if (index != -1) {
                val kotehan = comment.substring(index)
                // データベースにも入れる。コテハンデータベースの変更は自動でやってくれる
                viewModelScope.launch(Dispatchers.IO) {
                    val dao = KotehanDBInit.getInstance(context).kotehanDBDAO()
                    // すでに存在する場合・・・？
                    val kotehanData = dao.findKotehanByUserId(commentJSONParse.userId)
                    if (kotehanData != null) {
                        // 存在した
                        val kotehanDBEntity = kotehanData.copy(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        dao.update(kotehanDBEntity)
                    } else {
                        // 存在してない
                        val kotehanDBEntity = KotehanDBEntity(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000), userId = commentJSONParse.userId)
                        dao.insert(kotehanDBEntity)
                    }
                }
            }
        }
    }

    /**
     * 流量制限コメントサーバーに接続する関数。コルーチンで
     * 流量制限コメントサーバーってのはコメントが多すぎてコメントが溢れてしまう際、溢れてしまったコメントが流れてくるサーバーのことだと思います。
     * ただまぁ超がつくほどの大手じゃないとここのWebSocketに接続しても特に流れてこないと思う。
     * 公式番組では利用できない。
     * @param userId ユーザーIDが取れれば入れてね。無くてもなんか動く
     * @param yourPostKey 視聴セッションから流れてくる。けど無くても動く（yourpostが無くなるけど）
     * */
    private suspend fun connectionStoreCommentServer(userId: String? = null, yourPostKey: String? = null) = withContext(Dispatchers.Default) {
        // コメントサーバー取得API叩く
        val allRoomResponse = nicoLiveComment.getProgramInfo(liveId, userSession)
        if (!allRoomResponse.isSuccessful) {
            showToast("${getString(R.string.error)}\n${allRoomResponse.code}")
            return@withContext
        }
        // Store鯖へつなぐ
        val storeCommentServerData = nicoLiveComment.parseStoreRoomServerData(allRoomResponse.body?.string(), getString(R.string.room_limit))
        if (storeCommentServerData != null) {
            // Store鯖へ接続する。（超）大手でなければ別に接続する必要はない
            nicoLiveComment.connectionWebSocket(storeCommentServerData.webSocketUri, storeCommentServerData.threadId, storeCommentServerData.roomName, userId, yourPostKey, ::receiveCommentFun)
        }
    }

    /** 履歴DBに入れる */
    private suspend fun insertDB() = withContext(Dispatchers.IO) {
        val unixTime = System.currentTimeMillis() / 1000
        // 入れるデータ
        val nicoHistoryDBEntity = NicoHistoryDBEntity(
            type = "video",
            serviceId = liveId,
            userId = communityId,
            title = programTitle,
            unixTime = unixTime,
            description = ""
        )
        // 追加
        NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
    }

    /** ニコ生放送ページのHTML取得。コルーチンです */
    private suspend fun getNicoLiveHTML(): String? = withContext(Dispatchers.Default) {
        // ニコ生視聴ページリクエスト
        val livePageResponse = nicoLiveHTML.getNicoLiveHTML(liveId, userSession, isLoginMode)
        if (!livePageResponse.isSuccessful) {
            // 失敗のときは落とす
            messageLiveData.postValue("finish")
            showToast("${getString(R.string.error)}\n${livePageResponse.code}")
            null
        }
        if (!nicoLiveHTML.hasNiconicoID(livePageResponse)) {
            // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
            userSession = NicoLogin.reNicoLogin(context)
            // 視聴モードなら再度視聴ページリクエスト
            if (isLoginMode) {
                getNicoLiveHTML()
            }
        }
        livePageResponse.body?.string()
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

    /** 終了時 */
    override fun onCleared() {
        super.onCleared()
        nicoLiveHTML.destroy()
        nicoJK.destroy()
        nicoLiveComment.destroy()
    }
}