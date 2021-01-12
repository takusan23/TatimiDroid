package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoLive.Adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.databinding.BottomFragmentTagsBinding

/** タグ編集BottomFragment */
class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentTagsBinding.inflate(layoutInflater) }

   /** ViewModelで共有 */
   private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

   // RecyclerView
   private var recyclerViewList = arrayListOf<NicoLiveTagDataClass>()
   private val tagRecyclerViewAdapter by lazy { TagRecyclerViewAdapter(recyclerViewList, viewModel) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        viewBinding.bottomFragmentTagRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tagRecyclerViewAdapter
        }

        // データを監視
        viewModel.nicoLiveTagDataListLiveData.observe(viewLifecycleOwner) { list ->
            recyclerViewList.clear()
            recyclerViewList.addAll(list)
            tagRecyclerViewAdapter.notifyDataSetChanged()
        }

        // タグ追加
        viewBinding.bottomFragmentTagAddButton.setOnClickListener {
            val tagName = viewBinding.bottomFragmentTagEditText.text.toString()
            viewModel.addTag(tagName)
        }

    }
}