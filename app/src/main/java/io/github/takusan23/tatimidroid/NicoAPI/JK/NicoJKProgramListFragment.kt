package io.github.takusan23.tatimidroid.NicoAPI.JK

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_jk_program_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * ニコニコ実況チャンネル一覧
 * */
class NicoJKProgramListFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    lateinit var nicoJKProgramAdapter: NicoJKProgramAdapter
    var recyclerViewList = arrayListOf<NicoJKData>()

    // API叩いたりスクレイピングしたり
    val nicoJKHTML = NicoJKHTML()

    var userSession = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_jk_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        initRecyclerView()

        coroutine()

        // 引っ張って更新
        fragment_jk_program_list_swipe.setOnRefreshListener {
            coroutine()
        }

    }

    // データ取得
    fun coroutine() {
        // 配列クリア
        recyclerViewList.clear()
        nicoJKProgramAdapter.notifyDataSetChanged()
        fragment_jk_program_list_swipe.isRefreshing = true
        GlobalScope.launch {
            val listResponse = nicoJKHTML.getChannelListHTML("tv", userSession).await()
            // 取得できんときは落とす
            if (!listResponse.isSuccessful) {
                return@launch
            }
            recyclerViewList =
                ArrayList(nicoJKHTML.parseChannelListHTML(listResponse.body?.string()))
            // RecyclerView更新
            Handler(Looper.getMainLooper()).post {
                initRecyclerView()
                fragment_jk_program_list_swipe.isRefreshing = false
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        if (isAdded) {
            fragment_jk_program_list_recyclerview.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context) as RecyclerView.LayoutManager
                nicoJKProgramAdapter = NicoJKProgramAdapter(recyclerViewList)
                adapter = nicoJKProgramAdapter
            }
        }
    }

}