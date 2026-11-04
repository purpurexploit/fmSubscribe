package dev.fmsubscribe.service

import dev.fmsubscribe.config.PluginSettings
import dev.fmsubscribe.util.OnceLogger
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.PrefixNode
import net.luckperms.api.query.QueryOptions
import org.bukkit.entity.Player

/** Выставляет игроку префикс через LuckPerms API (без консольных команд). */
class PrefixService(
    private val luckPerms: LuckPerms,
    private val settings: () -> PluginSettings,
    private val once: OnceLogger,
) {
    fun refresh(player: Player, subscribed: Boolean) {
        val s = settings()
        val user = luckPerms.userManager.getUser(player.uniqueId) ?: return
        val base = basePrefix(user, s)
        if (base == null) {
            once.warn(
                "noprefix:${player.name}",
                "У игрока ${player.name} не найден префикс (ни от групп, ни собственный) - префикс подписки не выставлен",
            )
            return
        }
        val desired = s.buildPrefix(base, subscribed)
        if (currentPrefix(user, s.prefixPriority) == desired) return

        luckPerms.userManager.modifyUser(player.uniqueId) { u ->
            u.data().clear(NodeType.PREFIX.predicate { it.priority == s.prefixPriority && it.contexts.isEmpty })
            u.data().add(PrefixNode.builder(desired, s.prefixPriority).build())
        }.exceptionally { error ->
            once.warn("prefix:${player.name}", "Не удалось обновить префикс игрока ${player.name}", error)
            null
        }
    }

    /**
     * Базовый префикс игрока: самый приоритетный префикс из всех, что он наследует (его группы, любые контексты),
     * не считая того, который выставляем мы сами.
     */
    private fun basePrefix(user: User, s: PluginSettings): String? {
        val own = user.getNodes(NodeType.PREFIX)
            .filter { it.priority == s.prefixPriority && it.contexts.isEmpty }
            .toSet()
        val best = user.resolveInheritedNodes(QueryOptions.nonContextual())
            .filterIsInstance<PrefixNode>()
            .filter { it.value && it !in own }
            .maxByOrNull { it.priority }
            ?: return null
        val prefix = best.metaValue
        if (prefix.isEmpty()) return null
        return if (s.groupPrefixSuffix.isEmpty()) prefix else prefix.removeSuffix(s.groupPrefixSuffix)
    }

    private fun currentPrefix(user: User, priority: Int): String? =
        user.getNodes(NodeType.PREFIX)
            .firstOrNull { it.priority == priority && it.contexts.isEmpty }
            ?.metaValue
}
