package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoAccountViewModel
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoAccountViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModelFactory
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_account.*

/**
 * アカウント情報Fragment
 *
 * 投稿動画とか公開マイリストとか
 *
 * 入れてほしいもの
 *
 * userId   | String    | ユーザーID
 * */
class NicoAccountFragment : Fragment() {

    private lateinit var accountViewModel: NicoAccountViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")!!

        accountViewModel = ViewModelProvider(this, NicoAccountViewModelFactory(requireActivity().application, userId)).get(NicoAccountViewModel::class.java)

        // でーたをうけとる
        accountViewModel.userDataLiveData.observe(viewLifecycleOwner) { data ->
            fragment_account_user_name_text_view.text = data.nickName
            fragment_account_user_id_text_view.text = data.userId.toString()
            fragment_account_version_name_text_view.text = data.niconicoVersion
            fragment_account_description_text_view.text = data.description
            fragment_account_follow_count_text_view.text = data.followeeCount.toString()
            fragment_account_follower_count_text_view.text = data.followerCount.toString()
            Glide.with(fragment_account_avatar_image_view).load(data.largeIcon).into(fragment_account_avatar_image_view)
        }
    }

}