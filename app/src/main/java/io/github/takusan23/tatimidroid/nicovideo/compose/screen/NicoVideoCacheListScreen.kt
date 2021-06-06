package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.SimpleBackdrop
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoCacheListOption
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoList
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoCacheFragmentViewModel

/**
 * キャッシュ一覧画面。Composeでできている
 *
 * @param viewModel キャッシュ一覧ViewModel
 * @param onVideoClick 動画押したら呼ばれる
 * @param onMenuClick メニュー押したら呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoCacheListScreen(
    viewModel: NicoVideoCacheFragmentViewModel,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    val state = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed)
    // キャッシュ動画一覧
    val videoList = viewModel.filteredCacheVideoList.observeAsState()
    // キャッシュ使用容量
    val totalUsedStorageGB = viewModel.totalUsedStorageGB.observeAsState(initial = "")

    SimpleBackdrop(
        scaffoldState = state,
        openText = "キャッシュのフィルターなどはここを押して展開",
        backLayerContent = {
            NicoVideoCacheListOption(
                isSaveDevice = viewModel.isCacheFolderFromDeviceStorage,
                usingGB = totalUsedStorageGB.value,
                onCacheMusicModeClick = {},
                onPlaylistClick = {}
            )
        },
        frontLayerContent = {
            if (videoList.value == null) {
                FillLoadingScreen()
            } else {
                NicoVideoList(
                    list = videoList.value!!,
                    onVideoClick = { onVideoClick(it) },
                    onMenuClick = { onMenuClick(it) }
                )
            }
        }
    )
}

