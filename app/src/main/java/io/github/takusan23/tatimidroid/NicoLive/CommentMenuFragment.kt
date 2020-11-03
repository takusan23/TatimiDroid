package io.github.takusan23.tatimidroid.NicoLive

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.KotehanListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveQualitySelectBottomSheet
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.ContentShare
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_comment_menu.*


/**
 * ここをメンテしにきた私へ
 * CommentMenuBottomFragment と このクラスは違うよ。命名雑だった。ごめん
 * CommentFragmentのメニューはここ。
 * コメントを押した時にできる（ロックオン、コテハン登録）なんかは [CommentLockonBottomFragment] へどうぞ
 * */
class CommentMenuFragment : Fragment() {

    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting: SharedPreferences

    var liveId = ""

    // CommentFragmentとそれのViewModel
    val commentFragment by lazy { requireParentFragment() as CommentFragment }
    val viewModel by viewModels<NicoLiveViewModel>({ commentFragment })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        darkModeSupport = DarkModeSupport(requireContext())
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //CommentFragmentしゅとく～
        liveId = arguments?.getString("liveId") ?: ""

        //値設定
        setValue()

        //CommentFragmentへ値を渡す
        setCommentFragmentValue()

        // Dynamic Shortcutsテスト。とりあえず試験的にニコニコ実況だけ
        if (commentFragment.isJK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fragment_comment_fragment_menu_dymanic_shortcut_button.visibility = View.VISIBLE
        }

        //クリックイベント
        setClick()

        //OutlinedButtonのテキストの色
        darkmode()

