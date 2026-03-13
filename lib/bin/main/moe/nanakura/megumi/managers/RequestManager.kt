package moe.nanakura.megumi.managers

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import moe.nanakura.megumi.MegumiPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class RequestManager(private val plugin: MegumiPlugin) {

    private val pendingRequests = ConcurrentHashMap<UUID, TeleportRequest>()

    data class TeleportRequest(val sender: UUID, val target: UUID, val timestamp: Long)

    fun sendRequest(sender: Player, target: Player) {
        val request = TeleportRequest(sender.uniqueId, target.uniqueId, System.currentTimeMillis())
        pendingRequests[target.uniqueId] = request

        val expireSeconds = plugin.config.getLong("settings.request_expire_seconds", 30)
        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        Runnable {
                            val current = pendingRequests[target.uniqueId]
                            if (current != null && current.timestamp == request.timestamp) {
                                pendingRequests.remove(target.uniqueId)
                                sender.sendMessage(
                                        MegumiPlugin.translateColorCodes(
                                                MegumiPlugin.getMessage("request_expired")
                                                        .replace("{player}", target.name)
                                        )
                                )
                            }
                        },
                        expireSeconds * 20L
                )
    }

    fun getRequest(target: Player): TeleportRequest? {
        return pendingRequests[target.uniqueId]
    }

    fun removeRequest(target: Player) {
        pendingRequests.remove(target.uniqueId)
    }

    fun hasPendingRequest(sender: Player): Boolean {
        return pendingRequests.values.any { it.sender == sender.uniqueId }
    }
}
