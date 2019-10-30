package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_comment_menu.*


/*
* ここをメンテしにきた私へ
* CommentMenuBottomFragment と このクラスは違うよ。命名雑だった。ごめん
* CommentFragmentのメニューはここ。
* コメントを押した時にできる（ロックオン、コテハン登録）なんかはCommentMenuBottomFragmentへどうぞ
* */

class CommentMenuFragment : Fragment() {

    lateinit var commentFragment: CommentFragment

    var liveId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //CommentFragmentしゅとく～
        liveId = arguments?.getString("liveId") ?: ""
        commentFragment =
            activity?.supportFragmentManager?.findFragmentByTag(liveId) as CommentFragment

        //値設定
        setValue()

        //CommentFragmentへ値を渡す
        setCommentFragmentValue()

        //クリックイベント
        setClick()

    }

    //クリックイベント
    private fun setClick() {
        //画質変更
        fragment_comment_fragment_menu_quality_button.setOnClickListener {
            //画質変更
            commentFragment.qualitySelectBottomSheet.show(
                activity?.supportFragmentManager!!,
                "quality_bottom"
            )
        }
        //強制画面回転
        fragment_comment_fragment_menu_rotation_button.setOnClickListener {
            commentFragment.setLandscapePortrait()
        }
        //番組IDコピー
        fragment_comment_fragment_menu_copy_liveid_button.setOnClickListener {
            commentFragment.copyProgramId()
        }
        //コミュニティIDコピー
        fragment_comment_fragment_menu_copy_communityid_button.setOnClickListener {
            commentFragment.copyCommunityId()
        }
        //ブラウザで開く
        fragment_comment_fragment_menu_open_browser_button.setOnClickListener {
            val uri = "https://live2.nicovideo.jp/watch/$liveId".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        //NGリスト
        fragment_comment_fragment_menu_ng_list_button.setOnClickListener {
            val intent = Intent(context, NGListActivity::class.java)
            startActivity(intent)
        }
        //（画像添付しない）共有
        fragment_comment_fragment_menu_share_button.setOnClickListener {
            //Kotlinのapply便利だと思った。
            commentFragment.apply {
                programShare =
                    ProgramShare(commentActivity, this.live_video_view, programTitle, liveId)
                programShare.showShareScreen()
            }
        }
        //画像つき共有
        fragment_comment_fragment_menu_share_image_attach_button.setOnClickListener {
            commentFragment.apply {
                programShare =
                    ProgramShare(commentActivity, this.live_video_view, programTitle, liveId)
                programShare.shareAttacgImage()
            }
        }
    }

    //CommentFragmentの値を貰う
    private fun setValue() {
        //コメント非表示
        fragment_comment_fragment_menu_comment_hidden_switch.isChecked =
            commentFragment.isCommentHidden
        //Infoコメント非表示
        fragment_comment_fragment_menu_hide_info_perm_switch.isChecked =
            commentFragment.hideInfoUnnkome
        //匿名で投稿するか
        fragment_comment_fragment_menu_iyayo_comment_switch.isChecked =
            commentFragment.isTokumeiComment
    }

    //CommentFragmentへ値を渡す
    private fun setCommentFragmentValue() {
        //押したらすぐ反映できるように
        fragment_comment_fragment_menu_comment_hidden_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            //コメント非表示
            commentFragment.isCommentHidden = isChecked
        }
        fragment_comment_fragment_menu_hide_info_perm_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            //Infoコメント非表示
            commentFragment.hideInfoUnnkome = isChecked
        }
        fragment_comment_fragment_menu_iyayo_comment_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            //匿名かどうか。commandを直接触る
            commentFragment.isTokumeiComment = isChecked
            when (isChecked) {
                true -> {
                    commentFragment.commentCommand = "184"
                }
                false -> {
                    commentFragment.commentCommand = ""
                }
            }
        }
    }
}