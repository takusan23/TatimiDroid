package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRSS
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_ranking.*
import kotlinx.coroutines.*

/**
 * ランキングFragment
 * */
class NicoVideoRankingFragment : Fragment() {

    private val nicoRSS = NicoVideoRSS()
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var prefSetting: SharedPreferences

    lateinit var launch: Job

    // メニュー選んだ位置。RANKING_GENRE#indexOf()を使い場所を特定しNicoVideoRSS#rankingGenreUrlListを使う際に使う
    var rankingGenrePos = 0
    var rankingTimePos = 0

    /** ランキングのジャンル一覧。NicoVideoRSS#rankingGenreUrlList のURL一覧と一致している */
    private val RANKING_GENRE = arrayListOf(
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

    /** ランキングの集計時間。基本いじらない。NicoVideoRSS#rankingTimeList の配列の中身と一致している。 */
    private val RANKING_TIME = arrayListOf(
        "毎時",
        "２４時間",
        "週間",
        "月間",
        "全期間"
    )


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        if (savedInstanceState == null) {
            // しょかい
            // 前回開いてたランキングを開く
            val lastOpenGenre = prefSetting.getString("nicovideo_ranking_genre", RANKING_GENRE[0])
            val lastOpenTime = prefSetting.getString("nicovideo_ranking_time", RANKING_TIME[0])
            // 配列の位置を探す
            rankingGenrePos = RANKING_GENRE.indexOf(lastOpenGenre)
            rankingTimePos = RANKING_TIME.indexOf(lastOpenTime)
            // AutoCompleteTextViewにも入れる
            fragment_nicovideo_ranking_menu.setText(lastOpenGenre)
            fragment_nicovideo_ranking_time_menu.setText(lastOpenTime)
            // データ取得
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

        // 次表示するときのために今選んでいるジャンル・集権時間を記録しておく
        prefSetting.edit {
            putString("nicovideo_ranking_genre", fragment_nicovideo_ranking_menu.text.toString())
            putString("nicovideo_ranking_time", fragment_nicovideo_ranking_time_menu.text.toString())
        }

        // すでにあるならキャンセル
        if (::launch.isInitialized) {
            launch.cancel()
        }

        loadRanking()

    }

    private fun loadRanking() {
        // ジャンル
        val genre = nicoRSS.rankingGenreUrlList[rankingGenrePos]
        // 集計期間
        val time = nicoRSS.rankingTimeList[rankingTimePos]
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // RSS取得
        launch = lifecycleScope.launch(errorHandler) {
            val response = nicoRSS.getRanking(genre, time)
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
        // Adapterセット
        // ランキング
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, RANKING_GENRE)
        fragment_nicovideo_ranking_menu.apply {
            setAdapter(adapter)
            // 押したとき
            onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                rankingGenrePos = position
                getRanking()
            }
            setText(RANKING_GENRE[0], false)
        }
        // 集計時間
        val timeAdapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, RANKING_TIME)
        fragment_nicovideo_ranking_time_menu.apply {
            setAdapter(timeAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                rankingTimePos = position
                getRanking()
            }
            setText(RANKING_TIME[0], false)
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_video_ranking_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
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