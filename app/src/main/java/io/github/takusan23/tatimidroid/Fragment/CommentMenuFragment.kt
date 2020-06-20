package io.github.takusan23.tatimidroid.Fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.content.res.Configuration
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
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.ProgramShare
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_comment_menu.*


/*
* ここをメンテしにきた私へ
* CommentMenuBottomFragment と このクラスは違うよ。命名雑だった。ごめん
* CommentFragmentのメニューはここ。
* コメントを押した時にできる（ロックオン、コテハン登録）なんかは CommentLockonBottomFragment へどうぞ
* */

class CommentMenuFragment : Fragment() {

    lateinit var commentFragment: CommentFragment
    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting: SharedPreferences

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
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //CommentFragmentしゅとく～
        liveId = arguments?.getString("liveId") ?: ""
        commentFragment =
            activity?.supportFragmentManager?.findFragmentByTag(liveId) as CommentFragment

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
        //キャスト
        if (commentFragment.isInitGoogleCast()) {
            val googleCast = commentFragment.googleCast
            googleCast.setUpCastButton(fragment_comment_fragment_menu_cast_button)
        }

        // 画質変更
        fragment_comment_fragment_menu_quality_button.setOnClickListener {
            // 画質変更（視聴セッションWebSocket前だと見れない）
            if (commentFragment.isInitQualityChangeBottomSheet()) {
                commentFragment.qualitySelectBottomSheet.show(activity?.supportFragmentManager!!, "quality_bottom")
            }
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
                programShare = ProgramShare(commentActivity, this.live_surface_view, programTitle, liveId)
                // 今いる部屋の名前入れる
                val heyawari = "${roomName} - ${chairNo}"
                programShare.showShareScreen(heyawari)
            }
        }
        //画像つき共有
        fragment_comment_fragment_menu_share_image_attach_button.setOnClickListener {
            commentFragment.apply {
                programShare = ProgramShare(commentActivity, this.live_surface_view, programTitle, liveId)
                // 今いる部屋の名前入れる
                val heyawari = "${roomName} - ${chairNo}"
                programShare.shareAttachImage(heyawari)
            }
        }
        //生放送を再生ボタン
        fragment_comment_fragment_menu_view_live_button.setOnClickListener {
            (activity?.supportFragmentManager?.findFragmentByTag(liveId) as CommentFragment).apply {
                if (live_framelayout.visibility == View.VISIBLE) {
                    live_framelayout.visibility = View.GONE
                    if (!isNicoJK()) {
                        // JK以外はExoPlayerをRelease
                        exoPlayer.stop()
                    }
                } else {
                    live_framelayout.visibility = View.VISIBLE
                    setPlayVideoView()
                }
            }
        }
        //バッググラウンド再生。調子悪いのでServiceなんかで実装し直したほうがいいと思ってるけどまず使ってないので直さないと思います。
        fragment_comment_fragment_menu_background_button.setOnClickListener {
            commentFragment.apply {
                setBackgroundProgramPlay()
                if (isExoPlayerInitialized()) {
                    exoPlayer.stop()
                    live_framelayout.visibility = View.GONE
                }
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
                    startOverlayPlayer()
                    if (isExoPlayerInitialized()) {
                        exoPlayer.stop()
                        live_framelayout.visibility = View.GONE
                    }
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
            commentFragment.isTokumeiHide = isChecked
        }

        // 低遅延
        fragment_comment_fragment_menu_low_latency_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean("nicolive_low_latency", isChecked) }
            commentFragment.nicoLiveHTML.sendLowLatency(isChecked)
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

        // 常に番組情報（放送時間、来場者数）を表示する
        fragment_comment_fragment_menu_always_program_info_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_always_program_info", isChecked)
            }
            commentFragment.setAlwaysShowProgramInfo()
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

        // 横画面で番組情報を消す
        fragment_comment_fragment_menu_hide_program_info.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_landscape_hide_program_info", isChecked)
            }
            commentFragment.hideProgramInfo()
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
                    if (isExoPlayerInitialized()) exoPlayer.volume = 0F
                    setNicoNamaGame(isChecked)
                } else {
                    if (isExoPlayerInitialized()) exoPlayer.volume = 1F
                    removeNicoNamaGame()
                }
            }
        }

        fragment_comment_fragment_volume_seek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                commentFragment.apply {
                    if (isExoPlayerInitialized()) {
                        exoPlayer.volume = progress / 10F
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

    }

    /**
     * 「ホーム画面に追加」のやつ。
     * Android 8以降で利用できる。
     * */
    private fun createPinnedShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 7.1 以降のみ対応
            val shortcutManager =
                context?.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            if (shortcutManager.isRequestPinShortcutSupported) {
                // サポート済みのランチャーかどうか
                val shortcut = ShortcutInfo.Builder(context, commentFragment.liveId).apply {
                    setShortLabel(commentFragment.getFlvData.channelName)
                    setLongLabel(commentFragment.getFlvData.channelName)
                    setIcon(Icon.createWithResource(context, R.drawable.ic_flash_on_24px))
                    val intent = Intent(context, CommentActivity::class.java).apply {
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

    //CommentFragmentの値を貰う
    private fun setValue() {
        //コメント非表示
        fragment_comment_fragment_menu_comment_hidden_switch.isChecked =
            commentFragment.isCommentHide
        //Infoコメント非表示
        fragment_comment_fragment_menu_hide_info_perm_switch.isChecked =
            commentFragment.hideInfoUnnkome
        //匿名で投稿するか
        fragment_comment_fragment_menu_iyayo_comment_switch.isChecked =
            commentFragment.nicoLiveHTML.isTokumeiComment
        //匿名コメントを非表示にするか
        fragment_comment_fragment_menu_iyayo_hidden_switch.isChecked = commentFragment.isTokumeiHide
        //低遅延モードの有効無効
        fragment_comment_fragment_menu_low_latency_switch.isChecked =
            prefSetting.getBoolean("nicolive_low_latency", true)
        // コメント一行もーど
        fragment_comment_fragment_menu_comment_setting_hidden_id_swtich.isChecked =
            prefSetting.getBoolean("setting_id_hidden", false)
        // ユーザーID非表示モード
        fragment_comment_fragment_menu_setting_one_line_switch.isChecked =
            prefSetting.getBoolean("setting_one_line", false)
        //音量
        commentFragment.apply {
            if (isExoPlayerInitialized()) {
                fragment_comment_fragment_volume_seek.progress = (exoPlayer.volume * 10).toInt()
            }
        }
        //ニコ生ゲーム有効時
        fragment_comment_fragment_nico_nama_game_switch.isChecked =
            commentFragment.isAddedNicoNamaGame
        // 常に番組情報表示
        fragment_comment_fragment_menu_always_program_info_switch.isChecked =
            prefSetting.getBoolean("setting_always_program_info", false)
        // ノッチ領域に侵略
        fragment_comment_fragment_menu_display_cutout_info_switch.isChecked =
            prefSetting.getBoolean("setting_display_cutout", false)
        // 横画面UIで番組情報非表示
        fragment_comment_fragment_menu_hide_program_info.isChecked =
            prefSetting.getBoolean("setting_landscape_hide_program_info", false)
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
            //匿名かどうか。
            commentFragment.nicoLiveHTML.isTokumeiComment = isChecked
            /*when (isChecked) {
                true -> {
                    commentFragment.commentCommand = "184"
                }
                false -> {
                    commentFragment.commentCommand = ""
                }
            }*/
        }
    }

    // ニコニコ実況ならtrue
    fun isNicoJK(): Boolean {
        return commentFragment.isJK
    }

}