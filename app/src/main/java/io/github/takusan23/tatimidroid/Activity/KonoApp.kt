package io.github.takusan23.tatimidroid.Activity

import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import io.github.takusan23.tatimidroid.AutoAdmissionService
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_kono_app.*
import java.util.*
import java.util.regex.Pattern

class KonoApp : AppCompatActivity() {

    //データベース
    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

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
    val version = "2020/04/22"
    val codeName1 = "（秋）"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_kono_app)

        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName

        title = getString(R.string.kono_app)

        kono_app_codename.text = "$appVersion $version $codeName1"

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
            setEasterEgg("tatimidroid_app")
            false
        }

        kono_app_title.setOnLongClickListener {
            //いーすたーえっぐ
            setEasterEgg("nicolive_app")
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
        var clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var clipdata = clipboard.primaryClip
        if (clipboard != null) {
            //正規表現で取り出す
            val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(clipdata?.getItemAt(0)?.text ?: ""))
            if (nicoID_Matcher.find()) {
                //番組ID
                val liveId = nicoID_Matcher.group()

                //初期化済みか
                if (!this@KonoApp::autoAdmissionSQLiteSQLite.isInitialized) {
                    //初期化
                    autoAdmissionSQLiteSQLite =
                        AutoAdmissionSQLiteSQLite(this)
                    sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
                    //読み込む速度が上がる機能？データベースファイル以外の謎ファイルが生成されるので無効化。
                    autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
                }

                //10秒先を指定
                val calender = Calendar.getInstance()
                calender.add(Calendar.SECOND, +10)

                //書き込む
                val contentValues = ContentValues()
                contentValues.put("name", "イースターエッグ")
                contentValues.put("liveid", liveId)
                contentValues.put("start", (calender.timeInMillis / 1000L).toString())
                contentValues.put("app", app)
                contentValues.put("description", "")

                sqLiteDatabase.insert("auto_admission", null, contentValues)
                Snackbar.make(kono_app_imageview, "10秒後にコピーした番組へ移動します！", Snackbar.LENGTH_SHORT)
                    .show()
                //Service再起動
                val intent = Intent(this, AutoAdmissionService::class.java)
                stopService(intent)
                startService(intent)
            } else {
                Snackbar.make(kono_app_imageview, "番組IDをコピーしてね！", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(kono_app_imageview, "番組IDをコピーしてね！", Snackbar.LENGTH_SHORT).show()
        }
    }

}
