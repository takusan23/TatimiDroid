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
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoMyListFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoNicoRepoFragment
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
            // プレ垢
            fragment_account_premium.isVisible = data.isPremium

            // 投稿動画Fragmentへ遷移
            fragment_account_upload_video.setOnClickListener {
                val nicoVideoPOSTFragment = NicoVideoUploadVideoFragment().apply {
                    arguments = Bundle().apply {
                        putString("userId", data.userId.toString())
                    }
                }
                (requireParentFragment() as NicoVideoSelectFragment).setFragment(nicoVideoPOSTFragment, "post")
            }

            // ニコレポFragment
            fragment_account_nicorepo.setOnClickListener {
                val nicoRepoFragment = NicoVideoNicoRepoFragment().apply {
                    arguments = Bundle().apply {
                        putString("userId", data.userId.toString())
                    }
                }
                (requireParentFragment() as NicoVideoSelectFragment).setFragment(nicoRepoFragment, "nicorepo")
            }

            // マイリストFragment
            fragment_account_mylist.setOnClickListener {
                val myListFragment = NicoVideoMyListFragment().apply {
                    arguments = Bundle().apply {
                        putString("userId", data.userId.toString())
                    }
                }
                (requireParentFragment() as NicoVideoSelectFragment).setFragment(myListFragment, "mylist")
            }

        }
    }

}