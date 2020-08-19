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
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHistoryAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_nicovideo_history.*
import kotlinx.coroutines.*

class NicoVideoHistoryFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    // RecyclerView
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    // API
    var userSession = ""
    val nicoVideoHistoryAPI = NicoVideoHistoryAPI()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        if (savedInstanceState == null) {
            // 履歴取る
            getHistory()
        } else {
            // 画面回転
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                recyclerViewList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // 引っ張った
        fragment_comment_history_swipe_to_refresh.setOnRefreshListener {
            getHistory()
        }

    }

    // 履歴取得
    private fun getHistory() {
        recyclerViewList.clear()
        fragment_comment_history_swipe_to_refresh.isRefreshing = true
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            val response = nicoVideoHistoryAPI.getHistory(userSession)
            when {
                response.isSuccessful -> {
                    withContext(Dispatchers.Default) {
                        nicoVideoHistoryAPI.parseHistoryJSONParse(response.body?.string()).forEach {
                            recyclerViewList.add(it)
                        }
                    }
                    nicoVideoListAdapter.notifyDataSetChanged()
                    fragment_comment_history_swipe_to_refresh.isRefreshing = false
                }
                response.code == 401 -> {
                    // ログイン切れ。再ログイン勧める
                    Snackbar.make(fragment_comment_history_recyclerview, R.string.login_disable_message, Snackbar.LENGTH_INDEFINITE).apply {
                        anchorView = (activity as MainActivity).main_activity_bottom_navigationview
                        setAction(R.string.login) {
                            // ログインする
                            lifecycleScope.launch {
                                userSession = NicoLogin.reNicoLogin(context)
                                getHistory()
                            }
                        }
                        show()
                    }
                }
                else -> showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    // RecyclerView初期化
    fun initRecyclerView() {
        fragment_comment_history_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

}