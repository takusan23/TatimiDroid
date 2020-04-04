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
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*

/**
 * コメント表示Fragment
 * */
class DevNicoVideoCommentFragment : Fragment() {

    var recyclerViewList: ArrayList<ArrayList<String>> = arrayListOf()
    lateinit var nicoVideoAdapter: NicoVideoAdapter
    lateinit var prefSetting: SharedPreferences
    var usersession = ""
    var id = "sm157"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)


        //動画ID受け取る（sm9とかsm157とか）
        id = arguments?.getString("id") ?: "sm157"

        // //nicovideoFragment取る。
        // nicoVideoFragment =
        //     activity?.supportFragmentManager?.findFragmentByTag(id) as NicoVideoFragment

        usersession = prefSetting.getString("user_session", "") ?: ""

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
        recyclerViewList.clear()
        (fragmentManager?.findFragmentByTag(id) as DevNicoVideoFragment).commentList.forEach {
            recyclerViewList.add(it)
        }
        activity_nicovideo_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        activity_nicovideo_recyclerview.layoutManager = mLayoutManager
        // 3DSコメント非表示を実現するための面倒なやつ
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        val list = if (is3DSCommentHidden) {
            recyclerViewList.filter { arrayList -> !arrayList[5].contains("device:3DS") }
        } else {
            recyclerViewList
        } as ArrayList<ArrayList<String>>
        nicoVideoAdapter = NicoVideoAdapter(list)
        activity_nicovideo_recyclerview.adapter = nicoVideoAdapter
        //  Snackbar
        if (snackbarShow) {
            Snackbar.make(
                activity_nicovideo_sort_button,
                "${getString(R.string.get_comment_count)}：${recyclerViewList.size}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
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

                //配列の中身全部コピーする
                val tmp = arrayListOf<ArrayList<String>>()
                recyclerViewList.forEach {
                    tmp.add(it as ArrayList<String>)
                }
                //クリアに
                recyclerViewList.clear()
                when (item?.itemId) {
                    R.id.nicovideo_comment_sort_time -> {
                        //再生時間
                        tmp.sortedBy { arrayList -> arrayList[4].toInt() }
                            .forEach { recyclerViewList.add(it) }
                    }
                    R.id.nicovideo_comment_sort_reverse_time -> {
                        //再生時間逆順
                        tmp.sortedByDescending { arrayList -> arrayList[4].toInt() }
                            .forEach { recyclerViewList.add(it) }
                    }
                    R.id.nicovideo_comment_nicoru -> {
                        //ニコる数
                        tmp.sortedByDescending { arrayList ->
                            //ニコる数0の時対策
                            val count = arrayList[6]
                            if (count.isNotEmpty()) {
                                arrayList[6].toInt()
                            } else {
                                0
                            }
                        }.forEach { recyclerViewList.add(it) }
                    }
                    R.id.nicovideo_date -> {
                        //投稿日時(新→古)
                        tmp.sortedByDescending { arrayList -> arrayList[3].toFloat() }
                            .forEach { recyclerViewList.add(it) }
                    }
                    R.id.nicovideo_reverse_date -> {
                        //投稿日時(古→新)
                        tmp.sortedBy { arrayList -> arrayList[3].toFloat() }
                            .forEach { recyclerViewList.add(it) }
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
        var tmpList = arrayListOf<ArrayList<String>>()
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
                    tmpList = recyclerViewList.filter { arrayList ->
                        (arrayList[2] as String).contains(s)
                    } as ArrayList<ArrayList<String>>
                }
                // Adapter更新
                nicoVideoAdapter = NicoVideoAdapter(tmpList)
                activity_nicovideo_recyclerview.adapter = nicoVideoAdapter
            }
        })
    }

}