package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoMylistAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_mylist.*
import kotlinx.coroutines.*

/**
 * マイリスト追加BottomFragment
 * */
class NicoVideoAddMylistBottomFragment : BottomSheetDialogFragment() {

    // アダプター
    lateinit var nicoVideoMylistAdapter: NicoVideoMylistAdapter
    val recyclerViewList = arrayListOf<Pair<String, String>>()

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
                spMyListAPI.parseMyListList(myListListResponse.body?.string()).forEach { myList ->
                    val pair = Pair(myList.title, myList.id)
                    recyclerViewList.add(pair)
                }
            }
            // 一覧更新
            nicoVideoMylistAdapter.notifyDataSetChanged()
        }
    }

    fun initRecyclerView() {
        bottom_fragment_nicovideo_mylist_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoMylistAdapter = NicoVideoMylistAdapter(recyclerViewList)
            nicoVideoMylistAdapter.id = arguments?.getString("id", "") ?: ""
            nicoVideoMylistAdapter.mylistBottomFragment = this@NicoVideoAddMylistBottomFragment
            adapter = nicoVideoMylistAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}