package io.github.takusan23.tatimidroid.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.NicoHistoryAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.databinding.BottomFragmentHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 端末内履歴[NicoHistoryDB]を表示するボトムシート
 * */
class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    lateinit var editText: EditText
    lateinit var nicoHistoryAdapter: NicoHistoryAdapter
    var recyclerViewList = arrayListOf<NicoHistoryDBEntity>()

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentHistoryBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込み。初回のみ実行してあとはChip押したとき読み込む
        if (recyclerViewList.size == 0) {
            loadHistory()
        }
        // 件数表示
        showDBCount()

        // chip押したとき
        viewBinding.bottomFragmentHistoryLiveChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryVideoChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryTodayChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryDistinctChip.setOnClickListener { loadHistory() }

    }

    // 件数表示
    private fun showDBCount() {
        viewBinding.bottomFragmentHistoryTextView.text = "${getString(R.string.history)}：${recyclerViewList.size}"
    }

    /**
     * 履歴DB読み込み。
     * */
    private fun loadHistory() {
        recyclerViewList.clear()
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // DBから取り出す
                NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().getAll().forEach { history ->
                    recyclerViewList.add(0, history)
                }
                // 動画、生放送フィルター
                val isVideo = viewBinding.bottomFragmentHistoryVideoChip.isChecked
                val isLive = viewBinding.bottomFragmentHistoryLiveChip.isChecked
                when {
                    isVideo && isLive -> {
                        recyclerViewList = recyclerViewList.filter { history ->
                            history.type == "video" || history.type == "live"
                        } as ArrayList<NicoHistoryDBEntity>
                    }
                    isVideo -> {
                        recyclerViewList = recyclerViewList.filter { history ->
                            history.type == "video"
                        } as ArrayList<NicoHistoryDBEntity>
                    }
                    isLive -> {
                        recyclerViewList = recyclerViewList.filter { history ->
                            history.type == "live"
                        } as ArrayList<NicoHistoryDBEntity>
                    }
                }
                // 今日のみ
                if (viewBinding.bottomFragmentHistoryTodayChip.isChecked) {
                    // から
                    val calender = Calendar.getInstance()
                    calender.set(Calendar.HOUR, 0)
                    calender.set(Calendar.MINUTE, 0)
                    calender.set(Calendar.SECOND, 0)
                    val from = calender.time.time / 1000L
                    // まで
                    val to = System.currentTimeMillis() / 1000L
                    recyclerViewList = recyclerViewList.filter { history ->
                        history.unixTime in from..to // 範囲に入ってるか
                    } as ArrayList<NicoHistoryDBEntity>
                }
                // 重複を表示しない
                if (viewBinding.bottomFragmentHistoryDistinctChip.isChecked) {
                    recyclerViewList = recyclerViewList.distinctBy { history -> history.userId } as ArrayList<NicoHistoryDBEntity>
                }
            }
            // 結果表示
            initRecyclerView()
            showDBCount()
        }
    }

    private fun initRecyclerView() {
        viewBinding.bottomFragmentHistoryRecyclerview.apply {
            setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            layoutManager = mLayoutManager
            nicoHistoryAdapter = NicoHistoryAdapter(recyclerViewList)
            adapter = nicoHistoryAdapter
        }
        nicoHistoryAdapter.bottomSheetDialogFragment = this
    }

}