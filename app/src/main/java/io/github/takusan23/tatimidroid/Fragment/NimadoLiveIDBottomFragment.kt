package io.github.takusan23.tatimidroid.Fragment

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoLiveAPI.NicoLiveHTML
import io.github.takusan23.tatimidroid.NimadoActivity
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nimado_live_id.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

class NimadoLiveIDBottomFragment : BottomSheetDialogFragment() {

    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_fragment_nimado_live_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //クリップボード
        setClipBoardProgramID()

        //放送中であるかをAPIを叩き確認
        //番組IDだった

        nimado_liveid_bottom_fragment_button_comment_post_mode.setOnClickListener {
            addNimado("comment_post")
            allButtonDisable()
        }
        nimado_liveid_bottom_fragment_button_comment_nicocas_mode.setOnClickListener {
            addNimado("nicocas")
            allButtonDisable()
        }
        nimado_liveid_bottom_fragment_button_comment_viewer_mode.setOnClickListener {
            addNimado("comment_viewer")
            allButtonDisable()
        }
/*
        nimado_liveid_bottom_fragment_button.setOnClickListener {
            (activity as NimadoActivity).apply {
                addNimado(this@NimadoLiveIDBottomFragment.nimado_liveid_bottom_fragment_liveid_edittext.text.toString())
            }
            this@NimadoLiveIDBottomFragment.dismiss()
        }
*/
    }

    fun allButtonDisable() {
        nimado_liveid_bottom_fragment_button_comment_post_mode.isEnabled = false
        nimado_liveid_bottom_fragment_button_comment_nicocas_mode.isEnabled = false
        nimado_liveid_bottom_fragment_button_comment_viewer_mode.isEnabled = false
    }

    fun addNimado(mode: String) {
        GlobalScope.launch(Dispatchers.Main) {
            // コミュIDのときはコミュIDを取ってから
            val nicoLiveHTML = NicoLiveHTML()
            val user_session = pref_setting.getString("user_session", "")
            val html = if (!isCommunityOrChannelID()) {
                nicoLiveHTML.getNicoLiveHTML(getLiveIDRegex(), user_session).await()
            } else {
                val liveID = getLiveIDFromCommunityID().await()
                nicoLiveHTML.getNicoLiveHTML(liveID, user_session).await()
            }
            // JSONパース
            val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(html)
            //現在放送中か？
            val status = jsonObject.getJSONObject("program").getString("status")
            //公式番組かどうか
            val type = jsonObject.getJSONObject("program").getString("providerType")
            // 番組ID
            val programId = jsonObject.getJSONObject("program").getString("nicoliveProgramId")
            var isOfficial = false
            if (type.contains("official")) {
                isOfficial = true
            }
            if (status == "ON_AIR") {
                //生放送中！
                //二窓追加ボタン有効
                (activity as NimadoActivity).apply {
                    runOnUiThread {
                        addNimado(programId, mode, html, isOfficial, false)
                        this@NimadoLiveIDBottomFragment.dismiss()
                    }
                }
            } else {
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.program_end), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    // 番組情報を取得する
    //第2引数はない場合は正規表現で取り出す
    private fun getProgramInfo(
        watchMode: String,
        liveId: String = getLiveIDRegex()
    ): Deferred<String?> = GlobalScope.async {
        //番組が終わってる場合は落ちちゃうので修正。
        val programInfo = "https://live2.nicovideo.jp/watch/${liveId}";
        val request = Request.Builder()
            .url(programInfo)
            .header("Cookie", "user_session=${pref_setting.getString("user_session", "")}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val html = Jsoup.parse(response.body?.string())
            if (html.getElementById("embedded-data") != null) {
                val json = html.getElementById("embedded-data").attr("data-props")
                return@async json
            } else {
                return@async null
            }
        } else {
            //エラーなので閉じる
            activity?.runOnUiThread {
                dismiss()
                Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
            }
            return@async null
        }
    }

    //正規表現でURLから番組ID取る
    private fun getLiveIDRegex(): String {
        val text = nimado_liveid_bottom_fragment_liveid_edittext.text.toString()
        //正規表現で取り出す
        val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
            .matcher(SpannableString(text))
        val communityID_Matcher = Pattern.compile("(co|ch)([0-9]+)")
            .matcher(SpannableString(text))
        if (nicoID_Matcher.find()) {
            //取り出してEditTextに入れる
            return nicoID_Matcher.group()
        }
        if (communityID_Matcher.find()) {
            //取り出してEditTextに入れる
            return communityID_Matcher.group()
        }
        return ""
    }

    //正規表現でテキストがco|chか判断する
    //コミュニティIDの場合は番組IDを取ってこないといけない
    fun isCommunityOrChannelID(): Boolean {
        val text = nimado_liveid_bottom_fragment_liveid_edittext.text.toString()
        val communityID_Matcher = Pattern.compile("(co|ch)([0-9]+)")
            .matcher(SpannableString(text))
        return communityID_Matcher.find()
    }

    //コミュIDから番組IDとる
    //コルーチン練習する
    fun getLiveIDFromCommunityID(): Deferred<String> = GlobalScope.async {
        //いつまで使えるか知らんけどgetPlayerStatusつかう
        //取得中
        val user_session = pref_setting.getString("user_session", "")
        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus/${getLiveIDRegex()}")   //getplayerstatus、httpsでつながる？
            .addHeader("Cookie", "user_session=$user_session")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        //同期処理
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            //成功
            val response_string = response.body?.string()
            val document = Jsoup.parse(response_string)
            //番組ID取得
            val id = document.getElementsByTag("id").text()
            return@async id
        } else {
            activity?.runOnUiThread {
                Toast.makeText(
                    context,
                    "${getString(R.string.error)}\n${response.code}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return@async ""
    }

    //クリップボードから番組ID取り出し
    private fun setClipBoardProgramID() {
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        if (clipdata != null) {
            //正規表現で取り出す
            val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(clipdata.getItemAt(0)?.text ?: ""))
            val communityID_Matcher = Pattern.compile("(co|ch)([0-9]+)")
                .matcher(SpannableString(clipdata.getItemAt(0)?.text ?: ""))
            if (nicoID_Matcher.find()) {
                //取り出してEditTextに入れる
                val liveId = nicoID_Matcher.group()
                nimado_liveid_bottom_fragment_liveid_edittext.setText(liveId)
            }
            if (communityID_Matcher.find()) {
                //取り出してEditTextに入れる
                val liveId = communityID_Matcher.group()
                nimado_liveid_bottom_fragment_liveid_edittext.setText(liveId)
            }
        }
    }


}