package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.isLoginMode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

/**
 * キャッシュ取得サービス。Serviceに移管した。
 * 必要なもの↓
 *       id|   String|     動画ID
 *   is_eco|  Boolean|     エコノミーならtrue
 * */
class GetCacheService : Service() {
    // 通知系
    val NOTIFICAION_ID = 816
    lateinit var notificationManager: NotificationManager

    // 設定
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // キャッシュ取得用クラス
    lateinit var nicoVideoCache: NicoVideoCache

    // 動画キャッシュ予約リスト。キャッシュ取得成功すればここの配列の中身が使われていく
    val cacheList = arrayListOf<Pair<String, Boolean>>()

    // 終了済みリスト
    val cacheGuttedList = arrayListOf<String>()

    // 一度だけ動かすのに使う（フラグ用意しても良かった感）
    lateinit var launch: Job

    // 現在取得している動画ID
    var currentCacheVideoId = ""

    // キャンセル用Broadcast
    lateinit var broadcastReceiver: BroadcastReceiver

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // 通知出す
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        showNotification()
        // ブロードキャスト初期化
        initBroadcastReceiver()
    }

    private fun initBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("cache_service_stop")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "cache_service_stop" -> {
                        // DL中に中断したらファイルを消すように
                        NicoVideoCache(this@GetCacheService).deleteCache(currentCacheVideoId)
                        // DownloadManager中断
                        nicoVideoCache.cancelDownloadManagerEnqueue()
                        // Service強制終了
                        stopSelf()
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ユーザーセッション
        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)
        userSession = if (isLoginMode(this)) {
            prefSetting.getString("user_session", "") ?: "" // ログインする
        } else {
            ""  // ログインしない
        }
        // 動画ID
        val id = intent?.getStringExtra("id")
        // エコノミーなら
        val isEco1 = intent?.getBooleanExtra("is_eco", false) ?: false
        if (id != null) {
            // すでにあるか
            if (!cacheList.contains(Pair(id, isEco1))) {
                // 予約に追加
                cacheList.add(Pair(id, isEco1))
                // 一度だけ実行
                // Serviceは多重起動できないけど起動中ならonStartCommandが呼ばれる
                if (!::launch.isInitialized) {
                    // 取得
                    coroutine()
                } else {
                    // 予定に追加したよ！
                    showToast("${getString(R.string.cache_get_list_add)}。$id\n${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheGuttedList.size}")
                }
                showNotification("${getString(R.string.loading)}：$currentCacheVideoId / ${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheGuttedList.size}")
            } else {
                showToast(getString(R.string.cache_get_list_contains))
            }
        }
        return START_NOT_STICKY
    }

    // コルーチン
    private fun coroutine(position: Int = 0) {
        launch = GlobalScope.launch {
            // キャッシュ取得クラス
            val nicoVideoHTML = NicoVideoHTML()
            nicoVideoCache = NicoVideoCache(this@GetCacheService)
            nicoVideoCache.initBroadcastReceiver()
            // ID
            val videoId = cacheList[position].first
            currentCacheVideoId = videoId
            // エコノミーか
            val isEco1 = cacheList[position].second
            val eco = if (isEco1) {
                "1"
            } else {
                "0"
            }
            // 進捗通知
            showNotification("${getString(R.string.loading)}：$currentCacheVideoId / ${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheGuttedList.size}")
            // リクエスト
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco).await()
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)} : $videoId\n${response.code}")
                return@launch
            }
            val nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val jsonObject = nicoVideoHTML.parseJSON(response?.body?.string())
            if (!nicoVideoCache.isEncryption(jsonObject.toString())) {
                // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
                var contentUrl = ""
                if (nicoVideoHTML.isDMCServer(jsonObject)) {
                    // https://api.dmc.nico/api/sessions のレスポンス
                    val sessionAPIJSONObject =
                        nicoVideoHTML.callSessionAPI(jsonObject).await()
                    if (sessionAPIJSONObject != null) {
                        // 動画URL
                        contentUrl =
                            nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                        // ハートビート処理
                        nicoVideoHTML.heartBeat(jsonObject, sessionAPIJSONObject)
                    }
                } else {
                    // Smileサーバー。動画URL取得
                    contentUrl = nicoVideoHTML.getContentURI(jsonObject, null)
                }
                // キャッシュ取得
                nicoVideoCache.getCache(videoId, jsonObject.toString(), contentUrl, userSession, nicoHistory)
                // キャッシュ取得成功ブロードキャストを受け取る
                nicoVideoCache.initBroadcastReceiver {
                    /**
                     * Android 10からｇｍみたいな仕様変更が入った。（らしい）
                     * DownloadManager経由で落としたファイルがなんか勝手に削除されるようになってしまった。
                     * 対策でファイル名を変えるといいらしい
                     * */
                    nicoVideoCache.reNameVideoFile(videoId)
                    // 取得完了したら呼ばれる。
                    nicoVideoHTML.destory()
                    nicoVideoCache.destroy()
                    // 終了済みリスト
                    cacheGuttedList.add(videoId)
                    // 次の要素へ
                    if (position + 1 < cacheList.size) {
                        coroutine(position + 1)
                    } else {
                        // 全て終了
                        showToast(getString(R.string.cache_get_list_all_complete))
                        stopSelf()
                    }
                }
            } else {
                showToast(getString(R.string.encryption_not_download))
                // 取得完了したら呼ばれる。
                nicoVideoHTML.destory()
                nicoVideoCache.destroy()
                // 次の要素へ
                if (position + 1 < cacheList.size) {
                    coroutine(position + 1)
                } else {
                    // 終了
                    showToast(getString(R.string.cache_get_list_all_complete))
                    stopSelf()
                }
            }
        }
    }

    // サービス実行中通知出す
    private fun showNotification(contentText: String = getString(R.string.loading)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val notificationChannelId = "cache_service"
            val notificationChannel =
                NotificationChannel(notificationChannelId, getString(R.string.cache_service_title), NotificationManager.IMPORTANCE_HIGH)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val programNotification =
                NotificationCompat.Builder(this, notificationChannelId).apply {
                    setContentTitle(getString(R.string.cache_get_notification_title))
                    setContentText(contentText)
                    setSmallIcon(R.drawable.ic_save_alt_black_24dp)
                    // 強制終了ボタン置いておく
                    addAction(R.drawable.ic_outline_delete_24px, getString(R.string.cache_get_service_stop), PendingIntent.getBroadcast(this@GetCacheService, 811, Intent("cache_service_stop"), PendingIntent.FLAG_UPDATE_CURRENT))
                    setStyle(NotificationCompat.InboxStyle().also { inboxStyle ->
                        // キャッシュ予約リストを表示させるなど
                        // キャッシュ取得済みは表示させない
                        cacheList.filter { pair: Pair<String, Boolean> -> !cacheGuttedList.contains(pair.first) }
                            .forEach {
                                inboxStyle.addLine(it.first)
                            }
                    })
                }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        } else {
            // 通知チャンネル
            val programNotification = NotificationCompat.Builder(this).apply {
                setContentTitle(getString(R.string.cache_get_notification_title))
                setContentText(contentText)
                setSmallIcon(R.drawable.ic_save_alt_black_24dp)
                // 強制終了ボタン置いておく
                addAction(R.drawable.ic_outline_delete_24px, getString(R.string.cache_get_service_stop), PendingIntent.getBroadcast(this@GetCacheService, 811, Intent("cache_service_stop"), PendingIntent.FLAG_UPDATE_CURRENT))
                setStyle(NotificationCompat.InboxStyle().also { inboxStyle ->
                    // キャッシュ予約リストを表示させるなど
                    // キャッシュ取得済みは表示させない
                    cacheList.filter { pair: Pair<String, Boolean> -> !cacheGuttedList.contains(pair.first) }
                        .forEach {
                            inboxStyle.addLine(it.first)
                        }
                })
            }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

}

/**
 * キャッシュ取得サービス起動関数。
 * 注意：すでに起動している場合は今のキャッシュ取得が終わり次第取得します。
 * @param context Context
 * @param videoId 動画ID
 * @param isEco エコノミーで取得する場合はtrue。省略時はfalse（通常の画質）
 * */
internal fun startCacheService(context: Context?, videoId: String, isEco: Boolean = false) {
    val intent = Intent(context, GetCacheService::class.java).apply {
        putExtra("id", videoId)
        putExtra("is_eco", isEco)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context?.startForegroundService(intent)
    } else {
        context?.startService(intent)
    }
}
