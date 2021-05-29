package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.MenuItem
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveProgramListViewModel
import io.github.takusan23.tatimidroid.tool.isDarkMode
import kotlinx.coroutines.launch

/**
 * フォロー中番組、あなたへのおすすめ番組、ニコレポとかを表示してるやつ。
 *
 * BottomNavigationの生放送押したら開くやつ
 *
 * @param onClickProgram 番組押したら呼ばれる関数
 * */
@ExperimentalMaterialApi
@Composable
fun NicoLiveProgramListScreen(onClickProgram: (NicoLiveProgramData) -> Unit) {
    // Jetpack ComposeでViewModel使うなって話、Activityに近いComposeなら別にいいんだよね？
    val viewModel = viewModel<NicoLiveProgramListViewModel>()
    // Backdropの状態
    val backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 開いているベージ
    val currentPage = remember { mutableStateOf(context.getString(R.string.follow_program)) }

    /** Backdropを閉じる関数 */
    fun closeMenu() {
        scope.launch { backdropScaffoldState.conceal() }
    }

    BackdropScaffold(
        appBar = { },
        scaffoldState = backdropScaffoldState,
        frontLayerElevation = 10.dp,
        backLayerBackgroundColor = if (isDarkMode(LocalContext.current)) Color.Black else Color.White,
        backLayerContent = {
            MenuItem(text = stringResource(id = R.string.follow_program), painter = painterResource(id = R.drawable.ic_outline_people_outline_24px), onClick = {
                currentPage.value = it
                viewModel.getFollowingProgram()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.nicorepo), painter = painterResource(id = R.drawable.ic_outline_people_outline_24px), onClick = {
                currentPage.value = it
                viewModel.getNicorepoProgramList()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.osusume), painter = painterResource(id = R.drawable.ic_photo_filter), onClick = {
                currentPage.value = it
                viewModel.getRecommendProgram()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.nicolive_menu_konomi_tag), painter = painterResource(id = R.drawable.ic_outline_favorite_border_24), onClick = {
                currentPage.value = it
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.rookie), painter = painterResource(id = R.drawable.ic_new_releases_black_24dp), onClick = {
                currentPage.value = it
                viewModel.getRookieProgram()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.ranking), painter = painterResource(id = R.drawable.ic_format_list_numbered_black_24dp), onClick = {
                currentPage.value = it
                viewModel.getRanking()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.nicolive_top), painter = painterResource(id = R.drawable.ic_photo_filter), onClick = {
                currentPage.value = it
                viewModel.getFocusProgram()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.upcoming_programs), painter = painterResource(id = R.drawable.ic_outline_query_builder_24px), onClick = {
                currentPage.value = it
                viewModel.getRecentJustBeforeBroadcastStatusProgramListState()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.popular_reservation_program), painter = painterResource(id = R.drawable.ic_photo_filter), onClick = {
                currentPage.value = it
                viewModel.getPopularBeforeOpenBroadcastStatusProgramListState()
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.nicolive_jk), painter = painterResource(id = R.drawable.jk_icon), onClick = {
                currentPage.value = it
                viewModel.getNicoJKProgramList(true)
                closeMenu()
            })
            MenuItem(text = stringResource(id = R.string.nicolive_jk_program_tag), painter = painterResource(id = R.drawable.jk_icon), onClick = {
                currentPage.value = it
                viewModel.getNicoJKProgramList(false)
                closeMenu()
            })
        },
        frontLayerContent = {
            when (currentPage.value) {
                stringResource(id = R.string.nicolive_menu_konomi_tag) -> {
                    // 好みタグだけ特殊なので
                    NicoLiveKonomiTagScreen { onClickProgram(it) }
                }
                else -> {
                    // 読み込み中かどうか
                    val isLoading = viewModel.isLoadingLiveData.observeAsState(initial = true)
                    // 番組配列
                    val programList = viewModel.programListLiveData.observeAsState()
                    if (isLoading.value || programList.value == null) {
                        // ローディング
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // データ取得完了
                        NicoLiveProgramList(list = programList.value!!, onClickProgram = { onClickProgram(it) })
                    }
                }
            }
        }
    )
}
