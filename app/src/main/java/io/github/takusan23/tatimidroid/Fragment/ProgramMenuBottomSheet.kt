package io.github.takusan23.tatimidroid.Fragment

import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.os.Build
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
import io.github.takusan23.tatimidroid.AutoAdmissionService
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.NicoAPI.ProgramData
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.bottom_fragment_program_menu.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 番組の
 * TS予約、予約枠自動入場
 * 入れてほしいもの↓
 * liveId   | String | 番組ID
 * */
class ProgramMenuBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefSetting: SharedPreferences
    private lateinit var autoAdmissionSQLite: AutoAdmissionSQLiteSQLite
    private lateinit var sqLiteDatabase: SQLiteDatabase
    lateinit var programData: ProgramData

    // データ取得
    private val nicoLiveHTML = NicoLiveHTML()
    lateinit var nicoLiveJSONObject: JSONObject

    private var liveId = ""
    private var userSession = ""

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

        // 予約枠自動入場
        initDB()
        initAutoAdmission()

    }

    private fun initDB() {
        autoAdmissionSQLite = AutoAdmissionSQLiteSQLite(context!!)
        sqLiteDatabase = autoAdmissionSQLite.writableDatabase
        autoAdmissionSQLite.setWriteAheadLoggingEnabled(false)
    }

    private fun initAutoAdmission() {
        // 放送中以外で表示
        if (nicoLiveHTML.status != "ON_AIR") {
            bottom_fragment_program_info_auto_admission_more.visibility = View.VISIBLE
        }
        // 予約枠自動入場ボタン表示・非表示
        bottom_fragment_program_info_auto_admission_more.setOnClickListener {
            bottom_fragment_program_info_auto_admission_linearlayout.apply {
                if (this.visibility == View.GONE) {
                    this.visibility = View.VISIBLE
                    bottom_fragment_program_info_auto_admission_more.setCompoundDrawablesWithIntrinsicBounds(context?.getDrawable(R.drawable.ic_expand_less_black_24dp), null, null, null)
                } else {
                    this.visibility = View.GONE
                    bottom_fragment_program_info_auto_admission_more.setCompoundDrawablesWithIntrinsicBounds(context?.getDrawable(R.drawable.ic_expand_more_24px), null, null, null)
                }
            }
        }
        // 予約枠自動入場
        bottom_fragment_program_info_auto_admission_official_app.setOnClickListener { addAutoAdmissionDB(AutoAdmissionSQLiteSQLite.LAUNCH_OFFICIAL_APP) }
        bottom_fragment_program_info_auto_admission_tatimidroid_app.setOnClickListener { addAutoAdmissionDB(AutoAdmissionSQLiteSQLite.LAUNCH_TATIMIDROID_APP) }
        bottom_fragment_program_info_auto_admission_popup.setOnClickListener { addAutoAdmissionDB(AutoAdmissionSQLiteSQLite.LAUNCH_POPUP) }
        bottom_fragment_program_info_auto_admission_background.setOnClickListener { addAutoAdmissionDB(AutoAdmissionSQLiteSQLite.LAUNCH_BACKGROUND) }

        // Android 10だとバッググラウンドからActivity開けないので塞ぎ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bottom_fragment_program_info_auto_admission_tatimidroid_app.visibility = View.GONE
        }
    }

    /**
     * 予約枠自動入場DBに追加する
     * @param type AutoAdmissionSQLiteSQLite#LAUNCH_～から始まる定数のどれか。
     * */
    private fun addAutoAdmissionDB(type: String) {
        // 書き込み済みか確認
        if (!isAddedDB()) {
            // 未登録
            val contentValues = ContentValues()
            contentValues.put("name", programData.title)
            contentValues.put("liveid", programData.programId)
            contentValues.put("start", programData.beginAt)
            contentValues.put("app", type)
            contentValues.put("description", "")
            sqLiteDatabase.insert("auto_admission", null, contentValues)
            // Service再起動
            // Toastに表示させるアプリ名
            val appName = when (type) {
                AutoAdmissionSQLiteSQLite.LAUNCH_TATIMIDROID_APP -> getString(R.string.app_name)
                AutoAdmissionSQLiteSQLite.LAUNCH_OFFICIAL_APP -> getString(R.string.nicolive_app)
                AutoAdmissionSQLiteSQLite.LAUNCH_POPUP -> getString(R.string.popup_player)
                AutoAdmissionSQLiteSQLite.LAUNCH_BACKGROUND -> getString(R.string.background_play)
                else -> getString(R.string.app_name)
            }
            Toast.makeText(
                context,
                "${context?.getString(R.string.added)}\n${programData.title} ${nicoLiveHTML.iso8601ToFormat(programData.beginAt.toLong())} (${appName})",
                Toast.LENGTH_SHORT
            ).show()
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(intent)
            } else {
                context?.startService(intent)
            }
            dismiss()
        } else {
            // 登録済みなら削除できるように
            Snackbar.make(bottom_fragment_program_info_auto_admission_official_app, R.string.already_added, Snackbar.LENGTH_SHORT)
                .apply {
                    setAction(R.string.delete) {
                        deleteAutoAdmissionDB()
                        dismiss()
                    }
                    show()
                }
        }
    }

    // すでに予約枠自動入場DBに追加済みか。書き込み済みならtrue
    private fun isAddedDB(): Boolean {
        val cursor =
            sqLiteDatabase.query(AutoAdmissionSQLiteSQLite.TABLE_NAME, arrayOf("liveid"), "liveid=?", arrayOf(liveId), null, null, null)
        val count = cursor.count
        cursor.close()
        return count != 0
    }

    // 予約枠自動入場から番組を消す
    private fun deleteAutoAdmissionDB() {
        sqLiteDatabase.delete(AutoAdmissionSQLiteSQLite.TABLE_NAME, "liveid=?", arrayOf(liveId))
        //Service再起動
        val intent = Intent(context, AutoAdmissionService::class.java)
        context?.stopService(intent)
        context?.startService(intent)
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
            nicoLiveJSONObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string())
            nicoLiveHTML.initNicoLiveData(nicoLiveJSONObject)
            programData = nicoLiveHTML.getProgramData(nicoLiveJSONObject)
            // UI反映
            activity?.runOnUiThread {
                if (isAdded) {
                    bottom_fragment_program_info_title.text = nicoLiveHTML.programTitle
                    bottom_fragment_program_info_id.text = nicoLiveHTML.liveId
                    // 時間
                    val formattedStartTime =
                        nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programOpenTime)
                    val formattedEndTime =
                        nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programEndTime)
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