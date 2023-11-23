package com.sethia.terminalintegration

import android.content.Context
import com.sethia.terminalintegration.payments.model.ReaderInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReaderStorageManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("tie-storage", 0)

    companion object {
        private const val KEY_READER_ID = "pk-reader-id"
        private const val KEY_READER_LABEL = "pk-reader-label"
    }

    fun saveReader(readerInfo: ReaderInfo) {
        prefs.edit().apply {
            putString(KEY_READER_ID, readerInfo.id)
            putString(KEY_READER_LABEL, readerInfo.label)
            apply()
        }
    }

    fun removeSavedReader() {
        prefs.edit().apply {
            remove(KEY_READER_ID)
            remove(KEY_READER_LABEL)
            apply()
        }
    }

    fun getSavedReader(): ReaderInfo? {
        val id = prefs.getString(KEY_READER_ID, null)
        val label = prefs.getString(KEY_READER_LABEL, null)
        return if (!id.isNullOrBlank() && !label.isNullOrBlank()) {
            ReaderInfo(id, label)
        } else null
    }
}