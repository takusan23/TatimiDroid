package io.github.takusan23.tatimidroid.Activity

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Adapter.MenuRecyclerAdapter
import io.github.takusan23.tatimidroid.Adapter.MenuRecyclerAdapterDataClass
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.AutoAdmissionDBEntity
import io.github.takusan23.tatimidroid.Room.Init.AutoAdmissionDBInit
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import kotlinx.android.synthetic.main.activity_kono_app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

/**
 * このアプリについて。
 * */
class KonoApp : AppCompatActivity() {

    /*
    * 作者のTwitter、Mastodonリンク
    * */
    val twitterLink = "https://twitter.com/takusan__23"
    val mastodonLink = "https://best-friends.chat/@takusan_23"
    val source = "https://github.com/takusan23/TatimiDroid"
    val privacy_policy = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    /*
    * バージョンとか
    * */
    val version = "2020/07/14"
    val codeName1 = "（９)" // https://dic.nicovideo.jp/a/ニコニコ動画の変遷

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)
        if (isDarkMode(this)) {
            // バーを暗くする
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(this)))
        }

        setContentView(R.layout.activity_kono_app)

        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName

        title = getString(R.string.kono_app)

        kono_app_codename.text = "$appVersion\n$version\n$codeName1"

        kono_app_app_name_cardview.setOnClickListener {
            runEasterEgg()
        }

        // リンク集展開/非表示など
        kono_app_link_show_button.setOnClickListener { button ->
            kono_app_recyclerview.isVisible = !kono_app_recyclerview.isVisible
            // アイコン設定
            if (kono_app_recyclerview.isVisible) {
                // 格納
                (button as MaterialButton).apply {
                    text = getString(R.string.hide)
                    icon = getDrawable(R.drawable.ic_expand_less_black_24dp)
                }
            } else {
                // 表示
                (button as MaterialButton).apply {
                    text = getString(R.string.show)
                    icon = getDrawable(R.drawable.ic_expand_more_24px)
                }

            }
        }

        // リンク集初期化
        initRecyclerView()

    }

    private fun initRecyclerView() {
        kono_app_recyclerview.apply {
            val menuList = arrayListOf<MenuRecyclerAdapterDataClass>().apply {
                add(MenuRecyclerAdapterDataClass("Twitter", twitterLink, getDrawable(R.drawable.ic_outline_account_circle_24px), twitterLink))
                add(MenuRecyclerAdapterDataClass("Mastodon", mastodonLink, getDrawable(R.drawable.ic_outline_account_circle_24px), mastodonLink))
                add(MenuRecyclerAdapterDataClass(getString(R.string.sourcecode), source, getDrawable(R.drawable.ic_code_black_24dp), source))
                add(MenuRecyclerAdapterDataClass(getString(R.string.privacy_policy), privacy_policy, getDrawable(R.drawable.ic_policy_black), privacy_policy))
            }
            layoutManager = LinearLayoutManager(context)
            val menuAdapter = MenuRecyclerAdapter(menuList)
            adapter = menuAdapter
        }
    }

    //予約枠自動入場のお試し機能
    //10秒後に入場する機能。
    fun setEasterEgg(app: String) {
        //クリップボードのデータにアクセス。
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        //正規表現で取り出す
        val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
            .matcher(SpannableString(clipdata?.getItemAt(0)?.text ?: ""))
        if (nicoID_Matcher.find()) {
            // 番組ID
            val liveId = nicoID_Matcher.group()
            // 10秒先を指定
            val calender = Calendar.getInstance()
            calender.add(Calendar.SECOND, +10)
            GlobalScope.launch(Dispatchers.Main) {
                // DB追加
                val autoAdmissionDBEntity = AutoAdmissionDBEntity(name = "イースターエッグ", startTime = (calender.timeInMillis / 1000L).toString(), liveId = liveId, lanchApp = app, description = "")
                withContext(Dispatchers.IO) {
                    AutoAdmissionDBInit(this@KonoApp).commentCollectionDB.autoAdmissionDBDAO().insert(autoAdmissionDBEntity)
                }
                Snackbar.make(kono_app_imageview, "10秒後にコピーした番組へ移動します！", Snackbar.LENGTH_SHORT).show()
                //Service再起動
                val intent = Intent(this@KonoApp, AutoAdmissionService::class.java)
                stopService(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } else {
            Snackbar.make(kono_app_imageview, "番組IDをコピーしてね！", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * これ使って作った。 → https://www.nicovideo.jp/watch/sm37001529
     * イースターエッグの割には何の対策もしない
     * */
    private fun runEasterEgg() {
        val aa = """
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　█████████████████████　　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　　█████████████████████　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████　　　　　　　　　　 
　　　　　███████████████　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████　　　　　　　　　　　　　　 
　　　　　███████████　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
            """

        // 色ランダム
        val color = arrayListOf("pink", "blue", "cyan", "orange", "purple").random()
        aa.split("\n").forEach {
            val commentJSON = CommentJSONParse("{}", "arena", "sm157")
            commentJSON.comment = it
            commentJSON.mail = "small $color"
            // コメント描画
            konoapp_comment_canvas.postComment(it, commentJSON, false)
        }

    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool().setLanguageContext(newBase))
    }

}
