package moe.nanakura.megumi

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import moe.nanakura.megumi.commands.SupporterCommand;
import moe.nanakura.megumi.listeners.JoinListener;
import moe.nanakura.megumi.trakteer.TrakteerClient;

class MegumiPlugin : JavaPlugin() {
    private lateinit var trakteerClient: TrakteerClient

    override fun onEnable() {
        saveDefaultConfig()
        val apiKey = config.getString("trakteer.api-key") ?: ""

        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            logger.warning("Trakteer API key is not set in config.yml! Plugin will not function correctly.")
        }

        trakteerClient = TrakteerClient(apiKey)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            val supporterCommand = SupporterCommand(trakteerClient)

            commands.register(
                supporterCommand.build(),
                "View Trakteer supporters"
            )
        }

        server.pluginManager.registerEvents(JoinListener(trakteerClient, logger), this)

        logger.info("MegumiPlugin has been enabled!")
    }

    override fun onDisable() {
        if (::trakteerClient.isInitialized) {
            trakteerClient.close()
        }
        logger.info("MegumiPlugin has been disabled!")
    }
}
