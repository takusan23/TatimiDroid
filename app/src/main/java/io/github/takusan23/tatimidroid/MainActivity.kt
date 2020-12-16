package io.github.takusan23.tatimidroid

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.searchpreferencefragment.SearchPreferenceChildFragment
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment
import io.github.takusan23.tatimidroid.BottomFragment.NicoHistoryBottomFragment
import io.github.takusan23.tatimidroid.Fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.Fragment.LoginFragment
import io.github.takusan23.tatimidroid.Fragment.SettingsFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.DialogWatchModeBottomFragment
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.NicoLive.ProgramListFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoSelectFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoCacheFragment
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * アプリ起動時に一番最初に起動するActivity。
 * putExtra()で起動画面（Fragment）を直接指定できます。
 * - app_shortcut / String
 *     - nicolive
 *         - ニコ生
 *     - nicovideo
 *         - ニコ動
 *     - cache
 *         - キャッシュ一覧
 *     - jk
 *         - ニコニコ実況
 *
 * ```kotlin
 * // キャッシュ一覧を直接開く例
 * val intent = Intent(context, MainActivity::class.java)
 * intent.putExtra("app_shortcut", "cache")
 * startActivity(intent)
 * ```
 *
 * 生放送、動画再生画面を起動する方法
 * putExtra()にliveIdかidをつけることで起動できます。
 * その他の値もIntentに入れてくれれば、Fragmentに詰めて設置します。
 *
 * */
class MainActivity : AppCompatActivity() {

