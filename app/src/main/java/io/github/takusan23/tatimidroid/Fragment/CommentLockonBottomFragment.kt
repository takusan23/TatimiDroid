package io.github.takusan23.tatimidroid.Fragment

import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import kotlinx.android.synthetic.main.bottom_fragment_comment_menu_layout.*
import android.content.*
import android.text.SpannableString
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.NicoAPI.User
import io.github.takusan23.tatimidroid.Tool.NGDataBaseTool
import kotlinx.android.synthetic.main.adapter_comment_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

/**
 * ロックオン芸。生放送(CommentFragment)か動画(DevNicoVideoFragment)じゃないと動きません。
 * comment  | String  | コメント本文
 * user_id  | String  | ユーザーID
 * liveId   | String  | 生放送ID（動画なら動画ID）
 * label    | String  | 部屋名とか（コメント本文の上にあるユーザーID書いてある部分）
 * */
class CommentLockonBottomFragment : BottomSheetDialogFragment() {

    //  lateinit var commentFragment: CommentFragment
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    //それぞれ
    var comment = ""
    var userId = ""
    var liveId = ""

    //RecyclerView
    var recyclerViewList = arrayListOf<CommentJSONParse>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_comment_menu_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        liveId = arguments?.getString("liveId") ?: ""
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // ダークモード
        val darkModeSupport = DarkModeSupport(requireContext())
        bottom_fragment_comment_menu_parent_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())

        // Fragment取得
        val fragment = activity?.supportFragmentManager?.findFragmentByTag(liveId)

        // argmentから取り出す
        comment = arguments?.getString("comment") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        // コメント表示
        adapter_room_name_textview.text = arguments?.getString("label")
        adapter_comment_textview.text = comment

        /**
         * ロックオンできるようにする
         * ロックオンとはある一人のユーザーのコメントだけ見ることである
         * 生主が効いたときによくある
         * 動画にも対応する・・・？
         * */
        when {
            // 生放送
            fragment is CommentFragment -> {
                recyclerViewList = fragment.commentJSONList.filter { commentJSONParse -> commentJSONParse.userId == userId } as ArrayList<CommentJSONParse>
            }
            // 動画
            fragment is DevNicoVideoFragment -> {
                recyclerViewList = fragment.commentList.filter { commentJSONParse -> commentJSONParse.userId == userId } as ArrayList<CommentJSONParse>
            }
        }

        // RecyclerView
        if (fragment is CommentFragment) {
            // 生放送
            bottom_fragment_comment_menu_recyclerview.setHasFixedSize(true)
            bottom_fragment_comment_menu_recyclerview.layoutManager = LinearLayoutManager(context)
            val commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList)
            bottom_fragment_comment_menu_recyclerview.adapter = commentRecyclerViewAdapter
            commentRecyclerViewAdapter.setActivity((activity as AppCompatActivity?)!!)
        } else {
            // 動画
            bottom_fragment_comment_menu_recyclerview.setHasFixedSize(true)
            bottom_fragment_comment_menu_recyclerview.layoutManager = LinearLayoutManager(context)
            val nicoVideoAdapter = NicoVideoAdapter(recyclerViewList)
            nicoVideoAdapter.devNicoVideoFragment = fragment as DevNicoVideoFragment
            bottom_fragment_comment_menu_recyclerview.adapter = nicoVideoAdapter
        }


        // コテハン読み出し、登録
        val kotehanMap = when {
            fragment is CommentFragment -> fragment.kotehanMap
            fragment is DevNicoVideoFragment -> fragment.kotehanMap
            else -> null // まずありえないけど
        }
        if (kotehanMap != null) {
            bottom_fragment_comment_menu_kotehan_edit_text.setText(userId)
            loadKotehan(kotehanMap)
            bottom_fragment_comment_menu_kotehan_button.setOnClickListener {
                registerKotehan(kotehanMap)
            }
        }

        // NG関係
        setNGClick()

        // コピー
        setCopy()

        // URL取り出し
        regexURL()

        // 生IDのみ、ユーザー名取得ボタン
        if ("([0-9]+)".toRegex().matches(userId)) { // 生IDは数字だけで構成されているので正規表現（じゃなくてもできるだろうけど）
            // ユーザー名取得
            bottom_fragment_comment_menu_get_username.visibility = View.VISIBLE
            bottom_fragment_comment_menu_get_username.setOnClickListener {
                getUserName(userId)
            }
            // ユーザーページ取得
            bottom_fragment_comment_menu_open_userpage.visibility = View.VISIBLE
            bottom_fragment_comment_menu_open_userpage.setOnClickListener {
                openUserPage(userId)
            }
        }
    }

    /** ユーザーページを開く */
    private fun openUserPage(userId: String) {
        val intent = Intent(Intent.ACTION_VIEW, "https://www.nicovideo.jp/user/$userId".toUri())
        startActivity(intent)
    }

    /** ユーザー名取得。非同期処理 */
    private fun getUserName(userId: String) = GlobalScope.launch(Dispatchers.Main) {
        // API叩く
        val user = withContext(Dispatchers.IO) {
            User().getUserCoroutine(userId, userSession).await()
        }
        bottom_fragment_comment_menu_kotehan_edit_text.setText(user?.nickName)
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
        // Fragment取得
        val fragment = activity?.supportFragmentManager?.findFragmentByTag(liveId)
        val ngDataBaseTool = if (fragment is CommentFragment) {
            fragment.ngDataBaseTool // 生放送
        } else if (fragment is DevNicoVideoFragment) {
            fragment.ngDataBaseTool // 祝 動画にもNG機能
        } else {
            null // あ り え な い
        }
        //コメントNG追加
        //長押しで登録
        bottom_fragment_comment_menu_comment_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_comment_ng_button.setOnLongClickListener {
            // NG追加
            ngDataBaseTool?.addNGComment(comment)
            //とーすと
            showToast(getString(R.string.add_ng_comment_message))
            // 動画なら一覧更新する
            if (fragment is DevNicoVideoFragment) {
                GlobalScope.launch {
                    fragment.commentFilter().await()
                }
            }
            // 閉じる
            dismiss()
            true
        }
        //ユーザーNG追加
        //長押しで登録
        bottom_fragment_comment_menu_user_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_user_ng_button.setOnLongClickListener {
            // NG追加
            ngDataBaseTool?.addNGUser(userId)
            //とーすと
            showToast(getString(R.string.add_ng_user_message))
            // 動画なら一覧更新する
            if (fragment is DevNicoVideoFragment) {
                GlobalScope.launch {
                    fragment.commentFilter().await()
                }
            }
            // 閉じる
            dismiss()
            true
        }
    }

    //コテハン登録
    fun registerKotehan(kotehanMap: MutableMap<String, String>) {
        val kotehan = bottom_fragment_comment_menu_kotehan_edit_text.text.toString()
        if (kotehan.isNotEmpty()) {
            kotehanMap.put(userId, kotehan)
            //登録しました！
            Toast.makeText(context, "${getString(R.string.add_kotehan)}\n${userId}->${kotehan}", Toast.LENGTH_SHORT).show()
        }
    }

    //コテハン読み込み
    fun loadKotehan(kotehanMap: MutableMap<String, String>) {
        if (kotehanMap.containsKey(userId)) {
            bottom_fragment_comment_menu_kotehan_edit_text.setText(kotehanMap.get(userId))
        } else {
            //コテハンなかった
            bottom_fragment_comment_menu_kotehan_edit_text.setText(userId)
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}