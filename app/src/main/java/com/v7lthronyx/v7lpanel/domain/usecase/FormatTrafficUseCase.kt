package com.v7lthronyx.v7lpanel.domain.usecase

class FormatTrafficUseCase {
    fun formatGb(gb: Double): String = when {
        gb < 0.01  -> "%.1f MB".format(gb * 1024)
        gb >= 1000 -> "%.2f TB".format(gb / 1024)
        else       -> "%.2f GB".format(gb)
    }

    fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec < 1024          -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024  -> "%.1f KB/s".format(bytesPerSec / 1024.0)
        else                        -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024.0))
    }

    fun formatPercent(used: Double, limit: Double): Float =
        if (limit <= 0) 0f else (used / limit).coerceIn(0.0, 1.0).toFloat()
}
