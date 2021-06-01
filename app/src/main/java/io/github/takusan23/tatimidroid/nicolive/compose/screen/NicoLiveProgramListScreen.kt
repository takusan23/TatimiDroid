package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.MenuItem
import io.github.takusan23.tatimidroid.compose.SimpleBackdrop
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveProgramListViewModel
import kotlinx.coroutines.launch

/**
 * フォロー中番組、あなたへのおすすめ番組、ニコレポとかを表示してるやつ。
 *
 * BottomNavigationの生放送押したら開くやつ
 *
 * @param onClickProgram 番組押したら呼ばれる関数。予約枠の場合は呼ばれない
 * @param onClickMenu メニュー押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoLiveProgramListScreen(onClickProgram: (NicoLiveProgramData) -> Unit, onClickMenu: (NicoLiveProgramData) -> Unit) {
    // Jetpack ComposeでViewModel使うなって話、Activityに近いComposeなら別にいいんだよね？
    val viewModel = viewModel<NicoLiveProgramListViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 開いているベージ
    val currentPage = remember { mutableStateOf(context.getString(R.string.follow_program)) }
    // 読み込み中かどうか
    val isLoading = viewModel.isLoadingLiveData.observeAsState(initial = true)
    // 番組一覧
    val programList = viewModel.programListLiveData.observeAsState()
    // Backdropの状態
    val backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed)

    /** Backdropを開く */
    fun openMenu() = scope.launch { backdropScaffoldState.conceal() }

    /** Backdropを閉じる */
    fun closeMenu() = scope.launch { backdropScaffoldState.reveal() }

    // 簡略化したカスタム版Backdrop
    SimpleBackdrop(
        scaffoldState = backdropScaffoldState,
        backLayerContent = {
            LazyColumn {
                item {
                    MenuItem(
                        text = stringResource(id = R.string.follow_program),
                        isSelected = currentPage.value == stringResource(id = R.string.follow_program),
                        painter = painterResource(id = R.drawable.ic_outline_people_outline_24px),
                        onClick = {
                            currentPage.value = it
                            viewModel.getFollowingProgram()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.nicorepo),
                        isSelected = currentPage.value == stringResource(id = R.string.nicorepo),
                        painter = painterResource(id = R.drawable.ic_outline_people_outline_24px),
                        onClick = {
                            currentPage.value = it
                            viewModel.getNicorepoProgramList()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.osusume),
                        isSelected = currentPage.value == stringResource(id = R.string.osusume),
                        painter = painterResource(id = R.drawable.ic_photo_filter),
                        onClick = {
                            currentPage.value = it
                            viewModel.getRecommendProgram()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.nicolive_menu_konomi_tag),
                        isSelected = currentPage.value == stringResource(id = R.string.nicolive_menu_konomi_tag),
                        painter = painterResource(id = R.drawable.ic_outline_favorite_border_24),
                        onClick = {
                            currentPage.value = it
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.rookie),
                        isSelected = currentPage.value == stringResource(id = R.string.rookie),
                        painter = painterResource(id = R.drawable.ic_new_releases_black_24dp),
                        onClick = {
                            currentPage.value = it
                            viewModel.getRookieProgram()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.ranking),
                        isSelected = currentPage.value == stringResource(id = R.string.ranking),
                        painter = painterResource(id = R.drawable.ic_format_list_numbered_black_24dp),
                        onClick = {
                            currentPage.value = it
                            viewModel.getRanking()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.nicolive_top),
                        isSelected = currentPage.value == stringResource(id = R.string.nicolive_top),
                        painter = painterResource(id = R.drawable.ic_photo_filter),
                        onClick = {
                            currentPage.value = it
                            viewModel.getFocusProgram()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.upcoming_programs),
                        isSelected = currentPage.value == stringResource(id = R.string.upcoming_programs),
                        painter = painterResource(id = R.drawable.ic_outline_query_builder_24px),
                        onClick = {
                            currentPage.value = it
                            viewModel.getRecentJustBeforeBroadcastStatusProgramListState()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.popular_reservation_program),
                        isSelected = currentPage.value == stringResource(id = R.string.popular_reservation_program),
                        painter = painterResource(id = R.drawable.ic_photo_filter),
                        onClick = {
                            currentPage.value = it
                            viewModel.getPopularBeforeOpenBroadcastStatusProgramListState()
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.nicolive_jk),
                        isSelected = currentPage.value == stringResource(id = R.string.nicolive_jk),
                        painter = painterResource(id = R.drawable.jk_icon),
                        onClick = {
                            currentPage.value = it
                            viewModel.getNicoJKProgramList(true)
                            openMenu()
                        }
                    )
                    MenuItem(
                        text = stringResource(id = R.string.nicolive_jk_program_tag),
                        isSelected = currentPage.value == stringResource(id = R.string.nicolive_jk_program_tag),
                        painter = painterResource(id = R.drawable.jk_icon),
                        onClick = {
                            currentPage.value = it
                            viewModel.getNicoJKProgramList(false)
                            openMenu()
                        }
                    )
                }
            }
        },
        frontLayerContent = {
            when (currentPage.value) {
                stringResource(id = R.string.nicolive_menu_konomi_tag) -> {
                    // 好みタグだけ特殊なので
                    NicoLiveKonomiTagScreen(
                        onClickProgram = { onClickProgram(it) },
                        onClickMenu = { onClickMenu(it) }
                    )
                }
                else -> {
                    // その他のフォロー中番組とか
                    if (programList.value == null || isLoading.value) {
                        // 読み込み中
                        FillLoadingScreen()
                    } else {
                        NicoLiveProgramList(
                            list = programList.value!!,
                            onClickProgram = { nicoLiveProgramData ->
                                // 視聴画面へ
                                if (nicoLiveProgramData.lifeCycle != "RELEASED") {
                                    onClickProgram(nicoLiveProgramData)
                                }
                            },
                            onClickMenu = { nicoLiveProgramData ->
                                // メニュー画面へ
                                onClickMenu(nicoLiveProgramData)
                            },
                        )
                    }
                }
            }
        }
    )
}
