package moe.nanakura.megumi.managers

import java.util.*
import java.util.concurrent.CompletableFuture
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.data.SavePoint
import moe.nanakura.megumi.data.StorageManager
import org.bukkit.entity.Player

class SavePointManager(private val plugin: MegumiPlugin, private val storage: StorageManager) {

    fun createSavePoint(player: Player, name: String): CompletableFuture<Void> {
        val loc = player.location
        val savePoint =
                SavePoint(
                        owner = player.uniqueId,
                        name = name,
                        worldName = loc.world?.name ?: "world",
                        x = loc.x,
                        y = loc.y,
                        z = loc.z,
                        yaw = loc.yaw,
                        pitch = loc.pitch
                )
        return storage.saveSavePoint(savePoint)
    }

    fun deleteSavePoint(player: Player, name: String): CompletableFuture<Void> {
        return storage.deleteSavePoint(player.uniqueId, name)
    }

    fun getSavePoints(player: Player): CompletableFuture<List<SavePoint>> {
        return storage.getSavePoints(player.uniqueId)
    }

    fun getSavePoint(player: Player, name: String): CompletableFuture<SavePoint?> {
        return getSavePoints(player).thenApply { points ->
            points.find { it.name.equals(name, ignoreCase = true) }
        }
    }

    fun isNameValid(name: String): Boolean {
        return name.length <= 20 && name.matches(Regex("^[a-zA-Z0-9_]+$"))
    }
}
