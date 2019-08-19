package io.github.takusan23.tatimidroid.Fragment

import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import kotlinx.android.synthetic.main.bottom_fragment_comment_menu_layout.*
import kotlinx.android.synthetic.main.dialog_watchmode_layout.*
import android.R.attr.label
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.opengl.Visibility
import android.text.SpannableString
import android.widget.Button
import androidx.core.net.toUri
import java.util.regex.Pattern


class CommentMenuBottomFragment : BottomSheetDialogFragment() {

    //こてはん
    lateinit var kotehanMap: MutableMap<String, String>
    //それぞれ
    var comment = ""
    var userId = ""
    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_comment_menu_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        bottom_fragment_comment_menu_parent_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())
        //Map
        kotehanMap = (activity as CommentActivity).kotehanMap
        //取り出す
        comment = arguments?.getString("comment") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        //データベース
        ngListSQLiteHelper = NGListSQLiteHelper(context!!)
        sqLiteDatabase = ngListSQLiteHelper.writableDatabase
        ngListSQLiteHelper.setWriteAheadLoggingEnabled(false)

        //コテハン読み出し、登録
        bottom_fragment_comment_menu_user_id.text = userId
        loadKotehan()
        bottom_fragment_comment_menu_kotehan_button.setOnClickListener {
            registerKotehan()
        }

        //NG関係
        setNGClick()

        //コピー
        setCopy()

        //URL取り出し
        regexURL()
    }

    //URL正規表現
    private fun regexURL() {
        //正規表現で取り出す
        val urlRegex = Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+")
            .matcher(SpannableString(comment))
        if (urlRegex.find()) {
            bottom_fragment_comment_menu_comment_url.visibility = View.VISIBLE
            bottom_fragment_comment_menu_comment_url.setOnClickListener {
                val uri = urlRegex.group().toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    fun setCopy() {
        bottom_fragment_comment_menu_comment_copy.setOnClickListener {
            // コピーする
            val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", comment))
            Toast.makeText(context, getString(R.string.copy_successful), Toast.LENGTH_SHORT).show()
        }
    }

    fun setNGClick() {
        //コメントNG追加
        //長押しで登録
        bottom_fragment_comment_menu_comment_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_comment_ng_button.setOnLongClickListener {
            addNGComment()
            true
        }
        //ユーザーNG追加
        //長押しで登録
        bottom_fragment_comment_menu_user_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_user_ng_button.setOnLongClickListener {
            addNGUser()
            true
        }
    }

    //コテハン登録
    fun registerKotehan() {
        val kotehan = bottom_fragment_comment_menu_kotehan_edit_text.text.toString()
        if (kotehan.isNotEmpty()) {
            kotehanMap.put(userId, kotehan)
            //登録しました！
            Toast.makeText(context, "${getString(R.string.add_kotehan)}\n${userId}->${kotehan}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //コテハン読み込み
    fun loadKotehan() {
        if (kotehanMap.containsKey(userId)) {
            bottom_fragment_comment_menu_kotehan_edit_text.setText(kotehanMap.get(userId))
        } else {
            //コテハンなかった
            bottom_fragment_comment_menu_user_id.text = userId
        }
    }

    //NGコメント追加
    fun addNGComment() {
        val contentValues = ContentValues()
        contentValues.put("type", "comment")
        contentValues.put("value", comment)
        contentValues.put("description", "")
        sqLiteDatabase.insert("ng_list", null, contentValues)
        //とーすと
        showToast(getString(R.string.add_ng_comment_message))
        //リスト更新
        if (activity is CommentActivity) {
            (activity as CommentActivity).loadNGDataBase()
        }
    }

    //NGユーザー追加
    fun addNGUser() {
        val contentValues = ContentValues()
        contentValues.put("type", "user")
        contentValues.put("value", userId)
        contentValues.put("description", "")
        sqLiteDatabase.insert("ng_list", null, contentValues)
        //とーすと
        showToast(getString(R.string.add_ng_user_message))
        //リスト更新
        if (activity is CommentActivity) {
            (activity as CommentActivity).loadNGDataBase()
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}