package io.github.takusan23.tatimidroid.Fragment

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.NicoHistoryAdapter
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import kotlinx.android.synthetic.main.bottom_fragment_history.*
import kotlinx.android.synthetic.main.bottom_sheet_fragment_post_layout.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import java.util.*
import kotlin.collections.ArrayList

class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    lateinit var editText: EditText
    lateinit var nicoHistoryAdapter: NicoHistoryAdapter
    var recyclerViewList = arrayListOf<ArrayList<String>>()

    lateinit var nicoHistorySQLiteHelper: NicoHistorySQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //RecyclerView初期化
        initRecyclerView()

        //DB初期化
        initDB()

        // 読み込み。初回のみ実行してあとはChip押したとき読み込む
        if (recyclerViewList.size == 0) {
            loadHistory()
        }
        // 件数表示
        showDBCount()

        //削除
        bottom_fragment_history_delete_button.setOnClickListener {
            sqLiteDatabase.delete(NicoHistorySQLiteHelper.TABLE_NAME, null, null)
            dismiss()
        }

        // chip押したとき
        bottom_fragment_history_chip_live.setOnClickListener { loadHistory() }
        bottom_fragment_history_chip_video.setOnClickListener { loadHistory() }
        bottom_fragment_history_chip_today.setOnClickListener { loadHistory() }

    }

    // 件数表示
    private fun showDBCount() {
        bottom_fragment_history_textview.text =
            "${getString(R.string.history)}：${recyclerViewList.size}"
    }

    /**
     * 履歴DB読み込み。
     * */
    private fun loadHistory() {
        recyclerViewList.clear()
        val query =
            sqLiteDatabase.query(NicoHistorySQLiteHelper.TABLE_NAME, arrayOf("service_id", "type", "date", "title", "user_id"), null, null, null, null, null)
        query.moveToFirst()
        for (i in 0 until query.count) {
            val id = query.getString(0)
            val type = query.getString(1)
            val date = query.getLong(2)
            val title = query.getString(3)
            val communityId = query.getString(4)
            val item = arrayListOf<String>().apply {
                add("")
                add(id)
                add(type)
                add(date.toString())
                add(title)
                add(communityId)
            }
            recyclerViewList.add(0, item)
            query.moveToNext()
        }
        query.close()

        // 動画、生放送フィルター
        val isVideo = bottom_fragment_history_chip_video.isChecked
        val isLive = bottom_fragment_history_chip_live.isChecked
        when {
            isVideo && isLive -> {
                recyclerViewList = recyclerViewList.filter { arrayList ->
                    arrayList[2] == "video" || arrayList[2] == "live"
                } as ArrayList<ArrayList<String>>
            }
            isVideo -> {
                recyclerViewList = recyclerViewList.filter { arrayList ->
                    arrayList[2] == "video"
                } as ArrayList<ArrayList<String>>
            }
            isLive -> {
                recyclerViewList = recyclerViewList.filter { arrayList ->
                    arrayList[2] == "live"
                } as ArrayList<ArrayList<String>>
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
            recyclerViewList = recyclerViewList.filter { arrayList ->
                arrayList[3].toLong() in from..to // 範囲に入ってるか
            } as ArrayList<ArrayList<String>>
        }

        // 結果表示
        initRecyclerView()
        showDBCount()
    }

    private fun initDB() {
        nicoHistorySQLiteHelper = NicoHistorySQLiteHelper(context!!)
        sqLiteDatabase = nicoHistorySQLiteHelper.writableDatabase
        nicoHistorySQLiteHelper.setWriteAheadLoggingEnabled(false)
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