package com.v7lthronyx.v7lpanel.util

import androidx.compose.ui.graphics.Color
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import kotlin.math.roundToInt

object TrafficFormatter {
    /** Total volume (not per-second). */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val tb = gb / 1024.0
        return when {
            tb >= 1.0 -> "%.2f TB".format(tb)
            gb >= 1.0 -> "%.2f GB".format(gb)
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.0f KB".format(kb)
            else      -> "$bytes B"
        }
    }

    fun formatGb(gb: Double): String = when {
        gb >= 1000.0 -> "%.2f TB".format(gb / 1000.0)
        gb >= 1.0    -> "%.2f GB".format(gb)
        else         -> "%.0f MB".format(gb * 1024.0)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        val kbps = bytesPerSec / 1024.0
        val mbps = kbps / 1024.0
        return when {
            mbps >= 1.0  -> "%.1f MB/s".format(mbps)
            kbps >= 1.0  -> "%.0f KB/s".format(kbps)
            else         -> "${bytesPerSec} B/s"
        }
    }

    fun trafficPercent(usedGb: Double, limitGb: Double): Int {
        if (limitGb <= 0.0) return 0
        return ((usedGb / limitGb) * 100).roundToInt().coerceIn(0, 100)
    }

    fun trafficColor(percent: Int): Color = when {
        percent >= 90 -> V7LColors.red
        percent >= 70 -> V7LColors.yellow
        else          -> V7LColors.green
    }

    fun daysRemaining(expireAt: String?): Int? {
        expireAt ?: return null
        return try {
            val fmt  = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = java.time.LocalDateTime.parse(expireAt, fmt)
            val now  = java.time.LocalDateTime.now()
            java.time.temporal.ChronoUnit.DAYS.between(now, date).toInt()
        } catch (_: Exception) { null }
    }

    fun formatPercent(used: Double, limit: Double): String {
        val pct = trafficPercent(used, limit)
        return "$pct%"
    }
}
