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
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHistoryAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_history.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.net.ssl.SSLProtocolException

class DevNicoVideoHistoryFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    // RecyclerView
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter

    // API
    var userSession = ""
    val nicoVideoHistoryAPI = NicoVideoHistoryAPI()
    lateinit var coroutine: Job

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
        if (::coroutine.isInitialized) {
            coroutine.cancel()
        }
        coroutine = GlobalScope.launch {
            val response = nicoVideoHistoryAPI.getHistory(userSession).await()
            try {
                when {
                    response.isSuccessful -> {
                        nicoVideoHistoryAPI.parseHistoryJSONParse(response.body?.string()).forEach {
                            recyclerViewList.add(it)
                        }
                        activity?.runOnUiThread {
                            nicoVideoListAdapter.notifyDataSetChanged()
                            fragment_comment_history_swipe_to_refresh.isRefreshing = false
                        }
                    }
                    response.code == 401 -> {
                        // ログイン切れ。再ログイン勧める
                        Snackbar.make(fragment_comment_history_recyclerview, R.string.login_disable_message, Snackbar.LENGTH_SHORT)
                            .apply {
                                setAction(R.string.login) {
                                    // ログインする
                                    GlobalScope.launch {
                                        NicoLogin.loginCoroutine(context).await()
                                        userSession =
                                            prefSetting.getString("user_session", "") ?: ""
                                        getHistory()
                                    }
                                }
                                show()
                            }
                    }
                    else -> showToast("${getString(R.string.error)}\n${response.code}")

                }
            } catch (e: SSLProtocolException) {
                // Android 9の端末でSSLProtocolExceptionがスローされるのでやりたくないけど例外処理
                e.printStackTrace()
                showToast("${activity?.getString(R.string.error)}\n$e")
            }
        }
    }

    // RecyclerView初期化
    fun initRecyclerView() {
        fragment_comment_history_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
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