package io.github.takusan23.tatimidroid

import android.app.NotificationManager
import android.content.*
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoSelectFragment
import io.github.takusan23.tatimidroid.Fragment.*
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentPOSTListSQLiteHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setMainActivityTheme(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //ダークモード対応
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            main_activity_bottom_navigationview.backgroundTintList =
                ColorStateList.valueOf(darkModeSupport.getThemeColor())
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1234)

        //共有から起動した
        lunchShareIntent()

        // 新しいUIの説明表示
        initNewUIDescription()

        // 生放送・動画ID初期化
        initIDInput()

        // 履歴ボタン・接続ボタン等初期化
        initButton()

        // デフォ
        val fragmentTransitionSupport = supportFragmentManager.beginTransaction()
        fragmentTransitionSupport.replace(R.id.main_activity_linearlayout, DevNicoVideoCacheFragment(), "cache_fragment")
        fragmentTransitionSupport.commit()

        //画面切り替え
        main_activity_bottom_navigationview.selectedItemId = R.id.menu_cache
        main_activity_bottom_navigationview.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_login -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, LoginFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_community -> {
                    showProgramListFragment()
                }
                R.id.menu_setting -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, SettingsFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_nicovideo -> {
                    showVideoListFragment()
                }
                R.id.menu_cache -> {
                    val fragmentTransitionSupport = supportFragmentManager.beginTransaction()
                    fragmentTransitionSupport.replace(R.id.main_activity_linearlayout, DevNicoVideoCacheFragment())
                    fragmentTransitionSupport.commit()
                }
            }
            true
        }

        // App Shortcutから起動
        when (intent?.getStringExtra("app_shortcut")) {
            "nicolive" -> {
                main_activity_bottom_navigationview.selectedItemId = R.id.menu_community
                showProgramListFragment()
            }
            "nicovideo" -> {
                main_activity_bottom_navigationview.selectedItemId = R.id.menu_nicovideo
                showVideoListFragment()
            }
            "cache" -> {
                main_activity_bottom_navigationview.selectedItemId = R.id.menu_nicovideo
                val fragment = DevNicoVideoSelectFragment()
                val bundle = Bundle().apply {
                    putBoolean("cache", true)
                }
                fragment.arguments = bundle
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.main_activity_linearlayout, fragment)
                fragmentTransaction.commit()
            }
        }

        //データベース移行
        convertCommentPOSTListToCommentCollection()

    }

    // Android 10からアプリにフォーカスが当たらないとクリップボードの中身が取れなくなったため
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 一部の端末（LG製の端末？）で大量にエラーが出ているので。。。LG端末なんて持ってないのでわからん！
        if (!pref_setting.getBoolean("setting_deprecated_clipbord_get_id", false)) {
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
        //ログイン情報がない場合
        if (pref_setting.getString("mail", "")?.isNotEmpty() == true) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.main_activity_linearlayout, ProgramListFragment())
            fragmentTransaction.commit()
        } else {
            Toast.makeText(this, getString(R.string.mail_pass_error), Toast.LENGTH_SHORT).show()
        }
    }

    // 動画一覧
    private fun showVideoListFragment() {
        if (pref_setting.getString("mail", "")?.isNotEmpty() == true || isNotLoginMode(this)) {
            // ニコ動クライアント有効時
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.main_activity_linearlayout, DevNicoVideoSelectFragment())
            fragmentTransaction.commit()
            //タイトル
            supportActionBar?.title = getString(R.string.nicovideo)
        } else {
            //メアド設定してね！
            Toast.makeText(
                this,
                getString(R.string.mail_pass_error),
                Toast.LENGTH_SHORT
            ).show()
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

    //コメント投稿リストからコメントコメントコレクションにデータベース移動
    //移動理由は単純に名前がわかりにくいってだけです。
    fun convertCommentPOSTListToCommentCollection() {
        //コメント投稿リスト
        val commentPOSTListSQLiteHelper = CommentPOSTListSQLiteHelper(this)
        val commentPOSTListSqLiteDatabase = commentPOSTListSQLiteHelper.writableDatabase
        commentPOSTListSQLiteHelper.setWriteAheadLoggingEnabled(false)

        //コメントコレクション
        val commentCollection = CommentCollectionSQLiteHelper(this)
        val commentCollectionSqLiteDatabase = commentCollection.writableDatabase
        commentCollection.setWriteAheadLoggingEnabled(false)

        //コメント投稿リストの内容を読み込む
        val cursor =
            commentPOSTListSqLiteDatabase.query("comment_post_list", arrayOf("comment"), null, null, null, null, null)
        //リストがある・ない
        if (cursor.moveToFirst()) {
            //あるのでコメントコレクションへ移行
            for (i in 0 until cursor.count) {
                val comment = cursor.getString(0)
                val yomi = cursor.getString(0)
                //移行
                val contentValues = ContentValues()
                contentValues.put("comment", comment)
                contentValues.put("yomi", yomi)
                commentCollectionSqLiteDatabase.insert("comment_collection_db", null, contentValues)
                //次へ行こう
                cursor.moveToNext()
            }
            cursor.close()
            //移行完了後、コメント投稿リストの内容を全消去
            commentPOSTListSqLiteDatabase.delete("comment_post_list", null, null)
        } else {
            println("新データベース（コメントコレクション）へ移行済みです。")
        }
    }

    /**
     * 生放送、番組ID入力画面
     * */
    private fun initIDInput() {
        activity_main_liveid_edittext.addTextChangedListener {
            // 将来なにか書くかも
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
                    val dialog = BottomSheetDialogWatchMode()
                    dialog.arguments = bundle
                    dialog.show(supportFragmentManager, "watchmode")
                }
            }
            communityIDMatcher.find() -> {
                // コミュID
                val communityId = communityIDMatcher.group()
                if (hasMailPass(this)) {
                    GlobalScope.launch {
                        // コミュID->生放送ID
                        val nicoLiveHTML = NicoLiveHTML()
                        val response =
                            nicoLiveHTML.getNicoLiveHTML(communityId, pref_setting.getString("user_session", ""), false)
                                .await()
                        if (!response.isSuccessful) {
                            // 失敗時
                            showToast("${getString(R.string.error)}\n${response.code}")
                            return@launch
                        }
                        // パース
                        nicoLiveHTML.initNicoLiveData(nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string()))
                        //ダイアログ
                        val bundle = Bundle()
                        bundle.putString("liveId", nicoLiveHTML.liveId)
                        val dialog = BottomSheetDialogWatchMode()
                        dialog.arguments = bundle
                        dialog.show(supportFragmentManager, "watchmode")
                    }
                }
            }
            nicoVideoIdMatcher.find() -> {
                // 動画ID
                val videoId = nicoVideoIdMatcher.group()
                val intent = Intent(this, NicoVideoActivity::class.java)
                intent.putExtra("id", videoId)
                intent.putExtra("cache", false)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}
