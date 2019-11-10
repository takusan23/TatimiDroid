package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.DarkModeSupport
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
    lateinit var darkModeSupport: DarkModeSupport

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

        darkModeSupport = DarkModeSupport(context!!)

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

        //OutlinedButtonのテキストの色
        darkmode()

    }

    fun darkmode() {
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            //ダークモード時ボタンのテキストの色が変わらないので
            val color = ColorStateList.valueOf(Color.parseColor("#ffffff"))
            fragment_comment_fragment_menu_rotation_button.setTextColor(color)
            fragment_comment_fragment_menu_copy_liveid_button.setTextColor(color)
            fragment_comment_fragment_menu_copy_communityid_button.setTextColor(color)
            fragment_comment_fragment_menu_open_browser_button.setTextColor(color)
            fragment_comment_fragment_menu_ng_list_button.setTextColor(color)
            fragment_comment_fragment_menu_share_button.setTextColor(color)
        }
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
        //生放送を再生ボタン
        fragment_comment_fragment_menu_view_live_button.setOnClickListener {
            (activity?.supportFragmentManager?.findFragmentByTag(liveId) as CommentFragment).apply {
                if (live_framelayout.visibility == View.VISIBLE) {
                    live_framelayout.visibility = View.GONE
                    live_video_view.stopPlayback()
                } else {
                    live_framelayout.visibility = View.VISIBLE
                    setPlayVideoView()
                }
            }
        }
        //バッググラウンド再生。調子悪いのでServiceなんかで実装し直したほうがいいと思ってるけどまず使ってないので直さないと思います。
        fragment_comment_fragment_menu_background_button.setOnClickListener {
            commentFragment.setBackgroundProgramPlay()
        }

        //ポップアップ再生。いつか怒られそう（プレ垢限定要素だし）
        fragment_comment_fragment_menu_popup_button.setOnClickListener {
            commentFragment.apply {
                //ポップアップ再生。コメント付き
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(context)) {
                        //RuntimePermissionに対応させる
                        // 権限取得
                        val intent =
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context?.packageName}")
                            )
                        startActivityForResult(intent, 114)
                    } else {
                        startOverlayPlayer()
                    }
                } else {
                    //ろりぽっぷ
                    startOverlayPlayer()
                }
            }
        }

        //フローティングコメビュ
        fragment_comment_fragment_menu_floating_button.setOnClickListener {
            //Activity移動
            commentFragment.showBubbles()
        }

        //フローティングコメビュはAndroid10以降で利用可能
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            fragment_comment_fragment_menu_floating_button.isEnabled = false
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