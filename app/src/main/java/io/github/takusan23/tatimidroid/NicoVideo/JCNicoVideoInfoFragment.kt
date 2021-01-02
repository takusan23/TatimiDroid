package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.material.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoLikeBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.*
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSearchFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import kotlinx.coroutines.launch

/**
 * 動画情報Fragment。Jetpack Composeでレイアウトを作っている。Jetpack Composeたのし～～～。なんで動いてんのかよく知らんけど
 *
 * [io.github.takusan23.tatimidroid.BottomSheetPlayerBehavior.isDraggableAreaPlayerOnly]がtrueじゃないとうまくスクロールできない。
 *
 * [JCNicoVideoFragment]のViewModelを利用している。
 * */
class JCNicoVideoInfoFragment : Fragment() {

    /** [JCNicoVideoFragment]のViewModelを取得する */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            // Jetpack Compose
            setContent {
                MaterialTheme(
                    // ダークモード。動的にテーマ変更できるようになるんか？
                    colors = if (isDarkMode(AmbientContext.current)) DarkColors else LightColors,
                ) {
                    Surface {

                        // Snackbar表示で使う
                        val state = rememberScaffoldState()
                        val scope = rememberCoroutineScope()

                        Scaffold(
                            scaffoldState = state,
                            floatingActionButton = {
/*
                                // コメント一覧表示Fab
                                NicoVideoCommentListFab(
                                    isShowCommentList = isShowCommentList.value,
                                    click = { viewModel.commentListShowLiveData.postValue(!isShowCommentList.value) }
                                )
*/
                            }
                        ) {

                            // LiveDataをJetpack Composeで利用できるように
                            val data = viewModel.nicoVideoData.observeAsState()
                            // いいね状態
                            val isLiked = viewModel.isLikedLiveData.observeAsState(initial = false)
                            // 動画情報
                            val descroption = viewModel.nicoVideoDescriptionLiveData.observeAsState(initial = "")
                            // 関連動画
                            val recommendList = viewModel.recommendList.observeAsState()
                            // ユーザー情報
                            val userData = viewModel.userDataLiveData.observeAsState()
                            // タグ一覧
                            val tagList = viewModel.tagListLiveData.observeAsState()

                            // スクロールできるやつ
                            ScrollableColumn {
                                if (data.value != null) {
                                    // 動画情報表示Card
                                    NicoVideoInfoCard(
                                        nicoVideoData = data.value,
                                        isLiked = isLiked,
                                        scaffoldState = state,
                                        description = descroption.value,
                                        postLike = {
                                            // いいね登録
                                            NicoVideoLikeBottomFragment().show(parentFragmentManager, "like")
                                        },
                                        postRemoveLike = {
                                            // いいね解除
                                            viewModel.removeLike()
                                        },
                                        descriptionClick = { link, type ->
                                            // 押した時

                                        }
                                    )
                                }
                                // タグ
                                if (tagList.value != null) {
                                    NicoVideoTagCard(
                                        tagDataList = tagList.value!!,
                                        tagClick = { data ->
                                            // タグ押した時
                                            setTagSearchFragment(data.tagName)
                                        }
                                    )
                                }
                                // ユーザー情報
                                if (userData.value != null) {
                                    NicoVideoUserCard(
                                        userData = userData.value!!,
                                        userOpenClick = {
                                            setAccountFragment(userData.value!!.userId.toString())
                                        }
                                    )
                                }

                                // メニューカード。長いのでまとめた
                                NicoVideoMenuScreen(requireParentFragment())

                                // 関連動画表示Card
                                if (recommendList.value != null) {
                                    NicoVideoRecommendCard(recommendList.value!!)
                                }
                            }
                        }

                        // Snackbar表示。使い方合ってんのかはしらんけど
                        viewModel.likeThanksMessageLiveData.observe(viewLifecycleOwner) {
                            scope.launch {
                                state.snackbarHostState.showSnackbar(message = it, actionLabel = null, duration = SnackbarDuration.Indefinite)
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * アカウント情報Fragmentを表示
     * */
    private fun setAccountFragment(userId: String) {
        val accountFragment = NicoAccountFragment().apply {
            arguments= Bundle().apply {
                putString("userId",userId)
            }
        }
        setFragment(accountFragment,"account")
    }

    /**
     * タグの検索をするFragmentを表示する
     * */
    private fun setTagSearchFragment(tag: String) {
        val searchFragment = NicoVideoSearchFragment().apply {
            arguments = Bundle().apply {
                putString("search", tag)
            }
        }
        setFragment(searchFragment, "tag_search")
    }

    /**
     * Fragmentを置く関数
     *
     * @param fragment 置くFragment
     * @param backstack Fragmentを積み上げる場合は適当な値を入れて
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        // Fragment設置
        (requireActivity() as MainActivity).setFragment(fragment, backstack, true)
        // ミニプレイヤー化
        viewModel.isMiniPlayerMode.postValue(true)
    }

}