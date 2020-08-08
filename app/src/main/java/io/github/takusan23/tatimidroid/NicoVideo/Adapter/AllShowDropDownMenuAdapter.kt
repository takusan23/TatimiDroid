package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

// キャッシュの並び替えのAdapter。AutoCompleteTextViewで全て表示させるために参考にした：https://qiita.com/wa2c/items/2bf9172543ca29af76bc
class AllShowDropDownMenuAdapter(context: Context, layoutResourceId: Int, arrayList: ArrayList<String>) :
    ArrayAdapter<String>(context, layoutResourceId, arrayList) {

    override fun getFilter(): Filter {
        return AllShowDropDownMenuFilter()
    }

}