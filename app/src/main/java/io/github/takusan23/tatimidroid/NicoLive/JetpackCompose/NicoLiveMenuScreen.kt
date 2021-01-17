package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.AmbientContext
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.*

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
    val context = AmbientContext.current

    // Preference
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    // ここからレイアウト -----------------------------
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
                    NicoLiveButtonMenu(
                        onClickQualityChange = { /*TODO*/ },
                        onClickScreenRotation = { /*TODO*/ },
                        onClickCopyProgramId = { /*TODO*/ },
                        onClickCopyCommunityId = { /*TODO*/ },
                        onClickOpenBrowser = { /*TODO*/ },
                        onClickNGList = { /*TODO*/ },
                        onClickKotehanList = { /*TODO*/ },
                        onClickHomeScreenPin = { /*TODO*/ }
                    )
                }
                3 -> {
                    NicoVideoShareMenu(
                        share = { /*TODO*/ },
                        shareAttachImg = { /*TODO*/ }
                    )
                }
                4 -> {
                    NicoVideoVolumeMenu(
                        volume = 1f,
                        volumeChange = { /*TODO*/ }
                    )
                }
                5 -> {
                    NicoLiveNicoNamaGameCard(
                        isNicoNamaGame = false,
                        onSwitchNicoNamaGame = { /*TODO*/ }
                    )
                }
            }
        }
    }
}