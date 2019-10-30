package io.github.takusan23.tatimidroid.Fragment

import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_enquate_layout.view.*
import kotlinx.android.synthetic.main.overlay_player_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask

class CommentFragment : Fragment() {

    lateinit var commentActivity: AppCompatActivity

    lateinit var pref_setting: SharedPreferences

    //ユーザーセッション
    var usersession = ""
    //視聴に必要なデータ受信用WebSocket
    lateinit var connectionNicoLiveWebSocket: WebSocketClient
    //放送開始時間？こっちは放送開始前まである。
    var programStartTime: Long = 0
    //放送開始時間？こっちが正しい
    var programLiveTime: Long = 0
    //コメント送信用WebSocket
    lateinit var commentPOSTWebSocketClient: WebSocketClient
    //コメント送信時に必要なpostKeyを払い出す時に必要なthreadId
    var getPostKeyThreadId = ""
    //コメント投稿時につかうticketを入れておく
    //これはコメント送信用WebSocketにthreadメッセージ（過去コメントなど指定して）送信すると帰ってくるJSONObjectに入ってる
    var commentTicket = ""
    //コメント投稿に必須なユーザーID
    var userId = ""
    //コメント投稿に必須なプレミアム会員かどうか
    var premium = 0
    //コメントの内容
    var commentValue = ""
    //コマンド（匿名とか）
    var commentCommand = "184"
    //視聴モード（コメント投稿機能付き）かどうか
    var isWatchingMode = false
    //hls
    var hls_address = ""
    //こてはん（固定ハンドルネーム　配列
    val kotehanMap = mutableMapOf<String, String>()

    //生放送を見る場合はtrue
    var watchLive = false

    //TTS使うか
    var isTTS = false
    //Toast表示
    var isToast = false

    //定期的に投稿するやつ
    //視聴続けてますよ送信用
    var timer = Timer()

    //経過時間
    var programTimer = Timer()

    //アクティブ計算
    val activeTimer = Timer()
    val activeList = arrayListOf<String>()

    //番組ID
    var liveId = ""
    //番組名
    var programTitle = ""
    //コミュニティID
    var communityID = ""

    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase
    //コメントNG配列
    val commentNGList = arrayListOf<String>()
    //ユーザーNG配列
    val userNGList = arrayListOf<String>()

    //ポップアップ再生（オーバーレイ）
    var overlay_commentcamvas: CommentCanvas? = null
    lateinit var popupView: View
    lateinit var overlay_commentTextView: TextView

    //オーバーレイ再生中かどうか。
    var isPopupPlay = false
    //オーバーレイ再生の通知ID
    val overlayNotificationID = 5678

    //バックグラウンド再生MediaPlayer
    lateinit var mediaPlayer: MediaPlayer
    lateinit var broadcastReceiver: BroadcastReceiver
    //バックグラウンド再生できてるか
    var isBackgroundPlay = false
    //バックグラウンド再生の通知ID
    val backgroundNotificationID = 1234

    //NotificationManager
    lateinit var notificationManager: NotificationManager

    //コメント非表示？
    var isCommentHidden = false

    //アンケート内容いれとく
    var enquateJSONArray = ""

    //コメントコレクション
    val commentCollectionList = arrayListOf<String>()
    val commentCollectionYomiList = arrayListOf<String>()

    //アンケートView
    lateinit var enquateView: View
    //運営コメント
    lateinit var uncomeTextView: TextView
    //下のコメント（広告貢献、ランクイン等）
    lateinit var infoTextView: TextView

    //自動次枠移動
    var isAutoNextProgram = false
    var autoNextProgramTimer = Timer()

    //ミュート用
    lateinit var audioManager: AudioManager
    var volume = 0

    //運コメ・infoコメント非表示
    var hideInfoUnnkome = false

    //いやよ
    var isTokumeiComment = true

    //共有
    lateinit var programShare: ProgramShare

    //画質変更BottomSheetFragment
    lateinit var qualitySelectBottomSheet: QualitySelectBottomSheet
    //最初の画質
    var start_quality = ""
    //モバイルデータなら最低画質の設定で一度だけ動かすように
    var mobileDataQualityCheck = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentActivity = activity as AppCompatActivity

        val darkModeSupport = DarkModeSupport(context!!)
        darkModeSupport.setActivityTheme(activity as AppCompatActivity)

        //起動時の音量を保存しておく
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        //ダークモード対応
        activity_comment_bottom_navigation_bar.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            commentActivity.supportActionBar?.setBackgroundDrawable(
                ColorDrawable(
                    darkModeSupport.getThemeColor()
                )
            )
        }

        notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //横画面はLinearLayoutの向きを変える
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横画面
            activity_comment_main_linearlayout.orientation = LinearLayout.HORIZONTAL
        } else {
            //縦画面
            activity_comment_main_linearlayout.orientation = LinearLayout.VERTICAL
        }

        //liveId = intent?.getStringExtra("liveId") ?: ""
        liveId = arguments?.getString("liveId") ?: ""

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context!!)

        //視聴モードならtrue
        isWatchingMode = pref_setting.getBoolean("setting_watching_mode", false)

        //生放送を視聴する場合はtrue
        watchLive = pref_setting.getBoolean("setting_watch_live", false)

        //とりあえずコメントViewFragmentへ
        //LiveIDを詰める
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        val commentViewFragment = CommentViewFragment()
        commentViewFragment.arguments = bundle
        val fragmentTransaction =
            commentActivity.supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.activity_comment_linearlayout, commentViewFragment)
        fragmentTransaction.commit()

        //NGデータベース読み込み
        loadNGDataBase()

        //コメント投稿モード、nicocas式コメント投稿モード以外でFAB非表示
        val watchingmode = pref_setting.getBoolean("setting_watching_mode", false)
        val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)
        if (!watchingmode && !nicocasmode) {
            fab.hide()
        }

        //運営コメント、InfoコメントのTextView初期化
        uncomeTextView = TextView(context!!)
        live_framelayout.addView(uncomeTextView)
        uncomeTextView.visibility = View.GONE
        //infoコメント
        infoTextView = TextView(context!!)
        live_framelayout.addView(infoTextView)
        infoTextView.visibility = View.GONE

        //アンケートテスト
        //testEnquate()
        //showInfoComment("！！！！！！！！！！！！！！！！！！！！！！！！！")
        //setUnneiComment("\uD83D\uDC4C\uD83D\uDC7D\uD83D\uDC4C sm34995245 037_ヨーグルトプリン【オリメ】 投稿さ 対馬  残リク数 12件 1時間59分21秒")


        //視聴しない場合は非表示
        if (!watchLive) {
            live_framelayout.visibility = View.GONE
        }

        //コメント投稿画面開く
        fab.setOnClickListener {
            //表示アニメーションに挑戦した。
            val showAnimation =
                AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_show_animation)
            //表示
            comment_activity_comment_cardview.startAnimation(showAnimation)
            comment_activity_comment_cardview.visibility = View.VISIBLE
            fab.hide()
            //コメント投稿など
            commentCardView()
            //旧式はサポート切ります！
        }

        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            //UserSession取得
            //コルーチン全くわからん！
            runBlocking {

                //番組情報取得API叩く
                //視聴モード（コメント投稿機能付き）の場合とそうでない場合分ける
                if (isWatchingMode) {
                    //視聴モード
                    //PCなどで同時視聴してると怒られる。
                    GlobalScope.async {
                        NicoLogin(commentActivity, liveId)
                    }.await()
                    usersession = pref_setting.getString("user_session", "") ?: ""
                    //ユーザーID、プレミアム会員かどうか取得
                    getplayerstatus()
                    //ニコ生の視聴に必要な情報を流してくれるWebSocketへ接続するために
                    getNicoLiveWebPage()
                } else {
                    //User_Session取得
                    GlobalScope.async {
                        NicoLogin(commentActivity, liveId)
                    }.await()
                    usersession = pref_setting.getString("user_session", "") ?: ""
                    //座席番号、部屋番号取得
                    getplayerstatus()
                    //ニコ生の視聴に必要な情報を流してくれるWebSocketへ接続するために
                    getNicoLiveWebPage()
                }

            }

            //TabLayout選択
            //標準でコメントの欄を選んでおく
            activity_comment_tab_layout.selectTab(activity_comment_tab_layout.getTabAt(1))
            activity_comment_tab_layout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {

                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.text) {
                        getString(R.string.menu) -> {
                            //メニュー
                            //コメント
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveID付きで
                            val commentMenuFragment = CommentMenuFragment()
                            commentMenuFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                commentMenuFragment
                            )
                            fragmentTransaction.commit()
                        }
                        getString(R.string.comment) -> {
                            //コメント
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveID付きで
                            val commentViewFragment = CommentViewFragment()
                            commentViewFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                commentViewFragment
                            )
                            fragmentTransaction.commit()
                        }
                        getString(R.string.room_comment) -> {
                            //部屋べつ
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveID付きで
                            val commentRoomFragment = CommentRoomFragment()
                            commentRoomFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                commentRoomFragment
                            )
                            fragmentTransaction.commit()

                        }
                        getString(R.string.gift) -> {
                            //ギフト
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveID付きで
                            val giftFragment = GiftFragment()
                            giftFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                giftFragment
                            )
                            fragmentTransaction.commit()
                        }
                        getString(R.string.nicoads) -> {
                            //広告
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveID付きで
                            val nicoAdFragment = NicoAdFragment()
                            nicoAdFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                nicoAdFragment
                            )
                            fragmentTransaction.commit()
                        }
                        getString(R.string.program_info) -> {
                            //番組情報
                            val fragmentTransaction =
                                commentActivity.supportFragmentManager.beginTransaction()
                            //LiveIDを詰める
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            //LiveIDを詰める
                            val programInfoFragment = ProgramInfoFragment()
                            programInfoFragment.arguments = bundle
                            fragmentTransaction.replace(
                                R.id.activity_comment_linearlayout,
                                programInfoFragment
                            )
                            fragmentTransaction.commit()
                        }
                    }
                }
            })
