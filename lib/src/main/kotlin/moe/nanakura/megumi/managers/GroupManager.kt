package moe.nanakura.megumi.managers

import java.util.*
import java.util.concurrent.CompletableFuture
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.data.StorageManager
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.NodeType
import org.bukkit.entity.Player

class GroupManager(private val plugin: MegumiPlugin, private val storage: StorageManager) {

    private val luckPerms: LuckPerms? by lazy {
        try {
            LuckPermsProvider.get()
        } catch (e: Exception) {
            null
        }
    }

    enum class Group(val id: String, val priority: Int) {
        DEVELOPER("developer", 4),
        ADMIN("admin", 3),
        SUPPORTER("supporter", 2),
        FREE("free", 1);

        companion object {
            fun fromId(id: String): Group? =
                    Group.values().find { it.id.lowercase() == id.lowercase() }
        }
    }

    data class Limits(val maxSavePoints: Int, val maxTeleports: Int)

    fun getLimitsForGroup(group: Group): Limits {
        val path = "groups.${group.id}"
        val maxSavePoints =
                plugin.config.getInt(
                        "$path.max_savepoints",
                        when (group) {
                            Group.FREE -> 3
                            Group.SUPPORTER -> 10
                            Group.ADMIN -> 25
                            Group.DEVELOPER -> -1
                        }
                )
        val maxTeleports =
                plugin.config.getInt(
                        "$path.max_teleports",
                        when (group) {
                            Group.FREE -> 5
                            Group.SUPPORTER -> 15
                            Group.ADMIN -> 25
                            Group.DEVELOPER -> -1
                        }
                )
        return Limits(maxSavePoints, maxTeleports)
    }

    fun getPlayerGroup(player: Player): CompletableFuture<Group> {
        return storage.getPlayerData(player.uniqueId).thenApply { playerData ->
            val overrideId = playerData.groupOverride
            if (overrideId != null) {
                val group = Group.fromId(overrideId)
                if (group != null) return@thenApply group
            }

            val lp = luckPerms
            if (lp != null) {
                val user = lp.userManager.getUser(player.uniqueId)
                if (user != null) {
                    val groups = user.getNodes(NodeType.INHERITANCE).map { node -> node.groupName }

                    val detectedGroups = mutableListOf<Group>()
                    for (groupName in groups) {
                        Group.fromId(groupName)?.let { detectedGroups.add(it) }
                    }

                    if (detectedGroups.isNotEmpty()) {
                        return@thenApply detectedGroups.maxByOrNull { g -> g.priority }
                                ?: Group.FREE
                    }
                }
            }

            if (player.hasPermission("teleportx.group.developer")) return@thenApply Group.DEVELOPER
            if (player.hasPermission("teleportx.group.admin")) return@thenApply Group.ADMIN
            if (player.hasPermission("teleportx.group.supporter")) return@thenApply Group.SUPPORTER

            Group.FREE
        }
    }

    fun getLimits(player: Player): CompletableFuture<Limits> {
        if (player.hasPermission("teleportx.bypass")) {
            return CompletableFuture.completedFuture(getLimitsForGroup(Group.DEVELOPER))
        }
        return getPlayerGroup(player).thenApply { group -> getLimitsForGroup(group) }
    }

    fun setGroupOverride(uuid: UUID, groupName: String?): CompletableFuture<Void> {
        return storage.getPlayerData(uuid).thenCompose { data ->
            data.groupOverride = groupName
            storage.savePlayerData(data)
        }
    }
}
