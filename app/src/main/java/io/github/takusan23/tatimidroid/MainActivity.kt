package io.github.takusan23.tatimidroid

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Fragment.*
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentPOSTListSQLiteHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_main)

        //ダークモード対応
        main_activity_bottom_navigationview.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1234)


        //共有から起動した
        lunchShareIntent()

        //生放送ID入力
        val fragmentTransitionSupport = supportFragmentManager.beginTransaction()
        fragmentTransitionSupport.replace(R.id.main_activity_linearlayout, LiveIDFragment())
        fragmentTransitionSupport.commit()
        //画面切り替え
        main_activity_bottom_navigationview.setSelectedItemId(R.id.menu_liveid);
        main_activity_bottom_navigationview.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_login -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, LoginFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_liveid -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, LiveIDFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_community -> {
                    //ログイン情報がない場合は押させない
                    if (pref_setting.getString("mail", "")?.isNotEmpty() == true) {
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(
                            R.id.main_activity_linearlayout,
                            CommunityListFragment()
                        )
                        fragmentTransaction.commit()
                    } else {
                        //メアド設定してね！
                        Toast.makeText(
                            this,
                            getString(R.string.mail_pass_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                R.id.menu_setting -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, SettingsFragment())
                    fragmentTransaction.commit()
                }
            }
            true
        }

        //データベース移行
        convertCommentPOSTListToCommentCollection()

    }

    //共有から起動した場合
    private fun lunchShareIntent() {
        if (Intent.ACTION_SEND.equals(intent.action)) {
            val extras = intent.extras
            //URL
            val url = extras?.getCharSequence(Intent.EXTRA_TEXT)
            //生放送ID取得
            //正規表現で取り出す
            val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(url ?: ""))
            if (nicoID_Matcher.find()) {
                //ダイアログ
                val liveId = nicoID_Matcher.group()
                val bundle = Bundle()
                bundle.putString("liveId", liveId)
                val dialog = BottomSheetDialogWatchMode()
                dialog.arguments = bundle
                dialog.show(supportFragmentManager, "watchmode")
            } else {
                //なかった。
                Toast.makeText(this, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }
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
        val cursor = commentPOSTListSqLiteDatabase.query(
            "comment_post_list",
            arrayOf("comment"),
            null,
            null,
            null,
            null,
            null
        )
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

}
