package moe.nanakura.megumi.commands

import io.papermc.paper.command.brigadier.BasicCommand
import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.managers.AttemptManager
import moe.nanakura.megumi.managers.GroupManager
import moe.nanakura.megumi.managers.RequestManager
import moe.nanakura.megumi.managers.WarmupManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class TeleportCommand(
        private val plugin: MegumiPlugin,
        private val requestManager: RequestManager,
        private val groupManager: GroupManager,
        private val attemptManager: AttemptManager,
        private val warmupManager: WarmupManager
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

        if (args.isEmpty()) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("tpx_usage"))
            )
            return
        }

        when (args[0].lowercase()) {
            "accept" -> handleAccept(sender)
            "deny" -> handleDeny(sender)
            "status" -> handleStatus(sender)
            else -> handleRequest(sender, args[0])
        }
    }

    private fun handleRequest(sender: Player, targetName: String) {
        if (targetName.equals(sender.name, ignoreCase = true)) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("tpx_self"))
            )
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(
                            MegumiPlugin.getMessage("player_not_found")
                                    .replace("{player}", targetName)
                    )
            )
            return
        }

        if (requestManager.hasPendingRequest(sender)) {
            sender.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("tpx_already_pending"))
            )
            return
        }

        requestManager.sendRequest(sender, target)
        sender.sendMessage(
                MegumiPlugin.translateColorCodes(
                        MegumiPlugin.getMessage("request_sent").replace("{player}", target.name)
                )
        )
        target.sendMessage(
                MegumiPlugin.translateColorCodes(
                        MegumiPlugin.getMessage("request_received").replace("{player}", sender.name)
                )
        )
    }

    private fun handleAccept(player: Player) {
        val request = requestManager.getRequest(player)
        if (request == null) {
            player.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("request_none"))
            )
            return
        }

        val sender = Bukkit.getPlayer(request.sender)
        if (sender == null || !sender.isOnline) {
            player.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("player_offline"))
            )
            requestManager.removeRequest(player)
            return
        }

        groupManager.getLimits(sender).thenAccept { limits ->
            attemptManager.canTeleport(sender, limits.maxTeleports).thenAccept { canTeleport ->
                if (!canTeleport) {
                    attemptManager.getTimeUntilNextAttempt(sender).thenAccept { wait ->
                        sender.sendMessage(
                                MegumiPlugin.translateColorCodes(
                                        MegumiPlugin.getMessage("limit_reached")
                                                .replace("{time}", wait)
                                )
                        )
                    }
                    player.sendMessage(
                            MegumiPlugin.translateColorCodes(
                                    MegumiPlugin.getMessage("tpx_limit_requester")
                            )
                    )
                    requestManager.removeRequest(player)
                    return@thenAccept
                }

                requestManager.removeRequest(player)
                sender.sendMessage(
                        MegumiPlugin.translateColorCodes(
                                MegumiPlugin.getMessage("request_accepted")
                                        .replace("{player}", player.name)
                        )
                )

                warmupManager.startWarmup(sender, player.location) {
                    attemptManager.recordAttempt(sender).thenAccept {
                        plugin.server.scheduler.runTask(
                                plugin,
                                Runnable {
                                    sender.teleport(player.location)
                                    sender.sendMessage(
                                            MegumiPlugin.translateColorCodes(
                                                    MegumiPlugin.getMessage("teleport_success")
                                                            .replace("{destination}", player.name)
                                            )
                                    )
                                }
                        )
                    }
                }
            }
        }
    }

    private fun handleDeny(player: Player) {
        val request = requestManager.getRequest(player)
        if (request == null) {
            player.sendMessage(
                    MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("request_none"))
            )
            return
        }

        val sender = Bukkit.getPlayer(request.sender)
        sender?.sendMessage(
                MegumiPlugin.translateColorCodes(
                        MegumiPlugin.getMessage("request_denied").replace("{player}", player.name)
                )
        )

        requestManager.removeRequest(player)
        player.sendMessage(
                MegumiPlugin.translateColorCodes(MegumiPlugin.getMessage("tpx_denied_confirmation"))
        )
    }

    private fun handleStatus(player: Player) {
        groupManager.getPlayerGroup(player).thenAccept { group ->
            groupManager.getLimits(player).thenAccept { limits ->
                attemptManager.getTimeUntilNextAttempt(player).thenAccept { wait ->
                    val max = if (limits.maxTeleports == -1) "∞" else limits.maxTeleports.toString()

                    player.sendMessage(
                            MegumiPlugin.translateColorCodes(
                                    "&8&m-------&r &6Teleport Status &8&m-------"
                            )
                    )
                    player.sendMessage(MegumiPlugin.translateColorCodes("&eGroup: &f${group.id}"))
                    player.sendMessage(
                            MegumiPlugin.translateColorCodes("&eLimit: &f$max attempts per 24h")
                    )
                    if (wait != "now") {
                        player.sendMessage(
                                MegumiPlugin.translateColorCodes("&eNext available in: &f$wait")
                        )
                    } else {
                        player.sendMessage(
                                MegumiPlugin.translateColorCodes("&eYou can teleport &anow&e.")
                        )
                    }
                }
            }
        }
    }
}
