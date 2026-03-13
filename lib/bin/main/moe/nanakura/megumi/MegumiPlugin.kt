package moe.nanakura.megumi

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import moe.nanakura.megumi.commands.*
import moe.nanakura.megumi.data.StorageManager
import moe.nanakura.megumi.gui.SavePointGUI
import moe.nanakura.megumi.listeners.*
import moe.nanakura.megumi.managers.*
import moe.nanakura.megumi.trakteer.TrakteerClient
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin

class MegumiPlugin : JavaPlugin(), Listener {
    lateinit var storageManager: StorageManager
    lateinit var groupManager: GroupManager
    lateinit var attemptManager: AttemptManager
    lateinit var savePointManager: SavePointManager
    lateinit var requestManager: RequestManager
    lateinit var warmupManager: WarmupManager
    lateinit var savePointGUI: SavePointGUI

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        storageManager = StorageManager(this)

        groupManager = GroupManager(this, storageManager)
        attemptManager = AttemptManager(this, storageManager)
        savePointManager = SavePointManager(this, storageManager)
        requestManager = RequestManager(this)
        warmupManager = WarmupManager(this)

        savePointGUI = SavePointGUI(this, savePointManager, groupManager)

        val apiKey = config.getString("trakteer.api-key") ?: ""
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            logger.warning(
                    "Trakteer API key is not set in config.yml! Plugin will not function correctly."
            )
        }
        trakteerClient = TrakteerClient(apiKey)

        val manager = lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(
                    "supporter",
                    "View Trakteer supporters",
                    SupporterCommand(trakteerClient)
            )

            commands.register(
                    "sp",
                    "Save point management",
                    listOf("savepoint"),
                    SavePointCommand(
                            this,
                            savePointManager,
                            groupManager,
                            attemptManager,
                            warmupManager,
                            savePointGUI
                    )
            )

            commands.register(
                    "tpx",
                    "Player teleportation",
                    listOf("tpa"),
                    TeleportCommand(
                            this,
                            requestManager,
                            groupManager,
                            attemptManager,
                            warmupManager
                    )
            )

            commands.register(
                    "tpxadmin",
                    "Admin teleport overrides",
                    listOf(),
                    AdminCommand(this, groupManager, attemptManager, savePointManager)
            )
        }

        server.pluginManager.registerEvents(JoinListener(trakteerClient, logger), this)
        server.pluginManager.registerEvents(this, this)

        logger.info("MegumiPlugin has been enabled!")
    }

    override fun onDisable() {
        if (::trakteerClient.isInitialized) {
            trakteerClient.close()
        }
        if (::storageManager.isInitialized) {
            storageManager.close()
        }
        logger.info("MegumiPlugin has been disabled!")
    }

    companion object {
        lateinit var instance: MegumiPlugin
            private set

        fun translateColorCodes(s: String): Component =
                LegacyComponentSerializer.legacyAmpersand().deserialize(s)

        fun getMessage(path: String): String =
                instance.config.getString("messages.$path") ?: "&cMessage not found: $path"
    }

    private lateinit var trakteerClient: TrakteerClient

    // --- Teleport System Listeners ---

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (event.hasChangedBlock()) {
            warmupManager.handleMove(event.player, event.to)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity
        if (player is Player) {
            warmupManager.handleDamage(player)
        }
    }

    @EventHandler
    fun onQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        warmupManager.cancelWarmup(event.player)
    }

    @EventHandler
    fun onTeleport(event: org.bukkit.event.player.PlayerTeleportEvent) {
        // Cancel warmup if player teleports by other means (not our plugin)
        if (event.cause != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN) {
            warmupManager.cancelWarmup(event.player)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title =
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(event.view.title())

        if (title.contains("Save Points")) {
            event.isCancelled = true
            val item = event.currentItem ?: return
            if (item.type == org.bukkit.Material.COMPASS) {
                val displayName = item.itemMeta.displayName()
                val name =
                        if (displayName != null) {
                            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                    .plainText()
                                    .serialize(displayName)
                        } else null

                if (name != null) {
                    player.closeInventory()
                    player.performCommand("sp go $name")
                }
            }
        }
    }
}