/*

            activity_comment_bottom_navigation_bar.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.comment_view_menu_comment_view -> {
                        //コメント
                        val fragmentTransaction =
                            commentActivity.supportFragmentManager.beginTransaction()
                        //LiveIDを詰める
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        //LiveID付きで
                        val commentViewFragment = CommentViewFragment()
                        commentViewFragment.arguments = bundle
                        fragmentTransaction.replace(
                            R.id.activity_comment_linearlayout,
                            commentViewFragment
                        )
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_room -> {
                        //ギフト
                        val fragmentTransaction =
                            commentActivity.supportFragmentManager.beginTransaction()
                        //LiveIDを詰める
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        //LiveID付きで
                        val commentRoomFragment = CommentRoomFragment()
                        commentRoomFragment.arguments = bundle
                        fragmentTransaction.replace(
                            R.id.activity_comment_linearlayout,
                            commentRoomFragment
                        )
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_gift -> {
                        //ギフト
                        val fragmentTransaction =
                            commentActivity.supportFragmentManager.beginTransaction()
                        //LiveIDを詰める
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        //LiveID付きで
                        val giftFragment = GiftFragment()
                        giftFragment.arguments = bundle
                        fragmentTransaction.replace(
                            R.id.activity_comment_linearlayout,
                            giftFragment
                        )
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_nicoad -> {
                        //広告
                        val fragmentTransaction =
                            commentActivity.supportFragmentManager.beginTransaction()
                        //LiveIDを詰める
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        //LiveID付きで
                        val nicoAdFragment = NicoAdFragment()
                        nicoAdFragment.arguments = bundle
                        fragmentTransaction.replace(
                            R.id.activity_comment_linearlayout,
                            nicoAdFragment
                        )
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_info -> {
                        //番組情報
                        val fragmentTransaction =
                            commentActivity.supportFragmentManager.beginTransaction()
                        //LiveIDを詰める
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        //LiveIDを詰める
                        val programInfoFragment = ProgramInfoFragment()
                        programInfoFragment.arguments = bundle
                        fragmentTransaction.replace(
                            R.id.activity_comment_linearlayout,
                            programInfoFragment
                        )
                        fragmentTransaction.commit()
                    }
                }
                true
            }
*/
        } else {
            showToast(getString(R.string.mail_pass_error))
            commentActivity.finish()
            //startActivity(Intent(context!!@CommentActivity, MainActivity::class.java))
        }

        //アクティブ人数クリアなど、追加部分はCommentViewFragmentです
        activeUserClear()


        /*
        * ブロードキャスト
        * */
        val intentFilter = IntentFilter()
        intentFilter.addAction("background_program_stop")
        intentFilter.addAction("background_program_pause")
        intentFilter.addAction("program_popup_close")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                when (p1?.action) {
                    "program_popup_close" -> {
                        if (isPopupPlay) {
                            //ポップアップ再生終了
                            notificationManager.cancel(overlayNotificationID)//削除
                            val windowManager =
                                context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            windowManager.removeView(popupView)
                            isPopupPlay = false
                        }
                    }
                    "background_program_stop" -> {
                        //停止
                        isBackgroundPlay = false
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        notificationManager.cancel(backgroundNotificationID)//削除
                    }
                    "background_program_pause" -> {
                        isBackgroundPlay = true
                        if (mediaPlayer.isPlaying) {
                            //一時停止
                            mediaPlayer.pause()
                            //通知作成
                            backgroundPlayNotification(
                                getString(R.string.background_play_play),
                                NotificationCompat.FLAG_ONGOING_EVENT
                            )
                        } else {
                            //Liveで再生
                            mediaPlayer =
                                MediaPlayer.create(commentActivity, hls_address.toUri())
                            mediaPlayer.start()
                            //通知作成
                            backgroundPlayNotification(
                                getString(R.string.background_play_pause),
                                NotificationCompat.FLAG_ONGOING_EVENT
                            )
                        }
                    }
                }
            }
        }
        commentActivity.registerReceiver(broadcastReceiver, intentFilter)

    }

    private fun testEnquate() {
        setEnquetePOSTLayout(
            "/vote start あ 怪異 パイロン バイオ マヨ E大神 乙女 クロエ 発掘",
            "start"
        )
        Timer().schedule(timerTask {
            commentActivity.runOnUiThread {
                setEnquetePOSTLayout(
                    "/vote showresult per 538 168 132 82 80",
                    "result"
                )
            }
        }, 5000)
    }

    /*
    * アクティブ人数を1分ごとにクリア
    * */
    private fun activeUserClear() {
        //1分でリセット
        activeTimer.schedule(60000, 60000) {
            commentActivity.runOnUiThread {
                //println(activeList)
                activity_comment_comment_active_text.text =
                    "${activeList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}"
                activeList.clear()
            }
        }
    }

    fun loadNGDataBase() {
        //NGデータベース
        if (!this@CommentFragment::ngListSQLiteHelper.isInitialized) {
            //データベース
            ngListSQLiteHelper = NGListSQLiteHelper(context!!)
            sqLiteDatabase = ngListSQLiteHelper.writableDatabase
            ngListSQLiteHelper.setWriteAheadLoggingEnabled(false)
        }
        setNGList("user", userNGList)
        setNGList("comment", commentNGList)
    }

    fun setNGList(name: String, list: ArrayList<String>) {
        //コメントNG読み込み
        val cursor = sqLiteDatabase.query(
            "ng_list",
            arrayOf("type", "value"),
            "type=?", arrayOf(name), null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            list.add(cursor.getString(1))
            cursor.moveToNext()
        }
        cursor.close()
    }

    //getplayerstatus
    fun getplayerstatus() {

        val liveId = arguments?.getString("liveId") ?: ""

        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus/$liveId")   //getplayerstatus、httpsでつながる？
            .header("Cookie", "user_session=$usersession")
            .get()
            .build()
        val okHttpClient = OkHttpClient()


        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                val response_string = response.body?.string()
                if (response.isSuccessful) {
                    //必要なものを取り出していく
                    //add,port,thread
                    val document = Jsoup.parse(response_string)
                    //ひつようなやつ
                    val ms = document.select("ms")
                    val user = document.select("user")
                    val room = user.select("room_label").text()
                    val seet = user.select("room_seetno").text()
                    //番組名もほしい
                    val stream = document.select("stream")
                    programTitle = stream.select("title").text()
                    //コミュニティID
                    communityID = stream.select("default_community").text()
                    //コメント投稿に必須なゆーざーID、プレミアム会員かどうか
                    userId = user.select("user_id").text()
                    premium = user.select("is_premium").text().toInt()
                    //vpos
                    programStartTime = document.select("stream").select("base_time").text().toLong()
                    programLiveTime = document.select("stream").select("start_time").text().toLong()
                    commentActivity.runOnUiThread {
                        commentActivity.supportActionBar?.subtitle = "$room - $seet"
                    }
                    //経過時間計算
                    setLiveTime()
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //経過時間計算
    private fun setLiveTime() {
        //1秒ごとに
        programTimer = Timer()
        programTimer.schedule(0, 1000) {

            val unixtime = System.currentTimeMillis() / 1000L

            val calc = unixtime - programLiveTime

            //分、秒だけCalendar使う
            //val cale = Calendar.getInstance()
            //cale.timeInMillis = calc * 1000L

            val date = Date(calc * 1000L)

            //時間はUNIX時間から計算する
            val hour = (calc / 60 / 60)

            val simpleDateFormat = SimpleDateFormat("mm:ss")


            commentActivity.runOnUiThread {
                if (activity_comment_comment_time != null) {
                    activity_comment_comment_time.text =
                        "$hour:" + simpleDateFormat.format(date.time)
                }
            }
        }

    }

    //ニコ生の視聴用の情報を流してくれるWebSocketに接続する
//コメントに投稿したり、HLSのアドレスはWebSocketから取得する必要がある。
//で、WebSocketのアドレスはHTMLを解析する必要がある！？！？！？
    fun getNicoLiveWebPage() {
        //番組ID
        val id = arguments?.getString("liveId") ?: ""
        var request: Request

        //コメビュモードの場合はユーザーセッション無いので
        if (isWatchingMode) {
            //視聴モード（ユーザーセッション付き）
            request = Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .header("Cookie", "user_session=${usersession}")
                .get()
                .build()
        } else {
            //コメビュモード（ユーザーセッションなし）
            request = Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .get()
                .build()
        }

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    //HTML解析
                    val html = Jsoup.parse(response_string)
                    //謎のJSON取得
                    //この部分長すぎてChromeだとうまくコピーできないんだけど、Edgeだと完璧にコピーできたぞ！
                    if (html.getElementById("embedded-data") != null) {
                        val json = html.getElementById("embedded-data").attr("data-props")
                        val jsonObject = JSONObject(json)
                        val site = jsonObject.getJSONObject("site")
                        val relive = site.getJSONObject("relive")
                        //WebSocketリンク
                        val websocketUrl = relive.getString("webSocketUrl")
                        //broadcastId取得
                        val program = jsonObject.getJSONObject("program")
                        //broadcastId
                        val broadcastId = program.getString("broadcastId")
                        connectionNicoLiveWebSocket(websocketUrl, broadcastId)
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //ニコ生の視聴に必要なデータを流してくれるWebSocket
//視聴セッションWebSocket
    fun connectionNicoLiveWebSocket(url: String, broadcastId: String) {
        val uri = URI(url)
        connectionNicoLiveWebSocket = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //System.out.println("ニコ生視聴セッションWebSocket接続開始")
                //最初にクライアント側からサーバーにメッセージを送る必要がある
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")
                //body
                val bodyObject = JSONObject()
                bodyObject.put("command", "getpermit")
                //requirement
                val requirementObject = JSONObject()
                requirementObject.put("broadcastId", broadcastId)
                requirementObject.put("route", "")
                //stream
                val streamObject = JSONObject()
                streamObject.put("protocol", "hls")
                streamObject.put("requireNewStream", true)
                streamObject.put("priorStreamQuality", "high")
                streamObject.put("isLowLatency", true)
                streamObject.put("isChasePlay", false)
                //room
                val roomObject = JSONObject()
                roomObject.put("isCommentable", true)
                roomObject.put("protocol", "webSocket")

                requirementObject.put("stream", streamObject)
                requirementObject.put("room", roomObject)

                bodyObject.put("requirement", requirementObject)
                jsonObject.put("body", bodyObject)

                //もう一個
                val secondObject = JSONObject()
                secondObject.put("type", "watch")
                val secondbodyObject = JSONObject()
                secondbodyObject.put("command", "playerversion")
                val paramsArray = JSONArray()
                paramsArray.put("leo")
                secondbodyObject.put("params", paramsArray)
                secondObject.put("body", secondbodyObject)

                //送信
                connectionNicoLiveWebSocket.send(secondObject.toString())
                connectionNicoLiveWebSocket.send(jsonObject.toString())

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                //System.out.println(reason)
            }

            override fun onMessage(message: String?) {

                //HLSのアドレス　と　変更可能な画質一覧取る
                if (message?.contains("currentStream") == true) {
                    val jsonObject = JSONObject(message)
                    val currentObject =
                        jsonObject.getJSONObject("body").getJSONObject("currentStream")
                    hls_address = currentObject.getString("uri")
                    //System.out.println("HLSアドレス ${hls_address}")
                    //生放送再生
                    if (pref_setting.getBoolean("setting_watch_live", false)) {
                        //モバイルデータは最低画質で読み込む設定　
                        sendMobileDataQuality()
                        setPlayVideoView()
                    } else {
                        //レイアウト消す
                        live_framelayout.visibility = View.GONE
                    }
                    //画質変更一覧
                    val qualityTypesJSONArray = currentObject.getJSONArray("qualityTypes")
                    val selectQuality = currentObject.getString("quality")
                    // 画質変更BottomFragmentに詰める
                    val bundle = Bundle()
                    bundle.putString("select_quality", selectQuality)
                    bundle.putString("quality_list", qualityTypesJSONArray.toString())
                    bundle.putString("liveId", liveId)
                    qualitySelectBottomSheet = QualitySelectBottomSheet()
                    qualitySelectBottomSheet.arguments = bundle
                    //画質変更成功？
                    if (start_quality.isEmpty()) {
                        //最初の読み込みは入れるだけ
                        start_quality = selectQuality
                    } else {
                        //画質変更した。Snackbarでユーザーに教える
                        val oldQuality = QualitySelectBottomSheet.getQualityText(
                            start_quality,
                            context!!
                        )
                        val newQuality = QualitySelectBottomSheet.getQualityText(
                            selectQuality,
                            context!!
                        )
                        Snackbar.make(
                            live_video_view,
                            "${getString(R.string.successful_quality)}\n${oldQuality}→${newQuality}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        start_quality = selectQuality
                    }
                }

                //threadId、WebSocketURL受信
                //コメント投稿時に使う。
                if (message?.contains("threadId") == true) {
                    val jsonObject = JSONObject(message)
                    val room = jsonObject.getJSONObject("body").getJSONObject("room")
                    val threadId = room.getString("threadId")
                    val messageServerUri = room.getString("messageServerUri")

                    //コメント投稿時に必要なpostKeyを取得するために使う
                    getPostKeyThreadId = threadId

                    //System.out.println("コメントWebSocket情報 ${threadId} ${messageServerUri}")

                    //コメント投稿時に使うWebSocketに接続する
                    connectionCommentPOSTWebSocket(messageServerUri, threadId)
                }


                //postKey受信
                //今回は受信してpostKeyが取得できたらコメントを送信する仕様にします。
                //postKeyをもらう イコール　コメントを送信する
                if (message?.contains("postkey") == true) {
                    val jsonObject = JSONObject(message)
                    val command = jsonObject.getJSONObject("body").getString("command")
                    //コメント送信なので２重チェック
                    if (command.contains("postkey")) {

                        //JSON配列の０番目にpostkeyが入ってる。
                        val paramsArray = jsonObject.getJSONObject("body").getJSONArray("params")
                        val postkey = paramsArray.getString(0)
                        //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                        val vpos = (System.currentTimeMillis() / 1000L) - programStartTime
                        val jsonObject = JSONObject()
                        val chatObject = JSONObject()
                        chatObject.put("thread", getPostKeyThreadId)    //視聴用セッションWebSocketからとれる
                        chatObject.put(
                            "vpos",
                            vpos
                        )     //番組情報取得で取得した値 - = System.currentTimeMillis() UNIX時間
                        chatObject.put("mail", "184")
                        chatObject.put(
                            "ticket",
                            commentTicket
                        )     //視聴用セッションWebSocketからとれるコメントが流れるWebSocketに接続して最初に必要な情報を送ったら取得できる
                        chatObject.put("content", commentValue)
                        chatObject.put("postkey", postkey)
                        //この２つ、ユーザーIDとプレミアム会員かどうか。これどうやってとる？gatPlayerStatus？　もしgetPlayerStatusなくなってもHTML解析すればとれそう？
                        chatObject.put("premium", premium)
                        chatObject.put("user_id", userId)
                        jsonObject.put("chat", chatObject)

                        //System.out.println(jsonObject.toString())

                        //送信
                        //println(jsonObject.toString())
                        commentPOSTWebSocketClient.send(jsonObject.toString())
                    }
                }

                //総来場者数、コメント数
                if (message?.contains("statistics") == true) {
                    val jsonObject = JSONObject(message)
                    val params = jsonObject.getJSONObject("body").getJSONArray("params")
                    val watchCount = params[0]
                    val commentCount = params[1]
                    commentActivity.runOnUiThread {
                        if (activity_comment_watch_count != null && activity_comment_comment_count != null) {
                            activity_comment_watch_count.text = watchCount.toString()
                            activity_comment_comment_count.text = commentCount.toString()
                        }
                    }
                }

            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
                //System.out.println("ニコ生視聴セッションWebSocket　えらー")
            }
        }

        //それと別に30秒間隔で視聴を続けてますよメッセージを送信する必要がある模様
        timer = Timer()
        timer.schedule(30000, 30000) {
            if (!connectionNicoLiveWebSocket.isClosed) {
                //ひとつめ
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")

                val bodyJSONObject = JSONObject()
                bodyJSONObject.put("command", "watching")

                val paramsArray = JSONArray()
                paramsArray.put(broadcastId)
                paramsArray.put("-1")
                paramsArray.put("0")

                bodyJSONObject.put("params", paramsArray)
                jsonObject.put("body", bodyJSONObject)

                //ふたつめ
                val secondJSONObject = JSONObject()
                secondJSONObject.put("type", "pong")
                secondJSONObject.put("body", JSONObject())

                //それぞれを３０秒ごとに送る
                //なお他の環境と同時に視聴すると片方切断される（片方の画面に同じ番組を開くとだめ的なメッセージ出る）
                connectionNicoLiveWebSocket.send(jsonObject.toString())
                connectionNicoLiveWebSocket.send(secondJSONObject.toString())
                //System.out.println(jsonObject.toString())
            }
        }
        //接続
        connectionNicoLiveWebSocket.connect()
    }

    //コメント送信用WebSocket。今の部屋に繋がってる（アリーナならアリーナ）
    fun connectionCommentPOSTWebSocket(url: String, threadId: String) {
        //過去コメントか流れてきたコメントか
        var historyComment = -1000
        //過去コメントだとtrue
        var isHistoryComment = true
        //
        val uri = URI(url)
        //これはプロトコルの設定が必要
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        commentPOSTWebSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //System.out.println("コメント送信用WebSocket接続開始")

                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", threadId)
                //jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("nicoru", 0)
                jsonObject.put("with_global", 1)
                jsonObject.put("fork", 0)
                jsonObject.put("user_id", userId)
                jsonObject.put("res_from", historyComment)
                sendJSONObject.put("thread", jsonObject)
                commentPOSTWebSocketClient.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                //コメント送信に使うticketを取得する
                if (message != null) {
                    //過去コメントかな？
                    if (historyComment < 0) {
                        historyComment++
                    } else {
                        isHistoryComment = false
                    }
                    //thread
                    if (message.contains("ticket")) {
                        val jsonObject = JSONObject(message)
                        val thread = jsonObject.getJSONObject("thread")
                        commentTicket = thread.getString("ticket")
                    }
                    //chat_result
                    //コメント送信できたか
                    if (message.contains("chat_result")) {
                        val jsonObject = JSONObject(message)
                        val status = jsonObject.getJSONObject("chat_result").getString("status")
                        commentActivity.runOnUiThread {
                            if (status.toInt() == 0) {
                                Snackbar.make(
                                    fab,
                                    getString(R.string.comment_post_success),
                                    Snackbar.LENGTH_SHORT
                                )
                                    .setAnchorView(getSnackbarAnchorView())
                                    .show()
                            } else {
                                Snackbar.make(
                                    fab,
                                    "${getString(R.string.comment_post_error)}：${status}",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .setAnchorView(getSnackbarAnchorView()).show()
                            }
                        }
                    }

                    // /voteが流れてきたとき（アンケート）
                    // だたし過去コメントには反応しないようにする
                    if (message.contains("/vote")) {
                        if (!isHistoryComment) {
                            //コメント取得
                            val jsonObject = JSONObject(message)
                            val chatObject = jsonObject.getJSONObject("chat")
                            val content = chatObject.getString("content")
                            val premium = chatObject.getInt("premium")
                            if (premium == 3) {
                                //運営コメント
                                //アンケ開始
                                if (content.contains("/vote start")) {
                                    commentActivity.runOnUiThread {
                                        setEnquetePOSTLayout(content, "start")
                                    }
                                }
                                //アンケ結果
                                if (content.contains("/vote showresult")) {
                                    commentActivity.runOnUiThread {
                                        setEnquetePOSTLayout(content, "showresult")
                                    }
                                }
                                //アンケ終了
                                if (content.contains("/vote stop")) {
                                    commentActivity.runOnUiThread {
                                        if (this@CommentFragment::enquateView.isInitialized) {
                                            live_framelayout.removeView(enquateView)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        //忘れがちな接続
        commentPOSTWebSocketClient.connect()
    }

    //視聴モード
    fun setPlayVideoView() {
        //設定で読み込むかどうか
        commentActivity.runOnUiThread {
            //ウィンドウの半分ぐらいの大きさに設定
            val display = commentActivity.windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            val layoutParams = live_video_view.layoutParams

            //横画面のときの対応
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横画面
                layoutParams.width = point.x / 2
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                live_video_view.layoutParams = layoutParams
            } else {
                //縦画面
                layoutParams.height = point.y / 3
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                live_video_view.layoutParams = layoutParams
            }

            val tree = live_video_view.viewTreeObserver
            tree.addOnGlobalLayoutListener {
                //横画面のときの対応
                val layoutParams = live_framelayout.layoutParams
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //横画面
                    layoutParams.width = live_video_view.width
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    live_framelayout.layoutParams = layoutParams

                    //コメントキャンバス
                    val commentCanvasLayout =
                        comment_canvas.layoutParams as FrameLayout.LayoutParams
                    commentCanvasLayout.width = live_video_view.width
                    commentCanvasLayout.height = live_video_view.height
                    commentCanvasLayout.gravity = Gravity.CENTER
                    comment_canvas.layoutParams = commentCanvasLayout

                } else {
                    //縦画面
                    layoutParams.height = live_video_view.height
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    live_framelayout.layoutParams = layoutParams
                }
            }
            //再生
            live_video_view.setVideoURI(hls_address.toUri())
            live_video_view.setOnPreparedListener {
                live_video_view.start()
            }
            live_video_view.setOnClickListener {
                live_video_view.start()
            }
        }
    }


    //コメント投稿用
    fun sendComment(comment: String) {
        if (comment != "\n") {
            commentValue = comment
            //コメント投稿モード（コメントWebSocketに送信）
            val watchmode = pref_setting.getBoolean("setting_watching_mode", false)
            //nicocas式コメント投稿モード
            val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)
            if (watchmode) {
                //postKeyを視聴用セッションWebSocketに払い出してもらう
                //PC版ニコ生だとコメントを投稿のたびに取得してるので
                val postKeyObject = JSONObject()
                postKeyObject.put("type", "watch")
                val bodyObject = JSONObject()
                bodyObject.put("command", "getpostkey")
                val paramsArray = JSONArray()
                paramsArray.put(getPostKeyThreadId)
                bodyObject.put("params", paramsArray)
                postKeyObject.put("body", bodyObject)
                //送信する
                //この後の処理は視聴用セッションWebSocketでpostKeyを受け取る処理に行きます。
                connectionNicoLiveWebSocket.send(postKeyObject.toString())
            } else if (nicocasmode) {
                //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                val vpos = (System.currentTimeMillis() / 1000L) - programStartTime
                val jsonObject = JSONObject()
                jsonObject.put("message", commentValue)
                jsonObject.put("command", commentCommand)
                jsonObject.put("vpos", vpos.toString())

                val requestBodyJSON = jsonObject.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                //println(jsonObject.toString())

                val request = Request.Builder()
                    .url("https://api.cas.nicovideo.jp/v1/services/live/programs/${liveId}/comments")
                    .header("Cookie", "user_session=${usersession}")
                    .post(requestBodyJSON)
                    .build()
                val okHttpClient = OkHttpClient()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        showToast("${getString(R.string.error)}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        //System.out.println(response.body?.string())
                        if (response.isSuccessful) {
                            //成功
                            val snackbar =
                                Snackbar.make(
                                    fab,
                                    getString(R.string.comment_post_success),
                                    Snackbar.LENGTH_SHORT
                                )
                            snackbar.anchorView = getSnackbarAnchorView()
                            snackbar.show()
                        } else {
                            showToast("${getString(R.string.error)}\n${response.code}")
                        }
                    }
                })
            }
        }
    }

    fun showToast(message: String) {
        commentActivity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.comment_activity_menu_watch_live -> {
                if (live_framelayout.visibility == View.VISIBLE) {
                    live_framelayout.visibility = View.GONE
                    live_video_view.stopPlayback()
                } else {
                    live_framelayout.visibility = View.VISIBLE
                    setPlayVideoView()
                }
            }
            R.id.comment_activity_menu_open_browser -> {
                val uri = "https://live2.nicovideo.jp/watch/$liveId".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.comment_activity_menu_iyayo -> {
                if (item.isChecked) {
                    item.isChecked = false
                    commentCommand = ""
                } else {
                    item.isChecked = true
                    commentCommand = "184"
                }
            }
            R.id.comment_activity_menu_tts -> {
                if (item.isChecked) {
                    item.isChecked = false
                    isTTS = false
                } else {
                    item.isChecked = true
                    isTTS = true
                }
            }
            R.id.comment_activity_menu_toast -> {
                if (item.isChecked) {
                    item.isChecked = false
                    isToast = false
                } else {
                    item.isChecked = true
                    isToast = true
                }
            }
            R.id.comment_activity_menu_ng_list -> {
                val intent = Intent(context, NGListActivity::class.java)
                startActivity(intent)
            }

            R.id.comment_activity_menu_overlay -> {
                //ポップアップ再生。コメント付き
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(context)) {
                        //RuntimePermissionに対応させる
                        // 権限取得
                        val intent =
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context?.packageName}")
                            )
                        this@CommentFragment.startActivityForResult(intent, 114)
                    } else {
                        startOverlayPlayer()
                    }
                } else {
                    //ろりぽっぷ
                    startOverlayPlayer()
                }

            }
            R.id.comment_activity_menu_background -> {
                //バックグラウンド再生
                setBackgroundProgramPlay()
            }
            R.id.comment_activity_menu_comment_hidden -> {
                isCommentHidden = item.isChecked
            }
            R.id.comment_activity_menu_auto_next_program -> {
                //自動次枠移動
                if (item.isChecked) {
                    item.isChecked = false
                    isAutoNextProgram = false
                } else {
                    item.isChecked = true
                    isAutoNextProgram = true
                }
            }
            R.id.comment_activity_menu_landscape_portrait -> {
                //縦画面、横画面変更
                setLandscapePortrait()
            }
            R.id.comment_activity_menu_copy_programid -> {
                //番組IDコピー
                copyProgramId()
            }
            R.id.comment_activity_menu_mute -> {
                //ミュート
                if (item.isChecked) {
                    item.isChecked = false
                    setSound(volume)
                } else {
                    item.isChecked = true
                    setSound(0)
                }
            }
            R.id.comment_activity_menu_volume_control -> {
                //音量調節UIを出す。
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            R.id.comment_activity_menu_hide_info_unkome -> {
                //運営コメント・infoコメント非表示
                if (item.isChecked) {
                    item.isChecked = false
                    hideInfoUnnkome = false
                } else {
                    item.isChecked = true
                    hideInfoUnnkome = true
                }
            }
            R.id.comment_activity_menu_share_attach_image -> {
                //共有
                programShare =
                    ProgramShare(commentActivity, live_video_view, programTitle, programTitle)
                programShare.shareAttacgImage()
            }
            R.id.comment_activity_menu_share -> {
                programShare =
                    ProgramShare(commentActivity, live_video_view, programTitle, programTitle)
                programShare.showShareScreen()
            }
            R.id.comment_activity_menu_quality -> {
                //画質変更
                if (this@CommentFragment::qualitySelectBottomSheet.isInitialized) {
                    qualitySelectBottomSheet.show(
                        commentActivity.supportFragmentManager,
                        "quality_bottom"
                    )
                }
            }
            R.id.comment_activity_menu_floating_commentviewer -> {
                //Bubblesで表示。
                showBubbles()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showBubbles() {
        //Android Q以降で利用可能
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Activity終了
            commentActivity.finishAndRemoveTask()
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("liveid", liveId)
            val bubbleIntent = PendingIntent.getActivity(context, 0, intent, 0)
            //通知作成？
            val bubbleData = Notification.BubbleMetadata.Builder()
                .setDesiredHeight(600)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_library_books_24px))
                .setIntent(bubbleIntent)
                .build()
            val timelineBot = Person.Builder()
                .setBot(true)
                .setName(getString(R.string.floating_comment_viewer))
                .setImportant(true)
                .build()

            //通知送信
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //通知チャンネル作成
            val notificationId = "floating_comment_viewer"
            if (notificationManager.getNotificationChannel(notificationId) == null) {
                //作成
                val notificationChannel = NotificationChannel(
                    notificationId, getString(R.string.floating_comment_viewer),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }
            //通知作成
            val notification = Notification.Builder(context, notificationId)
                .setContentText(getString(R.string.floating_comment_viewer_description))
                .setContentTitle(getString(R.string.floating_comment_viewer))
                .setSmallIcon(R.drawable.ic_library_books_24px)
                .setBubbleMetadata(bubbleData)
                .addPerson(timelineBot)
                .build()
            //送信
            notificationManager.notify(5, notification)
        } else {
            //Android Pieなので..
            Toast.makeText(
                context,
                getString(R.string.floating_comment_viewer_version),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun setLandscapePortrait() {
        val conf = resources.configuration
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun copyProgramId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
        //コピーしました！
        Toast.makeText(
            context,
            "${getString(R.string.copy_program_id)} : $liveId",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    fun copyCommunityId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", communityID))
        //コピーしました！
        Toast.makeText(
            context,
            "${getString(R.string.copy_communityid)} : $communityID",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    fun setSound(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println(requestCode)
        when (requestCode) {
            114 -> {
                if (resultCode == PackageManager.PERMISSION_GRANTED) {
                    //権限ゲット！YATTA!
                    //ポップアップ再生
                    startOverlayPlayer()
                    Toast.makeText(context, "権限を取得しました。", Toast.LENGTH_SHORT).show()
                } else {
                    //何もできない。
                    Toast.makeText(context, "権限取得に失敗しました。", Toast.LENGTH_SHORT).show()
                }
            }
            ProgramShare.requestCode -> {
                //画像共有
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        val uri: Uri = data.data!!
                        //保存＆共有画面表示
                        programShare.saveActivityResult(uri)
                    }
                }
            }
        }
    }

    fun destroyCode() {
        if (this@CommentFragment::commentPOSTWebSocketClient.isInitialized) {
            connectionNicoLiveWebSocket.close()
            commentPOSTWebSocketClient.close()
        }
        timer.cancel()
        programTimer.cancel()
        activeTimer.cancel()
        //バックグラウンド再生止める
        notificationManager.cancel(backgroundNotificationID)
        if (isBackgroundPlay) {
            if (this@CommentFragment::mediaPlayer.isInitialized) {
                //MediaPlayer初期化済みなら止める
                mediaPlayer.stop()
                mediaPlayer.release()
                isBackgroundPlay = false
            }
        }
        //ポップアップ再生とめる
        if (this@CommentFragment::popupView.isInitialized) {
            if (isPopupPlay) {
                notificationManager.cancel(overlayNotificationID)
                val windowManager =
                    context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(popupView)
                isPopupPlay = false
            }
        }
        if (this@CommentFragment::broadcastReceiver.isInitialized) {
            //中野ブロードキャスト終了
            commentActivity.unregisterReceiver(broadcastReceiver)
        }
        autoNextProgramTimer.cancel()
    }

    //Activity終了時に閉じる
    override fun onDestroy() {
        super.onDestroy()
        destroyCode()
    }

    /*オーバーレイ*/
    fun startOverlayPlayer() {
        val width = 400
        val height = 200
        //レイアウト読み込み
        val layoutInflater = LayoutInflater.from(context)
        // オーバーレイViewの設定をする
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        }
        popupView = layoutInflater.inflate(R.layout.overlay_player_layout, null)


        //表示
        commentActivity.windowManager.addView(popupView, params)
        isPopupPlay = true
        popupView.overlay_commentCanvas.isFloatingView = true

        //通知表示
        showPopUpPlayerNotification()

        //VideoView再生。
        popupView.overlay_videoview.setVideoURI(hls_address.toUri())
        //再生
        popupView.overlay_videoview.start()
        //あと再生できたらサイズ調整
        popupView.overlay_videoview.setOnPreparedListener {
            //高さ、幅取得
            params.width = it.videoWidth
            params.height = it.videoHeight
            commentActivity.windowManager.updateViewLayout(popupView, params)
        }

        //閉じる
        popupView.overlay_close_button.setOnClickListener {
            isPopupPlay = false
            commentActivity.windowManager.removeView(popupView)
            notificationManager.cancel(overlayNotificationID)
        }
        //画面サイズ
        val displaySize: Point by lazy {
            val display = commentActivity.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            size
        }

        //コメント流し
        overlay_commentcamvas = popupView.findViewById(R.id.overlay_commentCanvas)

        //移動
        //https://qiita.com/farman0629/items/ce547821dd2e16e4399e
        popupView.setOnLongClickListener {
            val windowManager =
                context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            //長押し判定
            popupView.setOnTouchListener { view, motionEvent ->
                // タップした位置を取得する
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()

                when (motionEvent.action) {
                    // Viewを移動させてるときに呼ばれる
                    MotionEvent.ACTION_MOVE -> {
                        // 中心からの座標を計算する
                        val centerX = x - (displaySize.x / 2)
                        val centerY = y - (displaySize.y / 2)

                        // オーバーレイ表示領域の座標を移動させる
                        params.x = centerX
                        params.y = centerY

                        // 移動した分を更新する
                        windowManager.updateViewLayout(view, params)
                    }
                }
                false
            }
            true//OnclickListener呼ばないようにtrue
        }


        //ボタン表示
        popupView.setOnClickListener {
            if (popupView.overlay_button_layout.visibility == View.GONE) {
                //表示
                popupView.overlay_button_layout.visibility = View.VISIBLE
            } else {
                //非表示
                popupView.overlay_button_layout.visibility = View.GONE
            }
        }

    }

    /*ポップアップ再生通知*/
    fun showPopUpPlayerNotification() {

        val stopPopupIntent = Intent("program_popup_close")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_popup"
            val notificationChannel = NotificationChannel(
                notificationChannelId, getString(R.string.popup_notification_title),
                NotificationManager.IMPORTANCE_HIGH
            )

            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val programNotification = NotificationCompat.Builder(context!!, notificationChannelId)
                .setContentTitle(getString(R.string.popup_notification_description))
                .setContentText(programTitle)
                .setSmallIcon(R.drawable.ic_popup_icon)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        getString(R.string.finish),
                        PendingIntent.getBroadcast(
                            context,
                            24,
                            stopPopupIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .build()

            //消せないようにする
            programNotification.flags = NotificationCompat.FLAG_ONGOING_EVENT

            notificationManager.notify(overlayNotificationID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.notification_background_play))
                .setContentText(programTitle)
                .setSmallIcon(R.drawable.ic_popup_icon)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        getString(R.string.finish),
                        PendingIntent.getBroadcast(
                            context,
                            24,
                            stopPopupIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                ).build()

            //消せないようにする
            programNotification.flags = NotificationCompat.FLAG_ONGOING_EVENT

            notificationManager.notify(overlayNotificationID, programNotification)
        }
    }


    /*バックグラウンド再生*/
    fun setBackgroundProgramPlay() {
        mediaPlayer = MediaPlayer.create(context, hls_address.toUri())
        mediaPlayer.start()
        isBackgroundPlay = true
        //Nougatと分岐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_background"
            val notificationChannel = NotificationChannel(
                notificationChannelId, getString(R.string.notification_background_play),
                NotificationManager.IMPORTANCE_HIGH
            )

            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            //通知作成
            backgroundPlayNotification(
                getString(R.string.background_play_pause),
                NotificationCompat.FLAG_ONGOING_EVENT
            )
        } else {
            //通知作成
            backgroundPlayNotification(
                getString(R.string.background_play_pause),
                NotificationCompat.FLAG_ONGOING_EVENT
            )
        }
    }

    fun backgroundPlayNotification(pausePlayString: String, flag: Int) {
        //音楽コントロールブロードキャスト
        val stopIntent = Intent("background_program_stop")
        val pauseIntent = Intent("background_program_pause")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_background"
            val programNotification = NotificationCompat.Builder(context!!, notificationChannelId)
                .setContentTitle(getString(R.string.notification_background_play))
                .setContentText(programTitle)
                .setSmallIcon(R.drawable.ic_background_icon)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        pausePlayString,
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            pauseIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        getString(R.string.background_play_finish),
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            stopIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                ).build()
            //消せないようにする
            programNotification.flags = flag

            notificationManager.notify(backgroundNotificationID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(context!!)
                .setContentTitle(getString(R.string.notification_background_play))
                .setContentText(programTitle)
                .setSmallIcon(R.drawable.ic_background_icon)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        pausePlayString,
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            pauseIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        getString(R.string.background_play_finish),
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            stopIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                ).build()

            //消せないようにする
            programNotification.flags = flag

            notificationManager.notify(backgroundNotificationID, programNotification)
        }
    }

    //Activity復帰した時に呼ばれる
    override fun onStart() {
        super.onStart()
        //アプリ戻ってきたらバックグラウンド再生、ポップアップ再生を止める。
        //バックグラウンド再生
        val windowManager =
            context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (this@CommentFragment::mediaPlayer.isInitialized) {
            if (isBackgroundPlay) {
                mediaPlayer.release()   //リソース開放
                notificationManager.cancel(backgroundNotificationID) //通知削除
                Toast.makeText(
                    context!!,
                    getString(R.string.lunch_app_close_background),
                    Toast.LENGTH_SHORT
                )
                    .show()
                isBackgroundPlay = false
            }
        }
        //ポップアップ再生止める
        if (this@CommentFragment::popupView.isInitialized) {
            if (isPopupPlay) {
                isPopupPlay = false
                windowManager.removeView(popupView)
                notificationManager.cancel(overlayNotificationID) //通知削除
                Toast.makeText(
                    context,
                    getString(R.string.lunch_app_close_popup),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //再生部分を作り直す
        if (hls_address.isNotEmpty()) {
            setPlayVideoView()
        }
    }


/*
    //ホームボタンおした
    override fun onUserLeaveHint() {
        //別アプリを開いた時の処理
        if (pref_setting.getBoolean("setting_leave_background", false)) {
            //バックグラウンド再生
            setBackgroundProgramPlay()
        }
        if (pref_setting.getBoolean("setting_leave_popup", false)) {
            //ポップアップ再生
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    //RuntimePermissionに対応させる
                    // 権限取得
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context?.packageName}")
                        )
                    this@CommentFragment.startActivityForResult(intent, 114)
                } else {
                    startOverlayPlayer()
                }
            } else {
                //ろりぽっぷ
                startOverlayPlayer()
            }
        }
    }
*/

    fun setEnquetePOSTLayout(message: String, type: String) {
        enquateView = layoutInflater.inflate(R.layout.bottom_fragment_enquate_layout, null, false)
        if (type.contains("start")) {
            //アンケ開始
            live_framelayout.addView(enquateView)
            val jsonArray = JSONArray(enquateStartMessageToJSONArray(message))
            //println(enquateStartMessageToJSONArray(message))
            //アンケ内容保存
            enquateJSONArray = jsonArray.toString()

            //０個目はタイトル
            val title = jsonArray[0]
            enquateView.enquate_title.text = title.toString()

            //１個めから質問
            for (i in 0 until jsonArray.length()) {
                //println(i)
                val button = MaterialButton(context!!)
                button.text = jsonArray.getString(i)
                button.setOnClickListener {
                    //投票
                    //enquatePOST(i - 1)
                    //アンケ画面消す
                    live_framelayout.removeView(enquateView)
                    //Snackbar
                    Snackbar.make(
                        activity_comment_linearlayout,
                        getString(R.string.enquate) + " : " + jsonArray[i].toString(),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 1..3) {
                    enquateView.enquate_linearlayout_1.addView(button)
                }
                //4～6は一段目
                if (i in 4..6) {
                    enquateView.enquate_linearlayout_2.addView(button)
                }
                //7～9は一段目
                if (i in 7..9) {
                    enquateView.enquate_linearlayout_3.addView(button)
                }
            }
        } else {
            //println(enquateJSONArray)
            //アンケ結果
            live_framelayout.removeView(enquateView)

            live_framelayout.addView(enquateView)
            val jsonArray = JSONArray(enquateResultMessageToJSONArray(message))
            val questionJsonArray = JSONArray(enquateJSONArray)
            //０個目はタイトル
            val title = questionJsonArray.getString(0)
            enquateView.enquate_title.text = title
            //共有で使う文字
            var shareText = ""
            //結果は０個めから
            for (i in 0 until jsonArray.length()) {
                val result = jsonArray.getString(i)
                val question = questionJsonArray.getString(i + 1)
                val text = question + "\n" + enquatePerText(result)
                val button = MaterialButton(context!!)
                button.text = text
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 0..2) {
                    enquateView.enquate_linearlayout_1.addView(button)
                }
                //4～6は一段目
                if (i in 3..5) {
                    enquateView.enquate_linearlayout_2.addView(button)
                }
                //7～9は一段目
                if (i in 6..8) {
                    enquateView.enquate_linearlayout_3.addView(button)
                }
                //共有の文字
                shareText += "$question : ${enquatePerText(result)}\n"
            }
            //アンケ結果を共有
            Snackbar.make(
                activity_comment_linearlayout,
                getString(R.string.enquate_result),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.share)) {
                //共有する
                share(shareText, "$title($programTitle-$liveId)")
            }.show()
        }
    }

    fun enquateStartMessageToJSONArray(message: String): String {
        //無理やりJSON配列にする
        var comment = message
        comment = comment.replace("/vote start ", "[")
        comment += "]"
        comment = comment.replace("\\s".toRegex(), ",")  //正規表現でスペースを,にする
        return comment
    }

    fun enquateResultMessageToJSONArray(message: String): String {
        //無理やりJSON配列にする
        var comment = message
        comment = comment.replace("/vote showresult per ", "[")
        comment += "]"
        comment = comment.replace("\\s".toRegex(), ",")  //正規表現でスペースを,にする
        return comment
    }

    //アンケートの結果を％表示
    fun enquatePerText(per: String): StringBuilder {
        val result = StringBuilder(per).insert(per.length - 1, ".").append("%")
        return result
    }

    //アンケートへ応答。0が１番目？
    fun enquatePOST(pos: Int) {
        val jsonArray = JSONArray()
        jsonArray.put(pos)
        val bodyObject = JSONObject()
        bodyObject.put("command", "answerenquete")
        bodyObject.put("params", jsonArray)
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        jsonObject.put("body", bodyObject)
        connectionNicoLiveWebSocket.send(jsonObject.toString())
    }

    fun share(shareText: String, shareTitle: String) {
        val builder = ShareCompat.IntentBuilder.from(commentActivity)
        builder.setChooserTitle(shareTitle)
        builder.setSubject(shareTitle)
        builder.setText(shareText)
        builder.setType("text/plain")
        builder.startChooser()
    }

    //新しいコメント投稿画面
    fun commentCardView() {
        //投稿ボタンを押したら投稿
        comment_cardview_comment_send_button.setOnClickListener {
            val comment = comment_cardview_comment_textinputlayout.text.toString()
            sendComment(comment)
            comment_cardview_comment_textinputlayout.setText("")
        }
        //Enterキーを押したら投稿する
        comment_cardview_comment_textinputlayout.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER) {
                val text = comment_cardview_comment_textinputlayout.text.toString()
                if (text.isNotEmpty()) {
                    //コメント投稿
                    sendComment(text)
                    comment_cardview_comment_textinputlayout.setText("")
                }
            }
            false
        }
        //閉じるボタン
        comment_cardview_close_button.setOnClickListener {
            //非表示アニメーションに挑戦した。
            val hideAnimation =
                AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_hide_animation)
            //表示
            comment_activity_comment_cardview.startAnimation(hideAnimation)
            comment_activity_comment_cardview.visibility = View.GONE
            fab.show()
        }

        if (pref_setting.getBoolean("setting_comment_collection_useage", false)) {
            //コメント投稿リスト読み込み
            loadCommentPOSTList()
            //コメントコレクション補充機能
            if (pref_setting.getBoolean("setting_comment_collection_assist", false)) {
                comment_cardview_comment_textinputlayout.addTextChangedListener(object :
                    TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        comment_cardview_chipgroup.removeAllViews()
                        //コメントコレクション読み込み
                        if (p0?.length ?: 0 >= 1) {
                            commentCollectionYomiList.forEach {
                                //文字列完全一致
                                if (it.equals(p0.toString())) {
                                    val yomi = it
                                    val pos = commentCollectionYomiList.indexOf(it)
                                    val comment = commentCollectionList[pos]
                                    //Chip
                                    val chip = Chip(context)
                                    chip.text = comment
                                    //押したとき
                                    chip.setOnClickListener {
                                        //置き換える
                                        var text = p0.toString()
                                        text = text.replace(yomi, comment)
                                        comment_cardview_comment_textinputlayout.setText(text)
                                        //カーソル移動
                                        comment_cardview_comment_textinputlayout.setSelection(text.length)
                                        //消す
                                        comment_cardview_chipgroup.removeAllViews()
                                    }
                                    comment_cardview_chipgroup.addView(chip)
                                }
                            }
                        }
                    }
                })
            }
        } else {
            comment_cardview_comment_list_button.visibility = View.GONE
        }
    }


    fun loadCommentPOSTList() {
        //データベース
        val commentCollection = CommentCollectionSQLiteHelper(context!!)
        val sqLiteDatabase = commentCollection.writableDatabase
        commentCollection.setWriteAheadLoggingEnabled(false)
        val cursor = sqLiteDatabase.query(
            "comment_collection_db",
            arrayOf("comment", "yomi", "description"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        //ポップアップメニュー
        val popup = PopupMenu(context!!, comment_cardview_comment_list_button)
        for (i in 0 until cursor.count) {
            //コメント
            val comment = cursor.getString(0)
            val yomi = cursor.getString(1)
            //メニュー追加
            popup.menu.add(comment)
            //追加
            commentCollectionList.add(comment)
            commentCollectionYomiList.add(yomi)
            cursor.moveToNext()
        }
        //閉じる
        cursor.close()
        //ポップアップメニュー押したとき
        popup.setOnMenuItemClickListener {
            comment_cardview_comment_textinputlayout.text?.append(it.title)
            true
        }
        //表示
        comment_cardview_comment_list_button.setOnClickListener {
            popup.show()
        }
    }

/*
    override fun onBackPressed() {
        AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.back_dialog))
            .setMessage(getString(R.string.back_dialog_description))
            .setPositiveButton(getString(R.string.end)) { dialogInterface: DialogInterface, i: Int ->
                finish()
                super.onBackPressed()
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            }
            .show()
    }
*/

    fun getSnackbarAnchorView(): View? {
        if (fab.isShown) {
            return fab
        } else {
            return comment_activity_comment_cardview
        }
    }

    //運営コメント
    fun setUnneiComment(comment: String) {
        //UIスレッドで呼ばないと動画止まる？
        commentActivity.runOnUiThread {
            //テキスト、背景色
            uncomeTextView.visibility = View.VISIBLE
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uncomeTextView.text = Html.fromHtml(comment, Html.FROM_HTML_MODE_COMPACT)
        } else {
            uncomeTextView.text = Html.fromHtml(comment)
        }
*/
            uncomeTextView.text = HtmlCompat.fromHtml(comment, HtmlCompat.FROM_HTML_MODE_COMPACT)
            uncomeTextView.textSize = 20F
            uncomeTextView.setTextColor(Color.WHITE)
            uncomeTextView.background = ColorDrawable(Color.parseColor("#80000000"))
            //追加
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.TOP
            uncomeTextView.layoutParams = layoutParams
            uncomeTextView.gravity = Gravity.CENTER
            //表示アニメーション
            val showAnimation =
                AnimationUtils.loadAnimation(context!!, R.anim.unnei_comment_show_animation)
            //表示
            uncomeTextView.startAnimation(showAnimation)
            Timer().schedule(timerTask {
                removeUnneiComment()
            }, 5000)
        }
    }

    //運営コメント消す
    fun removeUnneiComment() {
        commentActivity.runOnUiThread {
            if (this@CommentFragment::uncomeTextView.isInitialized) {
                //表示アニメーション
                val hideAnimation =
                    AnimationUtils.loadAnimation(context!!, R.anim.unnei_comment_close_animation)
                //表示
                uncomeTextView.startAnimation(hideAnimation)
                //初期化済みなら
                uncomeTextView.visibility = View.GONE
            }
        }
    }

    //infoコメント
    fun showInfoComment(comment: String) {
        //テキスト、背景色
        infoTextView.text = comment
        infoTextView.textSize = 20F
        infoTextView.setTextColor(Color.WHITE)
        //infoTextView.background = ColorDrawable(Color.parseColor("#80000000"))
        //追加
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.BOTTOM
        infoTextView.layoutParams = layoutParams
        infoTextView.gravity = Gravity.CENTER
        //表示アニメーション
        val showAnimation =
            AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_show_animation)
        //表示
        infoTextView.startAnimation(showAnimation)
        infoTextView.visibility = View.VISIBLE
        //５秒後ぐらいで消す？
        Timer().schedule(timerTask {
            removeInfoComment()
        }, 5000)
    }

    //Infoコメント消す
    fun removeInfoComment() {
        commentActivity.runOnUiThread {
            //非表示アニメーション
            val hideAnimation =
                AnimationUtils.loadAnimation(
                    context,
                    R.anim.comment_cardview_hide_animation
                )
            infoTextView.startAnimation(hideAnimation)
            infoTextView.visibility = View.GONE
        }
    }


    //次枠移動機能
    fun checkNextProgram() {
        //getplayerstatusは番組ID以外にも放送開始していればコミュIDを入力すれば利用可能
        //１分ぐらいで取りに行く
        autoNextProgramTimer.schedule(0, 60000) {
            val request = Request.Builder()
                .url("https://live.nicovideo.jp/api/getplayerstatus/$communityID")   //getplayerstatus、httpsでつながる？
                .header("Cookie", "user_session=$usersession")
                .get()
                .build()
            val okHttpClient = OkHttpClient()


            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast("${getString(R.string.error)}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val response_string = response.body?.string()
                    if (response.isSuccessful) {
                        //HTMLパース
                        val document = Jsoup.parse(response_string)
                        //ひつようなやつ
                        val stream = document.select("stream")
                        val liveId = stream.select("id")
                        //成功
                        //Activity再起動
                        commentActivity.finish()
                        val intent = Intent(context, CommentActivity::class.java)
                        intent.putExtra("liveId", liveId)
                        startActivity(intent)
                    } else {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            })
        }
        //１時間経ったら止める
        Timer().schedule(timerTask {
            this.cancel()
            autoNextProgramTimer.cancel()
        }, 3600000)
    }

    /**
     * 画質変更メッセージ送信
     * */
    fun sendQualityMessage(quality: String) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        //body
        val bodyObject = JSONObject()
        bodyObject.put("command", "getstream")
        //requirement
        val requirementObjects = JSONObject()
        requirementObjects.put("protocol", "hls")
        requirementObjects.put("quality", quality)
        requirementObjects.put("isLowLatency", true)
        requirementObjects.put("isChasePlay", false)
        bodyObject.put("requirement", requirementObjects)
        jsonObject.put("body", bodyObject)
        //送信
        connectionNicoLiveWebSocket.send(jsonObject.toString())
    }

    fun sendMobileDataQuality() {
        if (!mobileDataQualityCheck) {
            //モバイルデータのときは最低画質で再生する設定
            if (pref_setting.getBoolean("setting_mobiledata_quality_low", false)) {
                //今の接続状態を取得
                val connectivityManager =
                    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //ろりぽっぷとましゅまろ以上で分岐
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasTransport(
                            NetworkCapabilities.TRANSPORT_CELLULAR
                        ) == true
                    ) {
                        //モバイルデータ通信なら画質変更メッセージ送信
                        sendQualityMessage("super_low")

                    }
                } else {
                    if (connectivityManager.activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                        //モバイルデータ通信なら画質変更メッセージ送信
                        sendQualityMessage("super_low")
                    }
                }
            }
        }
        mobileDataQualityCheck = true
    }


}