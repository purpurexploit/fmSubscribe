package dev.fmsubscribe.util

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/** Пишет одинаковое предупреждение один раз, чтобы повторяющаяся ошибка не засоряла консоль. */
class OnceLogger(private val logger: Logger) {
    private val seen = ConcurrentHashMap.newKeySet<String>()

    fun warn(key: String, message: String, error: Throwable? = null) {
        if (seen.add(key)) logger.log(Level.WARNING, "$message (дальше такие сообщения скрыты до /fmsubscribe reload)", error)
    }

    fun reset() = seen.clear()
}
