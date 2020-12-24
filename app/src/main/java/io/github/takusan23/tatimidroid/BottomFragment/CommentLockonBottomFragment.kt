package io.github.takusan23.tatimidroid.BottomFragment

import android.content.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.User.UserAPI
import io.github.takusan23.tatimidroid.NicoLive.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.ProgramMenuBottomSheet
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity
import io.github.takusan23.tatimidroid.Room.Init.KotehanDBInit
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.NICOLIVE_ID_REGEX
import io.github.takusan23.tatimidroid.Tool.NICOVIDEO_ID_REGEX
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.adapter_comment_layout.*
import kotlinx.android.synthetic.main.adapter_comment_layout.view.*
import kotlinx.android.synthetic.main.bottom_fragment_comment_menu_layout.*
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * ロックオン芸。生放送(CommentFragment)か動画(DevNicoVideoFragment)じゃないと動きません。
 * comment  | String  | コメント本文
 * user_id  | String  | ユーザーID
 * liveId   | String  | 生放送ID（動画なら動画ID）
 * label    | String  | 部屋名とか（コメント本文の上にあるユーザーID書いてある部分）
 * 動画なら
 * current_pos | Long   | コメントのvpos。1秒==100vpos
 * */
class CommentLockonBottomFragment : BottomSheetDialogFragment() {

    //  lateinit var commentFragment: CommentFragment
    lateinit var prefSetting: SharedPreferences
    private var userSession = ""

    //それぞれ
    private var comment = ""
    private var userId = ""

    //RecyclerView
    var recyclerViewList = arrayListOf<CommentJSONParse>()

    // 動画Fragmentかどうか
    var isNicoVideoFragment = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_comment_menu_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // ダークモード
        val darkModeSupport = DarkModeSupport(requireContext())
        bottom_fragment_comment_menu_parent_linearlayout.background = ColorDrawable(getThemeColor(darkModeSupport.context))

        // argmentから取り出す
        comment = arguments?.getString("comment") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        // コメント表示
        adapter_room_name_textview.text = arguments?.getString("label")
        adapter_comment_textview.text = comment

        // 複数行格納
        view.adapter_comment_parent.setOnClickListener {
            if (adapter_comment_textview.maxLines == 1) {
                adapter_comment_textview.maxLines = Int.MAX_VALUE
            } else {
                adapter_comment_textview.maxLines = 1
            }
        }

