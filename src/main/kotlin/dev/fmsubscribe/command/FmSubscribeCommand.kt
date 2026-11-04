package dev.fmsubscribe.command

import dev.fmsubscribe.config.PluginSettings
import dev.fmsubscribe.data.Subscription
import dev.fmsubscribe.service.SubscriptionService
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class FmSubscribeCommand(
    private val settings: () -> PluginSettings,
    private val subscriptions: SubscriptionService,
    private val reload: () -> Unit,
) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val s = settings()
        when (args.firstOrNull()?.lowercase()) {
            "give" -> give(sender, args, s)
            "take" -> take(sender, args, s)
            "info" -> info(sender, args, s)
            "reload" -> reload(sender, args, s)
            else -> help(sender, s)
        }
        return true
    }

    private fun give(sender: CommandSender, args: Array<out String>, s: PluginSettings) {
        if (args.size != 3) return help(sender, s)
        if (!sender.hasPermission("fmsubscribe.give")) return sender.sendMessage(s.messages.noPermission)
        val target = Bukkit.getPlayerExact(args[1]) ?: return sender.sendMessage(s.messages.nullPlayer)

        val forever = args[2].equals(Subscription.FOREVER, ignoreCase = true)
        val days: Int? = if (forever) null else (parseDays(args[2]) ?: return sender.sendMessage(s.messages.incorrectDate))

        subscriptions.grant(target, days)
        val message = if (days == null) s.messages.giveForever else s.messages.giveDuration.replace("%duration%", days.toString())
        sender.sendMessage(message.replace("%player%", target.name))
    }

    private fun take(sender: CommandSender, args: Array<out String>, s: PluginSettings) {
        if (args.size != 2) return help(sender, s)
        if (!sender.hasPermission("fmsubscribe.take")) return sender.sendMessage(s.messages.noPermission)
        val target = Bukkit.getPlayerExact(args[1]) ?: return sender.sendMessage(s.messages.nullPlayer)

        if (subscriptions.revoke(target)) {
            sender.sendMessage(s.messages.take.replace("%player%", target.name))
        } else {
            sender.sendMessage(s.messages.noExists)
        }
    }

    private fun info(sender: CommandSender, args: Array<out String>, s: PluginSettings) {
        if (args.size != 2) return help(sender, s)
        if (!sender.hasPermission("fmsubscribe.info")) return sender.sendMessage(s.messages.noPermission)
        val target = Bukkit.getPlayerExact(args[1]) ?: return sender.sendMessage(s.messages.nullPlayer)

        sender.sendMessage(if (subscriptions.find(target.name) != null) s.messages.yesSub else s.messages.noSub)
    }

    private fun reload(sender: CommandSender, args: Array<out String>, s: PluginSettings) {
        if (args.size != 1) return help(sender, s)
        if (!sender.hasPermission("fmsubscribe.reload")) return sender.sendMessage(s.messages.noPermission)
        reload()
        sender.sendMessage(settings().messages.reload) // уже новые настройки
    }

    private fun help(sender: CommandSender, s: PluginSettings) {
        s.messages.noArgs.forEach { sender.sendMessage(it) }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val current = args.last()
        val options: List<String> = when (args.size) {
            1 -> PERMISSIONS.filterValues { sender.hasPermission(it) }.keys.toList()
            2 -> if (args[0].lowercase() in PLAYER_SUBCOMMANDS && sender.hasPermission(PERMISSIONS.getValue(args[0].lowercase()))) {
                Bukkit.getOnlinePlayers().map { it.name }
            } else emptyList()
            3 -> if (args[0].equals("give", ignoreCase = true) && sender.hasPermission("fmsubscribe.give")) DURATIONS else emptyList()
            else -> emptyList()
        }
        return options.filter { it.startsWith(current, ignoreCase = true) }
    }

    private fun parseDays(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in 1..MAX_DAYS }

    private companion object {
        const val MAX_DAYS = 36_500
        val PERMISSIONS = linkedMapOf(
            "give" to "fmsubscribe.give",
            "take" to "fmsubscribe.take",
            "info" to "fmsubscribe.info",
            "reload" to "fmsubscribe.reload",
        )
        val PLAYER_SUBCOMMANDS = setOf("give", "take", "info")
        val DURATIONS = listOf("10", "20", "30", "40", "50", Subscription.FOREVER)
    }
}
