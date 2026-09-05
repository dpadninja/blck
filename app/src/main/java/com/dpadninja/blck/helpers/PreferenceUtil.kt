package com.dpadninja.blck.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

open class PreferenceUtil {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }

    fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    fun getInt(key: String, defValue: Int): Int = prefs.getInt(key, defValue)

    fun getBoolean(key: String, defValue: Boolean): Boolean = prefs.getBoolean(key, defValue)

    fun putStringSet(key: String, value: Set<String>) = prefs.edit { putStringSet(key, value) }

    fun getStringSet(key: String): Set<String> =
        prefs.getStringSet(key, null)?.toSet() ?: emptySet()
}
