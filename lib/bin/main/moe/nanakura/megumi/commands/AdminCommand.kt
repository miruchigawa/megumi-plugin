package moe.nanakura.megumi.commands

import io.papermc.paper.command.brigadier.BasicCommand
import java.util.*
import java.util.concurrent.CompletableFuture
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.managers.AttemptManager
import moe.nanakura.megumi.managers.GroupManager
import moe.nanakura.megumi.managers.SavePointManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class AdminCommand(
        private val plugin: MegumiPlugin,
        private val groupManager: GroupManager,
        private val attemptManager: AttemptManager,
        private val savePointManager: SavePointManager
) : BasicCommand {

    override fun execute(
            source: io.papermc.paper.command.brigadier.CommandSourceStack,
            args: Array<out String>
    ) {
        val sender = source.sender
        if (!sender.hasPermission("teleportx.admin")) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("no_permission"))
            )
            return
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }

        when (args[0].lowercase()) {
            "reset" -> handleReset(sender, args)
            "setgroup" -> handleSetGroup(sender, args)
            "clearoverride" -> handleClearOverride(sender, args)
            "info" -> handleInfo(sender, args)
            else -> sendUsage(sender)
        }
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("admin_usage")))
    }

    private fun getOfflinePlayer(name: String): CompletableFuture<org.bukkit.OfflinePlayer> {
        return CompletableFuture.supplyAsync({ Bukkit.getOfflinePlayer(name) })
    }

    private fun handleReset(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes("&cUsage: /tpxadmin reset <player>")
            )
            return
        }

        getOfflinePlayer(args[1]).thenAccept { target ->
            attemptManager.resetAttempts(target.uniqueId).thenAccept {
                sender.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                "&aReset teleport attempts for &6${target.name}&a."
                        )
                )
            }
        }
    }

    private fun handleSetGroup(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes("&cUsage: /tpxadmin setgroup <player> <group>")
            )
            return
        }

        val groupName = args[2]
        val group = GroupManager.Group.fromId(groupName)

        if (group == null) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(
                            "&cInvalid group! Available: free, supporter, admin, developer"
                    )
            )
            return
        }

        getOfflinePlayer(args[1]).thenAccept { target ->
            groupManager.setGroupOverride(target.uniqueId, group.id).thenAccept {
                sender.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                "&aSet group override for &6${target.name} &ato &6${group.id}&a."
                        )
                )
            }
        }
    }

    private fun handleClearOverride(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes("&cUsage: /tpxadmin clearoverride <player>")
            )
            return
        }

        getOfflinePlayer(args[1]).thenAccept { target ->
            groupManager.setGroupOverride(target.uniqueId, null).thenAccept {
                sender.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                "&aCleared group override for &6${target.name}&a. Now using LuckPerms."
                        )
                )
            }
        }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(MegumiPlugin.translateColorCodes("&cUsage: /tpxadmin info <player>"))
            return
        }

        getOfflinePlayer(args[1]).thenAccept { target ->
            val player = target.player
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(
                            "&8&m-------&r &6Player Info: ${target.name} &8&m-------"
                    )
            )

            if (player != null) {
                groupManager.getPlayerGroup(player).thenAccept { group ->
                    sender.sendMessage(MegumiPlugin.translateColorCodes("&eGroup: &f${group.id}"))
                    savePointManager.getSavePoints(player).thenAccept { points ->
                        sender.sendMessage(
                                MegumiPlugin.translateColorCodes("&eSave points: &f${points.size}")
                        )
                        groupManager.getLimits(player).thenAccept { limits ->
                            sender.sendMessage(
                                    MegumiPlugin.translateColorCodes(
                                            "&eMax save points: &f${if (limits.maxSavePoints == -1) "∞" else limits.maxSavePoints}"
                                    )
                            )
                        }
                    }
                }
            } else {
                sender.sendMessage(
                        MegumiPlugin.translateColorCodes("&ePlayer is offline. Basic info only.")
                )
            }
        }
    }
}
