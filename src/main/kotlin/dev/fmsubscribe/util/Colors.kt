package dev.fmsubscribe.util

import net.md_5.bungee.api.ChatColor

object Colors {
    private val HEX = Regex("#([a-fA-F0-9]{6})")

    /** Превращает &-коды и #RRGGBB в цветовые коды Minecraft. */
    fun color(text: String): String {
        val withHex = HEX.replace(text) { match ->
            "&x" + match.groupValues[1].map { "&$it" }.joinToString("")
        }
        return ChatColor.translateAlternateColorCodes('&', withHex)
    }
}
