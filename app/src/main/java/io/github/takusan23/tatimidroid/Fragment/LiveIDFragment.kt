package io.github.takusan23.tatimidroid.Fragment

import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import io.github.takusan23.tatimidroid.Activity.KonoApp
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.NimadoActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.regex.Pattern

class LiveIDFragment : Fragment() {
    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_liveid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //タイトル
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.liveid_fragment)

        //クリップボードに番組IDが含まれてればテキストボックスに自動で入れる
        setClipBoardProgramID()

        main_activity_button.setOnClickListener {
            //liveIdを取る。「https://live2.nicovideo.jp/watch/」をからの文字に置き換えてもいいんだけど後ろにパラメーターあるかもだし
            //スマホ用ページもあるからなあ

            // 正規表現
            val nicoIDMatcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(main_activity_liveid_inputedittext.text.toString()))
            val communityIDMatcher = Pattern.compile("(co|ch)([0-9]+)")
                .matcher(SpannableString(main_activity_liveid_inputedittext.text.toString()))
            val nicoVideoIdMatcher = Pattern.compile("(sm|so)([0-9]+)")
                .matcher(SpannableString(main_activity_liveid_inputedittext.text.toString()))
            when {
                nicoIDMatcher.find() -> {
                    val liveId = nicoIDMatcher.group()
                    //メアド、パスワードがあるか
                    if (pref_setting.getString("password", "")?.isNotEmpty() == true) {
                        // SnackbarProgress(context!!, main_activity_button, getString(R.string.loading)).show()
                        //ダイアログ
                        val bundle = Bundle()
                        bundle.putString("liveId", liveId)
                        val dialog = BottomSheetDialogWatchMode()
                        dialog.arguments = bundle
                        dialog.show((activity as AppCompatActivity).supportFragmentManager, "watchmode")
                        //val intent = Intent(context, CommentActivity::class.java)
                        //intent.putExtra("liveId", liveId)
                        //startActivity(intent)
                    } else {
                        Toast.makeText(context, getString(R.string.mail_pass_error), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                communityIDMatcher.find() -> {
                    //コニュニティIDから生放送IDを出す。
                    //getPlayerStatusで放送中の場合はコミュニティIDを入れれば使える
                    Toast.makeText(context, R.string.program_id_from_community_id, Toast.LENGTH_SHORT)
                        .show()
                    GlobalScope.launch {
                        var liveId = ""
                        async {
                            liveId = getProgramIDfromCommunityID() ?: ""
                        }.await()
                        if (liveId.isNotEmpty()) {
                            //ダイアログ
                            val bundle = Bundle()
                            bundle.putString("liveId", liveId)
                            val dialog = BottomSheetDialogWatchMode()
                            dialog.arguments = bundle
                            dialog.show(
                                (activity as AppCompatActivity).supportFragmentManager,
                                "watchmode"
                            )
                        }
                    }
                }
                nicoVideoIdMatcher.find() -> {
                    val nicoVideo = nicoVideoIdMatcher.group()
                    val intent = Intent(context, NicoVideoActivity::class.java)
                    intent.putExtra("id", nicoVideo)
                    intent.putExtra("cache", false)
                    context?.startActivity(intent)
                }
                else -> {
                    //正規表現失敗
                    Toast.makeText(context, getString(R.string.regix_error), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        /*
        * 二窓コメビュ
        * */
        main_activity_nimado_button.setOnClickListener {
            val intent = Intent(context, NimadoActivity::class.java)
            activity?.startActivity(intent)
        }

        // 履歴機能
        main_activity_liveid_history_button.setOnClickListener {
            val nicoHistoryBottomFragment = NicoHistoryBottomFragment()
            nicoHistoryBottomFragment.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "history"
            )
        }
    }

    //クリップボードから番組ID取り出し
    private fun setClipBoardProgramID() {
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        if (clipdata != null) {
            val clipboardText = clipdata.getItemAt(0)?.text


            // 正規表現
            val nicoIDMatcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(clipboardText))
            val communityIDMatcher = Pattern.compile("(co|ch)([0-9]+)")
                .matcher(SpannableString(clipboardText))
            val nicoVideoIdMatcher = Pattern.compile("(sm|so)([0-9]+)")
                .matcher(SpannableString(clipboardText))
            when {
                nicoIDMatcher.find() -> {
                    //取り出してEditTextに入れる
                    val liveId = nicoIDMatcher.group()
                    main_activity_liveid_inputedittext.setText(liveId)
                    //テキスト入れたよSnackbar
                    val snackbar = Snackbar.make(
                        main_activity_liveid_inputedittext,
                        getString(R.string.set_clipbord_programid),
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.setAnchorView((activity as MainActivity).main_activity_bottom_navigationview)
                    snackbar.show()
                }
                communityIDMatcher.find() -> {
                    //取り出してEditTextに入れる
                    val liveId = communityIDMatcher.group()
                    main_activity_liveid_inputedittext.setText(liveId)
                    //テキスト入れたよSnackbar
                    val snackbar = Snackbar.make(
                        main_activity_liveid_inputedittext,
                        getString(R.string.set_clipbord_programid),
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.setAnchorView((activity as MainActivity).main_activity_bottom_navigationview)
                    snackbar.show()
                }
                nicoVideoIdMatcher.find() -> {
                    //取り出してEditTextに入れる
                    val liveId = nicoVideoIdMatcher.group()
                    main_activity_liveid_inputedittext.setText(liveId)
                    //テキスト入れたよSnackbar
                    val snackbar = Snackbar.make(
                        main_activity_liveid_inputedittext,
                        getString(R.string.set_clipbord_programid),
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.setAnchorView((activity as MainActivity).main_activity_bottom_navigationview)
                    snackbar.show()
                }
                else -> {
                   // //正規表現失敗
                   // Toast.makeText(context, getString(R.string.regix_error), Toast.LENGTH_SHORT)
                   //     .show()
                }
            }

        }
    }

    //コニュニティIDから
    fun getProgramIDfromCommunityID(): String? {
        //取得中
        val user_session = pref_setting.getString("user_session", "")
        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus/${main_activity_liveid_inputedittext.text.toString()}")   //getplayerstatus、httpsでつながる？
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
            return id
        } else {
            activity?.runOnUiThread {
                Toast.makeText(
                    context,
                    "${getString(R.string.error)}\n${response.code}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return ""
    }

}