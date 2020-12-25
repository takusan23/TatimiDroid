package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoMyListFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoNicoRepoFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSeriesListFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoUploadVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoAccountViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoAccountViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_account.*

/**
 * アカウント情報Fragment
 *
 * 投稿動画とか公開マイリストとか。データ取得は[NicoAccountViewModel]に書いてあります
 *
 * 入れてほしいもの
 *
 * userId   | String    | ユーザーID。ない場合(nullのとき)は自分のアカウントの情報を取りに行きます
 * */
class NicoAccountFragment : Fragment() {

    private lateinit var accountViewModel: NicoAccountViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        accountViewModel = ViewModelProvider(this, NicoAccountViewModelFactory(requireActivity().application, userId)).get(NicoAccountViewModel::class.java)

        // ViewModelからでーたをうけとる
        accountViewModel.userDataLiveData.observe(viewLifecycleOwner) { data ->
            fragment_account_user_name_text_view.text = data.nickName
            fragment_account_user_id_text_view.text = data.userId.toString()
            fragment_account_version_name_text_view.text = data.niconicoVersion
            fragment_account_description_text_view.text = HtmlCompat.fromHtml(data.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
            fragment_account_follow_count_text_view.text = "${getString(R.string.follow_count)}：${data.followeeCount}"
            fragment_account_follower_count_text_view.text = "${getString(R.string.follower_count)}：${data.followerCount}"
            // あば＾～ー画像
            Glide.with(fragment_account_avatar_image_view).load(data.largeIcon).into(fragment_account_avatar_image_view)
            fragment_account_avatar_image_view.imageTintList = null
            // 自分ならフォローボタン潰す
            fragment_account_follow_button.isVisible = userId != null

            // フォローボタン押せるように
            setFollowButtonClick()

            // プレ垢
            fragment_account_premium.isVisible = data.isPremium

            // Fragmentにわたすデータ
            val bundle = Bundle().apply {
                putString("userId", data.userId.toString())
            }

            // 投稿動画Fragmentへ遷移
            fragment_account_upload_video.setOnClickListener {
                val nicoVideoPOSTFragment = NicoVideoUploadVideoFragment().apply {
                    arguments = bundle
                }
                setFragment(nicoVideoPOSTFragment, "post")
            }

            // ニコレポFragment
            fragment_account_nicorepo.setOnClickListener {
                val nicoRepoFragment = NicoVideoNicoRepoFragment().apply {
                    arguments = bundle
                }
                setFragment(nicoRepoFragment, "nicorepo")
            }

            // マイリストFragment
            fragment_account_mylist.setOnClickListener {
                val myListFragment = NicoVideoMyListFragment().apply {
                    arguments = bundle
                }
                setFragment(myListFragment, "mylist")
            }

            // シリーズFragment
            fragment_account_series.setOnClickListener {
                val seriesFragment = NicoVideoSeriesListFragment().apply {
                    arguments = bundle
                }
                setFragment(seriesFragment, "series")
            }

        }

        // フォロー状態をLiveDataで受け取る
        accountViewModel.followStatusLiveData.observe(viewLifecycleOwner) { isFollowing ->
            // フォロー中ならフォロー中にする
            if (isFollowing) {
                fragment_account_follow_button.text = getString(R.string.is_following)
            } else {
                fragment_account_follow_button.text = getString(R.string.follow_count)
            }
        }

    }

    private fun setFollowButtonClick() {
        // フォローボタン押した時
        fragment_account_follow_button.setOnClickListener {
            // フォロー状態を確認
            val isFollowing = accountViewModel.followStatusLiveData.value == true
            // メッセージ調整
            val snackbarMessage = if (!isFollowing) getString(R.string.nicovideo_account_follow_message_message) else getString(R.string.nicovideo_account_remove_follow_message)
            val snackbarAction = if (!isFollowing) getString(R.string.nicovideo_account_follow) else getString(R.string.nicovideo_account_remove_follow)
            // 確認する
            Snackbar.make(it, snackbarMessage, Snackbar.LENGTH_LONG).setAction(snackbarAction) {
                accountViewModel.postFollowRequest()
            }.show()
        }
    }

    /**
     * Fragmentを置く。第２引数はバックキーで戻るよう
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        parentFragmentManager.beginTransaction().replace(id, fragment).addToBackStack(backstack).commit()
    }

}