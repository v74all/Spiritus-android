package com.v7lthronyx.v7lpanel.util

object RTLDetector {
    private val RTL_RANGE = Regex("[\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]")

    fun isRTL(text: String): Boolean = RTL_RANGE.containsMatchIn(text)

    fun resolveAlignment(text: String): androidx.compose.ui.text.style.TextAlign =
        if (isRTL(text)) androidx.compose.ui.text.style.TextAlign.End
        else             androidx.compose.ui.text.style.TextAlign.Start
}
