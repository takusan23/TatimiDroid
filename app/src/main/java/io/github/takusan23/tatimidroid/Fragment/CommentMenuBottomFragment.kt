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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.CommentRecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_commentview.*
import okhttp3.*
import okhttp3.internal.notify
import org.json.JSONObject
import java.io.IOException
import java.lang.NullPointerException
import java.util.regex.Pattern


class CommentMenuBottomFragment : BottomSheetDialogFragment() {

    lateinit var commentFragment: CommentFragment

    //こてはん
    lateinit var kotehanMap: MutableMap<String, String>
    //それぞれ
    var comment = ""
    var userId = ""
    var liveId = ""
    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    //RecyclerView
    lateinit var recyclerViewList: ArrayList<ArrayList<String>>
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_fragment_comment_menu_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        liveId = arguments?.getString("liveId") ?: ""
        commentFragment =
            activity?.supportFragmentManager?.findFragmentByTag(liveId) as CommentFragment

        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        bottom_fragment_comment_menu_parent_linearlayout.background =
            ColorDrawable(darkModeSupport.getThemeColor())

        //RecyclerView
        recyclerViewList = arrayListOf()
        bottom_fragment_comment_menu_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        bottom_fragment_comment_menu_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList)
        bottom_fragment_comment_menu_recyclerview.adapter = commentRecyclerViewAdapter
        commentRecyclerViewAdapter.setActivity((activity as AppCompatActivity?)!!)

        //Map
        kotehanMap = commentFragment.kotehanMap
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

        //ユーザー情報出す
        getUserProfile()

        //ロックオンできるようにする
        //ロックオンとはある一人のユーザーのコメントだけ見ることである
        //生主が効いたときによくある
        try {
            setLockOnComment()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    fun setLockOnComment() {
        recyclerViewList.clear()
        //まずCommentFragmentを取る。この上に全部屋、部屋別のFragmentが乗る。
        val fragment =
            activity?.supportFragmentManager?.findFragmentByTag(liveId)
        //全部屋、部屋別のFragmentが表示されるレイアウトのIDを取る
        val commentFragment = (fragment as CommentFragment)

        //もう一回聞く。
        val commentViewFragment =
            commentFragment.childFragmentManager.findFragmentById(commentFragment.getFragmentLinearLayoutId())

        if (commentViewFragment is CommentViewFragment) {
            //全部屋コメントRecyclerViewを取得
            val adapterList: ArrayList<ArrayList<String>> = commentViewFragment.recyclerViewList
            //ConcurrentModificationExceptionが発生する。forEachはやめようね！
            val tmp = adapterList
            for (i in 0 until tmp.size) {
                if (tmp[i] != null) {
                    val item = tmp[i]
                    val commentJson: String = item[1]
                    val roomName: String = item[2]
                    val commentJSONParse = CommentJSONParse(commentJson, roomName)
                    //IDを比較
                    if (commentJSONParse.userId == userId) {
                        //ロックオンコメント取得
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(commentJson)
                        item.add(roomName)
                        item.add(commentJSONParse.userId)
                        item.add(liveId)
                        activity?.runOnUiThread {
                            recyclerViewList.add(item)
                            commentRecyclerViewAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }

    }


    //URL正規表現
    private fun regexURL() {
        //正規表現で取り出す
        val urlRegex =
            Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+")
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
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
            Toast.makeText(
                context,
                "${getString(R.string.add_kotehan)}\n${userId}->${kotehan}",
                Toast.LENGTH_SHORT
            )
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
        commentFragment.loadNGDataBase()
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
        commentFragment.loadNGDataBase()
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun getUserProfile() {
        val pattern = Pattern.compile("^[0-9]*$")
        val matcher = pattern.matcher(userId)
        if (matcher.find()) {
            //数字だけということは生ID
            //非公式APIを叩いてユーザー名・プロフィール画像を出す
            val id = matcher.group()
            val request = Request.Builder()
                .url("https://api.ce.nicovideo.jp/api/v1/user.info?user_id=$id&__format=json")
                .get()
                .build()
            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    val response_string = response.body?.string()
                    if (response.isSuccessful) {
                        val jsonObject = JSONObject(response_string)
                        if (jsonObject.has("nicovideo_user_response")) {
                            if (jsonObject.getJSONObject("nicovideo_user_response").has("user")) {
                                val user = jsonObject.getJSONObject("nicovideo_user_response")
                                    .getJSONObject("user")
                                val name = user.getString("nickname")
                                val profileImage = user.getString("thumbnail_url")
                                activity?.runOnUiThread {
                                    bottom_fragment_comment_menu_user_id.append("\n$name")
                                }
                            }
                        }
                    }
                }
            })
        }
    }

}