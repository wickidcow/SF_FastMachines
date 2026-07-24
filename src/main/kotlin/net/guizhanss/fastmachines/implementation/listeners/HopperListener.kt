package net.guizhanss.fastmachines.implementation.listeners

import me.mrCookieSlime.Slimefun.api.BlockStorage
import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.items.attributes.NotAHopper
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.InventoryType

class HopperListener(plugin: FastMachines) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onPickupItem(e: InventoryPickupItemEvent) {
        if (e.inventory.type != InventoryType.HOPPER) return

        val loc = e.inventory.location ?: return

        val sfItem = BlockStorage.check(loc)
        if (sfItem is NotAHopper) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onMoveItem(e: InventoryMoveItemEvent) {
        val sourceBlocked = e.source.type == InventoryType.HOPPER &&
            e.source.location?.let { BlockStorage.check(it) is NotAHopper } == true
        val destinationBlocked = e.destination.type == InventoryType.HOPPER &&
            e.destination.location?.let { BlockStorage.check(it) is NotAHopper } == true

        if (sourceBlocked || destinationBlocked) {
            e.isCancelled = true
        }
    }
}
