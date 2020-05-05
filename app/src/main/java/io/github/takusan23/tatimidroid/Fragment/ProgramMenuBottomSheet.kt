package io.github.takusan23.tatimidroid.Fragment

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_program_menu.*
import kotlinx.android.synthetic.main.bottom_fragment_program_reservation.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 番組の
 * TS予約、予約枠自動入場
 * 入れてほしいもの↓
 * liveId   | String | 番組ID
 * */
class ProgramMenuBottomSheet : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    // データ取得
    val nicoLiveHTML = NicoLiveHTML()

    var liveId = ""
    var userSession = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_program_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        liveId = arguments?.getString("liveId", "") ?: ""
        userSession = prefSetting.getString("user_session", "") ?: ""

        // 番組情報取得
        coroutine()

        // カレンダー追加
        initCalendarButton()

        // 共有ボタン
        initShareButton()

        // IDコピーとか
        initCopyButton()

        // TS予約とか
        initTSButton()

    }

    private fun initTSButton() {
        val timeShiftAPI = NicoLiveTimeShiftAPI()
        bottom_fragment_program_info_timeshift.setOnClickListener {
            GlobalScope.launch {
                // TS予約
                val registerTS = timeShiftAPI.registerTimeShift(liveId, userSession).await()
                if (registerTS.isSuccessful) {
                    // 成功
                    showToast(getString(R.string.timeshift_reservation_successful))
                } else if (registerTS.code == 500) {
                    // 失敗時。500エラーは登録済み
                    // 削除するか？Snackbar出す
                    Handler(Looper.getMainLooper()).post {
                        Snackbar.make(bottom_fragment_program_info_timeshift, R.string.timeshift_reserved, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.timeshift_delete_reservation_button) {
                                GlobalScope.launch {
                                    // TS削除API叩く
                                    val deleteTS =
                                        timeShiftAPI.deleteTimeShift(liveId, userSession).await()
                                    if (deleteTS.isSuccessful) {
                                        showToast(getString(R.string.timeshift_delete_reservation_successful))
                                    } else {
                                        showToast("${getString(R.string.error)}\n${deleteTS.code}")
                                    }
                                }
                            }.show()
                    }
                }
            }
        }
    }

    private fun initCopyButton() {
        bottom_fragment_program_info_community_copy.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", nicoLiveHTML.communityId))
            //コピーしました！
            Toast.makeText(context, "${getString(R.string.copy_communityid)} : ${nicoLiveHTML.communityId}", Toast.LENGTH_SHORT)
                .show()
        }
        bottom_fragment_program_info_id_copy.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            //コピーしました！
            Toast.makeText(context, "${getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun initShareButton() {
        bottom_fragment_program_info_share.setOnClickListener {
            val programShare =
                ProgramShare(activity as AppCompatActivity, bottom_fragment_program_info_share, nicoLiveHTML.programTitle, liveId)
            programShare.showShareScreen()
        }
    }

    private fun initCalendarButton() {
        bottom_fragment_program_info_calendar.setOnClickListener {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, nicoLiveHTML.programTitle)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nicoLiveHTML.programOpenTime * 1000L) // ミリ秒らしい。
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, nicoLiveHTML.programEndTime * 1000L) // ミリ秒らしい。
            }
            startActivity(intent)
        }
    }

    private fun coroutine() {
        GlobalScope.launch {
            val response = nicoLiveHTML.getNicoLiveHTML(liveId, userSession).await()
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string())
            nicoLiveHTML.initNicoLiveData(jsonObject)
            // UI反映
            activity?.runOnUiThread {
                if (isAdded) {
                    bottom_fragment_program_info_title.text = nicoLiveHTML.programTitle
                    bottom_fragment_program_info_id.text = nicoLiveHTML.liveId
                    // 時間
                    val formattedStartTime =
                        nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programOpenTime)
                    val formattedEndTime =
                        nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programStartTime)
                    bottom_fragment_program_info_time.text =
                        "開場時刻：$formattedStartTime\n終了時刻：$formattedEndTime"
                    // 項目表示
                    bottom_fragment_program_info_buttons_linearlayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}