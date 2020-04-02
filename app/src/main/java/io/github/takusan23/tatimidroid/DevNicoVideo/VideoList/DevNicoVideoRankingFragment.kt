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
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoRSS
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_ranking.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ランキングFragment
 * */
class DevNicoVideoRankingFragment : Fragment() {

    val nicoRSS = NicoVideoRSS()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    lateinit var launch: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ドロップダウンメニュー（Spinner）初期化
        initSpinner()

        // RecyclerView初期化
        initRecyclerView()

        // Spinner選択リスナー
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                getRanking()
            }
        }
        fragment_nicovideo_ranking_spinner.onItemSelectedListener = spinnerListener
        fragment_nicovideo_ranking_time_spinner.onItemSelectedListener = spinnerListener

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
        // ジャンル
        val genre =
            nicoRSS.rankingGenreUrlList[fragment_nicovideo_ranking_spinner.selectedItemPosition]
        // 集計期間
        val time =
            nicoRSS.rankingTimeList[fragment_nicovideo_ranking_time_spinner.selectedItemPosition]
        // RSS取得
        launch = GlobalScope.launch {
            val response = nicoRSS.getRanking(genre, time).await()
            if (response.isSuccessful) {
                nicoRSS.parseHTML(response).forEach {
                    recyclerViewList.add(it)
                }
                activity?.runOnUiThread {
                    nicoVideoListAdapter.notifyDataSetChanged()
                    fragment_video_ranking_swipe.isRefreshing = false
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
            }

        }
    }

    private fun initSpinner() {
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
        val adapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, rankingGenre)
        fragment_nicovideo_ranking_spinner.adapter = adapter

        val timeGenre =
            arrayListOf("毎時", "２４時間", "週間", "月間", "全期間")
        val timeAdapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, timeGenre)
        fragment_nicovideo_ranking_time_spinner.adapter = timeAdapter
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

}