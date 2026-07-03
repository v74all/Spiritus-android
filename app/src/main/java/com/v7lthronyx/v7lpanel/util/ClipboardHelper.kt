package com.v7lthronyx.v7lpanel.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun copyToClipboard(context: Context, text: String, label: String = "Copied") {
        val cm   = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        cm.setPrimaryClip(clip)
    }
}
