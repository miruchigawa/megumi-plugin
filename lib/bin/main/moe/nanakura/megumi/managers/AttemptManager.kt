package moe.nanakura.megumi.managers

import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.data.StorageManager
import org.bukkit.entity.Player

class AttemptManager(private val plugin: MegumiPlugin, private val storage: StorageManager) {

    private val cooldownMillis: Long
        get() = TimeUnit.HOURS.toMillis(plugin.config.getLong("settings.cooldown_hours", 24))

    /** Records a teleport attempt. */
    fun recordAttempt(player: Player): CompletableFuture<Void> {
        return storage.addTeleportAttempt(player.uniqueId, System.currentTimeMillis())
    }

    /** Checks if a player can teleport based on their limits. */
    fun canTeleport(player: Player, maxAttempts: Int): CompletableFuture<Boolean> {
        if (maxAttempts == -1) return CompletableFuture.completedFuture(true)
        return storage.getTeleportAttempts(
                        player.uniqueId,
                        System.currentTimeMillis() - cooldownMillis
                )
                .thenApply { attempts -> attempts < maxAttempts }
    }

    /** Gets the remaining time until the next attempt is available. */
    fun getTimeUntilNextAttempt(player: Player): CompletableFuture<String> {
        return storage.getOldestAttemptWithinWindow(
                        player.uniqueId,
                        System.currentTimeMillis() - cooldownMillis
                )
                .thenApply { oldest ->
                    if (oldest == null) return@thenApply "now"

                    val waitTime = (oldest + cooldownMillis) - System.currentTimeMillis()
                    if (waitTime <= 0) return@thenApply "now"

                    val hours = TimeUnit.MILLISECONDS.toHours(waitTime)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(waitTime) % 60

                    if (hours > 0) {
                        "${hours}h ${minutes}m"
                    } else {
                        "${minutes}m"
                    }
                }
    }

    /** Resets a player's attempts. */
    fun resetAttempts(uuid: UUID): CompletableFuture<Void> {
        return storage.clearTeleportAttempts(uuid)
    }

    /** Cleans up old database entries. */
    fun cleanup(): CompletableFuture<Void> {
        return storage.cleanOldAttempts(System.currentTimeMillis() - cooldownMillis)
    }
}
