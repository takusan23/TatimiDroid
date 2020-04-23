package io.github.takusan23.tatimidroid

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceManager

/**
 * Preferenceからメール、パスワードがあるかを確認する関数。
 * @param context Context
 * @return メール、パスワードがあればtrue
 * */
internal fun hasMailPass(context: Context?): Boolean {
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    return prefSetting.getString("mail", "")
        ?.isNotEmpty() == true && prefSetting.getString("password", "")?.isNotEmpty() == true
}