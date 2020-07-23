package io.github.takusan23.tatimidroid.Activity

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.AutoAdmissionDBEntity
import io.github.takusan23.tatimidroid.Room.Init.AutoAdmissionDBInit
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import kotlinx.android.synthetic.main.activity_kono_app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

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

        setContentView(R.layout.activity_kono_app)

        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName

        title = getString(R.string.kono_app)

        kono_app_codename.text = "$appVersion\n$version\n$codeName1"

        kono_app_twitter.setOnClickListener {
            startBrowser(twitterLink)
        }
        kono_app_mastodon.setOnClickListener {
            startBrowser(mastodonLink)
        }
        kono_app_sourcecode.setOnClickListener {
            startBrowser(source)
        }

        kono_app_imageview.setOnLongClickListener {
            //いーすたーえっぐ
            setEasterEgg(AutoAdmissionDBEntity.LAUNCH_POPUP)
            false
        }

        kono_app_title.setOnLongClickListener {
            //いーすたーえっぐ
            setEasterEgg(AutoAdmissionDBEntity.LAUNCH_POPUP)
            false
        }

        kono_app_privacy_policy.setOnClickListener {
            //プライバシーポリシー
            startBrowser(privacy_policy)
        }
    }

    fun startBrowser(link: String) {
        val i = Intent(Intent.ACTION_VIEW, link.toUri());
        startActivity(i);
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
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool().setLanguageContext(newBase))
    }

}
