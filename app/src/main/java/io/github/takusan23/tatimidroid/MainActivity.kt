package io.github.takusan23.tatimidroid

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import io.github.takusan23.searchpreferencefragment.SearchPreferenceChildFragment
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment
import io.github.takusan23.tatimidroid.activity.SettingActivity
import io.github.takusan23.tatimidroid.bottomfragment.NicoHistoryBottomFragment
import io.github.takusan23.tatimidroid.compose.MainActivityIDInput
import io.github.takusan23.tatimidroid.compose.MainActivityNavigation
import io.github.takusan23.tatimidroid.databinding.ActivityMainBinding
import io.github.takusan23.tatimidroid.fragment.SettingsFragment
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveHTML
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicolive.CommentFragment
import io.github.takusan23.tatimidroid.nicolive.ProgramListFragment
import io.github.takusan23.tatimidroid.nicolive.compose.JCNicoLiveCommentOnlyFragment
import io.github.takusan23.tatimidroid.nicolive.compose.JCNicoLiveFragment
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveProgramListScreen
import io.github.takusan23.tatimidroid.nicolive.compose.screen.NicoLivePlayerScreen
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveViewModelFactory
import io.github.takusan23.tatimidroid.nicologin.LoginFragment
import io.github.takusan23.tatimidroid.nicologin.TwoFactorAuthLoginActivity
import io.github.takusan23.tatimidroid.nicologin.compose.NicoLoginScreen
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoSelectFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.*
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoCacheListScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoPlayerScreen
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoCacheFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoViewModelFactory
import io.github.takusan23.tatimidroid.service.startLivePlayService
import io.github.takusan23.tatimidroid.service.startVideoPlayService
import io.github.takusan23.tatimidroid.tool.*
import kotlinx.coroutines.*
import java.util.*


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
 * ■ 生放送、動画再生画面を起動する方法
 *
 * putExtra()の第一引数に「liveId」か「videoId」をつけ、第二引数には各IDをセットすることで起動できます。
 *
 * その他の値もIntentに入れてくれれば、Fragmentに詰めて設置します。
 *
 * 他アプリから たちみどろいど で きしめん を再生する例
 *
 * ```kotlin
 * val intent = Intent()
 * intent.setClassName("io.github.takusan23.tatimidroid", "io.github.takusan23.tatimidroid.MainActivity")
 * intent.putExtra("videoId", "sm157")
 * startActivity(intent)
 * ```
 *
 * */
class MainActivity : AppCompatActivity() {

    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    /** findViewById駆逐 */
    val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

    @ExperimentalAnimationApi
    @ExperimentalComposeUiApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // val darkModeSupport = DarkModeSupport(this)
        // darkModeSupport.setMainActivityTheme(this)

        supportActionBar?.hide()

        val font = CustomFont(this)

