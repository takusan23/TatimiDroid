package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

import android.content.*
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
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoLive.Activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.ProgramData
import io.github.takusan23.tatimidroid.Tool.ProgramShare
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Database.AutoAdmissionDB
import io.github.takusan23.tatimidroid.Room.Entity.AutoAdmissionDBEntity
import io.github.takusan23.tatimidroid.Room.Init.AutoAdmissionDBInit
import io.github.takusan23.tatimidroid.Service.startLivePlayService
import kotlinx.android.synthetic.main.bottom_fragment_program_menu.*
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * 番組の
 * TS予約、予約枠自動入場
 * 入れてほしいもの↓
 * liveId   | String | 番組ID
 * */
class ProgramMenuBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefSetting: SharedPreferences
    private lateinit var autoAdmissionDB: AutoAdmissionDB
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

        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }

        lifecycleScope.launch(errorHandler) {
            withContext(Dispatchers.Main) {
                // 番組情報取得
                coroutine()

                // UI反映
                applyUI()

                // 予約枠自動入場初期化
                initAutoAdmission()

                // ポップアップ再生、バッググラウンド再生
                initLiveServiceButton()

                // フローティングコメビュ
                initFloatingCommentViewer()

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
            }
        }

    }

    private fun initFloatingCommentViewer() {
        // Android 10 以降 でなお 放送中の番組の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && nicoLiveHTML.status == "ON_AIR") {
            bottom_fragment_program_info_floaing_comment_viewer.visibility = View.VISIBLE
        }
        bottom_fragment_program_info_floaing_comment_viewer.setOnClickListener {
            // フローティングコメビュ起動
            FloatingCommentViewer.showBubbles(requireContext(), liveId, "comment_post", nicoLiveHTML.programTitle, nicoLiveHTML.thumb)
        }
    }

    private fun initDB() {
        autoAdmissionDB = AutoAdmissionDBInit(requireContext()).commentCollectionDB
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
        bottom_fragment_program_info_auto_admission_official_app.setOnClickListener { addAutoAdmissionDB(AutoAdmissionDBEntity.LAUNCH_OFFICIAL_APP) }
        bottom_fragment_program_info_auto_admission_tatimidroid_app.setOnClickListener { addAutoAdmissionDB(AutoAdmissionDBEntity.LAUNCH_TATIMIDROID_APP) }
        bottom_fragment_program_info_auto_admission_popup.setOnClickListener { addAutoAdmissionDB(AutoAdmissionDBEntity.LAUNCH_POPUP) }
        bottom_fragment_program_info_auto_admission_background.setOnClickListener { addAutoAdmissionDB(AutoAdmissionDBEntity.LAUNCH_BACKGROUND) }

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
        lifecycleScope.launch(Dispatchers.Main) {
            if (!isAddedDB()) {
                // 未登録なら登録
                withContext(Dispatchers.IO) {
                    val autoAdmissionData = AutoAdmissionDBEntity(name = programData.title, liveId = programData.programId, startTime = programData.beginAt, lanchApp = type, description = "")
                    autoAdmissionDB.autoAdmissionDBDAO().insert(autoAdmissionData)
                }
                // Service再起動
                // Toastに表示させるアプリ名
                val appName = when (type) {
                    AutoAdmissionDBEntity.LAUNCH_TATIMIDROID_APP -> getString(R.string.app_name)
                    AutoAdmissionDBEntity.LAUNCH_OFFICIAL_APP -> getString(R.string.nicolive_app)
                    AutoAdmissionDBEntity.LAUNCH_POPUP -> getString(R.string.popup_player)
                    AutoAdmissionDBEntity.LAUNCH_BACKGROUND -> getString(R.string.background_play)
                    else -> getString(R.string.app_name)
                }
                Toast.makeText(context, "${context?.getString(R.string.added)}\n${programData.title} ${nicoLiveHTML.iso8601ToFormat(programData.beginAt.toLong())} (${appName})", Toast.LENGTH_SHORT).show()
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
                Snackbar.make(bottom_fragment_program_info_auto_admission_official_app, R.string.already_added, Snackbar.LENGTH_SHORT).apply {
                    setAction(R.string.delete) {
                        deleteAutoAdmissionDB()
                        dismiss()
                    }
                    show()
                }
            }
        }
    }

    // すでに予約枠自動入場DBに追加済みか。書き込み済みならtrue。コルーチン内で使ってね
    private suspend fun isAddedDB() = withContext(Dispatchers.IO) {
        autoAdmissionDB.autoAdmissionDBDAO().getLiveIdList(liveId).isNotEmpty()
    }

    // 予約枠自動入場から番組を消す
    private fun deleteAutoAdmissionDB() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                autoAdmissionDB.autoAdmissionDBDAO().deleteById(liveId)
            }
            // Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            context?.startService(intent)
        }
    }


    private fun initTSButton() {
        val timeShiftAPI = NicoLiveTimeShiftAPI()
        bottom_fragment_program_info_timeshift.setOnClickListener {
            // エラー時
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                // TS予約
                val registerTS = timeShiftAPI.registerTimeShift(liveId, userSession)
                if (registerTS.isSuccessful) {
                    // 成功
                    showToast(getString(R.string.timeshift_reservation_successful))
                } else if (registerTS.code == 500) {
                    // 失敗時。500エラーは登録済み
                    // 削除するか？Snackbar出す
                    withContext(Dispatchers.Main) {
                        Snackbar.make(bottom_fragment_program_info_timeshift, R.string.timeshift_reserved, Snackbar.LENGTH_SHORT).setAction(R.string.timeshift_delete_reservation_button) {
                            lifecycleScope.launch {
                                // TS削除API叩く
                                val deleteTS = timeShiftAPI.deleteTimeShift(liveId, userSession)
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
            Toast.makeText(context, "${getString(R.string.copy_communityid)} : ${nicoLiveHTML.communityId}", Toast.LENGTH_SHORT).show()
        }
        bottom_fragment_program_info_id_copy.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            //コピーしました！
            Toast.makeText(context, "${getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT).show()
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

    /** データ取得。取得が終わるまで一時停止する系の関数です。 */
    private suspend fun coroutine() = withContext(Dispatchers.Default) {
        val response = nicoLiveHTML.getNicoLiveHTML(liveId, userSession)
        if (!response.isSuccessful) {
            // 失敗時
            showToast("${getString(R.string.error)}\n${response.code}")
            return@withContext
        }
        nicoLiveJSONObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string())
        nicoLiveHTML.initNicoLiveData(nicoLiveJSONObject)
        programData = nicoLiveHTML.getProgramData(nicoLiveJSONObject)
    }

    /** データ取得し終わったらUI更新 */
    private fun applyUI() {
        if (isAdded) {
            bottom_fragment_program_info_title.text = nicoLiveHTML.programTitle
            bottom_fragment_program_info_id.text = nicoLiveHTML.liveId
            // 時間
            val formattedStartTime =
                nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programOpenTime)
            val formattedEndTime =
                nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programEndTime)
            bottom_fragment_program_info_time.text = "開場時刻：$formattedStartTime\n終了時刻：$formattedEndTime"
            // 項目表示
            bottom_fragment_program_info_buttons_linearlayout.visibility = View.VISIBLE
        }
    }

    private fun initLiveServiceButton() {
        // 放送中以外なら非表示
        if (nicoLiveHTML.status != "ON_AIR") {
            bottom_fragment_program_info_popup.visibility = View.GONE
            bottom_fragment_program_info_background.visibility = View.GONE
        }
        bottom_fragment_program_info_popup.setOnClickListener {
            // ポップアップ再生
            startLivePlayService(context = context, mode = "popup", liveId = liveId, isCommentPost = true, isNicocasMode = false)
        }
        bottom_fragment_program_info_background.setOnClickListener {
            // バッググラウンド再生
            startLivePlayService(context = context, mode = "background", liveId = liveId, isCommentPost = true, isNicocasMode = false)
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}