package io.github.takusan23.tatimidroid.Fragment

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.NicoHistoryAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import kotlinx.android.synthetic.main.bottom_fragment_history.*

class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    lateinit var nicoHistoryAdapter: NicoHistoryAdapter
    val recyclerViewList = arrayListOf<ArrayList<String>>()

    lateinit var nicoHistorySQLiteHelper: NicoHistorySQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //RecyclerView初期化
        initRecyclerView()
        //DB初期化
        initDB()

        //読み込み
        loadHistory()

    }

    private fun loadHistory() {
        recyclerViewList.clear()
        val query = sqLiteDatabase.query(
            NicoHistorySQLiteHelper.TABLE_NAME,
            arrayOf("service_id", "type", "date"),
            null,
            null,
            null,
            null,
            null
        )
        query.moveToFirst()
        for (i in 0 until query.count){
            val id = query.getString(0)
            val type = query.getString(1)
            val date = query.getLong(2)
            val item = arrayListOf<String>().apply {
                add("")
                add(id)
                add(type)
                add(date.toString())
            }
            recyclerViewList.add(item)
            query.moveToNext()
        }
        query.close()
        nicoHistoryAdapter.notifyDataSetChanged()
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
    }

}