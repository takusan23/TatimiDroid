package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*

/**
 * コメント表示Fragment
 * */
class DevNicoVideoCommentFragment : Fragment() {

    var recyclerViewList = arrayListOf<CommentJSONParse>()
    lateinit var nicoVideoAdapter: NicoVideoAdapter
    lateinit var prefSetting: SharedPreferences
    lateinit var devNicoVideoFragment: DevNicoVideoFragment

    var usersession = ""
    var id = "sm157"

    lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        //動画ID受け取る（sm9とかsm157とか）
        id = arguments?.getString("id") ?: "sm157"
        usersession = prefSetting.getString("user_session", "") ?: ""
        recyclerView = view.findViewById(R.id.activity_nicovideo_recyclerview)
        devNicoVideoFragment = fragmentManager?.findFragmentByTag(id) as DevNicoVideoFragment

        // コメント検索ボタン、コメント並び替えボタンを非表示にするか
        if (prefSetting.getBoolean("nicovideo_hide_search_button", true)) {
            (activity_nicovideo_comment_serch_button.parent as View).visibility = View.GONE
        }

        //ポップアップメニュー初期化
        initSortPopupMenu()

        // RecyclerView
        initRecyclerView()

        // コメント検索
        initSearchButton()

    }

    /**
     * RecyclerView初期化とか
     * @param recyclerViewList RecyclerViewに表示させる中身の配列。省略時はDevNicoVideoCommentFragment.recyclerViewListを使います。
     * @param snackbarShow SnackBar（取得コメント数）を表示させる場合はtrue、省略時はfalse
     * */
    fun initRecyclerView(snackbarShow: Boolean = false) {
        if (!isAdded) {
            return
        }
        recyclerViewList = ArrayList(devNicoVideoFragment.commentList)
        recyclerView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = mLayoutManager
        // 3DSコメント非表示を実現するための面倒なやつ
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        val list = if (is3DSCommentHidden) {
            recyclerViewList.filter { commentJSONParse -> !commentJSONParse.mail.contains("device:3DS") }
        } else {
            recyclerViewList
        } as ArrayList<CommentJSONParse>
        nicoVideoAdapter = NicoVideoAdapter(list)
        // DevNicoVideoFragment渡す
        nicoVideoAdapter.devNicoVideoFragment =
            fragmentManager?.findFragmentByTag(id) as DevNicoVideoFragment
        recyclerView.adapter = nicoVideoAdapter
        //  Snackbar
        if (snackbarShow) {
            // DevNicoVideoFragment
            val fragment =
                fragmentManager?.findFragmentByTag(id) as DevNicoVideoFragment
            fragment.showSnackbar("${getString(R.string.get_comment_count)}：${recyclerViewList.size}", null, null)
        }
    }

    override fun onStart() {
        super.onStart()
        // initRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        initRecyclerView()
    }

    private fun initSortPopupMenu() {
        val menuBuilder = MenuBuilder(context)
        val menuInflater = MenuInflater(context)
        menuInflater.inflate(R.menu.nicovideo_comment_sort, menuBuilder)
        val menuPopupHelper =
            MenuPopupHelper(context!!, menuBuilder, activity_nicovideo_sort_button)
        menuPopupHelper.setForceShowIcon(true)
        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuModeChange(menu: MenuBuilder?) {

            }

            override fun onMenuItemSelected(menu: MenuBuilder?, item: MenuItem?): Boolean {

                when (item?.itemId) {
                    R.id.nicovideo_comment_sort_time -> {
                        //再生時間
                        recyclerViewList.sortBy { commentJSONParse -> commentJSONParse.vpos }
                    }
                    R.id.nicovideo_comment_sort_reverse_time -> {
                        //再生時間逆順
                        recyclerViewList.sortByDescending { commentJSONParse -> commentJSONParse.vpos }
                    }
                    R.id.nicovideo_comment_nicoru -> {
                        //ニコる数
                        recyclerViewList.sortByDescending { commentJSONParse -> commentJSONParse.nicoru }
                    }
                    R.id.nicovideo_date -> {
                        //投稿日時(新→古)
                        recyclerViewList.sortByDescending { commentJSONParse -> commentJSONParse.date }
                    }
                    R.id.nicovideo_reverse_date -> {
                        //投稿日時(古→新)
                        recyclerViewList.sortBy { commentJSONParse -> commentJSONParse.date }
                    }
                }
                nicoVideoAdapter.notifyDataSetChanged()
                return false
            }
        })
        //押したら開く
        activity_nicovideo_sort_button.setOnClickListener {
            menuPopupHelper.show()
        }
    }

    /**
     * コメント検索関係
     * */
    private fun initSearchButton() {
        activity_nicovideo_comment_serch_button.setOnClickListener {
            // 検索UI表示・非表示
            activity_nicovideo_comment_serch_linearlayout.visibility =
                if (activity_nicovideo_comment_serch_linearlayout.visibility == View.GONE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
        // テキストボックス監視
        var tmpList = arrayListOf<CommentJSONParse>()
        activity_nicovideo_comment_serch_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 一時的に
                tmpList.clear()
                recyclerViewList.forEach {
                    tmpList.add(it)
                }
                if (s?.isNotEmpty() == true) {
                    // フィルター
                    tmpList = recyclerViewList.filter { commentJSONParse ->
                        commentJSONParse.comment.contains(s)
                    } as ArrayList<CommentJSONParse>
                }
                // Adapter更新
                nicoVideoAdapter =
                    NicoVideoAdapter(tmpList)
                activity_nicovideo_recyclerview.adapter = nicoVideoAdapter
            }
        })
    }

}