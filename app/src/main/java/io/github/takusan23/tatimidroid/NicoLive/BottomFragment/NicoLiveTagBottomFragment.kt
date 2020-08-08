package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

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
import io.github.takusan23.tatimidroid.NicoLive.ProgramInfoFragment
import io.github.takusan23.tatimidroid.NicoLive.Adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_tags.*
import kotlinx.coroutines.*
import org.json.JSONObject

class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    lateinit var programFragment: ProgramInfoFragment

    // RecyclerView
    var recyclerViewList = arrayListOf<NicoLiveTagAPI.NicoLiveTagItemData>()
    lateinit var tagRecyclerViewAdapter: TagRecyclerViewAdapter

    lateinit var pref_setting: SharedPreferences

    var user_session = ""
    var liveId = ""
    var tagToken = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        // LiveID
        liveId = arguments?.getString("liveId", "") ?: ""
        // タグ編集に使うトークン
        tagToken = arguments?.getString("tagToken", "") ?: ""

        bottom_fragment_tag_recyclerview.apply {
            setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            layoutManager = mLayoutManager
            tagRecyclerViewAdapter = TagRecyclerViewAdapter(recyclerViewList)
            adapter = tagRecyclerViewAdapter
        }

        tagRecyclerViewAdapter.apply {
            this.tagToken = this@NicoLiveTagBottomFragment.tagToken
            this.user_session = this@NicoLiveTagBottomFragment.user_session
            this.liveId = this@NicoLiveTagBottomFragment.liveId
            this.bottomFragment = this@NicoLiveTagBottomFragment
        }

        bottom_fragment_tag_add_button.setOnClickListener {
            // エラー
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                activity?.runOnUiThread {
                    Toast.makeText(context, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
                }
            }
            // タグ追加
            lifecycleScope.launch(errorHandler) {
                val nicoLiveTagAPI = NicoLiveTagAPI()
                val tokenText = bottom_fragment_tag_edittext.text.toString()
                // タグ追加API叩く
                val response = nicoLiveTagAPI.addTag(liveId, user_session, tagToken, tokenText)
                if (!response.isSuccessful) {
                    return@launch
                }
                programFragment.apply {
                    showToast("${getString(R.string.added)}：$tokenText")
                    // 番組情報Fragmentのタグも取得（更新）
                    coroutineGetTag()
                    tagCoroutine()
                }
            }
        }

        tagCoroutine()

    }

    // タグAPI叩く。
    fun tagCoroutine() {
        recyclerViewList.clear()
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            activity?.runOnUiThread {
                Toast.makeText(context, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
            }
        }
        lifecycleScope.launch(errorHandler) {
            val nicoLiveTagAPI = NicoLiveTagAPI()
            val response = nicoLiveTagAPI.getTags(liveId, user_session)
            if (!response.isSuccessful) {
                return@launch
            }
            val jsonString = withContext(Dispatchers.Default) {
                response.body?.string()
            }
            // RecyclerViewへ
            val list = nicoLiveTagAPI.parseTags(jsonString)
            list.forEach {
                recyclerViewList.add(it)
            }
            tagRecyclerViewAdapter.notifyDataSetChanged()
            // タグ件数
            parseTagSize(jsonString)
        }
    }

    fun parseTagSize(json: String?) {
        val jsonObject = JSONObject(json)
        val data = jsonObject.getJSONObject("data")
        val userTagCount = data.getInt("userTagCount")
        val userTagMax = data.getInt("userTagMax")
        bottom_fragment_tag_textview.text = "タグ件数：$userTagCount/$userTagMax"
    }

}