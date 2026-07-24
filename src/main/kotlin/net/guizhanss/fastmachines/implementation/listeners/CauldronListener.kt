package net.guizhanss.fastmachines.implementation.listeners

import me.mrCookieSlime.Slimefun.api.BlockStorage
import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.items.attributes.NotACauldron
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.CauldronLevelChangeEvent

/** Prevents vanilla cauldron filling/emptying from replacing cauldron-based Slimefun machines. */
class CauldronListener(plugin: FastMachines) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCauldronLevelChange(event: CauldronLevelChangeEvent) {
        if (BlockStorage.check(event.block) is NotACauldron) {
            event.isCancelled = true
        }
    }
}
