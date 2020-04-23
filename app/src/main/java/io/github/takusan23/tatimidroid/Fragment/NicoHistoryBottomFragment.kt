package io.github.takusan23.tatimidroid.Fragment

import android.database.sqlite.SQLiteDatabase
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
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import kotlinx.android.synthetic.main.bottom_fragment_history.*
import kotlinx.android.synthetic.main.bottom_sheet_fragment_post_layout.*
import kotlinx.android.synthetic.main.fragment_liveid.*

class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    lateinit var editText: EditText
    lateinit var nicoHistoryAdapter: NicoHistoryAdapter
    val recyclerViewList = arrayListOf<ArrayList<String>>()

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

        //読み込み
        loadHistory()

        //削除
        bottom_fragment_history_delete_button.setOnClickListener {
            sqLiteDatabase.delete(NicoHistorySQLiteHelper.TABLE_NAME, null, null)
            dismiss()
        }

    }

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
            //println(id)
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

        // EditText渡す
        if (activity is MainActivity) {
            nicoHistoryAdapter.editText = editText
        } else {
            val liveIDBottomFragment =
                (activity as AppCompatActivity).supportFragmentManager.findFragmentByTag("liveid_fragment") as LiveIDFragment
            nicoHistoryAdapter.editText = liveIDBottomFragment.main_activity_liveid_inputedittext
        }

        nicoHistoryAdapter.bottomSheetDialogFragment = this
    }

}