        // Android 5の場合はWebViewが落ちてしまうので塞ぐ
        // こればっかりはandroidx.appcompat:appcompatのせいなので私悪くない
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            fragment_comment_fragment_nico_nama_game_switch.isEnabled = false
        }

        // ニコニコ実況で使わないボタンを消す
        hideNicoJK()

    }

    private fun hideNicoJK() {
        if (isNicoJK()) {
            fragment_comment_fragment_menu_background_button.isEnabled = false
            fragment_comment_fragment_menu_cast_button.isEnabled = false
            fragment_comment_fragment_menu_floating_button.isEnabled = false
            fragment_comment_fragment_menu_iyayo_comment_switch.isEnabled = false
            fragment_comment_fragment_menu_iyayo_hidden_switch.isEnabled = false
            fragment_comment_fragment_menu_low_latency_switch.isEnabled = false
            fragment_comment_fragment_menu_copy_liveid_button.isEnabled = false
            fragment_comment_fragment_menu_copy_communityid_button.isEnabled = false
            fragment_comment_fragment_menu_open_browser_button.isEnabled = false
            fragment_comment_fragment_menu_ng_list_button.isEnabled = false
            fragment_comment_fragment_nico_nama_game_switch.isEnabled = false
        }
    }

    fun darkmode() {
        if (isDarkMode(context)) {
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
        //キャスト
        if (commentFragment.isInitGoogleCast()) {
            val googleCast = commentFragment.googleCast
            googleCast.setUpCastButton(fragment_comment_fragment_menu_cast_button)
        }

        // 画質変更
        fragment_comment_fragment_menu_quality_button.setOnClickListener {
            NicoLiveQualitySelectBottomSheet().show(commentFragment.childFragmentManager, "quality_change")
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
            val contentShare = ContentShare(requireActivity() as AppCompatActivity, viewModel.programTitle, liveId)
            contentShare.shareContent()
        }
        //画像つき共有
        fragment_comment_fragment_menu_share_image_attach_button.setOnClickListener {
            val contentShare = ContentShare(requireActivity() as AppCompatActivity, viewModel.programTitle, liveId)
            // 今いる部屋の名前入れる
            contentShare.shareContentAttachPicture(commentFragment.live_surface_view, commentFragment.comment_canvas)
        }
        //生放送を再生ボタン
        fragment_comment_fragment_menu_view_live_button.setOnClickListener {
            (requireParentFragment() as CommentFragment).apply {
                setCommentOnlyMode(!viewModel.isCommentOnlyMode)
            }
        }
        // バッググラウンド再生。
        fragment_comment_fragment_menu_background_button.setOnClickListener {
            commentFragment.apply {
                startBackgroundPlay()
                exoPlayer.stop()
                live_framelayout.visibility = View.GONE
            }
        }

        //ポップアップ再生。いつか怒られそう（プレ垢限定要素だし）
        fragment_comment_fragment_menu_popup_button.setOnClickListener {
            commentFragment.apply {
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        !Settings.canDrawOverlays(context)
                    } else {
                        false
                    }
                ) {
                    // 上に重ねる権限無いとき。取りに行く
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
                    startActivity(intent)
                } else {
                    //ポップアップ再生。コメント付き
                    startPopupPlay()
                    exoPlayer.stop()
                    live_framelayout.visibility = View.GONE
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

        // （ホーム画面に追加のやつ）
        fragment_comment_fragment_menu_dymanic_shortcut_button.setOnClickListener {
            createPinnedShortcut()
        }

        //匿名非表示
        fragment_comment_fragment_menu_iyayo_hidden_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.isTokumeiHide = isChecked
        }

        // 低遅延
        fragment_comment_fragment_menu_low_latency_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean("nicolive_low_latency", isChecked) }
            viewModel.nicoLiveHTML.sendLowLatency(isChecked)
        }

        // コメント一行モード on/off
        fragment_comment_fragment_menu_comment_setting_hidden_id_swtich.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_id_hidden", isChecked)
                apply()
            }
        }

        // ユーザーID非表示モード
        fragment_comment_fragment_menu_setting_one_line_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_one_line", isChecked)
                apply()
            }
        }

        // コマンド保持モード
        fragment_comment_fragment_menu_command_save_switch.setOnCheckedChangeListener { compoundButton, b ->
            prefSetting.edit {
                putBoolean("setting_command_save", b)
            }
        }

        // ノッチ領域に侵略する
        fragment_comment_fragment_menu_display_cutout_info_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_display_cutout", isChecked)
            }
            activity?.runOnUiThread {
                commentFragment.hideStatusBarAndSetFullScreen()
            }
        }

        // ニコ生ゲーム
        fragment_comment_fragment_nico_nama_game_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            commentFragment.apply {
                if (isChecked) {
                    setNicoNamaGame()
                } else {
                    removeNicoNamaGame()
                }
            }
        }

        // ニコ生ゲーム（生放送・コメントもWebViewで利用する）
        fragment_comment_fragment_nico_nama_game_webview_player_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            commentFragment.apply {
                if (isChecked) {
                    exoPlayer.volume = 0F
                    setNicoNamaGame(isChecked)
                } else {
                    exoPlayer.volume = 1F
                    removeNicoNamaGame()
                }
            }
        }

        fragment_comment_fragment_volume_seek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                commentFragment.apply {
                    exoPlayer.volume = progress / 10F
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        // コテハン一覧画面
        fragment_comment_fragment_menu_kotehan_button.setOnClickListener {
            val intent = Intent(context, KotehanListActivity::class.java)
            startActivity(intent)
        }

    }

    /**
     * 「ホーム画面に追加」のやつ。
     * Android 8以降で利用できる。
     * */
    private fun createPinnedShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 7.1 以降のみ対応
            val shortcutManager = context?.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            if (shortcutManager.isRequestPinShortcutSupported) {
                // サポート済みのランチャーだった
                viewModel.nicoJKGetFlv.value?.apply {
                    val shortcut = ShortcutInfo.Builder(context, commentFragment.liveId).apply {
                        setShortLabel(channelName)
                        setLongLabel(channelName)
                        setIcon(Icon.createWithResource(context, R.drawable.ic_flash_on_24px))
                        val intent = Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                            putExtra("liveId", commentFragment.liveId)
                            putExtra("is_jk", commentFragment.isJK)
                            putExtra("watch_mode", "comment_post")
                        }
                        setIntent(intent)
                    }.build()
                    shortcutManager.requestPinShortcut(shortcut, null)
                }
            }
        }
    }

    // CommentFragmentのViewModelから値をもらう
    private fun setValue() {
        //コメント非表示
        fragment_comment_fragment_menu_comment_hidden_switch.isChecked = commentFragment.isCommentHide
        //Infoコメント非表示
        fragment_comment_fragment_menu_hide_info_perm_switch.isChecked = commentFragment.hideInfoUnnkome
        //匿名で投稿するか
        fragment_comment_fragment_menu_iyayo_comment_switch.isChecked = viewModel.nicoLiveHTML.isPostTokumeiComment
        //匿名コメントを非表示にするか
        fragment_comment_fragment_menu_iyayo_hidden_switch.isChecked = viewModel.isTokumeiHide
        //低遅延モードの有効無効
        fragment_comment_fragment_menu_low_latency_switch.isChecked = prefSetting.getBoolean("nicolive_low_latency", true)
        // コメント一行もーど
        fragment_comment_fragment_menu_comment_setting_hidden_id_swtich.isChecked = prefSetting.getBoolean("setting_id_hidden", false)
        // コマンド保持モード
        fragment_comment_fragment_menu_command_save_switch.isChecked = prefSetting.getBoolean("setting_command_save", false)
        // ユーザーID非表示モード
        fragment_comment_fragment_menu_setting_one_line_switch.isChecked = prefSetting.getBoolean("setting_one_line", false)
        //音量
        fragment_comment_fragment_volume_seek.progress = (commentFragment.exoPlayer.volume * 10).toInt()
        //ニコ生ゲーム有効時
        fragment_comment_fragment_nico_nama_game_switch.isChecked = commentFragment.isAddedNicoNamaGame
        // ノッチ領域に侵略
        fragment_comment_fragment_menu_display_cutout_info_switch.isChecked = prefSetting.getBoolean("setting_display_cutout", false)
    }

    //CommentFragmentへ値を渡す
    private fun setCommentFragmentValue() {
        //押したらすぐ反映できるように
        fragment_comment_fragment_menu_comment_hidden_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            //コメント非表示
            commentFragment.isCommentHide = isChecked
        }
        fragment_comment_fragment_menu_hide_info_perm_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            //Infoコメント非表示
            commentFragment.hideInfoUnnkome = isChecked
        }
        fragment_comment_fragment_menu_iyayo_comment_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 匿名で投稿するかどうか。
            viewModel.nicoLiveHTML.isPostTokumeiComment = isChecked
            prefSetting.edit { putBoolean("nicolive_post_tokumei", isChecked) }
        }
    }

    // ニコニコ実況ならtrue
    fun isNicoJK(): Boolean {
        return commentFragment.isJK
    }

}