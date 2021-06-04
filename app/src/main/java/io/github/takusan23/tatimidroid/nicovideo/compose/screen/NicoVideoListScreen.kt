package io.github.takusan23.tatimidroid.nicovideo.compose

import android.app.Application
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.MenuItem
import io.github.takusan23.tatimidroid.compose.SimpleBackdrop
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoUserProfileScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoHistoryScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoSearchScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoAccountViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoMyListViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoUploadVideoViewModelFactory
import kotlinx.coroutines.launch

/**
 * ニコ動のランキング、視聴履歴、マイリストへ移動する画面。Composeでできている
 *
 * @param application Activityで取れる
 * @param onVideoClick 動画押したときに呼ばれる
 * @param onMenuClick メニュー押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoListScreen(
    application: Application,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 開いているベージ
    val currentPage = remember { mutableStateOf(context.getString(R.string.ranking)) }
    // Backdropの状態
    val backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed)

    /** Backdropを閉じる */
    fun closeMenu() = scope.launch { backdropScaffoldState.conceal() }

    SimpleBackdrop(
        openText = stringResource(id = R.string.video_dropdown),
        scaffoldState = backdropScaffoldState,
        backLayerContent = {
            MenuItem(
                text = stringResource(id = R.string.ranking),
                isSelected = currentPage.value == stringResource(id = R.string.ranking),
                painter = painterResource(id = R.drawable.ic_format_list_numbered_black_24dp),
                onClick = { menu ->
                    closeMenu()
                    currentPage.value = menu
                }
            )
            MenuItem(
                text = stringResource(id = R.string.post_video),
                isSelected = currentPage.value == stringResource(id = R.string.post_video),
                painter = painterResource(id = R.drawable.ic_cloud_upload_black_24dp),
                onClick = { menu ->
                    currentPage.value = menu
                    closeMenu()
                }
            )
            MenuItem(
                text = stringResource(id = R.string.mylist),
                isSelected = currentPage.value == stringResource(id = R.string.mylist),
                painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                onClick = { menu ->
                    currentPage.value = menu
                    closeMenu()
                }
            )
            MenuItem(
                text = stringResource(id = R.string.history),
                isSelected = currentPage.value == stringResource(id = R.string.history),
                painter = painterResource(id = R.drawable.ic_history_24px),
                onClick = { menu ->
                    currentPage.value = menu
                    closeMenu()
                }
            )
            MenuItem(
                text = stringResource(id = R.string.serch),
                isSelected = currentPage.value == stringResource(id = R.string.serch),
                painter = painterResource(id = R.drawable.ic_24px),
                onClick = { menu ->
                    currentPage.value = menu
                    closeMenu()
                }
            )
            MenuItem(
                text = stringResource(id = R.string.mypage),
                isSelected = currentPage.value == stringResource(id = R.string.mypage),
                painter = painterResource(id = R.drawable.ic_outline_account_circle_24px),
                onClick = { menu ->
                    currentPage.value = menu
                    closeMenu()
                }
            )
        },
        frontLayerContent = {
            when (currentPage.value) {
                stringResource(id = R.string.ranking) -> {
                    NicoVideoRankingScreen(
                        viewModel = viewModel(),
                        onVideoClick = { onVideoClick(it) },
                        onMenuClick = { onMenuClick(it) }
                    )
                }
                stringResource(id = R.string.post_video) -> {
                    NicoVideoUploadScreen(
                        viewModel = viewModel(factory = NicoVideoUploadVideoViewModelFactory(application, null)),
                        onVideoClick = { onVideoClick(it) },
                        onMenuClick = { onMenuClick(it) }
                    )
                }
                stringResource(id = R.string.mylist) -> {
                    NicoVideoMylistParentScreen(
                        viewModel = viewModel(factory = NicoVideoMyListViewModelFactory(application, null)),
                        onClickMylist = { }
                    )
                }
                stringResource(id = R.string.history) -> {
                    NicoVideoHistoryScreen(
                        viewModel = viewModel(),
                        onVideoClick = { onVideoClick(it) },
                        onMenuClick = { onMenuClick(it) }
                    )
                }
                stringResource(id = R.string.serch) -> {
                    NicoVideoSearchScreen(
                        viewModel = viewModel(),
                        onVideoClick = { onVideoClick(it) },
                        onMenuClick = { onMenuClick(it) }
                    )
                }
                stringResource(id = R.string.mypage) -> {
                    NicoUserProfileScreen(viewModel = viewModel(factory = NicoAccountViewModelFactory(application, null)))
                }
            }
        },
    )
}