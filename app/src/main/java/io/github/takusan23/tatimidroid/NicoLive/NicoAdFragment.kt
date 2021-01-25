package io.github.takusan23.tatimidroid.NicoLive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.NicoLive.Adapter.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoAdHistoryAdapter
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoAdRankingAdapter
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveNicoadBinding

/**
 * ニコニ広告Fragment
 *
 * */
class NicoAdFragment : Fragment() {

    /** ギフト表示配列 */
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()

    /** ギフト表示RecyclerViewAdapter */
    val giftRecyclerViewAdapter = GiftRecyclerViewAdapter(recyclerViewList)

    /** ニコニ広告ランキング表示Adapter */
    private val nicoAdRankingAdapter = NicoAdRankingAdapter(arrayListOf())

    /** ニコニ広告履歴表示Adapter */
    private val nicoAdHistoryAdapter = NicoAdHistoryAdapter(arrayListOf())

    /** ニコニ広告APIを叩くコードはViewModelに書いてある */
    private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveNicoadBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewBinding.fragmentNicoLiveNicoadRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            // 区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        viewBinding.fragmentNicoLiveTabLayout.setBackgroundColor(getThemeColor(context))

        // APIを叩く
        if (viewModel.nicoAdLiveData.value == null) {
            viewModel.getNicoAd()
            viewModel.getNicoAdHistory()
            viewModel.getNicoAdRanking()
        }

        // トータルポイント取得
        viewModel.nicoAdLiveData.observe(viewLifecycleOwner) { data ->
            // UIに反映
            viewBinding.fragmentNicoLiveGiftTotalPointTextView.text = "${data.totalPoint}pt"
            viewBinding.fragmentNicoLiveGiftActivePointTextView.text = "${data.activePoint}pt"
        }

        // データをセット
        viewModel.nicoAdRankingLiveData.observe(viewLifecycleOwner) {
            nicoAdRankingAdapter.rankingList.clear()
            nicoAdRankingAdapter.rankingList.addAll(it)
            // 最初の画面はランキングだと思うのでセット
            viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdRankingAdapter
            viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter?.notifyDataSetChanged()
        }

        viewModel.nicoAdHistoryLiveData.observe(viewLifecycleOwner) {
            nicoAdHistoryAdapter.nicoAdHistoryList.clear()
            nicoAdHistoryAdapter.nicoAdHistoryList.addAll(it)
        }

        // TabLayout
        viewBinding.fragmentNicoLiveTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.nico_ad_history) -> {
                        viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdHistoryAdapter
                    }
                    getString(R.string.nico_ad_ranking) -> {
                        viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdRankingAdapter
                    }
                }
                viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter?.notifyDataSetChanged()
            }
        })
    }


    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}