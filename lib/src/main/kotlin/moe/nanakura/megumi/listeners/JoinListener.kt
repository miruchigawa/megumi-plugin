package moe.nanakura.megumi.listeners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.nanakura.megumi.trakteer.TrakteerClient
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.logging.Logger

class JoinListener(private val client: TrakteerClient, private val logger: Logger) : Listener {
    private val scope = CoroutineScope(Dispatchers.IO)

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        scope.launch {
            try {
                val response = client.getBalance()
                if (response.status == "success") {
                    val balance = response.result
                    player.sendMessage(
                        Component.text()
                            .append(Component.text("Donation Goal: ", NamedTextColor.GOLD))
                            .append(Component.text("${balance} IDR", NamedTextColor.GREEN, TextDecoration.BOLD))
                            .append(Component.text(" has been collected for this server!", NamedTextColor.WHITE))
                            .build()
                    )
                }
            } catch (e: Exception) {
                logger.warning("Failed to fetch Trakteer balance for ${player.name}: ${e.message}")
            }
        }
    }
}
