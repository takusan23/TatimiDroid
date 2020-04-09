package io.github.takusan23.tatimidroid.Fragment

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.bottom_fragment_program_reservation.*
import org.json.JSONObject

class ProgramReservationBottomFragment : BottomSheetDialogFragment() {

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

    lateinit var pref_setting: SharedPreferences
    var user_session = ""

    var liveId = ""
    val nicoLiveHTML =
        NicoLiveHTML()

    var programName = ""
    var beginTime = 0L
    var endTime = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_program_reservation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモードなど
        initDarkMode()

        // 初期化とか
        initDB()
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        // 受け取る
        liveId = arguments?.getString("liveId") ?: ""
        val json = arguments?.getString("json")

        // JSONパース
        val jsonObject = JSONObject(json)
        val program = jsonObject.getJSONObject("program")
        // タイトル、開始時間等
        programName = program.getString("title")
        beginTime = program.getLong("beginTime") // アニメ一挙とかは開始数十分前から入れるため開始時間を取る必要
        endTime = program.getLong("endTime")
        // 時間フォーマット
        val beginTimeFormatted = nicoLiveHTML.iso8601ToFormat(beginTime)
        val endTimeFormatted = nicoLiveHTML.iso8601ToFormat(endTime)

        bottom_fragment_program_reservation_title_textview.text = "$programName\n$liveId"
        bottom_fragment_program_reservation_time_textview.text =
            "${beginTimeFormatted} ${getString(R.string.program_start)}\n${endTimeFormatted} ${getString(R.string.end_of_program)}"


        // TS予約
        val nicoLiveTimeShiftAPI =
            NicoLiveTimeShiftAPI(context, user_session, liveId)

        // 予約枠自動入場追加
        bottom_fragment_program_reservation_auto_admission_tatimidroid_button.setOnClickListener {
            // たちみどろいどで開く
            addAutoAdmissionDB("tatimidroid_app")
        }
        bottom_fragment_program_reservation_auto_admission_nicolive_button.setOnClickListener {
            // ニコ生で開く
            addAutoAdmissionDB("nicolive_app")
        }

        // TS予約など
        bottom_fragment_program_reservation_add_ts_button.setOnClickListener {
            nicoLiveTimeShiftAPI.registerTimeShift {
                if (it.isSuccessful) {
                    // 成功時。登録成功
                    activity?.runOnUiThread {
                        Toast.makeText(context, R.string.timeshift_reservation_successful, Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (it.code == 500) {
                    // 失敗時。(500エラー)すでに登録済み
                    activity?.runOnUiThread {
                        // 削除するか？Snackbar出す
                        Snackbar.make(
                            bottom_fragment_program_reservation_add_ts_button,
                            R.string.timeshift_reserved,
                            Snackbar.LENGTH_SHORT
                        ).setAction(R.string.timeshift_delete_reservation_button) {
                            nicoLiveTimeShiftAPI.deleteTimeShift {
                                activity?.runOnUiThread {
                                    // 削除成功
                                    Toast.makeText(context, R.string.timeshift_delete_reservation_successful, Toast.LENGTH_LONG)
                                        .show()
                                }
                            }
                        }.show()
                    }
                }
            }
        }

        // カレンダー
        bottom_fragment_program_reservation_add_calender_button.setOnClickListener {
            addCalendar()
        }

        // 共有
        bottom_fragment_program_reservation_share_button.setOnClickListener {
            showShareScreen()
        }

    }

    // ダークモードなど
    private fun initDarkMode() {
        val darkModeSupport = DarkModeSupport(context!!)
        bottom_fragment_program_reservation_parent_linearlayout.background =
            ColorDrawable(darkModeSupport.getThemeColor())
        // ボタンの色
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            ColorStateList.valueOf(Color.parseColor("#ffffff")).let {
                bottom_fragment_program_reservation_auto_admission_tatimidroid_button.setTextColor(it)
                bottom_fragment_program_reservation_auto_admission_nicolive_button.setTextColor(it)
                bottom_fragment_program_reservation_add_ts_button.setTextColor(it)
                bottom_fragment_program_reservation_add_calender_button.setTextColor(it)
                bottom_fragment_program_reservation_share_button.setTextColor(it)
            }
        }

    }

    /**
     * 予約枠自動入場に追加
     *  @param type tatimidroid_app か nicolive_app
     * */
    fun addAutoAdmissionDB(app: String) {
        val dbBeginTime = findAutoAdmissionDB()
        if (dbBeginTime == -1L) {
            // DB未登録
            //書き込む
            val contentValues = ContentValues()
            contentValues.put("name", programName)
            contentValues.put("liveid", liveId)
            contentValues.put("start", beginTime)
            contentValues.put("app", "nicolive_app")
            contentValues.put("description", "")
            sqLiteDatabase.insert("auto_admission", null, contentValues)
            // Toastに表示させるアプリ名
            val appName = if (app == "tatimidroid_app") {
                getString(R.string.app_name)
            } else {
                getString(R.string.nicolive_app)
            }
            Toast.makeText(
                context,
                "${getString(R.string.added)}\n${programName} ${nicoLiveHTML.iso8601ToFormat(beginTime)} (${appName})",
                Toast.LENGTH_SHORT
            ).show()
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            context?.startService(intent)
        } else {
            // DB登録済み
            Snackbar.make(bottom_fragment_program_reservation_parent_linearlayout, "${getString(R.string.already_added)}（${nicoLiveHTML.iso8601ToFormat(dbBeginTime)}）\nお楽しみに！", Snackbar.LENGTH_SHORT)
                .setAction(R.string.delete) {
                    // 削除する
                    deleteAutoAdmissionDB()
                    Toast.makeText(
                        context,
                        R.string.remove_auto_admission,
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()
        }
    }

    // 予約枠自動入場のデータベース初期化
    fun initDB() {
        //初期化
        autoAdmissionSQLiteSQLite =
            AutoAdmissionSQLiteSQLite(context!!)
        sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
        //読み込む速度が上がる機能？データベースファイル以外の謎ファイルが生成されるので無効化。
        autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
    }

    /**
     *  予約枠自動入場DBから番組IDを検索してくる
     *  @return 予約済みなら開始時間（UnixTime）、なければ -1
     * */
    fun findAutoAdmissionDB(): Long {
        val cursor =
            sqLiteDatabase.query(AutoAdmissionSQLiteSQLite.TABLE_NAME, arrayOf("start"), "liveid=?", arrayOf(liveId), null, null, null)
        cursor.moveToFirst()
        var beginTime = -1L
        for (i in 0 until cursor.count) {
            beginTime = cursor.getString(0).toLong()
            cursor.moveToNext()
        }
        cursor.close()
        return beginTime
    }

    /**
     * 予約枠自動入場から番組を消す
     * */
    fun deleteAutoAdmissionDB() {
        sqLiteDatabase.delete(AutoAdmissionSQLiteSQLite.TABLE_NAME, "liveid=?", arrayOf(liveId))
        //Service再起動
        val intent = Intent(context, AutoAdmissionService::class.java)
        context?.stopService(intent)
        context?.startService(intent)
    }

    /**
     * カレンダーに予定を追加する
     * https://developer.android.com/guide/components/intents-common?hl=ja#AddEvent
     * */
    fun addCalendar() {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, programName)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime * 1000L) // ミリ秒らしい。
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime * 1000L) // ミリ秒らしい。
        }
        startActivity(intent)
    }

    // 共有画面出す
    fun showShareScreen() {
        val programShare =
            ProgramShare(activity as AppCompatActivity, bottom_fragment_program_reservation_share_button, programName, liveId)
        programShare.showShareScreen()
    }

}