    private lateinit var prefSetting: SharedPreferences

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setMainActivityTheme(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //ダークモード対応
        if (isDarkMode(this)) {
            main_activity_bottom_navigationview.backgroundTintList = ColorStateList.valueOf(getThemeColor(darkModeSupport.context))
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(darkModeSupport.context)))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1234)

        // 共有から起動した
        lunchShareIntent()

        // 生放送/動画画面
        launchPlayer()

        // 新しいUIの説明表示
        initNewUIDescription()

        // 生放送・動画ID初期化
        initIDInput()

        // 履歴ボタン・接続ボタン等初期化
        initButton()

        // 画面切り替え
        main_activity_bottom_navigationview.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_login -> {
                    if (isConnectionInternet(this)) {
                        setFragment(LoginFragment())
                    }
                }
                R.id.menu_community -> {
                    if (isConnectionInternet(this)) {
                        showProgramListFragment()
                    }
                }
                R.id.menu_setting -> {
                    // 自作ライブラリ https://github.com/takusan23/SearchPreferenceFragment
                    val searchSettingsFragment = SettingsFragment()
                    searchSettingsFragment.arguments = Bundle().apply {
                        // 階層化されている場合
                        val hashMap = hashMapOf<String, Int>()
                        putSerializable(SearchPreferenceFragment.PREFERENCE_XML_FRAGMENT_NAME_HASH_MAP, hashMap)
                        putInt(SearchPreferenceChildFragment.PREFERENCE_XML_RESOURCE_ID, R.xml.preferences)
                    }
                    setFragment(searchSettingsFragment)
                }
                R.id.menu_nicovideo -> {
                    if (isConnectionInternet(this)) {
                        showVideoListFragment()
                    }
                }
                R.id.menu_cache -> {
                    setFragment(NicoVideoCacheFragment())
                }
            }
            true
        }

        // 画面回転時・・・はsavedInstanceStateがnull以外になる。これないと画面回転時にFragmentを再生成（2個目作成）するはめになりますよ！
        if (savedInstanceState == null) {
            // モバイルデータ接続のときは常にキャッシュ一覧を開くの設定が有効かどうか
            val isMobileDataShowCacheList = prefSetting.getBoolean("setting_mobile_data_show_cache", false)
            if (isMobileDataShowCacheList && isConnectionMobileDataInternet(this)) {
                // キャッシュ表示
                main_activity_bottom_navigationview.selectedItemId = R.id.menu_cache
            } else {
                // 起動時の画面
                val launchFragmentName = prefSetting.getString("setting_launch_fragment", "live") ?: "live"
                // selectedItemIdでsetOnNavigationItemSelectedListener{}呼ばれるって。はよいえ
                when (launchFragmentName) {
                    "live" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_community
                    "video" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_nicovideo
                    "cache" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_cache
                    "jk" -> setFragment(ProgramListFragment().apply { arguments = Bundle().apply { putInt("fragment", R.id.nicolive_program_list_menu_nicolive_jk) } })
                }
            }
            // App Shortcutから起動
            when (intent?.getStringExtra("app_shortcut")) {
                "nicolive" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_community
                "nicovideo" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_nicovideo
                "cache" -> main_activity_bottom_navigationview.selectedItemId = R.id.menu_cache
                "jk" -> setFragment(ProgramListFragment().apply { arguments = Bundle().apply { putInt("fragment", R.id.nicolive_program_list_menu_nicolive_jk) } })
            }
        }

    }

    /** MainActivityのIntentに情報を詰めることにより、[setPlayer]を代わりに設置する関数 */
    private fun launchPlayer() {
        intent?.apply {
            val liveId = getStringExtra("liveId")
            val videoId = getStringExtra("id")
            if (!liveId.isNullOrEmpty()) {
                // 生放送 か 実況
                val commentFragment = CommentFragment().apply {
                    arguments = intent.extras
                }
                setPlayer(commentFragment, liveId)
            }
            if (!videoId.isNullOrEmpty()) {
                // 動画
                val videoFragment = NicoVideoFragment().apply {
                    arguments = intent.extras
                }
                setPlayer(videoFragment, videoId)
            }
        }
    }

    /**
     * 生放送Fragment[io.github.takusan23.tatimidroid.NicoLive.CommentFragment]、または動画Fragment[io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment]を置くための関数
     * もしMainActivityがない場合は(Service等)、MainActivityのIntentにデータを詰めて(liveId/id)起動することで開けます。
     * @param fragment 生放送Fragment か 動画Fragment。[MainActivityPlayerFragmentInterface]を実装してほしい
     * @param tag Fragmentを探すときのタグ。いまんところこのタグでFragmentを探してるコードはないはず
     * */
    fun setPlayer(fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_activity_fragment_layout, fragment, tag)
            .commit()
    }

    private fun setFragment(fragment: Fragment) {
        // 同じFragmentの場合はやらない（例：生放送開いてるのにもう一回生放送開いたときは何もしない）
        val findFragment = supportFragmentManager.findFragmentById(R.id.main_activity_linearlayout)
        if (findFragment != null && findFragment.javaClass == fragment.javaClass) {
            return // Fragmentはすでに設置済みなので
        }
        // Fragmentセット
        supportFragmentManager.beginTransaction().replace(R.id.main_activity_linearlayout, fragment).commit()
    }

    /**
     * 現在表示中のFragmentを返す
     * */
    fun currentFragment() = supportFragmentManager.findFragmentById(R.id.main_activity_linearlayout)

    // Android 10からアプリにフォーカスが当たらないとクリップボードの中身が取れなくなったため
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!prefSetting.getBoolean("setting_deprecated_clipbord_get_id", false)) {
            //クリップボードに番組IDが含まれてればテキストボックスに自動で入れる
            if (activity_main_liveid_edittext.text.toString().isEmpty()) {
                setClipBoardProgramID()
            }
        }
    }

    // クリップボードの中身取得
    private fun setClipBoardProgramID() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        if (clipdata?.getItemAt(0)?.text != null) {
            val clipboardText = clipdata.getItemAt(0).text
            val idRegex = IDRegex(clipboardText.toString())
            if (idRegex != null) {
                // IDを入れるEditTextに正規表現の結果を入れる
                activity_main_liveid_edittext.setText(idRegex)
            }
        }
    }

    // 新UIの説明出すボタン。「？」ボタン
    private fun initNewUIDescription() {
        main_activity_show_new_ui_description.setOnClickListener {
            activity_main_new_ui_text.apply {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                }
            }
        }
    }

    // 番組一覧
    private fun showProgramListFragment() {
        //ログイン情報があるかどうか
        if (prefSetting.getString("mail", "")?.isNotEmpty() == true) {
            setFragment(ProgramListFragment())
        } else {
            // ログイン画面へ切り替える
            setFragment(LoginFragment())
            Toast.makeText(this, getString(R.string.mail_pass_error), Toast.LENGTH_SHORT).show()
        }
    }

    // 動画一覧
    private fun showVideoListFragment() {
        if (prefSetting.getString("mail", "")?.isNotEmpty() == true || isNotLoginMode(this)) {
            // ニコニコ動画
            setFragment(NicoVideoSelectFragment())
            // setPage(MainActivityFragmentStateViewAdapter.MAIN_ACTIVITY_VIEWPAGER2_NICOVIDEO)
            //タイトル
            supportActionBar?.title = getString(R.string.nicovideo)
        } else {
            // ログイン画面へ切り替える か　ログインしないモードを有効にするか
            val dialogButtonList = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.nicovideo_init_not_login), R.drawable.ic_lock_open_black_24dp))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.nicovideo_init_login), R.drawable.ic_lock_outline_black_24dp))
            }
            DialogBottomSheet(getString(R.string.nicovideo_init_message), dialogButtonList) { i, bottomSheetDialogFragment ->
                when (i) {
                    0 -> {
                        // 「ログイン無しで利用する」と「起動時の画面を動画にする」設定有効
                        prefSetting.edit { putBoolean("setting_no_login", true) }
                        prefSetting.edit { putString("setting_launch_fragment", "video") }
                        showVideoListFragment()
                    }
                    1 -> {
                        // ログインする
                        main_activity_bottom_navigationview.selectedItemId = R.id.menu_login
                        setFragment(LoginFragment())
                        // setPage(MainActivityFragmentStateViewAdapter.MAIN_ACTIVITY_VIEWPAGER2_LOGIN)
                        //メアド設定してね！
                        Toast.makeText(this, getString(R.string.login_or_is_not_login_mode), Toast.LENGTH_SHORT).show()
                    }
                }
            }.show(supportFragmentManager, "nicovideo_init")
        }
    }

    //共有から起動した場合
    private fun lunchShareIntent() {
        if (Intent.ACTION_SEND.equals(intent.action)) {
            val extras = intent.extras
            // URL
            val url = extras?.getCharSequence(Intent.EXTRA_TEXT) ?: ""
            // 正規表現で取り出す
            idRegexLaunchPlay(url.toString())
        }
    }

    /**
     * 生放送、番組ID入力画面
     * */
    private fun initIDInput() {
        activity_main_liveid_edittext.addTextChangedListener {
            // 将来なにか書くかも
        }
        // Enter押したら再生する
        activity_main_liveid_edittext.setOnKeyListener { v, keyCode, event ->
            // 二回呼ばれる対策
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                idRegexLaunchPlay(activity_main_liveid_edittext.text.toString())
            }
            false
        }
    }

    // ボタン初期化
    private fun initButton() {
        activity_main_nimado_button.setOnClickListener {
            val intent = Intent(this, NimadoActivity::class.java)
            startActivity(intent)
        }
        activity_main_history_button.setOnClickListener {
            // 履歴ボタン
            val nicoHistoryBottomFragment = NicoHistoryBottomFragment()
            nicoHistoryBottomFragment.editText = activity_main_liveid_edittext
            nicoHistoryBottomFragment.show(supportFragmentManager, "history")
        }
        activity_main_connect_button.setOnClickListener {
            // 画面切り替え
            idRegexLaunchPlay(activity_main_liveid_edittext.text.toString())
        }
    }

    /**
     * 正規表現でIDを見つけて再生画面を表示させる。
     * @param text IDが含まれている文字列。
     * */
    private fun idRegexLaunchPlay(text: String) {
        // 正規表現
        val nicoIDMatcher = NICOLIVE_ID_REGEX.toPattern().matcher(text)
        val communityIDMatcher = NICOCOMMUNITY_ID_REGEX.toPattern().matcher(text)
        val nicoVideoIdMatcher = NICOVIDEO_ID_REGEX.toPattern().matcher(text)
        when {
            nicoIDMatcher.find() -> {
                // 生放送ID
                val liveId = nicoIDMatcher.group()
                if (hasMailPass(this)) {
                    //ダイアログ
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    val dialog = DialogWatchModeBottomFragment()
                    dialog.arguments = bundle
                    dialog.show(supportFragmentManager, "watchmode")
                }
            }
            communityIDMatcher.find() -> {
                // コミュID
                val communityId = communityIDMatcher.group()
                if (hasMailPass(this)) {
                    // エラー時
                    val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                        showToast("${getString(R.string.error)}\n${throwable}")
                    }
                    lifecycleScope.launch(errorHandler) {
                        // コミュID->生放送ID
                        val nicoLiveHTML = NicoLiveHTML()
                        val response = nicoLiveHTML.getNicoLiveHTML(communityId, prefSetting.getString("user_session", ""), false)
                        val responseString = withContext(Dispatchers.Default) {
                            response.body?.string()
                        }
                        if (!response.isSuccessful) {
                            // 失敗時
                            showToast("${getString(R.string.error)}\n${response.code}")
                            return@launch
                        }
                        // パース
                        nicoLiveHTML.initNicoLiveData(nicoLiveHTML.nicoLiveHTMLtoJSONObject(responseString))
                        //ダイアログ
                        val bundle = Bundle()
                        bundle.putString("liveId", nicoLiveHTML.liveId)
                        val dialog = DialogWatchModeBottomFragment()
                        dialog.arguments = bundle
                        dialog.show(supportFragmentManager, "watchmode")
                    }
                }
            }
            nicoVideoIdMatcher.find() -> {
                // 動画ID
                val videoId = nicoVideoIdMatcher.group()
                val nicoVideoFragment = NicoVideoFragment()
                val bundle = Bundle()
                bundle.putString("id", videoId)
                bundle.putBoolean("cache", false)
                nicoVideoFragment.arguments = bundle
                setPlayer(nicoVideoFragment, videoId)
            }
            else -> {
                Toast.makeText(this, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * [MainActivity]のBottomNavigationを非表示にする関数
     * [onPause]と[onResume]にそれぞれ呼ぶといい感じ？
     * */
    fun setVisibilityBottomNav(isVisible: Boolean) {
        main_activity_bottom_navigationview.isVisible = isVisible
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** 戻る押した時、MainActivityにおいたFragmentへ通知を飛ばす */
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_activity_fragment_layout)
        // もしFragmentが見つからなかった場合はActivityを終了させる
        if (fragment == null) super.onBackPressed()
        (fragment as? MainActivityPlayerFragmentInterface)?.apply {
            onBackButtonPress() // 戻るキー押した関数を呼ぶ
        }
    }

    /** アプリを離れたとき */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // アプリを離れたらポップアップ再生
        val isLeaveAppPopup = prefSetting.getBoolean("setting_leave_popup", false)
        // アプリを離れたらバックグラウンド再生
        val isLeaveAppBackground = prefSetting.getBoolean("setting_leave_background", false)
        // Fragment取得
        supportFragmentManager.findFragmentById(R.id.main_activity_fragment_layout).apply {
            when (this) {
                is CommentFragment -> {
                    // 生放送
                    when {
                        isLeaveAppPopup -> startPopupPlay()
                        isLeaveAppBackground -> startBackgroundPlay()
                    }
                }
                is NicoVideoFragment -> {
                    // 動画
                    when {
                        isLeaveAppPopup -> startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
                        isLeaveAppBackground -> startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
                    }
                }
            }
        }
    }

}
