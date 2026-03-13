package moe.nanakura.megumi.gui

import moe.nanakura.megumi.MegumiPlugin
import moe.nanakura.megumi.managers.GroupManager
import moe.nanakura.megumi.managers.SavePointManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SavePointGUI(
        private val plugin: MegumiPlugin,
        private val savePointManager: SavePointManager,
        private val groupManager: GroupManager
) {

    fun open(player: Player) {
        groupManager
                .getLimits(player)
                .thenCompose { limits ->
                    savePointManager.getSavePoints(player).thenApply { points ->
                        Pair(limits, points)
                    }
                }
                .thenAccept { (limits, savePoints) ->
                    // Switch back to main thread to open inventory
                    Bukkit.getScheduler()
                            .runTask(
                                    plugin,
                                    Runnable {
                                        val maxPoints =
                                                if (limits.maxSavePoints == -1) "∞"
                                                else limits.maxSavePoints.toString()

                                        val title =
                                                MegumiPlugin.translateColorCodes(
                                                        MegumiPlugin.getMessage("gui_title")
                                                                .replace(
                                                                        "{current}",
                                                                        savePoints.size.toString()
                                                                )
                                                                .replace("{max}", maxPoints)
                                                )

                                        val inv = Bukkit.createInventory(null, 27, title)

                                        // Fill background
                                        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
                                        val meta = filler.itemMeta
                                        meta.displayName(Component.empty())
                                        filler.itemMeta = meta
                                        for (i in 0 until 27) {
                                            inv.setItem(i, filler)
                                        }

                                        if (savePoints.isEmpty()) {
                                            val emptyItem = ItemStack(Material.BARRIER)
                                            val emptyMeta = emptyItem.itemMeta
                                            emptyMeta.displayName(
                                                    MegumiPlugin.translateColorCodes(
                                                            MegumiPlugin.getMessage("gui_no_saves")
                                                    )
                                            )
                                            emptyItem.itemMeta = emptyMeta
                                            inv.setItem(13, emptyItem)
                                        } else {
                                            savePoints.forEachIndexed { idx, sp ->
                                                if (idx < 27) {
                                                    val item = ItemStack(Material.COMPASS)
                                                    val spMeta = item.itemMeta
                                                    spMeta.displayName(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&6${sp.name}"
                                                            )
                                                    )
                                                    val lore = mutableListOf<Component>()
                                                    lore.add(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&7World: &e${sp.worldName}"
                                                            )
                                                    )
                                                    lore.add(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&7X: &e${String.format("%.1f", sp.x)}"
                                                            )
                                                    )
                                                    lore.add(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&7Y: &e${String.format("%.1f", sp.y)}"
                                                            )
                                                    )
                                                    lore.add(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&7Z: &e${String.format("%.1f", sp.z)}"
                                                            )
                                                    )
                                                    lore.add(Component.empty())
                                                    lore.add(
                                                            MegumiPlugin.translateColorCodes(
                                                                    "&aClick to teleport!"
                                                            )
                                                    )
                                                    spMeta.lore(lore)
                                                    item.itemMeta = spMeta
                                                    inv.setItem(idx, item)
                                                }
                                            }
                                        }

                                        player.openInventory(inv)
                                    }
                            )
                }
    }
}
