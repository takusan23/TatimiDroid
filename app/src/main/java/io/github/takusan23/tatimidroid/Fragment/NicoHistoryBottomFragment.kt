package io.github.takusan23.tatimidroid.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.NicoHistoryAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import kotlinx.android.synthetic.main.bottom_fragment_history.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * 端末内履歴[NicoHistoryDB]を表示するボトムシート
 * */
class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    lateinit var editText: EditText
    lateinit var nicoHistoryAdapter: NicoHistoryAdapter
    var recyclerViewList = arrayListOf<NicoHistoryDBEntity>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_history, container, false)
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

        // 削除
        bottom_fragment_history_delete_button.setOnClickListener {
            showDeleteDialog()
        }

        // chip押したとき
        bottom_fragment_history_chip_live.setOnClickListener { loadHistory() }
        bottom_fragment_history_chip_video.setOnClickListener { loadHistory() }
        bottom_fragment_history_chip_today.setOnClickListener { loadHistory() }

    }

    private fun showDeleteDialog() {
        val buttons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete)))
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel)))
        }
        DialogBottomSheet(getString(R.string.delete_message), buttons) { i, bottomSheetDialogFragment ->
            GlobalScope.launch(Dispatchers.Main) {
                // 吹っ飛ばす（全削除）
                withContext(Dispatchers.IO) {
                    NicoHistoryDBInit(requireContext()).nicoHistoryDB.nicoHistoryDBDAO().deleteAll()
                }
                bottomSheetDialogFragment.dismiss()
                dismiss()
            }
        }.show(getParentFragmentManager(), "delete")
    }

    // 件数表示
    private fun showDBCount() {
        bottom_fragment_history_textview.text = "${getString(R.string.history)}：${recyclerViewList.size}"
    }

    /**
     * 履歴DB読み込み。
     * */
    private fun loadHistory() {
        recyclerViewList.clear()
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // DBから取り出す
                NicoHistoryDBInit(requireContext()).nicoHistoryDB.nicoHistoryDBDAO().getAll().forEach { history ->
                    recyclerViewList.add(0, history)
                }
                // 動画、生放送フィルター
                val isVideo = bottom_fragment_history_chip_video.isChecked
                val isLive = bottom_fragment_history_chip_live.isChecked
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
                if (bottom_fragment_history_chip_today.isChecked) {
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
            }
            // 結果表示
            initRecyclerView()
            showDBCount()
        }
    }

    private fun initRecyclerView() {
        bottom_fragment_history_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        bottom_fragment_history_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        nicoHistoryAdapter = NicoHistoryAdapter(recyclerViewList)
        bottom_fragment_history_recyclerview.adapter = nicoHistoryAdapter

        // EditText渡す
        if (::editText.isInitialized) {
            nicoHistoryAdapter.editText = editText
        } else {
            val liveIDBottomFragment =
                (activity as AppCompatActivity).supportFragmentManager.findFragmentByTag("liveid_fragment") as LiveIDFragment
            nicoHistoryAdapter.editText = liveIDBottomFragment.main_activity_liveid_inputedittext
        }

        nicoHistoryAdapter.bottomSheetDialogFragment = this
    }

}