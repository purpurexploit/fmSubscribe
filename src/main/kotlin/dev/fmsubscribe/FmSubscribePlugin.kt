package dev.fmsubscribe

import dev.fmsubscribe.command.FmSubscribeCommand
import dev.fmsubscribe.config.PluginSettings
import dev.fmsubscribe.data.SubscriptionStorage
import dev.fmsubscribe.placeholder.FmSubscribeExpansion
import dev.fmsubscribe.service.PrefixService
import dev.fmsubscribe.service.SubscriptionService
import dev.fmsubscribe.util.OnceLogger
import net.luckperms.api.LuckPermsProvider
import org.bukkit.plugin.java.JavaPlugin

class FmSubscribePlugin : JavaPlugin() {
    @Volatile
    private lateinit var settings: PluginSettings
    private lateinit var storage: SubscriptionStorage
    private lateinit var once: OnceLogger
    private var expansion: FmSubscribeExpansion? = null

    override fun onEnable() {
        val luckPerms = try {
            LuckPermsProvider.get()
        } catch (e: IllegalStateException) {
            logger.severe("LuckPerms API недоступен: ${e.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        saveDefaultConfig()
        settings = PluginSettings.from(config)
        once = OnceLogger(logger)

        storage = SubscriptionStorage(this).also { it.load() }
        val prefixes = PrefixService(luckPerms, { settings }, once)
        val subscriptions = SubscriptionService(this, storage, prefixes, { settings }, once)

        val handler = FmSubscribeCommand({ settings }, subscriptions, ::reloadAll)
        val command = getCommand("fmsubscribe")
        if (command == null) {
            logger.severe("Команда fmsubscribe не найдена в plugin.yml")
            server.pluginManager.disablePlugin(this)
            return
        }
        command.setExecutor(handler)
        command.tabCompleter = handler

        expansion = FmSubscribeExpansion(this, subscriptions) { settings }.also { it.register() }

        server.scheduler.runTaskTimer(this, Runnable { subscriptions.tick() }, 0L, CHECK_INTERVAL_TICKS)
        logger.info("fmSubscribe v${description.version} включён")
    }

    override fun onDisable() {
        expansion?.unregister()
        if (::storage.isInitialized) storage.saveNow()
    }

    private fun reloadAll() {
        reloadConfig()
        settings = PluginSettings.from(config)
        once.reset()
        storage.load()
    }

    private companion object {
        const val CHECK_INTERVAL_TICKS = 60L // 3 секунды, как в оригинале
    }
}
