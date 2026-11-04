package dev.fmsubscribe.data

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

/**
 * Хранилище data.yml. Формат совместим со старым плагином: data.<ник в нижнем регистре>.date.
 * Данные лежат в памяти, на диск пишутся асинхронно и атомарно (tmp-файл + move).
 */
class SubscriptionStorage(private val plugin: Plugin) {
    private val file = File(plugin.dataFolder, "data.yml")
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    // Записи с нечитаемой датой не теряем: возвращаем в файл как есть
    private val unreadable = ConcurrentHashMap<String, String>()

    private val pending = AtomicReference<String?>(null)
    private val ioLock = Any()

    fun get(name: String): Subscription? = subscriptions[key(name)]

    fun set(name: String, subscription: Subscription) {
        subscriptions[key(name)] = subscription
        saveAsync()
    }

    fun remove(name: String) {
        subscriptions.remove(key(name))
        saveAsync()
    }

    fun load() {
        synchronized(ioLock) {
            try {
                flushPending()
            } catch (e: IOException) {
                plugin.logger.log(Level.SEVERE, "Не удалось сохранить data.yml перед перезагрузкой", e)
            }
            val yaml = newYaml()
            if (file.exists()) {
                try {
                    yaml.load(file)
                } catch (e: Exception) {
                    val backup = File(file.parentFile, "data.yml.broken-${System.currentTimeMillis()}")
                    file.renameTo(backup)
                    plugin.logger.log(Level.SEVERE, "data.yml повреждён, он сохранён как ${backup.name}", e)
                    return
                }
            }

            val loaded = HashMap<String, Subscription>()
            val bad = HashMap<String, String>()
            for (name in yaml.getConfigurationSection("data")?.getKeys(false).orEmpty()) {
                val raw = yaml.getString("data/$name/date") ?: continue
                val parsed = Subscription.parse(raw)
                if (parsed != null) {
                    loaded[key(name)] = parsed
                } else {
                    bad[name] = raw
                    plugin.logger.warning("Некорректная дата подписки у '$name': '$raw'. Запись пропущена, но в файле сохранена.")
                }
            }
            subscriptions.clear()
            subscriptions.putAll(loaded)
            unreadable.clear()
            unreadable.putAll(bad)
        }
    }

    /** Синхронная запись, для onDisable. */
    fun saveNow() {
        synchronized(ioLock) {
            pending.set(null)
            try {
                write(snapshot())
            } catch (e: IOException) {
                plugin.logger.log(Level.SEVERE, "Не удалось сохранить data.yml", e)
            }
        }
    }

    private fun saveAsync() {
        pending.set(snapshot()) // всегда побеждает последний снимок
        try {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                synchronized(ioLock) {
                    try {
                        flushPending()
                    } catch (e: IOException) {
                        plugin.logger.log(Level.SEVERE, "Не удалось сохранить data.yml", e)
                    }
                }
            })
        } catch (e: IllegalPluginAccessException) {
            saveNow() // плагин уже выключается
        }
    }

    private fun flushPending() {
        pending.getAndSet(null)?.let { write(it) }
    }

    private fun snapshot(): String {
        val yaml = newYaml()
        unreadable.forEach { (name, raw) -> yaml.set("data/$name/date", raw) }
        subscriptions.forEach { (name, sub) -> yaml.set("data/$name/date", sub.serialize()) }
        return yaml.saveToString()
    }

    private fun write(content: String) {
        file.parentFile.mkdirs()
        val tmp = File(file.parentFile, "data.yml.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    // Разделитель пути '/', чтобы ники с точкой (Bedrock/Floodgate) не ломали структуру
    private fun newYaml() = YamlConfiguration().also { it.options().pathSeparator('/') }

    private fun key(name: String) = name.lowercase()
}
