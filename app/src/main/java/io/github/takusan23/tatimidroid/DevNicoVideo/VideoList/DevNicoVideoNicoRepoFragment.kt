package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoRepoAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_nicorepo.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DevNicoVideoNicoRepoFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_nicorepo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        initRecyclerView()

        // データ取得
        coroutine()

        fragment_nicovideo_nicorepo_swipe.setOnRefreshListener {
            coroutine()
        }

    }

    // ニコレポ取得
    private fun coroutine() {
        recyclerViewList.clear()
        fragment_nicovideo_nicorepo_swipe.isRefreshing = true
        val nicoRepoAPI = NicoRepoAPI()
        GlobalScope.launch {
            val response = nicoRepoAPI.getNicoRepoResponse(userSession).await()
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            val nicorepo = nicoRepoAPI.parseNicoRepoResponse(response.body?.string())
            nicorepo.forEach {
                recyclerViewList.add(it)
            }
            activity?.runOnUiThread {
                nicoVideoListAdapter.notifyDataSetChanged()
                fragment_nicovideo_nicorepo_swipe.isRefreshing = false
            }
        }
    }

    // RecyclerViewを初期化
    private fun initRecyclerView() {
        fragment_nicovideo_nicorepo_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}