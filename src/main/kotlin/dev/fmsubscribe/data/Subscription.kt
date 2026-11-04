package dev.fmsubscribe.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class Subscription {
    object Forever : Subscription()
    data class Until(val date: LocalDate) : Subscription()

    /** Подписка заканчивается с началом дня [date] (как и в оригинальном плагине). */
    fun isExpired(today: LocalDate): Boolean = this is Until && !date.isAfter(today)

    fun serialize(): String = when (this) {
        Forever -> FOREVER
        is Until -> date.toString()
    }

    companion object {
        const val FOREVER = "forever"

        /** Формат data.yml тот же, что у старого плагина: "forever" или "yyyy-MM-dd". */
        fun parse(raw: String): Subscription? {
            val value = raw.trim()
            if (value.equals(FOREVER, ignoreCase = true)) return Forever
            return runCatching { Until(LocalDate.parse(value.take(10))) }.getOrNull()
        }
    }
}

fun Subscription.Until.daysLeft(today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, date).coerceAtLeast(0)
