package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRSS
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_ranking.*
import kotlinx.coroutines.*

/**
 * ランキングFragment
 * */
class DevNicoVideoRankingFragment : Fragment() {

    val nicoRSS = NicoVideoRSS()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    lateinit var launch: Job

    // メニュー選んだ位置
    var rankingMenuPos = 0
    var rankingTimePos = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        if (savedInstanceState == null) {
            // しょかい
            getRanking()
        } else {
            // 画面回転復帰時
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                recyclerViewList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // Swipe To Refresh
        fragment_video_ranking_swipe.setOnRefreshListener {
            getRanking()
        }
    }

    // RSS取得
    private fun getRanking() {
        // 消す
        recyclerViewList.clear()
        nicoVideoListAdapter.notifyDataSetChanged()
        fragment_video_ranking_swipe.isRefreshing = true
        // すでにあるならキャンセル
        if (::launch.isInitialized) {
            launch.cancel()
        }

        loadRanking()

    }

    fun loadRanking() {
        // ジャンル
        val genre = nicoRSS.rankingGenreUrlList[rankingMenuPos]
        // 集計期間
        val time = nicoRSS.rankingTimeList[rankingTimePos]
        // RSS取得
        launch = GlobalScope.launch {
            val response = nicoRSS.getRanking(genre, time).await()
            if (response.isSuccessful) {
                nicoRSS.parseHTML(response).forEach {
                    recyclerViewList.add(it)
                }
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        nicoVideoListAdapter.notifyDataSetChanged()
                        fragment_video_ranking_swipe.isRefreshing = false
                    }
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    private fun initDropDownMenu() {
        val rankingGenre =
            arrayListOf(
                "全ジャンル",
                "話題",
                "エンターテインメント",
                "ラジオ",
                "音楽・サウンド",
                "ダンス",
                "動物",
                "自然",
                "料理",
                "旅行・アウトドア",
                "乗り物",
                "スポーツ",
                "社会・政治・時事",
                "技術・工作",
                "解説・講座",
                "アニメ",
                "ゲーム",
                "その他"
            )
        // Adapterセット
        val adapter = AllShowDropDownMenuAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, rankingGenre)
        fragment_nicovideo_ranking_menu.apply {
            setAdapter(adapter)
            // 押したとき
            onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                rankingMenuPos = position
                getRanking()
            }
            setText(rankingGenre[0], false)
        }
        val timeGenre = arrayListOf("毎時", "２４時間", "週間", "月間", "全期間")
        val timeAdapter = AllShowDropDownMenuAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, timeGenre)
        fragment_nicovideo_ranking_time_menu.apply {
            setAdapter(timeAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                rankingTimePos = position
                getRanking()
            }
            setText(timeGenre[0], false)
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_video_ranking_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

}