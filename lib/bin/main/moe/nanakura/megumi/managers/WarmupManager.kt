package moe.nanakura.megumi.managers

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import moe.nanakura.megumi.MegumiPlugin
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class WarmupManager(private val plugin: MegumiPlugin) {

    private val activeWarmups = ConcurrentHashMap<UUID, WarmupTask>()

    data class WarmupTask(
            val player: Player,
            val destination: Location,
            val task: BukkitTask,
            val startLocation: Location,
            val onComplete: () -> Unit
    )

    fun startWarmup(player: Player, destination: Location, onComplete: () -> Unit) {
        cancelWarmup(player)

        val warmupSeconds = plugin.config.getInt("settings.teleport_warmup_seconds", 3)
        if (warmupSeconds <= 0) {
            onComplete()
            return
        }

        val startLocation = player.location.clone()

        val task =
                object : org.bukkit.scheduler.BukkitRunnable() {
                            var remaining = warmupSeconds

                            override fun run() {
                                if (remaining <= 0) {
                                    activeWarmups.remove(player.uniqueId)
                                    onComplete()
                                    cancel()
                                    return
                                }

                                val msg =
                                        MegumiPlugin.translateColorCodes(
                                                MegumiPlugin.getMessage("teleport_warmup")
                                                        .replace("{seconds}", remaining.toString())
                                        )
                                player.sendActionBar(msg)
                                remaining--
                            }
                        }
                        .runTaskTimer(plugin, 0L, 20L)

        activeWarmups[player.uniqueId] =
                WarmupTask(player, destination, task, startLocation, onComplete)
    }

    fun cancelWarmup(player: Player, reason: String? = null) {
        activeWarmups.remove(player.uniqueId)?.let { warmup ->
            warmup.task.cancel()
            reason?.let { msg ->
                player.sendMessage(MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage(msg)))
            }
        }
    }

    fun isWarmingUp(player: Player): Boolean {
        return activeWarmups.containsKey(player.uniqueId)
    }

    fun handleMove(player: Player, to: Location) {
        val warmup = activeWarmups[player.uniqueId] ?: return
        if (warmup.startLocation.distanceSquared(to) > 0.1) {
            cancelWarmup(player, "teleport_cancelled_move")
        }
    }

    fun handleDamage(player: Player) {
        cancelWarmup(player, "teleport_cancelled_damage")
    }
}
