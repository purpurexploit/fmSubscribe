package dev.fmsubscribe.service

import dev.fmsubscribe.config.PluginSettings
import dev.fmsubscribe.data.Subscription
import dev.fmsubscribe.data.SubscriptionStorage
import dev.fmsubscribe.util.OnceLogger
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.LocalDate
import java.util.logging.Level

/** Вся логика подписок. Методы, трогающие команды/игроков, вызывать только из основного потока. */
class SubscriptionService(
    private val plugin: Plugin,
    private val storage: SubscriptionStorage,
    private val prefixes: PrefixService,
    private val settings: () -> PluginSettings,
    private val once: OnceLogger,
) {
    fun find(playerName: String): Subscription? = storage.get(playerName)

    /** [days] == null означает «навсегда». */
    fun grant(player: Player, days: Int?) {
        val old = storage.get(player.name)
        val new = when {
            days == null -> Subscription.Forever
            old is Subscription.Until -> Subscription.Until(old.date.plusDays(days.toLong()))
            else -> Subscription.Until(LocalDate.now().plusDays(days.toLong())) // не было или была вечная, как в оригинале
        }
        storage.set(player.name, new)
        runCommands(settings().commandsOnGive, player)
        prefixes.refresh(player, subscribed = true)
    }

    /** @return false, если подписки не было. */
    fun revoke(player: Player): Boolean {
        if (storage.get(player.name) == null) return false
        runCommands(settings().commandsOnTake, player)
        storage.remove(player.name)
        prefixes.refresh(player, subscribed = false)
        return true
    }

    /** Периодическая проверка: снимает истёкшие подписки и поддерживает префиксы онлайн-игроков. */
    fun tick() {
        val today = LocalDate.now()
        for (player in plugin.server.onlinePlayers) {
            try {
                val sub = storage.get(player.name)
                if (sub != null && sub.isExpired(today)) {
                    revoke(player)
                } else {
                    prefixes.refresh(player, subscribed = sub != null)
                }
            } catch (e: Exception) {
                once.warn("tick:${player.name}:${e.javaClass.name}", "Ошибка при обработке игрока ${player.name}", e)
            }
        }
    }

    private fun runCommands(templates: List<String>, player: Player) {
        val console = plugin.server.consoleSender
        for (template in templates) {
            val command = template.replace("%player%", player.name).trim().removePrefix("/")
            if (command.isEmpty()) continue
            try {
                plugin.server.dispatchCommand(console, command)
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Ошибка при выполнении команды '$command'", e)
            }
        }
    }
}
