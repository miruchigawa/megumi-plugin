package moe.nanakura.megumi.commands

import io.papermc.paper.command.brigadier.BasicCommand
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.gui.SavePointGUI
import moe.nanakura.megumi.managers.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class SavePointCommand(
        private val plugin: MegumiPlugin,
        private val savePointManager: SavePointManager,
        private val groupManager: GroupManager,
        private val attemptManager: AttemptManager,
        private val warmupManager: WarmupManager,
        private val gui: SavePointGUI
) : BasicCommand {

    override fun execute(
            source: io.papermc.paper.command.brigadier.CommandSourceStack,
            args: Array<out String>
    ) {
        val sender = source.sender
        if (sender !is Player) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes("&cThis command can only be used by players.")
            )
            return
        }

        if (args.isEmpty() || args[0].equals("gui", ignoreCase = true)) {
            gui.open(sender)
            return
        }

        when (args[0].lowercase()) {
            "save" -> handleSave(sender, args)
            "go" -> handleGo(sender, args)
            "list" -> handleList(sender)
            "delete" -> handleDelete(sender, args)
            else ->
                    sender.sendMessage(
                            MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("sp_usage"))
                    )
        }
    }

    private fun handleSave(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(MegumiPlugin.translateColorCodes("&cUsage: /sp save <name>"))
            return
        }

        val name = args[1]
        if (!savePointManager.isNameValid(name)) {
            player.sendMessage(
                    MegumiPlugin.translateColorCodes(
                            MegumiPlugin.getMessage("savepoint_invalid_name")
                    )
            )
            return
        }

        groupManager
                .getLimits(player)
                .thenCompose { limits ->
                    savePointManager.getSavePoints(player).thenApply { points ->
                        Pair(limits, points)
                    }
                }
                .thenAccept label@{ (limits, points) ->
                    val current = points.size
                    if (limits.maxSavePoints != -1 && current >= limits.maxSavePoints) {
                        player.sendMessage(
                                MegumiPlugin.translateColorCodes(
                                        MegumiPlugin.getMessage("savepoint_limit")
                                                .replace("{max}", limits.maxSavePoints.toString())
                                )
                        )
                        return@label
                    }

                    savePointManager.createSavePoint(player, name).thenAccept {
                        player.sendMessage(
                                MegumiPlugin.translateColorCodes(
                                        MegumiPlugin.getMessage("savepoint_saved")
                                                .replace("{name}", name)
                                )
                        )
                    }
                }
    }

    private fun handleGo(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(MegumiPlugin.translateColorCodes("&cUsage: /sp go <name>"))
            return
        }

        val name = args[1]
        savePointManager.getSavePoint(player, name).thenAccept { sp ->
            if (sp == null) {
                player.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                MegumiPlugin.getMessage("savepoint_not_found")
                                        .replace("{name}", name)
                        )
                )
                return@thenAccept
            }

            groupManager
                    .getLimits(player)
                    .thenCompose { limits ->
                        attemptManager.canTeleport(player, limits.maxTeleports).thenApply { can ->
                            Pair(limits, can)
                        }
                    }
                    .thenAccept label@{ (_, canTeleport) ->
                        if (!canTeleport) {
                            attemptManager.getTimeUntilNextAttempt(player).thenAccept { wait ->
                                player.sendMessage(
                                        MegumiPlugin.translateColorCodes(
                                                MegumiPlugin.getMessage("limit_reached")
                                                        .replace("{time}", wait)
                                        )
                                )
                            }
                            return@label
                        }

                        val world = Bukkit.getWorld(sp.worldName)
                        if (world == null) {
                            player.sendMessage(
                                    MegumiPlugin.translateColorCodes(
                                            "&cWorld '${sp.worldName}' is no longer available."
                                    )
                            )
                            return@label
                        }

                        val loc = org.bukkit.Location(world, sp.x, sp.y, sp.z, sp.yaw, sp.pitch)
                        warmupManager.startWarmup(player, loc) {
                            attemptManager.recordAttempt(player).thenAccept {
                                plugin.server.scheduler.runTask(
                                        plugin,
                                        Runnable {
                                            player.teleport(loc)
                                            player.sendMessage(
                                                    MegumiPlugin.translateColorCodes(
                                                            MegumiPlugin.getMessage(
                                                                            "teleport_success"
                                                                    )
                                                                    .replace(
                                                                            "{destination}",
                                                                            sp.name
                                                                    )
                                                    )
                                            )
                                        }
                                )
                            }
                        }
                    }
        }
    }

    private fun handleList(player: Player) {
        savePointManager.getSavePoints(player).thenAccept { points ->
            if (points.isEmpty()) {
                player.sendMessage(
                        MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("savepoint_none"))
                )
                return@thenAccept
            }

            player.sendMessage(
                    MegumiPlugin.translateColorCodes("&8&m-------&r &6Your Save Points &8&m-------")
            )
            points.forEach { sp ->
                val w = Bukkit.getWorld(sp.worldName)
                val status = if (w == null) "&c[Invalid World]" else "&7(${sp.worldName})"
                player.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                "&e- &6${sp.name} &7@ &f${String.format("%.1f, %.1f, %.1f", sp.x, sp.y, sp.z)} $status"
                        )
                )
            }
        }
    }

    private fun handleDelete(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(MegumiPlugin.translateColorCodes("&cUsage: /sp delete <name>"))
            return
        }

        val name = args[1]
        savePointManager.getSavePoint(player, name).thenAccept { sp ->
            if (sp == null) {
                player.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                MegumiPlugin.getMessage("savepoint_not_found")
                                        .replace("{name}", name)
                        )
                )
                return@thenAccept
            }

            savePointManager.deleteSavePoint(player, name).thenAccept {
                player.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                MegumiPlugin.getMessage("savepoint_deleted").replace("{name}", name)
                        )
                )
            }
        }
    }
}
