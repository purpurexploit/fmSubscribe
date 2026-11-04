package dev.fmsubscribe.config

import dev.fmsubscribe.util.Colors
import org.bukkit.configuration.file.FileConfiguration

class Messages(
    val noPermission: String,
    val reload: String,
    val noArgs: List<String>,
    val nullPlayer: String,
    val noExists: String,
    val giveDuration: String,
    val giveForever: String,
    val take: String,
    val incorrectDate: String,
    val noSub: String,
    val yesSub: String,
)

class PlaceholderTexts(
    val hasYes: String,
    val hasNo: String,
    val endForever: String,
    val endRemainder: String,
    val endNone: String,
)

/** Неизменяемый снимок конфига: читается один раз при старте/reload, цвета применены заранее. */
class PluginSettings(
    val symbol: String,
    val prefixSubscribed: String,
    val prefixDefault: String,
    val prefixPriority: Int,
    val groupPrefixSuffix: String,
    val commandsOnGive: List<String>,
    val commandsOnTake: List<String>,
    val messages: Messages,
    val placeholders: PlaceholderTexts,
) {
    /** Префиксы хранятся в LuckPerms «сырыми» (с &), как и принято в LuckPerms. */
    fun buildPrefix(basePrefix: String, subscribed: Boolean): String =
        if (subscribed) {
            prefixSubscribed.replace("%prefix%", basePrefix).replace("%symbol%", symbol)
        } else {
            prefixDefault.replace("%prefix%", basePrefix)
        }

    companion object {
        // Недостающие ключи JavaPlugin сам добирает из config.yml внутри jar
        fun from(cfg: FileConfiguration): PluginSettings = PluginSettings(
            symbol = cfg.raw("settings.symbol"),
            prefixSubscribed = cfg.raw("settings.prefix-1"),
            prefixDefault = cfg.raw("settings.prefix-2"),
            prefixPriority = cfg.getInt("settings.prefix-priority", 100),
            groupPrefixSuffix = cfg.raw("settings.group-prefix-suffix"),
            commandsOnGive = cfg.getStringList("settings.commands-on-give"),
            commandsOnTake = cfg.getStringList("settings.commands-on-take"),
            messages = Messages(
                noPermission = cfg.text("messages.no-permission"),
                reload = cfg.text("messages.reload"),
                noArgs = cfg.getStringList("messages.no-args").map(Colors::color),
                nullPlayer = cfg.text("messages.null-player"),
                noExists = cfg.text("messages.no-exists"),
                giveDuration = cfg.text("messages.give-duration"),
                giveForever = cfg.text("messages.give-forever"),
                take = cfg.text("messages.take"),
                incorrectDate = cfg.text("messages.incorrect-date"),
                noSub = cfg.text("messages.no-sub"),
                yesSub = cfg.text("messages.yes-sub"),
            ),
            placeholders = PlaceholderTexts(
                hasYes = cfg.text("placeholders.has.yes-sub"),
                hasNo = cfg.text("placeholders.has.no-sub"),
                endForever = cfg.text("placeholders.end.forever"),
                endRemainder = cfg.text("placeholders.end.remainder"),
                endNone = cfg.text("placeholders.end.no-sub"),
            ),
        )

        private fun FileConfiguration.raw(path: String): String = getString(path).orEmpty()
        private fun FileConfiguration.text(path: String): String = Colors.color(raw(path))
    }
}
