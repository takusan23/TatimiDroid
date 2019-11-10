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
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoLogin
import io.github.takusan23.tatimidroid.NimadoActivity
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_fragment_nimado_live_id.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
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
        nimado_liveid_bottom_fragment_button_comment_viewer_mode.setOnClickListener {
            if (!isCommunityOrChannelID()) {
                getProgramInfo("comment_viewer")
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    val liveID = getLiveIDFromCommunityID().await()
                    getProgramInfo("comment_viewer", liveID)
                }
            }
        }
        nimado_liveid_bottom_fragment_button_comment_post_mode.setOnClickListener {
            if (!isCommunityOrChannelID()) {
                getProgramInfo("comment_post")
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    val liveID = getLiveIDFromCommunityID().await()
                    getProgramInfo("comment_post", liveID)
                }
            }
        }
        nimado_liveid_bottom_fragment_button_comment_nicocas_mode.setOnClickListener {
            if (!isCommunityOrChannelID()) {
                getProgramInfo("nicocas")
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    val liveID = getLiveIDFromCommunityID().await()
                    getProgramInfo("nicocas", liveID)
                }
            }
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

    //第2引数はない場合は正規表現で取り出す
    private fun getProgramInfo(watchMode: String, liveId: String = getLiveIDRegex()) {

        //番組が終わってる場合は落ちちゃうので修正。
        val programInfo = "https://live2.nicovideo.jp/watch/${liveId}/programinfo";
        val request = Request.Builder()
            .url(programInfo)
            .header("Cookie", "user_session=${pref_setting.getString("user_session", "")}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //エラーなので閉じる
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonObject = JSONObject(response?.body?.string())
                if (response.isSuccessful) {
                    val data = jsonObject.getJSONObject("data")
                    val status = data.getString("status")
                    if (status == "onAir") {
                        //二窓追加
                        (activity as NimadoActivity).apply {
                            runOnUiThread {
                                addNimado(liveId, watchMode)
                                this@NimadoLiveIDBottomFragment.dismiss()
                            }
                        }
                    } else {
                        //番組が終わっている
                        (activity as NimadoActivity).runOnUiThread {
                            Toast.makeText(
                                context,
                                getString(R.string.program_end),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    //ニコニコにログインをし直す
                    if (context != null) {
                        //４０１エラーのときはuser_sessionが切れた
                        if (response.code == 401) {
                            val nicoLogin = NicoLogin(context!!, liveId)
                            //こるーちん？
                            //再ログインする
                            activity?.runOnUiThread {
                                dismiss()
                                Toast.makeText(
                                    context,
                                    getString(R.string.re_login),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            //ログインしてから再度取得すr
                            GlobalScope.launch {
                                nicoLogin.getUserSession()
                                getProgramInfo(watchMode)
                            }
                        } else {
                            //エラーなので閉じる
                            activity?.runOnUiThread {
                                dismiss()
                                Toast.makeText(
                                    context,
                                    getString(R.string.error),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                }
            }
        })
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