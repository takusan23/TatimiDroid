package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.KotehanListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoLive.Activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveQualitySelectBottomSheet
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.*
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * 生放送メニューCard。長いのでまとめた
 *
 * @param parentFragment [JCNicoLiveFragment]を指すように（ViewModelで使う）
 * */
@Composable
fun NicoLiveMenuScreen(parentFragment: Fragment) {

    /** ViewModel取得 */
    val viewModel by parentFragment.viewModels<NicoLiveViewModel>({ parentFragment })

    /** FragmentManager */
    val fragmentManager = parentFragment.childFragmentManager

    /** Context */
    val context = LocalContext.current

    /** Coroutine */
    val scope = rememberCoroutineScope()

    // Preference
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * 関数たち
     * ---------------------
     * */

    /** 画質変更BottomFragment表示 */
    fun openQualityChangeBottomFragment() {
        NicoLiveQualitySelectBottomSheet().show(parentFragment.childFragmentManager, "quality")
    }

    /** 画面回転する */
    fun rotateScreen() {
        when (parentFragment.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }

    /** 番組IDコピー */
    fun copyProgramId() {
        viewModel.nicoLiveProgramData.value?.apply {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", programId))
            //コピーしました！
            Toast.makeText(context, "${context.getString(R.string.copy_program_id)} : $programId", Toast.LENGTH_SHORT).show()
        }
    }

    /** コミュIDコピー */
    fun copyCommunityId() {
        viewModel.nicoLiveCommunityOrChannelDataLiveData.value?.apply {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", id))
            //コピーしました！
            Toast.makeText(context, "${context.getString(R.string.copy_communityid)} : $id", Toast.LENGTH_SHORT).show()
        }
    }

    /** ブラウザを起動する */
    fun openBrowser() {
        val uri = "https://live2.nicovideo.jp/watch/${viewModel.nicoLiveProgramData.value?.programId}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    /** NG一覧表示など */
    fun openNGListActivity() {
        val intent = Intent(context, NGListActivity::class.java)
        context.startActivity(intent)
    }

    /** コテハン一覧 */
    fun openKotehanListActivity() {
        val intent = Intent(context, KotehanListActivity::class.java)
        context.startActivity(intent)
    }

    /** サムネを取得してアプリ固有ストレージ内に保存する */
    suspend fun getThumb(thumbnailURL: String, communityId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url(thumbnailURL)
            get()
        }.build()
        val response = try {
            OkHttpClientSingleton.okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        val iconFolder = File(context.getExternalFilesDir(null), "icon")
        if (!iconFolder.exists()) {
            iconFolder.mkdir()
        }
        val iconFile = File(iconFolder, "$communityId.jpg")
        return@withContext try {
            iconFile.writeBytes(response?.body?.bytes()!!)
            iconFile.path
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /** ホーム画面にショートカット（ピン留め）をする */
    fun createHomeScreenShortcut() {
        // ショートカットを押したときのインテント
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra("liveId", viewModel.nicoLiveHTML.communityId)
            putExtra("watch_mode", "comment_post")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 7.1 以降のみ対応
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            // サポート済みのランチャーだった
            if (shortcutManager.isRequestPinShortcutSupported) {
                // すべての番組でホーム画面にピン留めをサポート
                scope.launch {
                    // サムネイル取得！
                    val iconBitmap = getThumb(viewModel.thumbnailURL, viewModel.communityId)
                    // 一旦Bitmapに変換したあと、Iconに変換するとうまくいく。
                    val bitmap = BitmapFactory.decodeFile(iconBitmap)
                    val icon = Icon.createWithAdaptiveBitmap(bitmap)
                    // Android 11から？setShortLabelの値ではなくBuilder()の第二引数がタイトルに使われるようになったらしい
                    val name = viewModel.nicoLiveHTML.communityName
                    val shortcut = ShortcutInfo.Builder(context, name).apply {
                        setShortLabel(name)
                        setLongLabel(name)
                        setIcon(icon)
                        setIntent(intent)
                    }.build()
                    shortcutManager.requestPinShortcut(shortcut, null)
                }
            }
        } else {
            /**
             * Android 7以下も暗黙的ブロードキャストを送信することで作成できる
             *
             * 古いスマホならワンセグ目的で使えそうだし、ワンセグ+ニコニコ実況って使い方がいいのかも
             * */
            scope.launch {
                // コミュ画像取得
                val iconBitmap = getThumb(viewModel.thumbnailURL, viewModel.communityId)
                val bitmap = BitmapFactory.decodeFile(iconBitmap)
                // ショートカット作成ブロードキャストインテント
                val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                    putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
                    putExtra(Intent.EXTRA_SHORTCUT_NAME, viewModel.nicoLiveHTML.communityName)
                    putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
                }
                // ブロードキャスト送信
                context.sendBroadcast(intent)
                Toast.makeText(context, "ショートカットを作成しました", Toast.LENGTH_SHORT).show()
            }
        }
    }


    /** 画像つき共有をする */
    fun showShereSheetMediaAttach() {
        // 親のFragment取得
        (parentFragment as? JCNicoLiveFragment)?.showShareSheetMediaAttach()
    }

    /** 共有する */
    fun showShareSheet() {
        // 親のFragment取得
        (parentFragment as? JCNicoLiveFragment)?.showShareSheet()
    }

    /** フローティングコメビュ */
    fun openFloatingCommentViewer() {
        viewModel.nicoLiveProgramData.value?.apply {
            FloatingCommentViewer.showBubbles(context, this.programId, "comment_post", this.title, this.thum)
        }
    }

    /**
     * こっからレイアウト
     * ----------------------------
     * */
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            // コルーチン
            val scope = rememberCoroutineScope()
            // タブの選択位置
            val selectIndex = remember { mutableStateOf(0) }

            // メニューのタブ部分
            NicoLiveMenuTab(
                selectedIndex = selectIndex.value,
                tabClick = { index -> selectIndex.value = index }
            )
            // メニューの本命
            when (selectIndex.value) {
                0 -> {
                    // スイッチ系設定
                    val isNotReceiveLiveLiveData = viewModel.isNotReceiveLive.observeAsState(initial = false)
                    val isHideUNEICommentLivaData = viewModel.isHideInfoUnnkome.observeAsState(initial = false)
                    val isHideTokumeiCommentLiveData = viewModel.isHideTokumei.observeAsState(initial = false)
                    val isHideEmotionLiveData = viewModel.isHideEmotion.observeAsState(initial = false)

                    NicoLiveSwitchMenu(
                        isHideUNEIComment = isHideUNEICommentLivaData.value,
                        onSwitchHideUNEIComment = { viewModel.isHideInfoUnnkome.postValue(it) },
                        isHideEmotion = isHideEmotionLiveData.value,
                        onSwitchHideEmotion = { viewModel.isHideEmotion.postValue(it) },
                        isHideTokumeiComment = isHideTokumeiCommentLiveData.value,
                        onSwitchHideTokumeiComment = { viewModel.isHideTokumei.postValue(it) },
                        isLowLatency = viewModel.nicoLiveHTML.isLowLatency,
                        onSwitchLowLatency = { viewModel.nicoLiveHTML.sendLowLatency(it) },
                        isNotReceiveLive = isNotReceiveLiveLiveData.value,
                        onSwitchNotReceiveLive = { viewModel.isNotReceiveLive.postValue(it) }
                    )
                }
                1 -> {
                    // コメントビューワー設定
                    val isHideUserId = remember { mutableStateOf(prefSetting.getBoolean("setting_id_hidden", false)) }
                    val isCommentSingleLine = remember { mutableStateOf(prefSetting.getBoolean("setting_one_line", false)) }
                    NicoLiveCommentViewerMenu(
                        isHideUserId = isHideUserId.value,
                        onSwitchHideUserId = {
                            isHideUserId.value = it
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("setting_id_hidden", it) }
                        },
                        isCommentOneLine = isCommentSingleLine.value,
                        onSwitchCommentOneLine = {
                            isCommentSingleLine.value = it
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("setting_one_line", it) }
                        }
                    )
                }
                2 -> {
                    // コメビュメニュー
                    NicoLiveButtonMenu(
                        onClickQualityChange = { openQualityChangeBottomFragment() },
                        onClickScreenRotation = { rotateScreen() },
                        onClickCopyProgramId = { copyProgramId() },
                        onClickCopyCommunityId = { copyCommunityId() },
                        onClickOpenBrowser = { openBrowser() },
                        onClickNGList = { openNGListActivity() },
                        onClickKotehanList = { openKotehanListActivity() },
                        onClickHomeScreenPin = { createHomeScreenShortcut() },
                        onClickLaunchFloatingCommentViewer = { openFloatingCommentViewer() }
                    )
                }
                3 -> {
                    // 共有メニュー
                    NicoVideoShareMenu(
                        onClickShare = { showShareSheet() },
                        onClickShareAttachImg = { showShereSheetMediaAttach() }
                    )
                }
                4 -> {
                    // 音量調整。ちなみにAndroidの音量調整ではなく動画再生ライブラリ側で音量調整している。
                    val volumeLiveData = viewModel.exoplayerVolumeLiveData.observeAsState(initial = 1f)
                    NicoVideoVolumeMenu(
                        volume = volumeLiveData.value,
                        volumeChange = { volume -> viewModel.exoplayerVolumeLiveData.postValue(volume) }
                    )
                }
                5 -> {
                    // ニコ生ゲーム用WebView。
                    val isUseNicoNamaWebView = viewModel.isUseNicoNamaWebView.observeAsState(initial = false)
                    NicoLiveNicoNamaGameCard(
                        isNicoNamaGame = isUseNicoNamaWebView.value,
                        onSwitchNicoNamaGame = { isUse -> viewModel.isUseNicoNamaWebView.postValue(isUse) }
                    )
                }
            }
        }
    }
}

