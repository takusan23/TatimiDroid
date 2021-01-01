package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.*
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
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

                        // 区切り線
                        Divider()
                        Card(modifier = Modifier.padding(5.dp)) {
                            // マイリスト登録など
                            NicoVideoMylistsMenu(
                                addMylist = { },
                                addAtodemiru = { }
                            )
                        }

                        // 区切り線
                        Divider()
                        // その他のメニュー
                        Card(modifier = Modifier.padding(5.dp)) {
                            NicoVideoOtherButtonMenu(
                                qualityChane = {},
                                screenRotation = {},
                                openBrowser = {},
                                ngList = {},
                                kotehanList = {},
                                skipSetting = {}
                            )
                        }


                        // 区切り線
                        Divider()
                        // キャッシュ
                        Card(modifier = Modifier.padding(5.dp)) {
                            NicoVideoCacheMenu(
                                cacheGet = {

                                },
                            )
                        }

                        // 区切り線
                        Divider()
                        // 音量
                        Card(modifier = Modifier.padding(5.dp)) {
                            val volume = remember { mutableStateOf(1f) }
                            NicoVideoVolumeMenu(
                                volume = volume.value,
                                volumeChange = { volume.value = it }
                            )
                        }
                    }
                }
            }
        }
    }

}