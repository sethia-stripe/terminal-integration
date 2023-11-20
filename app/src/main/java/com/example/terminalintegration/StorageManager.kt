package com.example.terminalintegration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StorageManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("tie-storage", 0)

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String? = null): String? {
        return prefs.getString(key, default)
    }
}