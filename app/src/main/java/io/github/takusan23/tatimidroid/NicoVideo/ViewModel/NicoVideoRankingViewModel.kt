package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoRankingFragment]のデータを保持するViewModel
 * */
class NicoVideoRankingViewModel(application: Application) :AndroidViewModel(application){

    /** ランキングの配列 */
    val rankingVideoList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** ランキングのタグ配列 */
    val rankingTagList = MutableLiveData<ArrayList<String>>()

    /**
     * ランキングのHTMLをスクレイピングする
     * */
    fun loadRanking(genre:String,time:String,tag:String?=null){

    }

}