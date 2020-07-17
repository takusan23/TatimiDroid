package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoMylistAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_mylist.*
import kotlinx.coroutines.*
import org.json.JSONObject

class DevNicoVideoAddMylistBottomFragment : BottomSheetDialogFragment() {

    // アダプター
    lateinit var nicoVideoMylistAdapter: DevNicoVideoMylistAdapter
    val recyclerViewList = arrayListOf<Pair<String, String>>()

    // ユーザーセッション
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // マイリスト
    val myListAPI =
        NicoVideoMyListAPI()

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
            showToast("${getString(R.string.error)}\n${throwable.message}")
        }
        GlobalScope.launch(errorHandler) {
            recyclerViewList.clear()
            // マイリストToken取得
            val mylistHTMLResponse = myListAPI.getMyListHTML(userSession)
            if (mylistHTMLResponse.isSuccessful) {
                val token = myListAPI.getToken(mylistHTMLResponse.body?.string())
                if (token != null) {
                    // マイリスト一覧取得
                    val listResponse = myListAPI.getMyListList(token, userSession)
                    if (listResponse.isSuccessful) {
                        val jsonObject = JSONObject(listResponse.body?.string())
                        val mylistGroup = jsonObject.getJSONArray("mylistgroup")
                        // 追加
                        for (i in 0 until mylistGroup.length()) {
                            val title = mylistGroup.getJSONObject(i).getString("name")
                            val id = mylistGroup.getJSONObject(i).getString("id")
                            val pair = Pair(title, id)
                            recyclerViewList.add(pair)
                        }
                        withContext(Dispatchers.Main) {
                            nicoVideoMylistAdapter.notifyDataSetChanged()
                        }
                    } else {
                        showToast("${getString(R.string.error)}\n${mylistHTMLResponse.code}")
                    }
                }
            } else {
                showToast("${getString(R.string.error)}\n${mylistHTMLResponse.code}")
            }
        }
    }

    fun initRecyclerView() {
        bottom_fragment_nicovideo_mylist_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoMylistAdapter = DevNicoVideoMylistAdapter(recyclerViewList)
            nicoVideoMylistAdapter.id = arguments?.getString("id", "") ?: ""
            nicoVideoMylistAdapter.mylistBottomFragment = this@DevNicoVideoAddMylistBottomFragment
            adapter = nicoVideoMylistAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}