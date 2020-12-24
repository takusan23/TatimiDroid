package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAddMyListAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_mylist.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * マイリスト追加BottomFragment
 * */
class NicoVideoAddMylistBottomFragment : BottomSheetDialogFragment() {

    // アダプター
    lateinit var nicoVideoAddMyListAdapter: NicoVideoAddMyListAdapter
    val recyclerViewList = arrayListOf<NicoVideoSPMyListAPI.MyListData>()

    // ユーザーセッション
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // スマホ版マイリストAPI
    private val spMyListAPI = NicoVideoSPMyListAPI()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        // マイリスト取得
        coroutine()

    }


    fun coroutine() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            recyclerViewList.clear()
            // マイリスト一覧APIを叩く
            val myListListResponse = spMyListAPI.getMyListList(userSession)
            if (!myListListResponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${myListListResponse.code}")
                return@launch
            }
            // レスポンスをパースしてRecyclerViewに突っ込む
            withContext(Dispatchers.Default) {
                recyclerViewList.addAll(spMyListAPI.parseMyListList(myListListResponse.body?.string()))
            }
            // 一覧更新
            nicoVideoAddMyListAdapter.notifyDataSetChanged()
        }
    }

    fun initRecyclerView() {
        bottom_fragment_nicovideo_mylist_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoAddMyListAdapter = NicoVideoAddMyListAdapter(recyclerViewList)
            nicoVideoAddMyListAdapter.id = arguments?.getString("id", "") ?: ""
            nicoVideoAddMyListAdapter.mylistBottomFragment = this@NicoVideoAddMylistBottomFragment
            adapter = nicoVideoAddMyListAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}