package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_cache_filter.*
import org.json.JSONArray


/**
 * キャッシュ一覧でフィルターを書けるときに使うBottomSheet。
 * もうスクロールしまくるのは嫌なんや；；
 * */
class DevNicoVideoCacheFilterBottomFragment : BottomSheetDialogFragment() {

    lateinit var cacheFragment: DevNicoVideoCacheFragment

    val sortList =
        arrayListOf("取得日時が新しい順", "取得日時が古い順", "再生の多い順", "再生の少ない順", "投稿日時が新しい順", "投稿日時が古い順", "再生時間の長い順", "再生時間の短い順", "コメントの多い順", "コメントの少ない順", "マイリスト数の多い順", "マイリスト数の少ない順")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_cache_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 部分一致検索
        initContainsSearch()
        // 新しい順とかソート機能
        initSortSpinner()
        // タグSpinner
        initTagSpinner()
        // スイッチ
        initSwitch()
        // リセット
        initResetButton()

    }

    // タグのSpinner
    private fun initTagSpinner() {
        // RecyclerViewのNicoVideoDataの中からまずタグの配列を取り出す
        val tagVideoList =
            cacheFragment.nicoVideoListAdapter.nicoVideoDataList.map { nicoVideoData ->
                nicoVideoData.videoTag ?: arrayListOf()
            }
        // 全ての動画のタグを一つの配列にしてまとめる
        val tagList = tagVideoList.flatten().distinct()
        val adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, tagList)
        bottom_fragment_cache_filter_tag_autocomplete.setAdapter(adapter)
        bottom_fragment_cache_filter_tag_autocomplete.addTextChangedListener {
            // Chip追加
            val chip = Chip(context).apply {
                text = it.toString()
                isCloseIconVisible = true // 閉じる追加
                setOnCloseIconClickListener {
                    bottom_fragment_cache_filter_tag_chip.removeView(it)
                    filter()
                }
            }
            bottom_fragment_cache_filter_tag_chip.addView(chip)
            filter()
        }
    }

    // スイッチ初期化
    private fun initSwitch() {
        bottom_fragment_cache_filter_has_video_json.setOnCheckedChangeListener { buttonView, isChecked ->
            filter()
        }
    }

    // 並び替え初期化。Spinnerって言うらしいよ。SpiCaではない。
    private fun initSortSpinner() {
        val adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, sortList)
        bottom_fragment_cache_filter_dropdown.setAdapter(adapter)
        bottom_fragment_cache_filter_dropdown.setText(sortList[0], false)
        // 文字変更イベント
        bottom_fragment_cache_filter_dropdown.addTextChangedListener {
            filter()
        }
    }

    // 部分一致検索
    private fun initContainsSearch() {
        bottom_fragment_cache_filter_title.addTextChangedListener {
            filter()
        }
    }

    // リセットボタン
    private fun initResetButton() {
        bottom_fragment_cache_filter_reset.setOnClickListener {
            cacheFragment.initRecyclerView()
            dismiss()
        }
    }

    // 何かフィルター変更したらこれを呼べば解決！
    fun filter() {

        // 部分一致
        // filter便利
        var filterList = cacheFragment.recyclerViewList.filter { nicoVideoData ->
            nicoVideoData.title.contains(bottom_fragment_cache_filter_title.text.toString())
        } as ArrayList<NicoVideoData>

        // 指定中のタグの名前配列
        val filterChipNameList =
            bottom_fragment_cache_filter_tag_chip.children.map { view -> (view as Chip).text.toString() }
                .toList()
        filterList = filterList.filter { nicoVideoData ->
            nicoVideoData.videoTag.containsAll(filterChipNameList) // 含まれているか
        } as ArrayList<NicoVideoData>

        // 並び替え
        sort(filterList, sortList.indexOf(bottom_fragment_cache_filter_dropdown.text.toString()))

        // スイッチ関係
        // このアプリで取得したキャッシュのみを表示する設定
        if (bottom_fragment_cache_filter_has_video_json.isChecked) {
            filterList =
                filterList.filter { nicoVideoData -> nicoVideoData.commentCount != "-1" } as ArrayList<NicoVideoData>
        }

        cacheFragment.initRecyclerView(filterList)

    }

    private fun sort(list: ArrayList<NicoVideoData>, position: Int) {
        val recyclerViewList = list
        // 選択
        when (position) {
            0 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
            1 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.cacheAddedDate }
            2 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
    }


}