package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoCacheFilterSortDropDown.DevNicoVideoCacheFilterSortDropdownMenuAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheFilterDataClass
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_cache_filter.*


/**
 * キャッシュ一覧でフィルターを書けるときに使うBottomSheet。
 * もうスクロールしまくるのは嫌なんや；；
 * */
class DevNicoVideoCacheFilterBottomFragment : BottomSheetDialogFragment() {

    companion object {
        val CACHE_FILTER_SORT_LIST =
            arrayListOf("取得日時が新しい順", "取得日時が古い順", "再生の多い順", "再生の少ない順", "投稿日時が新しい順", "投稿日時が古い順", "再生時間の長い順", "再生時間の短い順", "コメントの多い順", "コメントの少ない順", "マイリスト数の多い順", "マイリスト数の少ない順")
    }

    lateinit var cacheFragment: DevNicoVideoCacheFragment

    var uploaderNameList = arrayListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_cache_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 部分一致検索
        initContainsSearch()
        // 投稿者ソート
        initUploaderSort()
        // 新しい順とかソート機能
        initSortSpinner()
        // タグSpinner
        initTagSpinner()
        // スイッチ
        initSwitch()
        // リセット
        initResetButton()

        // JSONファイル（filter.json）読み込む
        readJSON()
        filter()

    }

    // 投稿者ソート
    private fun initUploaderSort() {
        // RecyclerViewのNicoVideoDataの中から投稿者の配列を取る
        uploaderNameList = cacheFragment.recyclerViewList.map { nicoVideoData ->
            nicoVideoData.uploaderName ?: ""
        }.distinct() as ArrayList<String> // 被ってる内容を消す
        uploaderNameList.remove("") // 空文字削除
        // Adapter作成
        val adapter =
            DevNicoVideoCacheFilterSortDropdownMenuAdapter(context!!, android.R.layout.simple_list_item_1, uploaderNameList)
        bottom_fragment_cache_filter_uploader_textview.setAdapter(adapter)
        bottom_fragment_cache_filter_uploader_textview.addTextChangedListener {
            filter()
        }
        bottom_fragment_cache_filter_uploader_clear.setOnClickListener {
            bottom_fragment_cache_filter_uploader_textview.setText("")
            filter()
        }
    }

    // タグのSpinner
    private fun initTagSpinner() {
        // RecyclerViewのNicoVideoDataの中からまずタグの配列を取り出す
        val tagVideoList = cacheFragment.recyclerViewList.map { nicoVideoData ->
            nicoVideoData.videoTag ?: arrayListOf()
        }
        // 全ての動画のタグを一つの配列にしてまとめる
        val tagList = tagVideoList.flatten().distinct()
        val adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, tagList)
        bottom_fragment_cache_filter_tag_autocomplete.setAdapter(adapter)
        bottom_fragment_cache_filter_tag_autocomplete.addTextChangedListener {
            if (it.toString().isNotEmpty()) {
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
                bottom_fragment_cache_filter_tag_autocomplete.setText("", false)
                filter()
            }
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
        val adapter =
            DevNicoVideoCacheFilterSortDropdownMenuAdapter(context!!, android.R.layout.simple_list_item_1, CACHE_FILTER_SORT_LIST)
        bottom_fragment_cache_filter_dropdown.setAdapter(adapter)
        bottom_fragment_cache_filter_dropdown.setText(CACHE_FILTER_SORT_LIST[0], false)
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
            dismiss()
            cacheFragment.filterDeleteMessageShow() // 本当に消していい？
        }
    }

    // 何かフィルター変更したらこれを呼べば解決！
    fun filter() {

        // 値をCacheFilterDataClassへ
        // 指定中のタグの名前配列
        val filterChipNameList = bottom_fragment_cache_filter_tag_chip.children.map { view ->
            (view as Chip).text.toString()
        }.toList()
        // JSON化する
        val cacheJson = CacheJSON()
        val cacheFilter = CacheFilterDataClass(
            bottom_fragment_cache_filter_title.text.toString(),
            bottom_fragment_cache_filter_uploader_textview.text.toString(),
            filterChipNameList,
            bottom_fragment_cache_filter_dropdown.text.toString(),
            bottom_fragment_cache_filter_has_video_json.isChecked
        )

        // リスト操作
        cacheFragment.applyFilter(cacheFilter)

        // 保存
        cacheJson.saveJSON(context, cacheJson.createJSON(cacheFilter))
    }

    // JSON取得
    private fun readJSON() {
        // filter.json取得
        val cacheJson = CacheJSON()
        val data = cacheJson.readJSON(context)
        data?.apply {
            bottom_fragment_cache_filter_title.setText(data.titleContains)
            bottom_fragment_cache_filter_uploader_textview.setText(data.uploaderName)
            bottom_fragment_cache_filter_dropdown.setText(data.sort)
            bottom_fragment_cache_filter_has_video_json.isChecked = data.isTatimiDroidGetCache
            // タグ
            data.tagItems.forEach {
                // Chip追加
                val chip = Chip(context).apply {
                    text = it
                    isCloseIconVisible = true // 閉じる追加
                    setOnCloseIconClickListener {
                        bottom_fragment_cache_filter_tag_chip.removeView(it)
                        filter()
                    }
                }
                bottom_fragment_cache_filter_tag_chip.addView(chip)
            }
        }
    }

    private fun sort(list: ArrayList<NicoVideoData>, position: Int) {
        // 選択
        when (position) {
            0 -> list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
            1 -> list.sortBy { nicoVideoData -> nicoVideoData.cacheAddedDate }
            2 -> list.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> list.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> list.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> list.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> list.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> list.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> list.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> list.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> list.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> list.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
    }


}