package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nguploader.NGUploaderTool
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import kotlinx.coroutines.*

/**
 * [io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoRankingFragment]のデータを保持するViewModel
 * */
class NicoVideoRankingViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ランキングページスクレイピング */
    private val nicoVideoRankingHTML = NicoVideoRankingHTML()

    /** ランキングの配列 */
    val rankingVideoList = MutableLiveData<List<NicoVideoData>>()

    /** ランキングのタグ配列 */
    val rankingTagList = MutableLiveData<List<String>>()

    /** コルーチンキャンセル用 */
    private val coroutineJob = Job()

    /** ランキングのジャンル一覧。[NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE] のURL一覧と一致している */
    val RANKING_GENRE = listOf(
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

    /** ランキングの集計時間。[NicoVideoRankingHTML.NICOVIDEO_RANKING_TIME] の配列の中身と一致している。 */
    val RANKING_TIME = listOf(
        "毎時",
        "２４時間",
        "週間",
        "月間",
        "全期間"
    )

    /** 最後に開いたランキングのジャンル。なければ全ジャンル */
    val lastOpenGenre = prefSetting.getString("nicovideo_ranking_genre", RANKING_GENRE[0])!!

    /** 最後に開いたランキングの集計時間。なければ毎時 */
    val lastOpenTime = prefSetting.getString("nicovideo_ranking_time", RANKING_TIME[0])!!

    init {
        // とりあえず最後に開いたランキングでも
        val rankingGenrePos = RANKING_GENRE.indexOf(lastOpenGenre)
        val rankingTimePos = RANKING_TIME.indexOf(lastOpenTime)
        // その他 -> genre/other へ変換する
        val genre = NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE[rankingGenrePos]
        val time = NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE[rankingTimePos]
        loadRanking(genre, time)
    }

    /**
     * ランキングのHTMLをスクレイピングして配列に入れる
     * @param genre genre/all など。[io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE]から選んで
     * @param time hour など。[io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML.NICOVIDEO_RANKING_TIME]から選んで
     * @param tag VOCALOID など。無くても良い
     * @return [rankingVideoList]等に入れます
     * */
    fun loadRanking(genre: String, time: String, tag: String? = null) {
        // 読み込み中ならキャンセル
        coroutineJob.cancelChildren()
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + coroutineJob + Dispatchers.Default) {
            val response = nicoVideoRankingHTML.getRankingHTML(genre, time, tag)
            if (!response.isSuccessful) {
                showToast("${context.getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // パース
            val responseString = response.body?.string() ?: return@launch
            val rawVideoList = nicoVideoRankingHTML.parseRankingVideo(responseString)
            if (rawVideoList != null) {
                rankingVideoList.postValue(NGUploaderTool.filterNGUploaderVideoId(context, rawVideoList))
                rankingTagList.postValue(ArrayList(nicoVideoRankingHTML.parseRankingGenreTag(responseString)))
            } else {
                showToast(context.getString(R.string.error))
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}