        /**
         * ロックオンできるようにする
         * ロックオンとはある一人のユーザーのコメントだけ見ることである
         * 生主が効いたときによくある
         * 動画にも対応する・・・？
         * */
        val fragment = requireParentFragment()
        when (fragment) {
            is CommentFragment -> {
                // 生放送
                recyclerViewList = fragment.viewModel.commentList.filter { commentJSONParse -> if (commentJSONParse != null) commentJSONParse.userId == userId else false } as ArrayList<CommentJSONParse>
                lifecycleScope.launch {
                    // コメントが届いたら反映させる。コルーチンすごいね
                    fragment.viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
                        if (comment.userId == userId && !recyclerViewList.contains(comment)) {
                            recyclerViewList.add(0, comment)
                            bottom_fragment_comment_menu_recyclerview.adapter?.notifyDataSetChanged()
                            showInfo()
                        }
                    }
                }
            }
            // 動画
            is NicoVideoFragment -> {
                recyclerViewList = fragment.viewModel.rawCommentList.filter { commentJSONParse -> commentJSONParse.userId == userId } as ArrayList<CommentJSONParse>
                isNicoVideoFragment = true
            }
        }

        // RecyclerView
        when {
            fragment is CommentFragment -> {
                // 生放送
                bottom_fragment_comment_menu_recyclerview.setHasFixedSize(true)
                bottom_fragment_comment_menu_recyclerview.layoutManager = LinearLayoutManager(context)
                val commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList, fragment)
                bottom_fragment_comment_menu_recyclerview.adapter = commentRecyclerViewAdapter
                //区切り線いれる
                val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                bottom_fragment_comment_menu_recyclerview.addItemDecoration(itemDecoration)
            }
            fragment is NicoVideoFragment -> {
                // 動画
                bottom_fragment_comment_menu_recyclerview.setHasFixedSize(true)
                bottom_fragment_comment_menu_recyclerview.layoutManager = LinearLayoutManager(context)
                val nicoVideoAdapter = NicoVideoAdapter(recyclerViewList, fragment)
                bottom_fragment_comment_menu_recyclerview.adapter = nicoVideoAdapter
                //区切り線いれる
                val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                bottom_fragment_comment_menu_recyclerview.addItemDecoration(itemDecoration)
            }
        }

        // コテハンを読み出す
        lifecycleScope.launch(Dispatchers.Main) {
            // とりあえずユーザーID表示
            bottom_fragment_comment_menu_kotehan_edit_text.setText(userId)
            // コテハン読み込み
            val kotehan = loadKotehan()
            // 存在すればテキストに入れる。なければnullになる
            if (kotehan != null) {
                bottom_fragment_comment_menu_kotehan_edit_text.setText(kotehan.kotehan)
            }
            // コテハン登録
            bottom_fragment_comment_menu_kotehan_button.setOnClickListener {
                if (fragment != null) {
                    registerKotehan()
                }
            }
        }

        // NGスコア表示など
        showInfo()

        // NG関係
        setNGClick()

        // コピー
        bottom_fragment_comment_menu_comment_copy.setOnClickListener {
            commentCopy()
        }

        // URL取り出し
        regexURL()

        // 動画の場合は「ここから再生」ボタンを表示する
        if (fragment != null) {
            showJumpButton(fragment)
        }

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

        /**
         * 公式の動画と生放送アプリが独立した今できなさそうなことを。アプリ内完結
         * 生放送で流れたコメントに動画IDが含まれている時に例えばマイリスト登録をしたり、ポップアップ再生をしたり、
         * 生放送IDでもTS予約、予定追加機能が使えるように。
         * */
        val videoIdRegex = NICOVIDEO_ID_REGEX.toRegex().find(comment)
        val liveIdRegex = NICOLIVE_ID_REGEX.toRegex().find(comment)
        when {
            videoIdRegex?.value != null -> {
                // どーが
                bottom_fragment_comment_menu_nicovideo_menu.visibility = View.VISIBLE
                bottom_fragment_comment_menu_nicovideo_menu.setOnClickListener {
                    // 動画メニュー出す
                    val nicoVideoMenuBottomFragment = NicoVideoListMenuBottomFragment()
                    val bundle = Bundle()
                    bundle.putString("video_id", videoIdRegex.value)
                    bundle.putBoolean("is_cache", false)
                    nicoVideoMenuBottomFragment.arguments = bundle
                    nicoVideoMenuBottomFragment.show(activity?.supportFragmentManager!!, "video_menu")
                }
            }
            liveIdRegex?.value != null -> {
                // なまほーそー
                bottom_fragment_comment_menu_nicolive_menu.visibility = View.VISIBLE
                bottom_fragment_comment_menu_nicolive_menu.setOnClickListener {
                    // 生放送メニュー出す
                    val programMenuBottomSheet = ProgramMenuBottomSheet()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveIdRegex.value)
                    programMenuBottomSheet.arguments = bundle
                    programMenuBottomSheet.show(activity?.supportFragmentManager!!, "video_menu")
                }
            }
        }

    }

    /** こっから再生 */
    private fun showJumpButton(fragment: Fragment) {
        // 移動先
        val currentPos = arguments?.getLong("current_pos", -1) ?: -1
        // ボタン表示。動画Fragmentでかつcurrent_posが-1以外のとき表示
        if (isNicoVideoFragment && currentPos != -1L) {
            bottom_fragment_comment_menu_nicovideo_seek.visibility = View.VISIBLE
            bottom_fragment_comment_menu_nicovideo_seek.append("(${formatTime(currentPos / 100F)})") // 移動先時間追記
        }
        bottom_fragment_comment_menu_nicovideo_seek.setOnClickListener {
            // こっから再生できるようにする
            if (fragment is NicoVideoFragment) {
                // LiveData経由でExoPlayerを操作
                val viewModel by viewModels<NicoVideoViewModel>({ fragment })
                viewModel.playerSetSeekMs.postValue(currentPos * 10)
                fragment.fragment_nicovideo_comment_canvas.seekComment()
            }
        }
    }

    private fun showInfo() {
        // NGスコア/個数など
        bottom_fragment_comment_menu_count.text = "${getString(R.string.comment_count)}：${recyclerViewList.size}"
        bottom_fragment_comment_menu_ng.text = "${getString(R.string.ng_score)}：${recyclerViewList.firstOrNull()?.score}"
    }

    /** ユーザーページを開く */
    private fun openUserPage(userId: String) {
        val intent = Intent(Intent.ACTION_VIEW, "https://www.nicovideo.jp/user/$userId".toUri())
        startActivity(intent)
    }

    /** ユーザー名取得。非同期処理 */
    private fun getUserName(userId: String) = lifecycleScope.launch(Dispatchers.Main) {
        // API叩く
        val user = withContext(Dispatchers.IO) {
            val user = UserAPI()
            val response = user.getUserData(userId, userSession)
            if (!response.isSuccessful) return@withContext null
            user.parseUserData(response.body?.string())
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

    private fun commentCopy() {
        // コピーする
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", comment))
        Toast.makeText(context, getString(R.string.copy_successful), Toast.LENGTH_SHORT).show()
    }

    private fun setNGClick() {
        // コメントNG追加
        // 長押しで登録
        bottom_fragment_comment_menu_comment_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_comment_ng_button.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    // NGコメント追加。あとは生放送/動画Fragmentでデータベースを監視してるのでこれで終わり
                    NGDBInit.getInstance(requireContext()).ngDBDAO().insert(NGDBEntity(value = comment, type = "comment", description = ""))
                }
                //とーすと
                showToast(getString(R.string.add_ng_comment_message))
                // 閉じる
                dismiss()
            }
            true
        }
        // ユーザーNG追加
        // 長押しで登録
        bottom_fragment_comment_menu_user_ng_button.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        bottom_fragment_comment_menu_user_ng_button.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    // NGユーザー追加
                    NGDBInit.getInstance(requireContext()).ngDBDAO().insert(NGDBEntity(value = userId, type = "user", description = ""))
                }
                // とーすと
                showToast(getString(R.string.add_ng_user_message))
                // 閉じる
                dismiss()
            }
            true
        }
    }

    //コテハン登録。非同期
    private fun registerKotehan() {
        val kotehan = bottom_fragment_comment_menu_kotehan_edit_text.text.toString()
        if (kotehan.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                // データベース用意。ここでは追加のみすればいい。後は各Fragmentが変更を検知して最新のを適用してくれる(NicoVideoFragment.setKotehanDBChangeObserve()参照)
                val kotehanDB = KotehanDBInit.getInstance(requireContext())
                withContext(Dispatchers.IO) {
                    // すでに存在する場合・・・？
                    val kotehanData = kotehanDB.kotehanDBDAO().findKotehanByUserId(userId)
                    if (kotehanData != null) {
                        // 存在するなら上書き
                        val kotehanEntity = kotehanData.copy(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        kotehanDB.kotehanDBDAO().update(kotehanEntity)
                    } else {
                        // データ追加
                        val kotehanEntity = KotehanDBEntity(userId = userId, kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        kotehanDB.kotehanDBDAO().insert(kotehanEntity)
                    }
                }
                //登録しました！
                Toast.makeText(context, "${getString(R.string.add_kotehan)}\n${userId}->${kotehan}", Toast.LENGTH_SHORT).show()
                // 一覧更新など
                bottom_fragment_comment_menu_recyclerview.adapter?.notifyDataSetChanged()
            }
        }
    }

    //コテハン読み込み
    private suspend fun loadKotehan() = withContext(Dispatchers.IO) {
        KotehanDBInit.getInstance(requireContext()).kotehanDBDAO().findKotehanByUserId(userId)
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 時間表記をきれいにする関数
     * @param time 秒。ミリ秒ではない
     * */
    private fun formatTime(time: Float): String {
        val minutes = time / 60
        val hour = (minutes / 60).toInt()
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "${hour}:${simpleDateFormat.format(time * 1000)}"
    }


}