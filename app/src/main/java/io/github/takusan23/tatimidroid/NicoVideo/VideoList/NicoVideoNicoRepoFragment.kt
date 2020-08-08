package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoRepoAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_nicorepo.*
import kotlinx.coroutines.*

/**
 * 動画ニコレポFragment
 * */
class NicoVideoNicoRepoFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_nicorepo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        initRecyclerView()

        if (savedInstanceState == null) {
            // データ取得
            coroutine()
        } else {
            // 画面回転復帰時
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                recyclerViewList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        fragment_nicovideo_nicorepo_swipe.setOnRefreshListener {
            coroutine()
        }

    }

    // ニコレポ取得
    private fun coroutine() {
        recyclerViewList.clear()
        fragment_nicovideo_nicorepo_swipe.isRefreshing = true
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            val nicoRepoAPI = NicoRepoAPI()
            val response = nicoRepoAPI.getNicoRepoResponse(userSession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            withContext(Dispatchers.Default) {
                nicoRepoAPI.parseNicoRepoResponse(response.body?.string()).forEach {
                    recyclerViewList.add(it)
                }
            }
            nicoVideoListAdapter.notifyDataSetChanged()
            fragment_nicovideo_nicorepo_swipe.isRefreshing = false
        }
    }

    // RecyclerViewを初期化
    private fun initRecyclerView() {
        fragment_nicovideo_nicorepo_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

}