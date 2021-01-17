package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveTagBottomFragment
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.NicoVideoUserCard
import io.github.takusan23.tatimidroid.NicoVideo.NicoAccountFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isDarkMode

/**
 * 番組詳細Fragment
 * */
class JCNicoLiveInfoFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(
                    // ダークモード。動的にテーマ変更できるようになるんか？
                    colors = if (isDarkMode(AmbientContext.current)) DarkColors else LightColors,
                ) {

                    // 番組情報
                    val programData = viewModel.nicoLiveProgramData.observeAsState()
                    // 説明文
                    val description = viewModel.nicoLiveProgramDescriptionLiveData.observeAsState()
                    // ユーザー情報
                    val userData = viewModel.nicoLiveUserDataLiveData.observeAsState()
                    // コミュ、チャンネル情報
                    val communityOrChannelData = viewModel.nicoLiveCommunityOrChannelDataLiveData.observeAsState()
                    // コミュ、チャンネルフォロー中か
                    val isCommunityOrChannelFollow = viewModel.isCommunityOrChannelFollowLiveData.observeAsState(initial = false)
                    // タグ
                    val tagDataList = viewModel.nicoLiveTagDataListLiveData.observeAsState()
                    // タグが編集可能かどうか
                    val isEditableTag = viewModel.isEditableTag.observeAsState()
                    // 好みタグ
                    val konomiTagList = viewModel.nicoLiveKonomiTagListLiveData.observeAsState(initial = arrayListOf())

                    Surface {
                        Scaffold {
                            ScrollableColumn {
                                // 番組情報
                                if (programData.value != null && description.value != null) {
                                    NicoLiveInfoCard(
                                        nicoLiveProgramData = programData.value!!,
                                        programDescription = description.value!!
                                    )
                                }
                                // ユーザー情報。ニコ動用のがそのまま使えた
                                if (userData.value != null) {
                                    NicoVideoUserCard(userData = userData.value!!, onUserOpenClick = {
                                        setAccountFragment(userData.value!!.userId.toString())
                                    })
                                }
                                // コミュ、番組情報
                                if (communityOrChannelData.value != null) {
                                    NicoLiveCommunityCard(
                                        communityOrChannelData = communityOrChannelData.value!!,
                                        isFollow = isCommunityOrChannelFollow.value,
                                        onFollowClick = {
                                            if (isCommunityOrChannelFollow.value) {
                                                // 解除
                                                requestRemoveCommunityFollow(communityOrChannelData.value!!.id)
                                            } else {
                                                // コミュをフォローする
                                                requestCommunityFollow(communityOrChannelData.value!!.id)
                                            }
                                        },
                                        onCommunityOpenClick = {
                                            launchBrowser("https://com.nicovideo.jp/community/${communityOrChannelData.value!!.id}")
                                        }
                                    )
                                }
                                // タグ
                                if (tagDataList.value != null && isEditableTag.value != null) {
                                    NicoLiveTagCard(
                                        list = tagDataList.value!!,
                                        onTagClick = { },
                                        isEditable = isEditableTag.value!!,
                                        onEditClick = { showTagEditBottomFragment() }
                                    )
                                }
                                // 好みタグ
                                NicoLiveKonomiCard(konomiTagList = konomiTagList.value)

                                // メニュー
                                NicoLiveMenuScreen(requireParentFragment())

                                // スペース
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    /** タグ編集画面を出す */
    private fun showTagEditBottomFragment() {
        NicoLiveTagBottomFragment().show(parentFragmentManager, "edit_tag")
    }

    /**
     * ブラウザを開く
     * @param url うらる。
     * */
    private fun launchBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    /**
     * アカウント情報Fragmentを表示
     * @param userId ゆーざーID
     * */
    private fun setAccountFragment(userId: String) {
        val accountFragment = NicoAccountFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
        setFragment(accountFragment, "account")
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

    /** コミュをフォローする関数 */
    private fun requestCommunityFollow(communityId: String) {
        (requireParentFragment() as? JCNicoLiveFragment)?.showSnackBar(getString(R.string.nicovideo_account_follow_message_message), getString(R.string.follow_count)) {
            viewModel.requestCommunityFollow(communityId)
        }
    }

    /** コミュのフォローを解除する関数 */
    private fun requestRemoveCommunityFollow(communityId: String) {
        (requireParentFragment() as? JCNicoLiveFragment)?.showSnackBar(getString(R.string.nicovideo_account_remove_follow_message), getString(R.string.nicovideo_account_remove_follow)) {
            viewModel.requestRemoveCommunityFollow(communityId)
        }
    }

}