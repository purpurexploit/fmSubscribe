package dev.fmsubscribe.placeholder

import dev.fmsubscribe.config.PluginSettings
import dev.fmsubscribe.data.Subscription
import dev.fmsubscribe.data.daysLeft
import dev.fmsubscribe.service.SubscriptionService
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.time.LocalDate

/** %fmsubscribe_has% и %fmsubscribe_end% */
class FmSubscribeExpansion(
    private val plugin: Plugin,
    private val subscriptions: SubscriptionService,
    private val settings: () -> PluginSettings,
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "fmsubscribe"
    override fun getAuthor(): String = plugin.description.authors.firstOrNull() ?: "fmSubscribe"
    override fun getVersion(): String = plugin.description.version
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val texts = settings().placeholders
        val sub = player?.name?.let { subscriptions.find(it) }
        return when (params.lowercase()) {
            "has" -> if (sub != null) texts.hasYes else texts.hasNo
            "end" -> when (sub) {
                null -> texts.endNone
                Subscription.Forever -> texts.endForever
                is Subscription.Until -> texts.endRemainder.replace("%days%", sub.daysLeft(LocalDate.now()).toString())
            }
            else -> null
        }
    }
}
