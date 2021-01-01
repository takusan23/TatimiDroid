package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.KotehanListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.NicoVideo.JCNicoVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.*
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import kotlinx.coroutines.launch

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.NicoVideoMenuFragment]の代替
 * */
class JCNivoVideoMenuBottomFragment : BottomSheetDialogFragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    /** Preference */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(AmbientContext.current)) DarkColors else LightColors
                ) {
                    ScrollableColumn {

                        // コルーチン
                        val scope = rememberCoroutineScope()

                        // 設定読み出し
                        val isHide3DS = remember { mutableStateOf(prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)) }
                        val isHideKantanComment = remember { mutableStateOf(prefSetting.getBoolean("nicovideo_comment_kantan_comment_hidden", false)) }

                        // スイッチ系
                        Card(modifier = Modifier.padding(5.dp)) {
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
                        Card(modifier = Modifier.padding(5.dp)) {
                            // マイリスト登録など
                            NicoVideoMylistsMenu(
                                addMylist = { showAddMylistBottomFragment() },
                                addAtodemiru = { viewModel.addAtodemiruList() }
                            )
                        }
                        // その他のメニュー
                        Card(modifier = Modifier.padding(5.dp)) {
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
                        // キャッシュ
                        Card(modifier = Modifier.padding(5.dp)) {
                            NicoVideoCacheMenu(
                                isCachePlay = viewModel.isOfflinePlay.value ?: false,
                                cacheGet = { startCacheService(requireContext(), viewModel.playingVideoId.value!!, false) }, // 取得
                                cacheGetEco = { startCacheService(requireContext(), viewModel.playingVideoId.value!!, true) }, // エコノミーで取得
                                cacheUpdate = { viewModel.nicoVideoCache.getReGetVideoInfoComment(viewModel.playingVideoId.value!!, viewModel.userSession, context) }, // 再取得
                            )
                        }
                        // 共有
                        Card(modifier = Modifier.padding(5.dp)) {
                            NicoVideoShareMenu(
                                share = { showShareSheet() },
                                shareAttachImg = { showShereSheetMediaAttach() },
                            )
                        }
                        // 音量
                        Card(modifier = Modifier.padding(5.dp)) {
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
    }

    /** 画像つき共有をする */
    private fun showShereSheetMediaAttach() {
        // 親のFragment取得
        (requireParentFragment() as? JCNicoVideoFragment)?.showShareSheetMediaAttach()
    }

    /** 共有する */
    private fun showShareSheet() {
        // 親のFragment取得
        (requireParentFragment() as? JCNicoVideoFragment)?.showShareSheet()
    }

    /** Toast表示 */
    private fun showToast(s: String) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()
    }

    /** マイリスト追加BottomFragmentを表示する */
    private fun showAddMylistBottomFragment() {
        val addMylistBottomFragment = NicoVideoAddMylistBottomFragment()
        val bundle = Bundle()
        bundle.putString("id", viewModel.playingVideoId.value)
        addMylistBottomFragment.arguments = bundle
        addMylistBottomFragment.show(parentFragmentManager, "mylist")
    }

    /** 画質変更BottomFragmentを表示する */
    private fun showQualityBottomSheet() {
        // キャッシュ利用時は使わない
        if (viewModel.isOfflinePlay.value == false) {
            val nicoVideoQualityBottomFragment = NicoVideoQualityBottomFragment()
            nicoVideoQualityBottomFragment.show(parentFragmentManager, "quality")
        }
    }

    /** 動画IDをコピーする */
    private fun copyVideoId() {
        val videoId = viewModel.playingVideoId.value
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", videoId))
        Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${videoId}", Toast.LENGTH_SHORT).show()
    }

    /** 強制的に画面回転をする関数 */
    private fun setScreenRotation() {
        val conf = resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }

    /** スキップ秒数変更BottomFragmentを表示する */
    private fun showSkipSettingBottomFragment() {
        val skipCustomizeBottomFragment = NicoVideoSkipCustomizeBottomFragment()
        skipCustomizeBottomFragment.show(parentFragmentManager, "skip")
    }

    /** ブラウザで動画を開く関数 */
    private fun openWatchPage() {
        val videoId = viewModel.playingVideoId.value
        openBrowser("https://nico.ms/$videoId")
    }

    /** NG一覧を開く */
    private fun launchNGListActivity() {
        val intent = Intent(context, NGListActivity::class.java)
        startActivity(intent)
    }

    /** コテハン一覧を開く */
    private fun launchKotehanListActivity() {
        // コテハン一覧
        val intent = Intent(context, KotehanListActivity::class.java)
        startActivity(intent)
    }

    /** ブラウザを開く関数 */
    private fun openBrowser(addr: String) {
        val intent = Intent(Intent.ACTION_VIEW, addr.toUri())
        startActivity(intent)
    }

}