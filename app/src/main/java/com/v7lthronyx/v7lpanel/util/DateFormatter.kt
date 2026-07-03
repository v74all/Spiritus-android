package com.v7lthronyx.v7lpanel.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateFormatter {
    private val serverFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val displayFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeFmt  = DateTimeFormatter.ofPattern("HH:mm")

    fun parseServer(raw: String?): LocalDateTime? {
        raw ?: return null
        return try { LocalDateTime.parse(raw, serverFmt) } catch (_: DateTimeParseException) { null }
    }

    fun formatDate(raw: String?): String {
        parseServer(raw)?.let { return displayFmt.format(it) }
        return raw ?: "—"
    }

    fun formatDateTime(raw: String?): String {
        parseServer(raw)?.let { return "${displayFmt.format(it)} ${timeFmt.format(it)}" }
        return raw ?: "—"
    }

    fun isExpired(raw: String?): Boolean {
        val dt = parseServer(raw) ?: return false
        return dt.isBefore(LocalDateTime.now())
    }

    fun daysRemaining(raw: String?): Long? {
        val dt = parseServer(raw) ?: return null
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dt)
    }
}
