package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.AmbientContext
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.KotehanListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoQualityBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoSkipCustomizeBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startCacheService
import kotlinx.coroutines.launch


/**
 * メニュー画面のUI。長いからまとめた。
 *
 * なんかすごいごちゃごちゃしてしまった。
 *
 * @param parentFragment [JCNicoVideoFragment]を指すように（ViewModelで使う）
 * */
@Composable
fun NicoVideoMenuScreen(parentFragment: Fragment) {

    /** ViewModel取得 */
    val viewModel by parentFragment.viewModels<NicoVideoViewModel>({ parentFragment })

    /** FragmentManager */
    val fragmentManager = parentFragment.childFragmentManager

    /** Context */
    val context = AmbientContext.current

    /** マイリスト追加BottomFragmentを表示する */
    fun showAddMylistBottomFragment() {
        val addMylistBottomFragment = NicoVideoAddMylistBottomFragment()
        val bundle = Bundle()
        bundle.putString("id", viewModel.playingVideoId.value)
        addMylistBottomFragment.arguments = bundle
        addMylistBottomFragment.show(fragmentManager, "mylist")
    }

    /** 画質変更BottomFragmentを表示する */
    fun showQualityBottomSheet() {
        // キャッシュ利用時は使わない
        if (viewModel.isOfflinePlay.value == false) {
            val nicoVideoQualityBottomFragment = NicoVideoQualityBottomFragment()
            nicoVideoQualityBottomFragment.show(fragmentManager, "quality")
        }
    }

    /** 動画IDをコピーする */
    fun copyVideoId() {
        val videoId = viewModel.playingVideoId.value
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", videoId))
        Toast.makeText(context, "${context.getString(R.string.video_id_copy_ok)}：${videoId}", Toast.LENGTH_SHORT).show()
    }

    /** 強制的に画面回転をする関数 */
    fun setScreenRotation() {
        val conf = context.resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }

    /** スキップ秒数変更BottomFragmentを表示する */
    fun showSkipSettingBottomFragment() {
        val skipCustomizeBottomFragment = NicoVideoSkipCustomizeBottomFragment()
        skipCustomizeBottomFragment.show(fragmentManager, "skip")
    }

    /** ブラウザを開く関数 */
    fun openBrowser(addr: String) {
        val intent = Intent(Intent.ACTION_VIEW, addr.toUri())
        context.startActivity(intent)
    }

    /** ブラウザで動画を開く関数 */
    fun openWatchPage() {
        val videoId = viewModel.playingVideoId.value
        openBrowser("https://nico.ms/$videoId")
    }

    /** NG一覧を開く */
    fun launchNGListActivity() {
        val intent = Intent(context, NGListActivity::class.java)
        context.startActivity(intent)
    }

    /** コテハン一覧を開く */
    fun launchKotehanListActivity() {
        // コテハン一覧
        val intent = Intent(context, KotehanListActivity::class.java)
        context.startActivity(intent)
    }

    /** 画像つき共有をする */
    fun showShereSheetMediaAttach() {
        // 親のFragment取得
        (parentFragment as? JCNicoVideoFragment)?.showShareSheetMediaAttach()
    }

    /** 共有する */
    fun showShareSheet() {
        // 親のFragment取得
        (parentFragment as? JCNicoVideoFragment)?.showShareSheet()
    }

    // ここからレイアウト -----------------------------
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {

            // コルーチン
            val scope = rememberCoroutineScope()
            // Preference
            val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            // タブの選択位置
            val selectIndex = remember { mutableStateOf(0) }

            // メニューのタブ部分
            NicoVideoMenuTab(
                selectedIndex = selectIndex.value,
                tabClick = { index -> selectIndex.value = index }
            )
            // メニューの本命
            when (selectIndex.value) {
                0 -> {
                    NicoVideoMylistsMenu(
                        // マイリスト追加
                        addMylist = { showAddMylistBottomFragment() },
                        // あとでみる追加
                        addAtodemiru = { viewModel.addAtodemiruList() })
                }
                1 -> {
                    // 設定読み出し
                    val isHide3DS = remember { mutableStateOf(prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)) }
                    val isHideKantanComment = remember { mutableStateOf(prefSetting.getBoolean("nicovideo_comment_kantan_comment_hidden", false)) }
                    // 3DS消したり
                    NicoVideoCommentHideMenu(
                        is3DSHide = isHide3DS.value,
                        isKandanCommentHide = isHideKantanComment.value,
                        dsSwitchChange = {
                            isHide3DS.value = !isHide3DS.value
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("nicovideo_comment_3ds_hidden", isHide3DS.value) }
                            // コメント更新
                            scope.launch { viewModel.commentFilter() }
                        },
                        kantanCommentSwitchChange = {
                            isHideKantanComment.value = !isHideKantanComment.value
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("nicovideo_comment_kantan_comment_hidden", isHideKantanComment.value) }
                            // コメント更新
                            scope.launch { viewModel.commentFilter() }
                        }
                    )
                }
                2 -> {
                    NicoVideoOtherButtonMenu(
                        qualityChange = { showQualityBottomSheet() },
                        copyVideoId = { copyVideoId() },
                        screenRotation = { setScreenRotation() },
                        openBrowser = { openWatchPage() },
                        ngList = { launchNGListActivity() },
                        kotehanList = { launchKotehanListActivity() },
                        skipSetting = { showSkipSettingBottomFragment() }
                    )
                }
                3 -> {
                    NicoVideoShareMenu(
                        share = { showShareSheet() },
                        shareAttachImg = { showShereSheetMediaAttach() },
                    )
                }
                4 -> {
                    NicoVideoCacheMenu(
                        isCachePlay = viewModel.isOfflinePlay.value ?: false,
                        cacheGet = { startCacheService(context, viewModel.playingVideoId.value!!, false) }, // 取得
                        cacheGetEco = { startCacheService(context, viewModel.playingVideoId.value!!, true) }, // エコノミーで取得
                        cacheUpdate = { viewModel.nicoVideoCache.getReGetVideoInfoComment(viewModel.playingVideoId.value!!, viewModel.userSession, context) }, // 再取得
                    )
                }
                5 -> {
                    val volume = viewModel.volumeControlLiveData.observeAsState(initial = 1f)
                    NicoVideoVolumeMenu(
                        volume = volume.value,
                        volumeChange = { viewModel.volumeControlLiveData.postValue(it) }
                    )
                }
            }
        }
    }

}