        // Jetpack Compose
        setContent {
            MaterialTheme(
                colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                typography = if (font.typeface != null) Typography(defaultFontFamily = FontFamily(font.typeface!!)) else Typography()
            ) {
                // ContentId
                val contentId = remember { mutableStateOf("") }
                // ナビゲーション
                val navController = rememberNavController()
                // プレイヤーナビゲーション
                val playerNavController = rememberNavController()
                Scaffold(
                    topBar = { MainActivityIDInput(onClickHistoryButton = {}, onClickPlayButton = {}) }, // ID入力欄
                    bottomBar = {
                        MainActivityNavigation { route ->
                            if (route == "setting") {
                                startActivity(Intent(this@MainActivity, SettingActivity::class.java))
                            } else {
                                navController.navigate(route)
                            }
                        }
                    },
                    content = {
                        // 画面切り替え
                        NavHost(
                            navController = navController,
                            startDestination = "cache",
                        ) {
                            composable("nicolive") { NicoLiveProgramListScreen(onMenuClick = {}, onProgramClick = { playerNavController.navigate("nicolive/${it.programId}") }) }
                            composable("nicovideo") { NicoVideoListScreen(application = application, onMenuClick = {}, onVideoClick = { playerNavController.navigate("nicovideo/${it.videoId}") }) }
                            composable("cache") { NicoVideoCacheListScreen(viewModel = viewModel(), onMenuClick = { }, onVideoClick = { playerNavController.navigate("nicovideo/${it.videoId}") }) }
                            composable("login") { NicoLoginScreen(viewModel = viewModel(), onTwoFactorLogin = { nicoLoginDataClass -> startTwoFactorAuthActivity(nicoLoginDataClass) }) }
                        }
                    }
                )

                // プレイヤー部分
                NavHost(navController = playerNavController, startDestination = "empty") {
                    composable("empty") {
                        // Navigation、初期値が必要らしいので適当に透明なUIでも
                        Box(
                            modifier = Modifier
                            // .background(Color.Red.copy(0.2f))
                            // .fillMaxSize()
                        )
                    }
                    composable("nicovideo/{id}") { entry ->
                        val videoId = entry.arguments?.getString("id")
                        NicoVideoPlayerScreen(
                            nicoVideoViewModel = viewModel(
                                factory = NicoVideoViewModelFactory(
                                    application = application,
                                    videoId = videoId,
                                    isCache = false,
                                    isEco = false,
                                    useInternet = false,
                                    startFullScreen = false,
                                    videoList = null,
                                    startPos = null
                                )
                            ),
                            onDestroy = { playerNavController.popBackStack("empty", false) },
                        )
                    }
                    composable("nicolive/{id}") { entry ->
                        val liveId = entry.arguments?.getString("id")!!
                        NicoLivePlayerScreen(
                            viewModel = viewModel(
                                factory = NicoLiveViewModelFactory(
                                    application = application,
                                    liveId = liveId,
                                    isLoginMode = true,
                                )
                            ),
                            onDestroy = { playerNavController.popBackStack("empty", false) }
                        )
                    }
                }

            }
        }
        return

        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.activityMainToolBar)

        /**
         * 共有から起動した
         *
         * ただし、画面回転した場合は動かさない
         * */
        if (savedInstanceState == null) {
            // 共有から起動
            lunchShareIntent()
            // ブラウザから起動
            launchBrowser()
            // ホーム画面に追加から起動した際
            launchPlayer()
        }


        // 新しいUIの説明表示
        initNewUIDescription()

        // 生放送・動画ID初期化
        initIDInput()

        // 履歴ボタン・接続ボタン等初期化
        initButton()

        // クラッシュレポート保存など
        initCrashReportGenerator()

        // 画面切り替え
        viewBinding.mainActivityBottomNavigationView.setOnNavigationItemSelectedListener { item ->
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
                viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_cache
            } else {
                // 起動時の画面
                val launchFragmentName = prefSetting.getString("setting_launch_fragment", "live") ?: "live"
                // selectedItemIdでsetOnNavigationItemSelectedListener{}呼ばれるって。はよいえ
                when (launchFragmentName) {
                    "live" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_community
                    "video" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_nicovideo
                    "cache" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_cache
                    "jk" -> setFragment(ProgramListFragment().apply { arguments = Bundle().apply { putInt("fragment", R.id.nicolive_program_list_menu_nicolive_jk) } })
                }
            }
            // App Shortcutから起動
            when (intent?.getStringExtra("app_shortcut")) {
                "nicolive" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_community
                "nicovideo" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_nicovideo
                "cache" -> viewBinding.mainActivityBottomNavigationView.selectedItemId = R.id.menu_cache
                "jk" -> setFragment(ProgramListFragment().apply { arguments = Bundle().apply { putInt("fragment", R.id.nicolive_program_list_menu_nicolive_jk) } })
            }
        }

    }

    /** 二段階認証開始画面へ遷移する */
    private fun startTwoFactorAuthActivity(nicoLoginDataClass: NicoLoginDataClass) {
        // 二段階認証画面へ飛ばす
        val twoFactorAuthLoginActivity = Intent(this@MainActivity, TwoFactorAuthLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("login", nicoLoginDataClass)
        }
        startActivity(twoFactorAuthLoginActivity)
    }

    /** クラッシュレポートを回収する */
    private fun initCrashReportGenerator() {
        if (CrashReportGenerator.isEnableSaveCrashReport(this)) {
            CrashReportGenerator.initCrashReportGenerator(this)
        }
    }

    /** ブラウザから起動 */
    private fun launchBrowser() {
        val url = intent.data?.toString()
        if (url != null) {
            launchIdRegexPlay(url)
        }
    }

    /**
     * MainActivityのIntentに情報を詰めることにより、[setPlayer]を代わりに設置する関数
     *
     * ただし、画面回転後に呼んではいけない
     * */
    private fun launchPlayer() {
        intent?.apply {
            val liveId = getStringExtra("liveId")
            val videoId = getStringExtra("videoId")
            if (!liveId.isNullOrEmpty()) {
                // 生放送 か 実況
                setNicoliveFragment(liveId)
            }
            if (!videoId.isNullOrEmpty()) {
                // 動画
                val startPos = getIntExtra("start_pos", 0)
                setNicovideoFragment(videoId = videoId, startPos = startPos)
            }
        }
    }

    /**
     * ニコ動の再生Fragmentを置く関数
     *
     * @param videoId 動画ID、sm157とか。きしめええええええええええん
     * @param isCache キャッシュ再生ならtrue
     * @param useInternet キャッシュが有っても強制的にインターネットから取得する場合はtrue
     * @param isEco エコノミーで取得する場合はtrue
     * @param startFullScreen 全画面で再生する場合はtrue
     * @param startPos
     * @param _videoList 連続再生が決定している場合は[NicoVideoData]の配列を入れてください。なおFragment生成後でも連続再生が可能です。
     * */
    fun setNicovideoFragment(videoId: String, isCache: Boolean? = null, isEco: Boolean? = null, useInternet: Boolean? = null, startFullScreen: Boolean? = null, _videoList: ArrayList<NicoVideoData>? = null, startPos: Int? = null) {
        println(videoId)
        val fragment: Fragment = when {
            prefSetting.getBoolean("setting_nicovideo_comment_only", false) -> JCNicoVideoCommentOnlyFragment()// コメントのみ表示
            prefSetting.getBoolean("setting_nicovideo_use_old_ui", true) -> NicoVideoFragment() // 旧UI。JetpackCompose、 Android 7 以前で表示が乱れる
            else -> JCNicoVideoFragment() // Jetpack Compose 利用版
        }
        fragment.apply {
            arguments = Bundle().apply {
                putString("id", videoId)
                isCache?.let { putBoolean("cache", it) }
                isEco?.let { putBoolean("eco", it) }
                useInternet?.let { putBoolean("internet", it) }
                startFullScreen?.let { putBoolean("fullscreen", it) }
                _videoList?.let { putSerializable("video_list", _videoList) }
                startPos?.let { putInt("start_pos", it) }
            }
        }
        if (fragment is JCNicoVideoCommentOnlyFragment) {
            setFragment(fragment, "comment_list")
        } else {
            setPlayer(fragment, videoId)
        }
    }

    /**
     * ニコ生の再生Fragmentを置く関数。設定を呼んでJetpack Compose版と分岐させます。
     *
     * ちなみに (requireActivity() as? MainActivity)でnull許容な形にしている理由ですが、MainActivity以外で落ちないようにするだけ。多分意味ない！
     *
     * @param liveId 生放送ID
     * @param isOfficial 公式どうか。わかるなら入れてくれ
     * @param isCommentOnly コメント一覧のみを表示するやつ。コメントビューアー的な
     * @param watchMode 視聴モード。以下のどれか。将来的には comment_post だけにしたい。
     * - comment_viewer (コメント投稿無し)
     * - comment_post (デフォルト)
     * - nicocas (コメント投稿にnicocasAPIを利用。てかこれまだ使えるの？)
     * */
    fun setNicoliveFragment(liveId: String, isOfficial: Boolean? = null, isCommentOnly: Boolean = false) {
        val fragment: Fragment = when {
            isCommentOnly -> JCNicoLiveCommentOnlyFragment() // コメントのみ表示
            prefSetting.getBoolean("setting_nicovideo_use_old_ui", true) -> CommentFragment() // 旧UI。JetpackCompose、 Android 7 以前で表示が乱れる
            else -> JCNicoLiveFragment() // Jetpack Compose 利用版
        }
        fragment.apply {
            arguments = Bundle().apply {
                putString("liveId", liveId)
                isOfficial?.let { putBoolean("isOfficial", it) }
            }
        }
        setPlayer(fragment, liveId)
    }

    /**
     * 生放送Fragment[io.github.takusan23.tatimidroid.nicolive.CommentFragment]、または動画Fragment[io.github.takusan23.tatimidroid.nicovideo.NicoVideoFragment]を置くための関数
     * もしMainActivityがない場合は(Service等)、MainActivityのIntentにデータを詰めて(liveId/id)起動することで開けます。
     * @param fragment 生放送Fragment か 動画Fragment。[MainActivityPlayerFragmentInterface]を実装してほしい
     * @param tag Fragmentを探すときのタグ。いまんところこのタグでFragmentを探してるコードはないはず
     * */
    fun setPlayer(fragment: Fragment, tag: String) {
        // とりまFragment終了させる
        val findFragment = supportFragmentManager.findFragmentById(R.id.main_activity_fragment_layout)
        if (findFragment != null) {
            supportFragmentManager
                .beginTransaction()
                .remove(findFragment)
                .commit()
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_activity_fragment_layout, fragment, tag)
            .commit()
    }

    /**
     * Fragmentを置く。戻るキーに対応させる場合は第二引数にnull以外を入れてね
     *
     * @param fragment おくFragment
     * @param backstack null以外を入れるとFragmentを積み上げます（履歴に入れる。戻るキー対応）
     * @param forceAdd 同じFragmentでも強制的に置き換える場合はtrue
     * */
    fun setFragment(fragment: Fragment, backstack: String? = null, forceAdd: Boolean = false) {
        // 同じFragmentの場合はやらない（例：生放送開いてるのにもう一回生放送開いたときは何もしない）
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_activity_linearlayout)
        if (currentFragment != null && currentFragment.javaClass == fragment.javaClass) {
            if (!forceAdd) {
                // Fragmentはすでに設置済みなので
                return
            }
        }
        // Fragmentセット
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.main_activity_linearlayout, fragment)
            if (backstack != null) {
                addToBackStack(backstack)
            }
            commit()
        }

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
            if (viewBinding.activityMainContentIdEditText.text.toString().isEmpty()) {
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
                viewBinding.activityMainContentIdEditText.setText(idRegex)
            }
        }
    }

    // 新UIの説明出すボタン。「？」ボタン
    private fun initNewUIDescription() {
        viewBinding.mainActivityShowNewUiDescriptionImageView.setOnClickListener {
            viewBinding.activityMainNewUiTextLinearLayout.isVisible = !viewBinding.activityMainNewUiTextLinearLayout.isVisible
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
        //ログイン情報があるかどうか
        if (prefSetting.getString("mail", "")?.isNotEmpty() == true || isNotLoginMode(this)) {
            setFragment(NicoVideoSelectFragment())
        } else {
            // ログイン画面へ切り替える
            setFragment(LoginFragment())
            Toast.makeText(this, getString(R.string.mail_pass_error), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 共有から起動した場合
     *
     * ただし、画面回転のときは呼んではいけません。
     * */
    private fun lunchShareIntent() {
        if (Intent.ACTION_SEND == intent.action) {
            val extras = intent.extras
            // URL
            val url = extras?.getCharSequence(Intent.EXTRA_TEXT) ?: ""
            // 正規表現で取り出す
            launchIdRegexPlay(url.toString())
        }
    }

    /**
     * 生放送、番組ID入力画面
     * */
    private fun initIDInput() {
        // Enter押したら再生する
        viewBinding.activityMainContentIdEditText.setOnKeyListener { v, keyCode, event ->
            // 二回呼ばれる対策
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                launchIdRegexPlay(viewBinding.activityMainContentIdEditText.text.toString())
            }
            false
        }
    }

    // ボタン初期化
    private fun initButton() {
        viewBinding.activityMainNimadoButton.setOnClickListener {
            val intent = Intent(this, NimadoActivity::class.java)
            startActivity(intent)
        }
        viewBinding.activityMainHistoryButton.setOnClickListener {
            // 履歴ボタン
            val nicoHistoryBottomFragment = NicoHistoryBottomFragment()
            nicoHistoryBottomFragment.editText = viewBinding.activityMainContentIdEditText
            nicoHistoryBottomFragment.show(supportFragmentManager, "history")
        }
        viewBinding.activityMainConnectButton.setOnClickListener {
            // 画面切り替え
            launchIdRegexPlay(viewBinding.activityMainContentIdEditText.text.toString())
        }
    }

    /**
     * 正規表現でIDを見つけて再生画面を表示させる。
     * @param text IDが含まれている文字列。
     * */
    private fun launchIdRegexPlay(text: String) {
        // 正規表現
        val nicoIDMatcher = NICOLIVE_ID_REGEX.toPattern().matcher(text)
        val communityIDMatcher = NICOCOMMUNITY_ID_REGEX.toPattern().matcher(text)
        val nicoVideoIdMatcher = NICOVIDEO_ID_REGEX.toPattern().matcher(text)
        when {
            nicoIDMatcher.find() -> {
                // 生放送ID
                val liveId = nicoIDMatcher.group()
                if (hasMailPass(this)) {
                    setNicoliveFragment(liveId)
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
                        setNicoliveFragment(nicoLiveHTML.liveId)
                    }
                }
            }
            nicoVideoIdMatcher.find() -> {
                // 動画ID
                val videoId = nicoVideoIdMatcher.group()
                // URLから再生時間を出せる場合
                val from = text.toUri().getQueryParameter("from")?.toInt()
                setNicovideoFragment(videoId = videoId, startPos = from)
            }
            else -> {
                Toast.makeText(this, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * [MainActivity]のBottomNavigationを非表示にする関数
     *
     * 表示するかどうかは、表示中Fragmentの[MainActivityPlayerFragmentInterface.isMiniPlayerMode]の返り値で判断してます。
     * */
    fun setVisibilityBottomNav() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_activity_fragment_layout)
        viewBinding.mainActivityBottomNavigationView.isVisible = (fragment as? MainActivityPlayerFragmentInterface)?.isMiniPlayerMode() ?: true
    }

    /**
     * BottomNavigationのサイズ変更
     * @param argHeight 高さ。0を入れると戻る
     * */
    fun setBottomNavigationHeight(argHeight: Int) {
        viewBinding.mainActivityBottomNavigationView.updateLayoutParams {
            height = argHeight
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                // 生放送
                is CommentFragment -> {
                    when {
                        isLeaveAppPopup -> startPopupPlay()
                        isLeaveAppBackground -> startBackgroundPlay()
                    }
                }
                is JCNicoLiveFragment -> {
                    viewModel.nicoLiveProgramData.value?.apply {
                        when {
                            isLeaveAppPopup -> startLivePlayService(context = context, mode = "popup", liveId = programId, isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment, startQuality = viewModel.currentQuality)
                            isLeaveAppBackground -> startLivePlayService(context = context, mode = "background", liveId = programId, isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment, startQuality = viewModel.currentQuality)
                        }
                    }
                }
                // 動画
                is NicoVideoFragment -> {
                    viewModel.nicoVideoData.value?.apply {
                        when {
                            isLeaveAppPopup -> startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality, seek = viewModel.currentPosition)
                            isLeaveAppBackground -> startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality, seek = viewModel.currentPosition)
                        }
                    }
                }
                is JCNicoVideoFragment -> {
                    viewModel.nicoVideoData.value?.apply {
                        when {
                            isLeaveAppPopup -> startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality, seek = viewModel.currentPosition)
                            isLeaveAppBackground -> startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality, seek = viewModel.currentPosition)
                        }
                    }
                }
            }
        }
    }
}